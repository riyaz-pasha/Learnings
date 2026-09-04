import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Distinct Subsequences
 * Given two strings, s and t, determine how many distinct subsequences of s 
 * match t exactly.
 * 
 * Constraints:
 * 1 <= s.length, t.length <= 1000
 * s and t consist of English letters.
 * The result is guaranteed to fit in a 32-bit signed integer.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, acknowledge the output constraint immediately:
 * 
 * Q: "Can the answer overflow a standard integer?"
 * A: The prompt explicitly guarantees it fits in a 32-bit signed integer. 
 *    Otherwise, for lengths up to 1000, combinatorial explosions would absolutely 
 *    require a `long` or BigInteger. Calling this out shows system-level awareness.
 * 
 * Q: "If t is longer than s, what should I return?"
 * A: 0. It is physically impossible to form a target string out of a source 
 *    string that is strictly shorter than it. We can add this as a rapid 
 *    O(1) short-circuit at the start of our function.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We are looking for combinations. At every character `i` in the source string `s`, 
 * and character `j` in the target string `t`, we compare them:
 * 
 * SCENARIO A: s[i] == t[j]
 * They match! We actually have TWO choices here:
 * 1. USE the match: We consume both characters and ask 'How many ways can I 
 *    form the rest of t using the rest of s?'
 * 2. IGNORE the match: What if there is another matching character later in `s` 
 *    that forms a different distinct subsequence? We ignore this `s[i]` and 
 *    ask 'How many ways can I form this exact same t using the rest of s?'
 * The total ways is the sum of both choices.
 * 
 * SCENARIO B: s[i] != t[j]
 * They don't match. We have no choice but to throw away `s[i]` and keep searching 
 * for `t[j]` in the rest of `s`.
 * 
 * Since multiple paths will inevitably evaluate the same string suffixes 
 * (overlapping subproblems), this requires Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: s = "rabbbit", t = "rabbit"
 * 
 * Notice the three 'b's in s, but we only need two 'b's for t.
 * Let's denote the b's in s as b1, b2, b3.
 * We can form "rabbit" by picking:
 * 1. (b1, b2) -> drops b3
 * 2. (b1, b3) -> drops b2
 * 3. (b2, b3) -> drops b1
 * 
 * Total distinct subsequences = 3.
 */
public class DistinctSubsequences {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Evaluate from the end of both strings backwards, branching when 
     * characters match.
     * 
     * Time Complexity: O(2^m) - In the worst case (e.g., s="aaaaa", t="aaa"), 
     * we branch twice for every single character.
     * Space Complexity: O(m) - Maximum depth of the recursion tree (length of s).
     */
    public int numDistinctRecursive(String s, String t) {
        if (s == null || t == null || s.length() < t.length()) return 0;
        return solveRecursive(s, t, s.length() - 1, t.length() - 1);
    }

    private int solveRecursive(String s, String t, int i, int j) {
        // BASE CASE REASONING (Order matters immensely here!):
        
        // 1. Has the target string been completely matched?
        // If 'j' drops below 0, it means we have successfully matched every single 
        // character in the target string 't'. We have found exactly 1 valid 
        // distinct subsequence. Return 1 to count it.
        if (j < 0) {
            return 1;
        }

        // 2. Has the source string run out BEFORE the target string?
        // If 'i' drops below 0, but 'j' hasn't, it means we have no more letters 
        // left in 's' to supply, but we still need letters to finish 't'. 
        // This path is a failure. Return 0.
        if (i < 0) {
            return 0;
        }

        // If characters match, we sum the two alternate universes:
        if (s.charAt(i) == t.charAt(j)) {
            // Universe 1: Use the match (consume both characters)
            int useMatch = solveRecursive(s, t, i - 1, j - 1);
            
            // Universe 2: Ignore the match (consume only source character)
            int ignoreMatch = solveRecursive(s, t, i - 1, j);
            
            return useMatch + ignoreMatch;
        } 
        
        // If they do not match, we are forced to ignore the source character.
        else {
            return solveRecursive(s, t, i - 1, j);
        }
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the results for specific index pairs [i][j] to avoid 
     * repeatedly computing the same suffix comparisons.
     * 
     * Time Complexity: O(m * n) - Evaluate each pair of indices exactly once.
     * Space Complexity: O(m * n) - For the 2D memo array + call stack.
     */
    public int numDistinctMemo(String s, String t) {
        if (s == null || t == null || s.length() < t.length()) return 0;
        
        int m = s.length();
        int n = t.length();
        int[][] memo = new int[m][n];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(s, t, m - 1, n - 1, memo);
    }

    private int solveMemo(String s, String t, int i, int j, int[][] memo) {
        // BASE CASES (Same physical logic as brute force)
        if (j < 0) return 1;
        if (i < 0) return 0;

        if (memo[i][j] != -1) {
            return memo[i][j];
        }

        if (s.charAt(i) == t.charAt(j)) {
            memo[i][j] = solveMemo(s, t, i - 1, j - 1, memo) + solveMemo(s, t, i - 1, j, memo);
        } else {
            memo[i][j] = solveMemo(s, t, i - 1, j, memo);
        }

        return memo[i][j];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build a 2D spreadsheet. dp[i][j] signifies: "How many ways can I 
     * form the first 'j' characters of 't' using ONLY the first 'i' characters of 's'?"
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n)
     */
    public int numDistinctTabulation(String s, String t) {
        if (s == null || t == null || s.length() < t.length()) return 0;

        int m = s.length();
        int n = t.length();
        int[][] dp = new int[m + 1][n + 1];

        // BASE CASE REASONING:
        // dp[i][0] (Col 0): The target string 't' is length 0 (an empty string).
        // How many ways can you form an empty string out of any string 's'? 
        // Exactly 1 way: by simply deleting every single character in 's'.
        // So, the entire first column becomes 1.
        for (int i = 0; i <= m; i++) {
            dp[i][0] = 1;
        }

        // dp[0][j] (Row 0, where j > 0): The source string 's' is length 0, 
        // but the target 't' requires characters.
        // It is physically impossible to form a word out of nothing.
        // So the rest of the first row remains 0. (Handled automatically by Java).

        // Outer loop: Slowly revealing characters of the source string 's' (i)
        for (int i = 1; i <= m; i++) {
            
            char charS = s.charAt(i - 1);
            
            // Inner loop: Slowly revealing characters of the target string 't' (j)
            for (int j = 1; j <= n; j++) {
                
                char charT = t.charAt(j - 1);

                // PHYSICAL CHECK: Do the two characters match?
                if (charS == charT) {
                    
                    // YES! They match. We combine two alternate realities:
                    
                    // REALITY 1 (Use the Match): 
                    // We lock these two characters together. 
                    // How many ways did we successfully form the target PREVIOUSLY, 
                    // without these two characters? (Look diagonally UP-LEFT).
                    int useMatch = dp[i - 1][j - 1];
                    
                    // REALITY 2 (Ignore the Match):
                    // We pretend 'charS' doesn't exist, hoping another matching character 
                    // exists earlier in 's'. 
                    // How many ways did we already form the exact same target string 
                    // WITHOUT using this current 'charS'? (Look directly UP).
                    int ignoreMatch = dp[i - 1][j];
                    
                    // The total ways is the sum of both valid paths.
                    dp[i][j] = useMatch + ignoreMatch;
                    
                } else {
                    
                    // NO MATCH. We cannot use 'charS' to satisfy 'charT'.
                    // Our ONLY option is to throw away 'charS'.
                    // The number of ways we can form the target is strictly equal to 
                    // however many ways we could form it before we revealed 'charS'.
                    // (Look directly UP).
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        return dp[m][n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, notice that row 'i' only ever looks at row 'i-1' 
     * (directly UP, or UP-LEFT). We don't need a full 2D matrix. We can compress 
     * this into a single 1D array representing the target string 't'.
     * 
     * CRITICAL SENIOR INSIGHT:
     * We MUST traverse the inner loop (j) BACKWARDS. 
     * Why? If `charS == charT`, we need to read `dp[j-1]` (the UP-LEFT value 
     * from the previous row). If we traversed forwards, we would have ALREADY 
     * overwritten `dp[j-1]` with data from the current row. By traversing backwards, 
     * `dp[j-1]` is guaranteed to still hold the pristine data from row 'i-1'.
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(n) - Extremely efficient.
     */
    public int numDistinctSpaceOptimized(String s, String t) {
        if (s == null || t == null || s.length() < t.length()) return 0;

        int m = s.length();
        int n = t.length();
        
        // dp[j] holds the number of ways to form a target prefix of length 'j'
        int[] dp = new int[n + 1];

        // BASE CASE REASONING:
        // Forming an empty string target (length 0) always has exactly 1 way.
        dp[0] = 1;

        for (int i = 1; i <= m; i++) {
            
            char charS = s.charAt(i - 1);
            
            // Traverse BACKWARDS to avoid overwriting values we need for diagonal lookups!
            for (int j = n; j >= 1; j--) {
                
                char charT = t.charAt(j - 1);

                // If they match, we add the UP-LEFT value (dp[j - 1]) to the 
                // existing UP value (dp[j]).
                if (charS == charT) {
                    dp[j] = dp[j] + dp[j - 1];
                }
                
                // If they DON'T match, we do nothing. The array implicitly keeps 
                // the UP value (dp[j] = dp[j]), which is exactly what we want.
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
        var solver = new DistinctSubsequences();
        
        record TestCase(String s, String t, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("rabbbit", "rabbit", 3), // Dropping one of the 3 'b's
            new TestCase("babgbag", "bag", 5),    // Converging subpaths
            new TestCase("abc", "def", 0),        // Impossible
            new TestCase("a", "a", 1),            // Exact match
            new TestCase("aaaaa", "a", 5)         // Pick any 1 of 5
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Source (s): " + tc.s);
            System.out.println("Target (t): " + tc.t);
            System.out.println("Expected  : " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.numDistinctRecursive(tc.s, tc.t));
            System.out.println("Memoization       : " + solver.numDistinctMemo(tc.s, tc.t));
            System.out.println("Tabulation 2D     : " + solver.numDistinctTabulation(tc.s, tc.t));
            System.out.println("Space Optimized   : " + solver.numDistinctSpaceOptimized(tc.s, tc.t));
            System.out.println();
        }
    }
}

