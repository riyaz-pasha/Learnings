
import java.util.HashMap;
import java.util.Map;

/*
 * PROBLEM: Distinct Subsequences
 * 
 * INTERVIEW APPROACH - How to Think Through This Problem:
 * ========================================================
 * 
 * 1. UNDERSTAND THE PROBLEM:
 *    - Given: source string 's' and target string 't'
 *    - Find: How many DIFFERENT ways can we form 't' as a subsequence of 's'
 *    - Key: We're COUNTING, not just finding if it exists
 * 
 * 2. PATTERN RECOGNITION:
 *    This is a COUNTING DP problem, related to LCS but different:
 *    - LCS: Find longest common subsequence
 *    - Previous problems: Find length or construct the sequence
 *    - THIS problem: COUNT how many ways to form a specific subsequence
 * 
 * 3. THE KEY INSIGHT (Critical for solving):
 *    
 *    When we're at position i in 's' and position j in 't':
 *    
 *    If s[i] == t[j]:
 *       We have TWO choices:
 *       a) USE s[i] to match t[j]: count = dp[i-1][j-1]
 *       b) SKIP s[i], don't use it: count = dp[i-1][j]
 *       Total ways = dp[i-1][j-1] + dp[i-1][j]
 *    
 *    If s[i] != t[j]:
 *       We have NO choice but to SKIP s[i]
 *       Total ways = dp[i-1][j]
 * 
 * 4. WHY THIS RECURRENCE WORKS:
 *    
 *    Example: s = "rabbbit", t = "rabbit"
 *    When we see 'b' in s that could match 'b' in t:
 *    - Option 1: Use this 'b' → count subsequences ending here
 *    - Option 2: Don't use this 'b' → count from previous positions
 *    - Both are VALID and DISTINCT ways, so we ADD them
 * 
 * 5. VISUAL EXAMPLE: s = "rabb", t = "rab"
 *    
 *    We want to count ways to form "rab" from "rabb"
 *    
 *    s[0]='r' matches t[0]='r': Use it, now count ways for "" → "ab"
 *    s[1]='a' matches t[1]='a': Use it, now count ways for "r" → "ab"
 *    s[2]='b' matches t[2]='b': Use it OR skip it
 *    s[3]='b' matches t[2]='b': Use it OR skip it
 *    
 *    This gives us multiple distinct ways!
 * 
 * 6. BASE CASES (Very Important):
 *    dp[i][0] = 1 for all i
 *    → There's exactly ONE way to form empty string: select nothing
 *    
 *    dp[0][j] = 0 for j > 0
 *    → There's NO way to form non-empty target from empty source
 */

class DistinctSubsequences {
    
    /*
     * APPROACH 1: 2D Dynamic Programming (Most Intuitive)
     * ====================================================
     * 
     * DP STATE DEFINITION:
     * dp[i][j] = number of distinct subsequences of s[0...i-1] that equals t[0...j-1]
     * 
     * In other words:
     * "How many ways can we form the first j characters of t 
     *  using the first i characters of s?"
     * 
     * BASE CASES:
     * dp[i][0] = 1  (empty target can be formed in 1 way: select nothing)
     * dp[0][j] = 0  (non-empty target can't be formed from empty source)
     * 
     * RECURRENCE RELATION:
     * if s[i-1] == t[j-1]:
     *     dp[i][j] = dp[i-1][j-1] + dp[i-1][j]
     *     ↑          ↑              ↑
     *     |          |              |__ Don't use s[i-1]
     *     |          |_________________ Use s[i-1] to match t[j-1]
     *     |____________________________ Total distinct ways
     * 
     * else:
     *     dp[i][j] = dp[i-1][j]
     *     (Can't use s[i-1], so skip it)
     * 
     * TIME COMPLEXITY: O(m * n) where m = len(s), n = len(t)
     * SPACE COMPLEXITY: O(m * n)
     */
    public int numDistinct(String s, String t) {
        int m = s.length();
        int n = t.length();
        
        // Early termination: if s is shorter than t, impossible
        if (m < n) return 0;
        
        // dp[i][j] = number of ways to form t[0..j-1] from s[0..i-1]
        long[][] dp = new long[m + 1][n + 1];
        
        // Base case: empty target can be formed in exactly 1 way
        for (int i = 0; i <= m; i++) {
            dp[i][0] = 1;
        }
        
        // Base case: non-empty target from empty source = 0 ways
        // (already initialized to 0)
        
        // Fill the DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                // Always have option to skip current character in s
                dp[i][j] = dp[i - 1][j];
                
                // If characters match, we can also use current character
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    dp[i][j] += dp[i - 1][j - 1];
                }
            }
        }
        
        return (int) dp[m][n];
    }
    
    /*
     * DETAILED WALKTHROUGH: s = "rabbbit", t = "rabbit"
     * ==================================================
     * 
     * Building the DP table:
     * 
     *         ""  r  a  b  b  i  t
     *     ""   1  0  0  0  0  0  0
     *     r    1  1  0  0  0  0  0
     *     a    1  1  1  0  0  0  0
     *     b    1  1  1  1  0  0  0
     *     b    1  1  1  2  1  0  0
     *     b    1  1  1  3  3  0  0
     *     i    1  1  1  3  3  3  0
     *     t    1  1  1  3  3  3  3
     * 
     * Key observations at dp[4][3] (s="rabb", t="rab"):
     * - s[3]='b' matches t[2]='b'
     * - Can USE this 'b': dp[3][2] = 1 way
     * - Can SKIP this 'b': dp[3][3] = 1 way
     * - Total: 1 + 1 = 2 ways
     * 
     * At dp[5][3] (s="rabbb", t="rab"):
     * - s[4]='b' matches t[2]='b'
     * - Can USE: dp[4][2] = 1
     * - Can SKIP: dp[4][3] = 2
     * - Total: 1 + 2 = 3 ways ← This is our answer!
     * 
     * The 3 ways are:
     * 1. r a b b b i t  (use 1st 'b')
     * 2. r a b b b i t  (use 2nd 'b')
     * 3. r a b b b i t  (use 3rd 'b')
     */
    
    /*
     * APPROACH 2: Space-Optimized 1D DP
     * ==================================
     * 
     * OPTIMIZATION INSIGHT:
     * We only need the previous row to compute the current row
     * → Use just two 1D arrays (or even one with careful updating)
     * 
     * KEY TRICK:
     * Iterate j from RIGHT to LEFT to avoid overwriting values we still need
     * 
     * WHY RIGHT TO LEFT?
     * When computing dp[j], we need:
     * - dp[j] (previous row, same column)
     * - dp[j-1] (previous row, previous column)
     * 
     * If we go left→right, we'd overwrite dp[j-1] before using it
     * If we go right→left, dp[j-1] still has the old (previous row) value
     * 
     * TIME COMPLEXITY: O(m * n)
     * SPACE COMPLEXITY: O(n) - single array!
     */
    public int numDistinct_SpaceOptimized(String s, String t) {
        int m = s.length();
        int n = t.length();
        
        if (m < n) return 0;
        
        // Only need one array!
        long[] dp = new long[n + 1];
        dp[0] = 1; // Base case: empty target
        
        for (int i = 1; i <= m; i++) {
            // Iterate from right to left to avoid overwriting
            for (int j = n; j >= 1; j--) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    dp[j] += dp[j - 1];
                }
                // If no match, dp[j] stays the same (skip s[i-1])
            }
        }
        
        return (int) dp[n];
    }
    
    /*
     * APPROACH 3: Memoization (Top-Down DP)
     * ======================================
     * 
     * WHEN TO USE:
     * - If recursion comes naturally to you
     * - Good for explaining the logic in interviews
     * 
     * RECURSIVE DEFINITION:
     * count(i, j) = number of ways to form t[j..] from s[i..]
     * 
     * BASE CASES:
     * - If j == t.length(): formed entire target, return 1
     * - If i == s.length(): ran out of source chars, return 0
     * 
     * RECURSIVE CASES:
     * - If s[i] == t[j]: count(i+1, j+1) + count(i+1, j)
     * - Else: count(i+1, j)
     * 
     * TIME COMPLEXITY: O(m * n) with memoization
     * SPACE COMPLEXITY: O(m * n) for memo + O(m + n) recursion stack
     */
    public int numDistinct_Memoization(String s, String t) {
        Long[][] memo = new Long[s.length()][t.length()];
        return (int) helper(s, t, 0, 0, memo);
    }
    
    private long helper(String s, String t, int i, int j, Long[][] memo) {
        // Base case: formed entire target
        if (j == t.length()) {
            return 1;
        }
        
        // Base case: ran out of source characters
        if (i == s.length()) {
            return 0;
        }
        
        // Check memo
        if (memo[i][j] != null) {
            return memo[i][j];
        }
        
        long count = 0;
        
        if (s.charAt(i) == t.charAt(j)) {
            // Option 1: Use s[i] to match t[j]
            count += helper(s, t, i + 1, j + 1, memo);
            // Option 2: Skip s[i]
            count += helper(s, t, i + 1, j, memo);
        } else {
            // Can only skip s[i]
            count = helper(s, t, i + 1, j, memo);
        }
        
        memo[i][j] = count;
        return count;
    }
    
    /*
     * APPROACH 4: With Detailed Debugging/Visualization
     * ==================================================
     * 
     * This version prints the DP table for educational purposes
     * Useful for understanding how the algorithm works
     */
    public int numDistinct_WithVisualization(String s, String t) {
        int m = s.length();
        int n = t.length();
        
        if (m < n) return 0;
        
        long[][] dp = new long[m + 1][n + 1];
        
        // Base case
        for (int i = 0; i <= m; i++) {
            dp[i][0] = 1;
        }
        
        // Fill DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = dp[i - 1][j];
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    dp[i][j] += dp[i - 1][j - 1];
                }
            }
        }
        
        // Print the table
        System.out.println("\nDP Table for s=\"" + s + "\", t=\"" + t + "\":");
        System.out.print("       \"\"");
        for (char c : t.toCharArray()) {
            System.out.printf("%4c", c);
        }
        System.out.println();
        
        System.out.print("  \"\"");
        for (int j = 0; j <= n; j++) {
            System.out.printf("%4d", dp[0][j]);
        }
        System.out.println();
        
        for (int i = 1; i <= m; i++) {
            System.out.printf("%4c", s.charAt(i - 1));
            for (int j = 0; j <= n; j++) {
                System.out.printf("%4d", dp[i][j]);
            }
            System.out.println();
        }
        System.out.println();
        
        return (int) dp[m][n];
    }
    
    /*
     * INTERVIEW STRATEGY:
     * ===================
     * 
     * 1. CLARIFY THE PROBLEM (1 minute):
     *    "So we're counting DISTINCT ways to form t as a subsequence of s?"
     *    "Multiple uses of the same characters in s count as different ways?"
     *    "Should I worry about integer overflow?" (problem says it fits in 32-bit)
     * 
     * 2. WORK THROUGH EXAMPLE (2 minutes):
     *    s = "rabb", t = "rab"
     *    Walk through manually:
     *    - Way 1: r a b _
     *    - Way 2: r a _ b
     *    "So the answer is 2"
     * 
     * 3. IDENTIFY THE PATTERN (2 minutes):
     *    "This is a counting DP problem"
     *    "Similar to LCS but we're counting, not finding"
     *    "Key insight: when chars match, we have two choices: use or skip"
     * 
     * 4. DEFINE DP STATE (1 minute):
     *    "dp[i][j] = ways to form t[0..j-1] from s[0..i-1]"
     *    Draw small table to visualize
     * 
     * 5. EXPLAIN RECURRENCE (2 minutes):
     *    "If chars match: can use it OR skip it, so ADD both"
     *    "If chars don't match: must skip, so just take previous"
     *    
     * 6. DISCUSS BASE CASES (1 minute):
     *    "Empty target: 1 way (select nothing)"
     *    "Empty source, non-empty target: 0 ways"
     * 
     * 7. CODE THE SOLUTION (5 minutes):
     *    Write clean 2D DP solution
     *    Add comments for clarity
     * 
     * 8. TEST WITH EXAMPLE (2 minutes):
     *    Walk through with given example
     *    Verify base cases
     * 
     * 9. DISCUSS OPTIMIZATIONS (if time):
     *    "Can optimize space to O(n) using 1D array"
     *    "Need to iterate right-to-left to avoid overwriting"
     * 
     * 10. COMPLEXITY ANALYSIS:
     *     Time: O(m * n)
     *     Space: O(m * n), can optimize to O(n)
     * 
     * COMMON PITFALLS TO AVOID:
     * - Forgetting that matching chars give us TWO options (use + skip)
     * - Wrong base case (dp[i][0] should be 1, not 0)
     * - Integer overflow (use long then cast)
     * - Wrong iteration direction in space-optimized version
     */
    
    /*
     * WHY USE LONG THEN CAST TO INT?
     * ===============================
     * 
     * Even though the problem guarantees the answer fits in 32-bit int,
     * INTERMEDIATE calculations might overflow!
     * 
     * Example: If dp[i-1][j-1] = 2^30 and dp[i-1][j] = 2^30
     * Their sum = 2^31 which overflows int (max is 2^31 - 1)
     * 
     * Using long prevents overflow during calculation
     * Final cast to int is safe because problem guarantees final answer fits
     */
    
    /*
     * RELATIONSHIP TO OTHER PROBLEMS:
     * ===============================
     * 
     * 1. LCS (Longest Common Subsequence):
     *    - LCS finds the LENGTH
     *    - This counts DISTINCT WAYS
     * 
     * 2. Edit Distance:
     *    - Edit distance finds MIN operations
     *    - This counts DISTINCT subsequences
     * 
     * 3. Shortest Common Supersequence:
     *    - SCS builds a merged string
     *    - This counts occurrences
     * 
     * All are DP problems on strings, but with different objectives!
     */
    
    // Comprehensive test cases
    public static void main(String[] args) {
        DistinctSubsequences solution = new DistinctSubsequences();
        
        System.out.println("=".repeat(70));
        System.out.println("DISTINCT SUBSEQUENCES - TEST CASES");
        System.out.println("=".repeat(70));
        
        // Test Case 1
        String s1 = "rabbbit";
        String t1 = "rabbit";
        System.out.println("\nTest Case 1:");
        System.out.println("s = \"" + s1 + "\", t = \"" + t1 + "\"");
        int result1 = solution.numDistinct_WithVisualization(s1, t1);
        System.out.println("Result: " + result1);
        System.out.println("Expected: 3");
        System.out.println("2D DP: " + solution.numDistinct(s1, t1));
        System.out.println("1D DP: " + solution.numDistinct_SpaceOptimized(s1, t1));
        System.out.println("Memoization: " + solution.numDistinct_Memoization(s1, t1));
        
        // Test Case 2
        System.out.println("\n" + "=".repeat(70));
        String s2 = "babgbag";
        String t2 = "bag";
        System.out.println("\nTest Case 2:");
        System.out.println("s = \"" + s2 + "\", t = \"" + t2 + "\"");
        int result2 = solution.numDistinct_WithVisualization(s2, t2);
        System.out.println("Result: " + result2);
        System.out.println("Expected: 5");
        
        // Test Case 3: Empty target
        System.out.println("\n" + "=".repeat(70));
        String s3 = "abc";
        String t3 = "";
        System.out.println("\nTest Case 3 (Empty target):");
        System.out.println("s = \"" + s3 + "\", t = \"" + t3 + "\"");
        int result3 = solution.numDistinct(s3, t3);
        System.out.println("Result: " + result3);
        System.out.println("Expected: 1 (one way: select nothing)");
        
        // Test Case 4: Target longer than source
        System.out.println("\n" + "=".repeat(70));
        String s4 = "abc";
        String t4 = "abcd";
        System.out.println("\nTest Case 4 (Target longer):");
        System.out.println("s = \"" + s4 + "\", t = \"" + t4 + "\"");
        int result4 = solution.numDistinct(s4, t4);
        System.out.println("Result: " + result4);
        System.out.println("Expected: 0 (impossible)");
        
        // Test Case 5: All same characters
        System.out.println("\n" + "=".repeat(70));
        String s5 = "aaaa";
        String t5 = "aa";
        System.out.println("\nTest Case 5 (Repeated chars):");
        System.out.println("s = \"" + s5 + "\", t = \"" + t5 + "\"");
        int result5 = solution.numDistinct_WithVisualization(s5, t5);
        System.out.println("Result: " + result5);
        System.out.println("Expected: 6 (choose 2 from 4: C(4,2) = 6)");
        
        // Test Case 6: No match
        System.out.println("\n" + "=".repeat(70));
        String s6 = "abc";
        String t6 = "def";
        System.out.println("\nTest Case 6 (No match):");
        System.out.println("s = \"" + s6 + "\", t = \"" + t6 + "\"");
        int result6 = solution.numDistinct(s6, t6);
        System.out.println("Result: " + result6);
        System.out.println("Expected: 0");
        
        System.out.println("\n" + "=".repeat(70));
    }
}


class DistinctSubsequencesMemo {

    private static Map<String, Integer> memo = new HashMap<>();

    public static int numDistinct(String s, String t) {
        memo.clear();
        return helper(s, t, s.length(), t.length());
    }

    private static int helper(String s, String t, int i, int j) {

        // If t is fully matched → 1 valid way
        if (j == 0) return 1;

        // If s is exhausted but t is not → no way
        if (i == 0) return 0;

        String key = i + "|" + j;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }

        int result;

        if (s.charAt(i - 1) == t.charAt(j - 1)) {
            // Use + Skip
            result = helper(s, t, i - 1, j - 1) +
                     helper(s, t, i - 1, j);
        } else {
            // Must skip s[i-1]
            result = helper(s, t, i - 1, j);
        }

        memo.put(key, result);
        return result;
    }

    // Test
    public static void main(String[] args) {
        System.out.println(numDistinct("rabbbit", "rabbit")); // 3
        System.out.println(numDistinct("babgbag", "bag"));    // 5
    }
}
