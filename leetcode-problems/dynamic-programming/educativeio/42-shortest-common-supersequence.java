import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Shortest Common Supersequence (SCS)
 * Given two strings str1 and str2, return the shortest string that has both 
 * str1 and str2 as subsequences. If there are multiple valid strings, 
 * return any of them.
 * 
 * Constraints:
 * 1 <= str1.length, str2.length <= 1000
 * str1 and str2 consist of lowercase English letters.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, your first goal is to conceptually link this problem 
 * to a known foundational algorithm.
 * 
 * Q: "What is the mathematical relationship between the lengths of the two strings, 
 *     their Longest Common Subsequence (LCS), and the Shortest Common Supersequence?"
 * A: This is the critical insight. 
 *    Length(SCS) = Length(str1) + Length(str2) - Length(LCS(str1, str2)).
 *    Because the LCS represents the characters that both strings naturally share 
 *    in the same relative order, we only need to write those shared characters 
 *    ONCE. We then just strategically "weave" the remaining unshared characters 
 *    from both strings around that shared backbone.
 * 
 * CRITICAL SENIOR INSIGHT - PATH RECONSTRUCTION:
 * "In standard DP problems, we just want a number (e.g., 'What is the max area?'). 
 * Here, we need to return the ACTUAL string. 
 * This means we cannot just run a DP loop and throw away the history. We must 
 * keep the entire 2D DP matrix in memory so we can backtrack through it, using 
 * the numeric gradients to retrace exactly which characters formed the optimal path."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "Step 1: We will build a standard 2D Tabulation table for the Longest Common 
 *          Subsequence (LCS) of str1 and str2.
 * 
 * Step 2: We will place a pointer at the bottom-right of the DP table (representing 
 *          the end of both strings) and backtrack to the top-left (0, 0).
 * 
 * BACKTRACKING LOGIC:
 * - If the current characters in str1 and str2 MATCH, they are part of the LCS. 
 *   We include the character exactly once and move diagonally (up-left).
 * - If they DO NOT MATCH, we look at the DP table to see which direction gave 
 *   us a higher LCS value (Up or Left). 
 *   - If moving UP is better, it means the current character of str1 is NOT in 
 *     the LCS, so we include it in our supersequence and move Up.
 *   - If moving LEFT is better, we include the current character of str2 and move Left.
 * - Finally, if we hit the edge of one string, we just dump the remaining 
 *   characters of the other string into our result."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: str1 = "abac", str2 = "cab"
 * 
 * LCS Table (Length):
 *      ""  c  a  b
 * "" [ 0, 0, 0, 0 ]
 * a  [ 0, 0, 1, 1 ]
 * b  [ 0, 0, 1, 2 ]
 * a  [ 0, 0, 1, 2 ]
 * c  [ 0, 1, 1, 2 ]
 * 
 * Backtracking from bottom-right (4, 3):
 * 1. 'c' != 'b'. DP[3][3] (2) > DP[4][2] (1). Go UP. Append 'c'. 
 * 2. 'a' != 'b'. DP[2][3] (2) > DP[3][2] (1). Go UP. Append 'a'.
 * 3. 'b' == 'b'. Match! Go DIAGONAL. Append 'b'.
 * 4. 'a' != 'a' (Wait, index mismatch). DP[1][2] vs DP[2][1]. 
 * ... Tracing up to (0,0), we append the characters in REVERSE order.
 * Reversed Path gives: "c a b a c".
 */
public class ShortestCommonSupersequence {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force - Conceptual)
     * ========================================================================
     * Idea: Recursively build the string by trying to match characters. If they 
     * don't match, branch out (include str1's char vs include str2's char) and 
     * return the shorter resulting string.
     * 
     * Time Complexity: O(2^(M+N)) - Exponential branching.
     * Space Complexity: O(M+N) - Recursion depth, plus massive overhead for 
     * String creation at every return step.
     */
    public String shortestCommonSupersequenceBrute(String str1, String str2) {
        return solveRecursive(str1, str2, 0, 0);
    }

    private String solveRecursive(String s1, String s2, int i, int j) {
        if (i == s1.length()) return s2.substring(j);
        if (j == s2.length()) return s1.substring(i);

        if (s1.charAt(i) == s2.charAt(j)) {
            return s1.charAt(i) + solveRecursive(s1, s2, i + 1, j + 1);
        }

        String option1 = s1.charAt(i) + solveRecursive(s1, s2, i + 1, j);
        String option2 = s2.charAt(j) + solveRecursive(s1, s2, i, j + 1);

        return option1.length() < option2.length() ? option1 : option2;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the strings generated by the recursive calls. 
     * 
     * CRITICAL WARNING: Caching full Strings in a 2D matrix of size 1000x1000 
     * will instantly cause Memory Limit Exceeded (MLE) in Java due to string pool 
     * and object overhead. We would need to memoize the LENGTHS and then backtrack, 
     * which naturally leads us directly to Tabulation.
     */

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up DP (LCS Tabulation) + Backtracking (Optimal)
     * ========================================================================
     * Idea: Generate the numeric LCS table. Then use a while loop to backtrack 
     * from the bottom-right corner to assemble the final string efficiently 
     * using a StringBuilder.
     * 
     * Time Complexity: O(M * N) to build the table, O(M + N) to backtrack.
     * Space Complexity: O(M * N) for the DP table.
     */
    public String shortestCommonSupersequenceOptimal(String str1, String str2) {
        int m = str1.length();
        int n = str2.length();

        // Step 1: Build the DP table for the Longest Common Subsequence (LCS)
        int[][] dp = new int[m + 1][n + 1];

        // Standard LCS logic
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    // Match found: Diagonal + 1
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    // Mismatch: Take the max of UP or LEFT
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Step 2: Backtrack to build the Supersequence
        StringBuilder sb = new StringBuilder();
        int i = m;
        int j = n;

        while (i > 0 && j > 0) {
            
            // If the characters match, they are part of the LCS backbone.
            // We write the character ONCE, and move diagonally.
            if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                sb.append(str1.charAt(i - 1));
                i--;
                j--;
            } 
            // If they don't match, we check which way the DP gradient flows.
            // We want to move towards the larger DP value.
            else if (dp[i - 1][j] > dp[i][j - 1]) {
                // Moving UP in the table corresponds to dropping a char from str1.
                // This means str1's char is unique to str1, so we must add it.
                sb.append(str1.charAt(i - 1));
                i--;
            } else {
                // Moving LEFT corresponds to dropping a char from str2.
                sb.append(str2.charAt(j - 1));
                j--;
            }
        }

        // Step 3: Clean up remaining characters (Edge cases where we hit the 
        // boundary of one string before the other).
        while (i > 0) {
            sb.append(str1.charAt(i - 1));
            i--;
        }
        while (j > 0) {
            sb.append(str2.charAt(j - 1));
            j--;
        }

        // Because we backtracked from the end to the start, the string is built backwards.
        return sb.reverse().toString();
    }

    /**
     * ========================================================================
     * APPROACH 4: Note on Space Optimization Limitation (L4/L5 Expectation)
     * ========================================================================
     * In standard LCS problems (where we only want the integer length of the LCS), 
     * we optimize the 2D DP array down to a 1D array of size N, reducing space 
     * from O(M * N) to O(N).
     * 
     * CRITICAL INSIGHT:
     * We **CANNOT** use 1D Space Optimization here! 
     * 
     * To reconstruct the actual string, we MUST trace our exact footsteps backward 
     * through the matrix (Step 2 above). If we overwrite the array row-by-row, 
     * we permanently destroy the topological history needed for the backtracking 
     * gradient. 
     * 
     * Explicitly stating that "Space optimization must be sacrificed to enable 
     * path reconstruction" is exactly what differentiates a senior engineer who 
     * understands the *tradeoffs* of DP from someone who just memorized the code.
     */

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new ShortestCommonSupersequence();
        
        record TestCase(String str1, String str2, String validOutputExample) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("abac", "cab", "cabac"),
            new TestCase("aaaaaaaa", "aaaaaaaa", "aaaaaaaa"), // Identical strings
            new TestCase("abc", "def", "abcdef"),             // Completely disjoint strings
            new TestCase("bbbaaaba", "bbababbb", "bbbaaababbb")
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("String 1: " + tc.str1);
            System.out.println("String 2: " + tc.str2);
            
            // Execute the optimal algorithm
            String result = solver.shortestCommonSupersequenceOptimal(tc.str1, tc.str2);
            
            System.out.println("Generated SCS : " + result);
            System.out.println("Valid length? : " + 
                (result.length() == tc.str1.length() + tc.str2.length() - lcsLength(tc.str1, tc.str2))
            );
            System.out.println();
        }
    }
    
    // Helper to verify mathematically if the output length is correct during testing
    private static int lcsLength(String s1, String s2) {
        int[] dp = new int[s2.length() + 1];
        for (int i = 1; i <= s1.length(); i++) {
            int prev = 0;
            for (int j = 1; j <= s2.length(); j++) {
                int temp = dp[j];
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[j] = prev + 1;
                } else {
                    dp[j] = Math.max(dp[j], dp[j - 1]);
                }
                prev = temp;
            }
        }
        return dp[s2.length()];
    }
}