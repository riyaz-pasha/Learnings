import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Interleaving String
 * Given strings s1, s2, and s3, find whether s3 is formed by an interleaving 
 * of s1 and s2.
 * 
 * An interleaving of two strings s and t is a configuration where s and t 
 * are divided into n and m substrings respectively, and they are woven 
 * together maintaining their original relative characters' order.
 * 
 * Constraints:
 * 0 <= s1.length, s2.length <= 100
 * 0 <= s3.length <= 200
 * s1, s2, and s3 consist of lowercase English letters.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, this problem is a classic string matching DP. 
 * The very first thing you MUST do before writing any complex logic is check 
 * the constraints and validate the physical lengths.
 * 
 * Q: "If the lengths of s1 and s2 do not add up to s3, is it automatically false?"
 * A: Yes! An interleaving consumes exactly all characters of s1 and s2. 
 *    If `s1.length() + s2.length() != s3.length()`, we immediately return false. 
 *    This O(1) check shows pragmatic engineering sense.
 * 
 * CRITICAL SENIOR INSIGHT - THE "TWO POINTER" TRAP:
 * "Many candidates try to solve this greedily using two pointers (one for s1, 
 * one for s2). They iterate through s3 and pick the matching character. 
 * But what happens if BOTH s1 and s2 have the same character that matches s3? 
 * 
 * Example: s1 = 'aab', s2 = 'axy', s3 = 'aaabxy'
 * If you greedily consume the 'a' from s2 first, you might get stuck later. 
 * Because a greedy choice can lead to a dead end while an alternative choice 
 * would have succeeded, we must explore ALL valid branching paths. 
 * Branching with overlapping subproblems = Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We place a pointer `i` on s1, a pointer `j` on s2, and their sum `i + j` 
 * inherently acts as our pointer for s3.
 * 
 * At any state (i, j), we ask: Can the remaining characters of s3 be built?
 * We have two parallel universes:
 * 1. If s1[i] == s3[i+j], we can consume a character from s1 and move to (i+1, j).
 * 2. If s2[j] == s3[i+j], we can consume a character from s2 and move to (i, j+1).
 * 
 * If EITHER of these universes leads to a successful build (returns true), 
 * our current state is true."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: s1 = "ab", s2 = "bc", s3 = "babc"
 * Length check: 2 + 2 == 4. (Pass)
 * 
 * DP Table (Bottom-Up Tabulation):
 * dp[i][j] = Can s1[0..i] and s2[0..j] form s3[0..i+j]?
 * 
 *       s2: ""   'b'    'c'
 * s1: ""  [ T,   T,     F ]   <- (Empty s1. "b" matches s3[0]. "bc" fails s3[0..1] which is "ba")
 *    'a'  [ F,   T,     F ]   <- ('a' fails s3[0] "b". s1="a", s2="b" makes s3="ba" -> True!)
 *    'b'  [ F,   T,     T ]   <- (s1="ab", s2="bc" makes "babc" -> True!)
 */
public class InterleavingString {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Test both possibilities (taking from s1 or taking from s2) whenever 
     * there is a character match.
     * 
     * Time Complexity: O(2^(M+N)) - In the worst case, we branch twice at every step.
     * Space Complexity: O(M+N) - Maximum depth of the recursion tree.
     */
    public boolean isInterleaveRecursive(String s1, String s2, String s3) {
        if (s1.length() + s2.length() != s3.length()) return false;
        return solveRecursive(s1, s2, s3, 0, 0);
    }

    private boolean solveRecursive(String s1, String s2, String s3, int i, int j) {
        // BASE CASE REASONING:
        // If we have successfully navigated through both s1 and s2, and our 
        // pointer indices match their respective lengths, we've perfectly 
        // constructed s3.
        if (i == s1.length() && j == s2.length()) {
            return true;
        }

        // The current character we need to match in s3 is strictly at index i + j.
        int k = i + j;
        boolean canInterleave = false;

        // Universe 1: Does the current character in s1 match?
        if (i < s1.length() && s1.charAt(i) == s3.charAt(k)) {
            canInterleave = solveRecursive(s1, s2, s3, i + 1, j);
        }

        // Universe 2: Does the current character in s2 match?
        // Note: We only evaluate this if Universe 1 hasn't already found a winning path.
        if (!canInterleave && j < s2.length() && s2.charAt(j) == s3.charAt(k)) {
            canInterleave = solveRecursive(s1, s2, s3, i, j + 1);
        }

        return canInterleave;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the boolean result of reaching state `(i, j)`.
     * 
     * Time Complexity: O(M * N) - We evaluate each index pair at most once.
     * Space Complexity: O(M * N) - For the 2D memo array + call stack.
     */
    public boolean isInterleaveMemo(String s1, String s2, String s3) {
        if (s1.length() + s2.length() != s3.length()) return false;
        
        // Use Boolean object so we can use null to represent an uncalculated state
        Boolean[][] memo = new Boolean[s1.length() + 1][s2.length() + 1];
        
        return solveMemo(s1, s2, s3, 0, 0, memo);
    }

    private boolean solveMemo(String s1, String s2, String s3, int i, int j, Boolean[][] memo) {
        if (i == s1.length() && j == s2.length()) return true;

        if (memo[i][j] != null) {
            return memo[i][j];
        }

        int k = i + j;
        boolean isValid = false;

        if (i < s1.length() && s1.charAt(i) == s3.charAt(k)) {
            isValid = solveMemo(s1, s2, s3, i + 1, j, memo);
        }

        if (!isValid && j < s2.length() && s2.charAt(j) == s3.charAt(k)) {
            isValid = solveMemo(s1, s2, s3, i, j + 1, memo);
        }

        memo[i][j] = isValid;
        return isValid;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build an (M+1) x (N+1) grid. dp[i][j] signifies whether the first 
     * 'i' characters of s1 and the first 'j' characters of s2 can successfully 
     * form the first 'i+j' characters of s3.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(M * N)
     */
    public boolean isInterleaveTabulation(String s1, String s2, String s3) {
        int m = s1.length();
        int n = s2.length();
        
        if (m + n != s3.length()) return false;

        boolean[][] dp = new boolean[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                
                // BASE CASE REASONING (0, 0):
                // 0 characters from s1 and 0 characters from s2 perfectly form 
                // 0 characters of s3. This anchors the DP matrix.
                if (i == 0 && j == 0) {
                    dp[i][j] = true;
                } 
                // BASE CASE REASONING (Top Edge - s1 is empty):
                // If we use nothing from s1, we must strictly check if s2 matches s3.
                // It's only valid if the character matches AND the previous prefix was valid.
                else if (i == 0) {
                    dp[i][j] = dp[i][j - 1] && s2.charAt(j - 1) == s3.charAt(i + j - 1);
                } 
                // BASE CASE REASONING (Left Edge - s2 is empty):
                else if (j == 0) {
                    dp[i][j] = dp[i - 1][j] && s1.charAt(i - 1) == s3.charAt(i + j - 1);
                } 
                // --- DETAILED TABULATION EXPLANATION ---
                // For internal cells, we can arrive here by moving DOWN (consuming from s1) 
                // OR moving RIGHT (consuming from s2).
                else {
                    boolean takeS1 = dp[i - 1][j] && s1.charAt(i - 1) == s3.charAt(i + j - 1);
                    boolean takeS2 = dp[i][j - 1] && s2.charAt(j - 1) == s3.charAt(i + j - 1);
                    
                    dp[i][j] = takeS1 || takeS2;
                }
            }
        }

        return dp[m][n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, to calculate row `i`, we ONLY ever look at the cell 
     * directly above it `dp[i-1][j]` (from the previous row) and the cell directly 
     * to its left `dp[i][j-1]` (calculated a moment ago in the current row).
     * 
     * We can collapse the 2D grid into a single 1D array of size `n+1`. 
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(N) - Massively reduced memory footprint.
     */
    public boolean isInterleaveSpaceOptimized(String s1, String s2, String s3) {
        int m = s1.length();
        int n = s2.length();
        
        if (m + n != s3.length()) return false;

        // The 1D array acts as a sliding window representing the columns.
        boolean[] dp = new boolean[n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                
                if (i == 0 && j == 0) {
                    dp[j] = true;
                } 
                else if (i == 0) {
                    // Right side: dp[j-1] represents the cell to the left.
                    dp[j] = dp[j - 1] && s2.charAt(j - 1) == s3.charAt(i + j - 1);
                } 
                else if (j == 0) {
                    // Right side: dp[j] holds the pristine data from the PREVIOUS row (up).
                    dp[j] = dp[j] && s1.charAt(i - 1) == s3.charAt(i + j - 1);
                } 
                else {
                    // MAGIC OF THE 1D ARRAY:
                    // dp[j]   -> Result from taking a char from s1 (coming from the row above)
                    // dp[j-1] -> Result from taking a char from s2 (coming from the cell left)
                    boolean takeS1 = dp[j] && s1.charAt(i - 1) == s3.charAt(i + j - 1);
                    boolean takeS2 = dp[j - 1] && s2.charAt(j - 1) == s3.charAt(i + j - 1);
                    
                    dp[j] = takeS1 || takeS2;
                }
            }
        }

        return dp[n];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new InterleavingString();
        
        record TestCase(String s1, String s2, String s3, boolean expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("aabcc", "dbbca", "aadbbcbcac", true),
            new TestCase("aabcc", "dbbca", "aadbbbaccc", false), // Fails due to sequence mismatch
            new TestCase("", "", "", true),                      // Edge case: all empty strings
            new TestCase("a", "", "c", false),                   // Length mismatch test
            new TestCase("a", "b", "a", false),                  // Length mismatch test
            new TestCase("a", "b", "ab", true)                   // Simplest interleaving
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("s1: \"" + tc.s1 + "\" | s2: \"" + tc.s2 + "\" | s3: \"" + tc.s3 + "\"");
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.isInterleaveRecursive(tc.s1, tc.s2, tc.s3));
            System.out.println("Memoization       : " + solver.isInterleaveMemo(tc.s1, tc.s2, tc.s3));
            System.out.println("Tabulation 2D     : " + solver.isInterleaveTabulation(tc.s1, tc.s2, tc.s3));
            System.out.println("Space Optimized   : " + solver.isInterleaveSpaceOptimized(tc.s1, tc.s2, tc.s3));
            System.out.println();
        }
    }
}
