import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Number of Ways to Form a Target String Given a Dictionary
 * You are given an array of strings 'words' (all of the same length) and a 
 * string 'target'. You must build 'target' from left to right by picking 
 * characters from 'words'.
 * 
 * Rule: If you pick a character from index 'k' of ANY string in 'words', 
 * all future characters must be picked from indices strictly greater than 'k'.
 * 
 * Return the number of ways to form 'target' modulo 10^9 + 7.
 * 
 * Constraints:
 * 1 <= words.length <= 1000
 * 1 <= words[i].length <= 1000
 * 1 <= target.length <= 1000
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, recognizing the hidden dimensional collapse is critical:
 * 
 * Q: "Does it matter exactly WHICH string I pick a character from?"
 * A: No! The rule states that if we pick a character from index 'k' in ANY string, 
 *    the next character must come from index > 'k'. 
 *    This means the strings themselves do not matter; only the COLUMNS matter.
 * 
 * CRITICAL SENIOR INSIGHT - COLUMN COMPRESSION:
 * "Instead of passing the entire array of words through our recursive calls 
 * and checking every string one by one (which adds an O(N) multiplier), we can 
 * precompute a frequency map. We collapse the entire 'words' array into an 
 * array of character frequencies per column.
 * 
 * Example: How many 'a's are in column 0? How many 'b's in column 1?
 * Our problem instantly transforms into: 'Match the target string against an 
 * array of character bags, moving strictly left-to-right.'"
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We place a pointer `targetIndex` on our target string, and `colIndex` on 
 * our precomputed frequency array.
 * 
 * At any column, we have two choices:
 *  1. SKIP THIS COLUMN: We decide not to pick any character from this column. 
 *     We move to the next column and keep our target pointer the same.
 *  2. USE THIS COLUMN: We check if the current column has the character we 
 *     need for `target[targetIndex]`. If it does, we multiply our historical 
 *     paths by the frequency of that character in this column, and move BOTH 
 *     pointers forward.
 * 
 * This perfectly mirrors the '0/1 Knapsack' or 'Distinct Subsequences' 
 * state transitions -> Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: words = ["acca","bbbb","caca"], target = "aba"
 * 
 * Precomputed Frequency Map (Columns):
 * Col 0: a:1, b:1, c:1
 * Col 1: a:1, b:1, c:1
 * Col 2: b:1, c:2
 * Col 3: a:2, b:1
 * 
 * Target: "aba"
 * 
 * Trace DP (Matching 'a' at targetIndex=0):
 * - Col 0 has one 'a'. Ways to match "a" at col 0 = 1.
 * - Col 1 has one 'a'. Ways to match "a" at col 1 = 1 (using col 1) + 1 (skipped col 0) = 2.
 * 
 * Trace DP (Matching 'b' at targetIndex=1):
 * - Col 1 has one 'b'. To use it, we must have matched "a" BEFORE Col 1.
 *   Ways to match "ab" at col 1 = (Ways to match "a" before Col 1) * Freq('b' at Col 1)
 *                             = 1 * 1 = 1.
 * 
 * This column-by-column multiplication is vastly more efficient than checking 
 * the raw strings over and over!
 */
public class TargetStringFromDictionary {

    private static final int MOD = 1_000_000_007;

    /**
     * Helper method to precompute the frequency of each character per column.
     * This turns O(N) operations inside our DP into O(1) operations.
     */
    private int[][] buildFrequencyMap(String[] words) {
        int m = words[0].length();
        int[][] freq = new int[m][26];
        
        for (String word : words) {
            for (int k = 0; k < m; k++) {
                freq[k][word.charAt(k) - 'a']++;
            }
        }
        return freq;
    }

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Traverse the frequency map and branch into "skip column" and "use column".
     * 
     * Time Complexity: O(2^M) - We branch 2 ways at every column.
     * Space Complexity: O(M) - Maximum depth of the recursion tree.
     */
    public int numWaysRecursive(String[] words, String target) {
        if (words == null || words.length == 0 || target == null || target.length() == 0) return 0;
        if (words[0].length() < target.length()) return 0;
        
        int[][] freq = buildFrequencyMap(words);
        return solveRecursive(freq, target, 0, 0);
    }

    private int solveRecursive(int[][] freq, String target, int targetIndex, int colIndex) {
        // BASE CASE REASONING:
        // We successfully built the entire target string! This is 1 valid path.
        if (targetIndex == target.length()) {
            return 1;
        }
        
        // We ran out of columns before finishing the target string. Dead end.
        // OPTIMIZATION: If remaining target characters > remaining columns, impossible.
        int remainingTarget = target.length() - targetIndex;
        int remainingCols = freq.length - colIndex;
        if (remainingCols < remainingTarget) {
            return 0;
        }

        // Universe 1: SKIP this column completely
        long ways = solveRecursive(freq, target, targetIndex, colIndex + 1);

        // Universe 2: USE this column (if the character exists here)
        char neededChar = target.charAt(targetIndex);
        int charCount = freq[colIndex][neededChar - 'a'];
        
        if (charCount > 0) {
            long waysUsingCol = solveRecursive(freq, target, targetIndex + 1, colIndex + 1);
            // Multiply the downstream paths by the number of valid character choices in this column
            ways = (ways + (waysUsingCol * charCount) % MOD) % MOD;
        }

        return (int) ways;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the number of ways to build the target from `targetIndex` 
     * starting at `colIndex`.
     * 
     * Time Complexity: O(T * M) - Where T is target.length and M is words[0].length.
     * Space Complexity: O(T * M) - For the 2D memo array + call stack.
     */
    public int numWaysMemo(String[] words, String target) {
        int m = words[0].length();
        int t = target.length();
        if (m < t) return 0;
        
        int[][] freq = buildFrequencyMap(words);
        
        // memo[targetIndex][colIndex]
        int[][] memo = new int[t][m];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(freq, target, 0, 0, memo);
    }

    private int solveMemo(int[][] freq, String target, int targetIndex, int colIndex, int[][] memo) {
        if (targetIndex == target.length()) return 1;
        
        int remainingTarget = target.length() - targetIndex;
        int remainingCols = freq.length - colIndex;
        if (remainingCols < remainingTarget) return 0;

        if (memo[targetIndex][colIndex] != -1) {
            return memo[targetIndex][colIndex];
        }

        long ways = solveMemo(freq, target, targetIndex, colIndex + 1, memo);

        char neededChar = target.charAt(targetIndex);
        int charCount = freq[colIndex][neededChar - 'a'];
        
        if (charCount > 0) {
            long waysUsingCol = solveMemo(freq, target, targetIndex + 1, colIndex + 1, memo);
            ways = (ways + (waysUsingCol * charCount) % MOD) % MOD;
        }

        memo[targetIndex][colIndex] = (int) ways;
        return (int) ways;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: dp[i][k] signifies the number of ways to form the first 'i' characters 
     * of the target using ONLY the first 'k' columns of the dictionary.
     * 
     * Time Complexity: O(M * N + T * M) - To build freq map, then populate DP array.
     * Space Complexity: O(T * M)
     */
    public int numWaysTabulation(String[] words, String target) {
        int m = words[0].length();
        int t = target.length();
        if (m < t) return 0;
        
        int[][] freq = buildFrequencyMap(words);
        long[][] dp = new long[t + 1][m + 1];
        
        // BASE CASE REASONING:
        // An empty target (length 0) can be formed exactly 1 way from any number 
        // of columns (by just picking nothing).
        for (int k = 0; k <= m; k++) {
            dp[0][k] = 1;
        }
        
        for (int i = 1; i <= t; i++) {
            for (int k = 1; k <= m; k++) {
                
                // --- DETAILED TABULATION EXPLANATION ---
                // 1. Implicitly skip the current column 'k'
                // We carry over the exact number of ways we built the prefix using 
                // only the previous (k - 1) columns.
                dp[i][k] = dp[i][k - 1];
                
                // 2. Try to use the current column 'k'
                char neededChar = target.charAt(i - 1);
                int charCount = freq[k - 1][neededChar - 'a'];
                
                if (charCount > 0) {
                    // We successfully match!
                    // We take the number of ways we built the (i - 1) prefix using 
                    // the (k - 1) historical columns, and multiply by the frequency 
                    // of our matching character in this new column.
                    long addedWays = (dp[i - 1][k - 1] * charCount) % MOD;
                    dp[i][k] = (dp[i][k] + addedWays) % MOD;
                }
            }
        }
        
        return (int) dp[t][m];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In the 2D Tabulation, calculating column `k` ONLY relies on data from 
     * column `k-1` (the previous column). We can collapse the 2D matrix into a 
     * single 1D array of size `T`!
     * 
     * Because using column `k` updates `dp[i]` based on `dp[i-1]`, we MUST 
     * iterate the target index `i` BACKWARDS. If we iterated forwards, we would 
     * accidentally use the newly updated `dp[i-1]` (which represents data from 
     * column `k`) instead of the historical data from column `k-1`.
     * 
     * Time Complexity: O(M * N + T * M)
     * Space Complexity: O(M + T) - O(M * 26) for frequency map, O(T) for DP array. 
     * Massively reduced memory footprint.
     */
    public int numWaysSpaceOptimized(String[] words, String target) {
        int m = words[0].length();
        int t = target.length();
        if (m < t) return 0;
        
        int[][] freq = buildFrequencyMap(words);
        
        // dp[i] represents the number of ways to form a prefix of length 'i'
        long[] dp = new long[t + 1];
        
        // BASE CASE REASONING:
        // Empty target (length 0) can be formed 1 way.
        dp[0] = 1;
        
        // We lock the column on the outside (iterating left to right across words)
        for (int k = 0; k < m; k++) {
            
            // We iterate the target length BACKWARDS!
            for (int i = t; i >= 1; i--) {
                
                char neededChar = target.charAt(i - 1);
                int count = freq[k][neededChar - 'a'];
                
                if (count > 0) {
                    // MAGIC OF THE 1D ARRAY:
                    // dp[i]   -> Matches carrying over from skipping column 'k'
                    // dp[i-1] -> Matches from column 'k-1' (because we iterate backwards)
                    dp[i] = (dp[i] + (dp[i - 1] * count) % MOD) % MOD;
                }
            }
        }
        
        return (int) dp[t];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new TargetStringFromDictionary();
        
        record TestCase(String[] words, String target, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new String[]{"acca", "bbbb", "caca"}, "aba", 6),
            // "aba" formed 6 ways:
            // "a" from words[0][0], "b" from words[1][1], "a" from words[0][3]
            // "a" from words[0][0], "b" from words[1][2], "a" from words[0][3]
            // ... and 4 more paths.
            
            new TestCase(new String[]{"abba", "baab"}, "bab", 4),
            new TestCase(new String[]{"abc", "def"}, "x", 0),  // Character not present
            new TestCase(new String[]{"a", "b"}, "ab", 0)      // Target longer than words
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Target  : \"" + tc.target + "\"");
            System.out.println("Expected: " + tc.expected);
            
            // Limit brute force recursion execution
            if (tc.words[0].length() <= 10) {
                System.out.println("Recursive (Brute) : " + solver.numWaysRecursive(tc.words, tc.target));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Too slow for M > 10)");
            }
            
            System.out.println("Memoization       : " + solver.numWaysMemo(tc.words, tc.target));
            System.out.println("Tabulation 2D     : " + solver.numWaysTabulation(tc.words, tc.target));
            System.out.println("Space Optimized   : " + solver.numWaysSpaceOptimized(tc.words, tc.target));
            System.out.println();
        }
    }
}
