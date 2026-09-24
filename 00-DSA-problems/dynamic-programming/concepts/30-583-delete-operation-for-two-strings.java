
import java.util.HashMap;
import java.util.Map;

/*
 * PROBLEM: Minimum Number of Steps to Make Two Strings the Same
 * 
 * INTERVIEW APPROACH - How to Think Through This Problem:
 * ========================================================
 * 
 * 1. UNDERSTAND THE PROBLEM:
 *    - We can only DELETE characters (not insert or replace)
 *    - Goal: Make both strings identical
 *    - Question: What does "identical" mean here?
 *      Answer: Both strings should have the same characters in the same order
 * 
 * 2. KEY INSIGHT (The "Aha!" Moment):
 *    - If we delete characters to make strings equal, the remaining characters 
 *      form a COMMON SUBSEQUENCE
 *    - To minimize deletions, we want to MAXIMIZE what we keep
 *    - What we keep = Longest Common Subsequence (LCS)
 *    - Deletions needed = (length of word1 - LCS) + (length of word2 - LCS)
 * 
 * 3. EXAMPLE WALKTHROUGH:
 *    word1 = "sea", word2 = "eat"
 *    - LCS = "ea" (length 2)
 *    - Delete from word1: 's' (1 deletion)
 *    - Delete from word2: 't' (1 deletion)
 *    - Total: 2 deletions
 * 
 * 4. PATTERN RECOGNITION:
 *    This is a VARIANT of the classic LCS problem
 *    Once you recognize this, the solution becomes clear
 * 
 * 5. IMPLEMENTATION STRATEGIES:
 *    - Approach 1: Find LCS length, then calculate deletions
 *    - Approach 2: Direct DP tracking deletion count
 *    - Both have same time complexity, but Approach 2 is more direct
 */

class MinimumDeleteSteps {
    
    /*
     * APPROACH 1: Using Longest Common Subsequence (LCS)
     * ===================================================
     * 
     * INTUITION:
     * - The characters we DON'T delete form the LCS
     * - To minimize deletions, maximize what we keep (find longest LCS)
     * - Answer = total characters - 2 * LCS_length
     * 
     * WHY THIS WORKS:
     * - LCS gives us the maximum characters we can keep in both strings
     * - Everything else must be deleted
     * - From word1: delete (m - LCS) characters
     * - From word2: delete (n - LCS) characters
     * - Total = m + n - 2*LCS
     * 
     * TIME COMPLEXITY: O(m * n) where m, n are string lengths
     * SPACE COMPLEXITY: O(m * n) for the DP table
     */
    public int minDistance_LCS(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        
        // Find the length of Longest Common Subsequence
        int lcsLength = longestCommonSubsequence(word1, word2);
        
        // Total deletions = characters not in LCS from both strings
        return m + n - 2 * lcsLength;
    }
    
    /*
     * CLASSIC LCS IMPLEMENTATION
     * ==========================
     * 
     * DP STATE DEFINITION:
     * dp[i][j] = length of LCS of word1[0...i-1] and word2[0...j-1]
     * 
     * BASE CASE:
     * dp[0][j] = 0 (empty word1, no common subsequence)
     * dp[i][0] = 0 (empty word2, no common subsequence)
     * 
     * RECURRENCE RELATION:
     * if word1[i-1] == word2[j-1]:
     *     dp[i][j] = dp[i-1][j-1] + 1  // Characters match, extend LCS
     * else:
     *     dp[i][j] = max(dp[i-1][j], dp[i][j-1])  // Take best from either side
     * 
     * VISUALIZATION (word1="sea", word2="eat"):
     *     ""  e  a  t
     * ""   0  0  0  0
     * s    0  0  0  0
     * e    0  1  1  1
     * a    0  1  2  2
     * 
     * LCS length = 2 (for "ea")
     */
    private int longestCommonSubsequence(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        // Fill the DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    // Characters match: extend previous LCS
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    // Characters don't match: take maximum from excluding either character
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /*
     * APPROACH 2: Direct DP (More Intuitive for This Specific Problem)
     * =================================================================
     * 
     * INTUITION:
     * Instead of finding LCS then calculating deletions,
     * directly count minimum deletions needed
     * 
     * DP STATE DEFINITION:
     * dp[i][j] = minimum deletions to make word1[0...i-1] and word2[0...j-1] equal
     * 
     * BASE CASES:
     * dp[i][0] = i  // Delete all i characters from word1
     * dp[0][j] = j  // Delete all j characters from word2
     * 
     * RECURRENCE RELATION:
     * if word1[i-1] == word2[j-1]:
     *     dp[i][j] = dp[i-1][j-1]  // No deletion needed, characters match
     * else:
     *     dp[i][j] = 1 + min(dp[i-1][j], dp[i][j-1])
     *     // Either delete from word1 (dp[i-1][j]) or word2 (dp[i][j-1])
     * 
     * WHY THIS RECURRENCE WORKS:
     * - If characters match: keep both, no deletion
     * - If they don't match: we must delete one
     *   - Option 1: Delete word1[i-1], solve for word1[0...i-2] vs word2[0...j-1]
     *   - Option 2: Delete word2[j-1], solve for word1[0...i-1] vs word2[0...j-2]
     *   - Take minimum
     * 
     * TIME COMPLEXITY: O(m * n)
     * SPACE COMPLEXITY: O(m * n)
     */
    public int minDistance_DirectDP(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        
        // dp[i][j] = min deletions to make word1[0..i-1] equal to word2[0..j-1]
        int[][] dp = new int[m + 1][n + 1];
        
        // Base cases: when one string is empty
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i; // Delete all characters from word1
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j; // Delete all characters from word2
        }
        
        // Fill the DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    // Characters match - no deletion needed
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    // Characters don't match - delete from either string
                    // Take minimum of:
                    // 1. Delete from word1: 1 + dp[i-1][j]
                    // 2. Delete from word2: 1 + dp[i][j-1]
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /*
     * APPROACH 3: Space Optimized DP
     * ===============================
     * 
     * OBSERVATION:
     * We only need the previous row to compute the current row
     * 
     * SPACE OPTIMIZATION:
     * - Use two 1D arrays instead of 2D array
     * - prev[] = previous row, curr[] = current row
     * - Alternate between them
     * 
     * TIME COMPLEXITY: O(m * n)
     * SPACE COMPLEXITY: O(n) - only store two rows
     * 
     * WHEN TO USE THIS IN INTERVIEWS:
     * - Mention this optimization after explaining the 2D approach
     * - Shows deeper understanding of DP patterns
     * - Only implement if time permits or interviewer asks
     */
    public int minDistance_SpaceOptimized(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        
        // Only keep track of previous and current row
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        
        // Base case: first row (empty word1)
        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            // First column: delete all i characters from word1
            curr[0] = i;
            
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    curr[j] = prev[j - 1];
                } else {
                    curr[j] = 1 + Math.min(prev[j], curr[j - 1]);
                }
            }
            
            // Swap arrays for next iteration
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return prev[n];
    }
    
    /*
     * APPROACH 4: Memoization (Top-Down DP)
     * ======================================
     * 
     * WHEN TO USE:
     * - If you think of recursion first in the interview
     * - More intuitive for some people
     * - Can be easier to explain the recurrence
     * 
     * PROS:
     * - Natural recursive thinking
     * - Only computes needed states
     * 
     * CONS:
     * - Recursion overhead
     * - Stack space usage
     * 
     * TIME COMPLEXITY: O(m * n)
     * SPACE COMPLEXITY: O(m * n) for memo + O(m + n) for recursion stack
     */
    public int minDistance_Memoization(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        Integer[][] memo = new Integer[m][n];
        return helper(word1, word2, 0, 0, memo);
    }
    
    /*
     * RECURSIVE HELPER WITH MEMOIZATION
     * 
     * PARAMETERS:
     * i = current index in word1
     * j = current index in word2
     * 
     * RETURNS:
     * Minimum deletions to make word1[i..] equal to word2[j..]
     * 
     * BASE CASES:
     * - If i reaches end: delete all remaining in word2
     * - If j reaches end: delete all remaining in word1
     * 
     * RECURSIVE CASES:
     * - If chars match: no deletion, move both pointers
     * - If chars don't match: try deleting from either, take minimum
     */
    private int helper(String word1, String word2, int i, int j, Integer[][] memo) {
        // Base case: reached end of word1, delete all remaining in word2
        if (i == word1.length()) {
            return word2.length() - j;
        }
        
        // Base case: reached end of word2, delete all remaining in word1
        if (j == word2.length()) {
            return word1.length() - i;
        }
        
        // Check memo
        if (memo[i][j] != null) {
            return memo[i][j];
        }
        
        int result;
        if (word1.charAt(i) == word2.charAt(j)) {
            // Characters match - no deletion, move both pointers
            result = helper(word1, word2, i + 1, j + 1, memo);
        } else {
            // Characters don't match - try deleting from either
            int deleteFromWord1 = 1 + helper(word1, word2, i + 1, j, memo);
            int deleteFromWord2 = 1 + helper(word1, word2, i, j + 1, memo);
            result = Math.min(deleteFromWord1, deleteFromWord2);
        }
        
        memo[i][j] = result;
        return result;
    }
    
    /*
     * INTERVIEW STRATEGY & TIPS:
     * ==========================
     * 
     * 1. CLARIFY THE PROBLEM (30 seconds):
     *    - "So we can only delete, not insert or replace?"
     *    - "The goal is to make both strings identical?"
     *    - Ask about constraints (string length, character set)
     * 
     * 2. WORK THROUGH EXAMPLES (1-2 minutes):
     *    - Use the given examples
     *    - Try a simple case like "abc" and "abc" (answer: 0)
     *    - Try "abc" and "def" (answer: 6, delete everything)
     * 
     * 3. IDENTIFY THE PATTERN (2 minutes):
     *    - "This feels like an edit distance problem"
     *    - "The key insight is that we want to maximize what we keep"
     *    - "What we keep must be a common subsequence"
     *    - "To minimize deletions, we need the LONGEST common subsequence"
     * 
     * 4. PROPOSE APPROACH (1 minute):
     *    - "I can solve this using dynamic programming"
     *    - "Either find LCS first, or directly compute min deletions"
     *    - "I'll use the direct DP approach as it's more intuitive"
     * 
     * 5. EXPLAIN RECURRENCE (2 minutes):
     *    - Draw a small DP table
     *    - Explain base cases
     *    - Explain the recurrence relation
     * 
     * 6. CODE (5-7 minutes):
     *    - Start with the cleaner bottom-up DP
     *    - Add comments as you code
     * 
     * 7. TEST (2 minutes):
     *    - Walk through one example with your code
     *    - Check edge cases (empty strings, identical strings)
     * 
     * 8. DISCUSS OPTIMIZATION (if time):
     *    - Mention space optimization to O(n)
     *    - Discuss trade-offs
     * 
     * COMMON MISTAKES TO AVOID:
     * - Confusing this with edit distance (which allows insert/replace)
     * - Forgetting base cases
     * - Off-by-one errors in array indexing
     * - Not checking if characters match before applying recurrence
     */
    
    // Test cases
    public static void main(String[] args) {
        MinimumDeleteSteps solution = new MinimumDeleteSteps();
        
        // Test Case 1
        String word1 = "sea";
        String word2 = "eat";
        System.out.println("Test Case 1: word1 = \"" + word1 + "\", word2 = \"" + word2 + "\"");
        System.out.println("Expected: 2");
        System.out.println("LCS Approach: " + solution.minDistance_LCS(word1, word2));
        System.out.println("Direct DP: " + solution.minDistance_DirectDP(word1, word2));
        System.out.println("Space Optimized: " + solution.minDistance_SpaceOptimized(word1, word2));
        System.out.println("Memoization: " + solution.minDistance_Memoization(word1, word2));
        System.out.println();
        
        // Test Case 2
        word1 = "leetcode";
        word2 = "etco";
        System.out.println("Test Case 2: word1 = \"" + word1 + "\", word2 = \"" + word2 + "\"");
        System.out.println("Expected: 4");
        System.out.println("LCS Approach: " + solution.minDistance_LCS(word1, word2));
        System.out.println("Direct DP: " + solution.minDistance_DirectDP(word1, word2));
        System.out.println("Space Optimized: " + solution.minDistance_SpaceOptimized(word1, word2));
        System.out.println("Memoization: " + solution.minDistance_Memoization(word1, word2));
        System.out.println();
        
        // Edge Case: Empty strings
        word1 = "";
        word2 = "abc";
        System.out.println("Edge Case 1: word1 = \"\", word2 = \"abc\"");
        System.out.println("Expected: 3");
        System.out.println("Direct DP: " + solution.minDistance_DirectDP(word1, word2));
        System.out.println();
        
        // Edge Case: Identical strings
        word1 = "same";
        word2 = "same";
        System.out.println("Edge Case 2: word1 = \"same\", word2 = \"same\"");
        System.out.println("Expected: 0");
        System.out.println("Direct DP: " + solution.minDistance_DirectDP(word1, word2));
        System.out.println();
        
        // Edge Case: No common characters
        word1 = "abc";
        word2 = "def";
        System.out.println("Edge Case 3: word1 = \"abc\", word2 = \"def\"");
        System.out.println("Expected: 6");
        System.out.println("Direct DP: " + solution.minDistance_DirectDP(word1, word2));
    }
}


class DeleteOperationDirect {

    private static Map<String, Integer> memo = new HashMap<>();

    public static int minDistance(String word1, String word2) {
        memo.clear();
        return helper(word1, word2, word1.length(), word2.length());
    }

    private static int helper(String s1, String s2, int i, int j) {

        // If one string is empty, delete all chars from the other
        if (i == 0) return j;
        if (j == 0) return i;

        String key = i + "|" + j;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }

        int result;

        // If last characters match → no deletion needed here
        if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
            result = helper(s1, s2, i - 1, j - 1);
        } 
        // Else → delete from either s1 or s2
        else {
            result = 1 + Math.min(
                    helper(s1, s2, i - 1, j),  // delete from s1
                    helper(s1, s2, i, j - 1)   // delete from s2
            );
        }

        memo.put(key, result);
        return result;
    }

    // Test
    public static void main(String[] args) {
        System.out.println(minDistance("sea", "eat"));        // 2
        System.out.println(minDistance("leetcode", "etco")); // 4
    }
}
