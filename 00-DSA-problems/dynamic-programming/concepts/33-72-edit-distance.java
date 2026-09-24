
import java.util.HashMap;
import java.util.Map;

/**
 * EDIT DISTANCE (Levenshtein Distance) - Complete Analysis and Implementation
 * 
 * PROBLEM UNDERSTANDING:
 * Given two strings word1 and word2, find the minimum number of operations needed
 * to convert word1 to word2. Operations allowed: Insert, Delete, Replace.
 * 
 * This is a CLASSIC Dynamic Programming problem - one of the most frequently asked
 * in technical interviews at companies like Google, Facebook, Amazon, Microsoft.
 * 
 * ============================================================================
 * HOW TO APPROACH THIS IN AN INTERVIEW:
 * ============================================================================
 * 
 * Step 1: CLARIFY THE PROBLEM (1-2 minutes)
 * -----------------------------------------
 * Ask questions:
 * - Can strings be empty? (Yes, handle edge cases)
 * - Are strings case-sensitive? (Usually yes)
 * - What characters are allowed? (Usually any ASCII/Unicode)
 * - What's the expected string length? (Helps decide optimization level)
 * 
 * Step 2: THINK OUT LOUD - Start with Examples (2-3 minutes)
 * ----------------------------------------------------------
 * Use the given examples and create your own:
 * - "horse" -> "ros" requires 3 operations
 * - "" -> "abc" requires 3 insertions
 * - "abc" -> "" requires 3 deletions
 * - "abc" -> "abc" requires 0 operations
 * 
 * Step 3: IDENTIFY THE PATTERN (3-5 minutes)
 * ------------------------------------------
 * Key insight: This is a DECISION TREE problem at each position:
 * 
 * For each pair of characters word1[i] and word2[j], we have choices:
 * 1. If characters match: No operation needed, move to next characters
 * 2. If they don't match:
 *    - INSERT a character into word1 (move j forward)
 *    - DELETE a character from word1 (move i forward)
 *    - REPLACE character in word1 (move both forward)
 * 
 * This recursive structure with overlapping subproblems = DYNAMIC PROGRAMMING!
 * 
 * Step 4: START WITH RECURSION (Brute Force)
 * ------------------------------------------
 * Always start simple in interviews! Show you understand the problem.
 * Then optimize.
 * 
 * Step 5: OPTIMIZE WITH MEMOIZATION
 * ---------------------------------
 * Add caching to avoid recalculating same subproblems.
 * 
 * Step 6: CONVERT TO BOTTOM-UP DP (Most Expected Solution)
 * --------------------------------------------------------
 * Build a 2D table iteratively.
 * 
 * Step 7: OPTIMIZE SPACE (Bonus Points)
 * -------------------------------------
 * Use 1D array instead of 2D if asked.
 * 
 * ============================================================================
 */

class EditDistance {
    
    /**
     * ========================================================================
     * APPROACH 1: PURE RECURSION (Brute Force)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(3^(m+n)) - Exponential! Each call branches into 3 recursive calls
     * SPACE COMPLEXITY: O(m+n) - Recursion stack depth
     * 
     * WHY THIS WORKS:
     * At each step, we compare characters at positions i and j:
     * - If they match: No cost, move both pointers
     * - If they don't match: Try all 3 operations and pick minimum
     * 
     * INTERVIEW TIP: Start with this to show you understand the problem logic.
     * Then immediately say "but this has exponential time complexity due to 
     * overlapping subproblems, so we should use DP."
     */
    public int minDistanceRecursive(String word1, String word2) {
        return recursiveHelper(word1, word2, 0, 0);
    }
    
    private int recursiveHelper(String word1, String word2, int i, int j) {
        // BASE CASES:
        // If word1 is exhausted, we need to insert all remaining chars from word2
        if (i == word1.length()) {
            return word2.length() - j;
        }
        
        // If word2 is exhausted, we need to delete all remaining chars from word1
        if (j == word2.length()) {
            return word1.length() - i;
        }
        
        // RECURSIVE CASE:
        // If characters match, no operation needed - move both pointers
        if (word1.charAt(i) == word2.charAt(j)) {
            return recursiveHelper(word1, word2, i + 1, j + 1);
        }
        
        // If characters don't match, try all three operations:
        
        // 1. INSERT: Add word2[j] to word1, then compare word1[i] with word2[j+1]
        //    Example: "hor" -> "ros", insert 'r', now compare "hor" with "os"
        int insertOp = 1 + recursiveHelper(word1, word2, i, j + 1);
        
        // 2. DELETE: Remove word1[i], then compare word1[i+1] with word2[j]
        //    Example: "horse" -> "ros", delete 'h', now compare "orse" with "ros"
        int deleteOp = 1 + recursiveHelper(word1, word2, i + 1, j);
        
        // 3. REPLACE: Replace word1[i] with word2[j], move both pointers
        //    Example: "horse" -> "ros", replace 'h' with 'r', compare "orse" with "os"
        int replaceOp = 1 + recursiveHelper(word1, word2, i + 1, j + 1);
        
        // Return minimum of all three operations
        return Math.min(insertOp, Math.min(deleteOp, replaceOp));
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
     * Notice that recursiveHelper(i, j) gets called multiple times with same values.
     * For example, when converting "abc" to "def":
     * - recursiveHelper(1, 1) might be called from multiple paths
     * 
     * We cache results in a 2D array: memo[i][j] = answer for word1[i...] to word2[j...]
     * 
     * INTERVIEW TIP: This is a good intermediate solution. Shows you understand
     * optimization and can identify overlapping subproblems.
     */
    public int minDistanceMemoization(String word1, String word2) {
        // Create memoization table
        // memo[i][j] = -1 means not computed yet
        // memo[i][j] >= 0 means we've already computed the answer
        Integer[][] memo = new Integer[word1.length() + 1][word2.length() + 1];
        return memoHelper(word1, word2, 0, 0, memo);
    }
    
    private int memoHelper(String word1, String word2, int i, int j, Integer[][] memo) {
        // BASE CASES (same as recursive approach)
        if (i == word1.length()) {
            return word2.length() - j;
        }
        if (j == word2.length()) {
            return word1.length() - i;
        }
        
        // CHECK MEMO: If we've already computed this, return cached result
        if (memo[i][j] != null) {
            return memo[i][j];
        }
        
        // COMPUTE AND CACHE
        int result;
        if (word1.charAt(i) == word2.charAt(j)) {
            result = memoHelper(word1, word2, i + 1, j + 1, memo);
        } else {
            int insertOp = 1 + memoHelper(word1, word2, i, j + 1, memo);
            int deleteOp = 1 + memoHelper(word1, word2, i + 1, j, memo);
            int replaceOp = 1 + memoHelper(word1, word2, i + 1, j + 1, memo);
            result = Math.min(insertOp, Math.min(deleteOp, replaceOp));
        }
        
        // Store in memo before returning
        memo[i][j] = result;
        return result;
    }
    
    /**
     * ========================================================================
     * APPROACH 3: BOTTOM-UP DYNAMIC PROGRAMMING (Most Common Interview Solution)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(m * n) - Fill entire table
     * SPACE COMPLEXITY: O(m * n) - DP table
     * 
     * THE CORE DP INSIGHT:
     * ---------------------
     * Define: dp[i][j] = minimum operations to convert word1[0...i-1] to word2[0...j-1]
     * 
     * BASE CASES:
     * - dp[0][j] = j (need j insertions to get from empty string to word2[0...j-1])
     * - dp[i][0] = i (need i deletions to get from word1[0...i-1] to empty string)
     * 
     * RECURRENCE RELATION:
     * If word1[i-1] == word2[j-1]:
     *     dp[i][j] = dp[i-1][j-1]  // No operation needed
     * Else:
     *     dp[i][j] = 1 + min(
     *         dp[i-1][j],      // DELETE: Remove word1[i-1], convert word1[0...i-2] to word2[0...j-1]
     *         dp[i][j-1],      // INSERT: Convert word1[0...i-1] to word2[0...j-2], then insert word2[j-1]
     *         dp[i-1][j-1]     // REPLACE: Replace word1[i-1] with word2[j-1], convert prefixes
     *     )
     * 
     * VISUALIZATION for "horse" -> "ros":
     * 
     *       ""  r   o   s
     *   ""   0   1   2   3
     *   h    1   1   2   3
     *   o    2   2   1   2
     *   r    3   2   2   2
     *   s    4   3   3   2
     *   e    5   4   4   3
     * 
     * Answer is dp[5][3] = 3
     * 
     * HOW TO TRACE THE PATH (if asked):
     * Start from dp[m][n] and work backwards:
     * - If word1[i-1] == word2[j-1]: Move diagonally (no operation)
     * - Otherwise, move to the cell that gave minimum value
     * 
     * INTERVIEW TIP: This is the EXPECTED solution. Be able to:
     * 1. Draw the table
     * 2. Explain the recurrence relation clearly
     * 3. Code it cleanly without bugs
     */
    public int minDistanceDP(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        
        // Create DP table
        // dp[i][j] represents min operations to convert word1[0...i-1] to word2[0...j-1]
        int[][] dp = new int[m + 1][n + 1];
        
        // INITIALIZE BASE CASES
        // First row: converting empty string to word2[0...j-1] requires j insertions
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        // First column: converting word1[0...i-1] to empty string requires i deletions
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        
        // FILL THE DP TABLE
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                // If characters match, no new operation needed
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    // Take minimum of three operations:
                    
                    // DELETE: dp[i-1][j] + 1
                    // We delete word1[i-1] and solve for word1[0...i-2] -> word2[0...j-1]
                    int delete = dp[i - 1][j] + 1;
                    
                    // INSERT: dp[i][j-1] + 1
                    // We insert word2[j-1] at end of word1[0...i-1], 
                    // then solve for word1[0...i-1] -> word2[0...j-2]
                    int insert = dp[i][j - 1] + 1;
                    
                    // REPLACE: dp[i-1][j-1] + 1
                    // We replace word1[i-1] with word2[j-1],
                    // then solve for word1[0...i-2] -> word2[0...j-2]
                    int replace = dp[i - 1][j - 1] + 1;
                    
                    dp[i][j] = Math.min(delete, Math.min(insert, replace));
                }
            }
        }
        
        // The answer is in the bottom-right cell
        return dp[m][n];
    }
    
    /**
     * ========================================================================
     * APPROACH 4: SPACE-OPTIMIZED DP (Advanced Optimization)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(m * n) - Same as 2D DP
     * SPACE COMPLEXITY: O(min(m, n)) - Only need two rows/columns
     * 
     * KEY OBSERVATION:
     * In the 2D DP table, to compute dp[i][j], we only need:
     * - dp[i-1][j-1] (diagonal)
     * - dp[i-1][j] (above)
     * - dp[i][j-1] (left)
     * 
     * This means we only need the PREVIOUS ROW and CURRENT ROW!
     * We can reduce space from O(m*n) to O(n) by using rolling arrays.
     * 
     * INTERVIEW TIP: Only implement this if specifically asked about space optimization
     * or if you have extra time. This shows advanced understanding but isn't always necessary.
     */
    public int minDistanceSpaceOptimized(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        
        // Optimization: use shorter string for columns to minimize space
        if (m < n) {
            return minDistanceSpaceOptimized(word2, word1);
        }
        
        // Now n <= m, so we use O(n) space
        // prev represents the previous row, curr represents current row
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        
        // Initialize first row
        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }
        
        // Fill row by row
        for (int i = 1; i <= m; i++) {
            curr[0] = i; // First column initialization
            
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    curr[j] = prev[j - 1]; // No operation needed
                } else {
                    int delete = prev[j] + 1;      // From previous row, same column
                    int insert = curr[j - 1] + 1;  // From current row, previous column
                    int replace = prev[j - 1] + 1; // From previous row, previous column
                    curr[j] = Math.min(delete, Math.min(insert, replace));
                }
            }
            
            // Swap arrays for next iteration
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return prev[n];
    }
    
    /**
     * ========================================================================
     * APPROACH 5: WITH OPERATION TRACKING (Follow-up Question)
     * ========================================================================
     * 
     * Sometimes interviewers ask: "Can you also return the sequence of operations?"
     * 
     * We can track operations using backtracking from the DP table.
     * This doesn't change time/space complexity asymptotically.
     */
    public static class EditResult {
        int operations;
        java.util.List<String> steps;
        
        EditResult(int operations, java.util.List<String> steps) {
            this.operations = operations;
            this.steps = steps;
        }
    }
    
    public EditResult minDistanceWithSteps(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        // Fill DP table (same as before)
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], 
                                   Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }
        
        // BACKTRACK to find operations
        java.util.List<String> steps = new java.util.ArrayList<>();
        int i = m, j = n;
        
        while (i > 0 || j > 0) {
            // Reached beginning of word1
            if (i == 0) {
                steps.add(0, "Insert '" + word2.charAt(j - 1) + "'");
                j--;
                continue;
            }
            
            // Reached beginning of word2
            if (j == 0) {
                steps.add(0, "Delete '" + word1.charAt(i - 1) + "'");
                i--;
                continue;
            }
            
            // Characters match - no operation
            if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                i--;
                j--;
            } 
            // Find which operation was used
            else if (dp[i][j] == dp[i - 1][j - 1] + 1) {
                steps.add(0, "Replace '" + word1.charAt(i - 1) + "' with '" + word2.charAt(j - 1) + "'");
                i--;
                j--;
            } else if (dp[i][j] == dp[i - 1][j] + 1) {
                steps.add(0, "Delete '" + word1.charAt(i - 1) + "'");
                i--;
            } else {
                steps.add(0, "Insert '" + word2.charAt(j - 1) + "'");
                j--;
            }
        }
        
        return new EditResult(dp[m][n], steps);
    }
    
    /**
     * ========================================================================
     * TESTING AND VALIDATION
     * ========================================================================
     */
    public static void main(String[] args) {
        EditDistance solution = new EditDistance();
        
        // Test Case 1: Basic example from problem
        System.out.println("=== Test Case 1: 'horse' -> 'ros' ===");
        String word1 = "horse", word2 = "ros";
        System.out.println("Recursive: " + solution.minDistanceRecursive(word1, word2));
        System.out.println("Memoization: " + solution.minDistanceMemoization(word1, word2));
        System.out.println("DP: " + solution.minDistanceDP(word1, word2));
        System.out.println("Space Optimized: " + solution.minDistanceSpaceOptimized(word1, word2));
        
        EditResult result1 = solution.minDistanceWithSteps(word1, word2);
        System.out.println("Operations needed: " + result1.operations);
        System.out.println("Steps:");
        for (String step : result1.steps) {
            System.out.println("  " + step);
        }
        
        // Test Case 2: Another example from problem
        System.out.println("\n=== Test Case 2: 'intention' -> 'execution' ===");
        word1 = "intention";
        word2 = "execution";
        System.out.println("DP: " + solution.minDistanceDP(word1, word2));
        
        EditResult result2 = solution.minDistanceWithSteps(word1, word2);
        System.out.println("Operations needed: " + result2.operations);
        System.out.println("Steps:");
        for (String step : result2.steps) {
            System.out.println("  " + step);
        }
        
        // Test Case 3: Edge case - empty strings
        System.out.println("\n=== Test Case 3: '' -> 'abc' ===");
        System.out.println("DP: " + solution.minDistanceDP("", "abc")); // Should be 3
        
        // Test Case 4: Edge case - identical strings
        System.out.println("\n=== Test Case 4: 'abc' -> 'abc' ===");
        System.out.println("DP: " + solution.minDistanceDP("abc", "abc")); // Should be 0
        
        // Test Case 5: Edge case - one empty
        System.out.println("\n=== Test Case 5: 'abc' -> '' ===");
        System.out.println("DP: " + solution.minDistanceDP("abc", "")); // Should be 3
        
        // Test Case 6: Single character difference
        System.out.println("\n=== Test Case 6: 'cat' -> 'cut' ===");
        System.out.println("DP: " + solution.minDistanceDP("cat", "cut")); // Should be 1
        
        // Test Case 7: All replacements
        System.out.println("\n=== Test Case 7: 'abc' -> 'def' ===");
        System.out.println("DP: " + solution.minDistanceDP("abc", "def")); // Should be 3
    }
}

/**
 * ============================================================================
 * INTERVIEW TIPS SUMMARY:
 * ============================================================================
 * 
 * 1. ALWAYS start by understanding the problem with examples
 * 2. Identify it as DP by recognizing:
 *    - Optimal substructure (optimal solution contains optimal solutions to subproblems)
 *    - Overlapping subproblems (same subproblems computed multiple times)
 * 3. Start with recursive solution to show understanding
 * 4. Mention memoization as optimization
 * 5. Implement bottom-up DP as main solution
 * 6. Discuss space optimization if time permits
 * 7. Be ready to trace through an example on the table
 * 8. Know how to backtrack for operation sequence
 * 
 * COMMON MISTAKES TO AVOID:
 * =========================
 * 1. Off-by-one errors in array indexing (i-1 vs i)
 * 2. Confusing dp[i][j] meaning (be clear: "first i chars" vs "up to index i")
 * 3. Wrong base cases (forgetting to initialize first row/column)
 * 4. Not handling empty string edge cases
 * 5. In operations: confusing which string you're operating on
 * 
 * TIME MANAGEMENT IN INTERVIEW:
 * =============================
 * - Problem understanding: 2-3 min
 * - Recursive solution discussion: 3-5 min
 * - DP solution: 10-15 min
 * - Testing: 5 min
 * - Follow-up questions: 5-10 min
 * 
 * VARIATIONS YOU MIGHT SEE:
 * =========================
 * 1. Return the actual operations, not just count
 * 2. Different operation costs (e.g., delete costs 2, insert costs 1)
 * 3. Longest Common Subsequence (related problem)
 * 4. Minimum ASCII delete sum for two strings
 * 
 * ADDITIONAL INSIGHTS:
 * ===================
 * 
 * 1. RELATIONSHIP TO OTHER PROBLEMS:
 *    - Longest Common Subsequence (LCS): Related formula
 *      Edit Distance = m + n - 2*LCS(word1, word2)
 *    - String alignment in bioinformatics
 *    - Spell checkers use this algorithm
 *    - DNA sequence alignment
 * 
 * 2. REAL-WORLD APPLICATIONS:
 *    - Autocorrect and spell checking
 *    - DNA sequence analysis
 *    - Plagiarism detection
 *    - Version control (diff algorithms)
 *    - Speech recognition
 *    - OCR error correction
 * 
 * 3. ALGORITHM HISTORY:
 *    - Invented by Vladimir Levenshtein in 1965
 *    - Also independently discovered by others
 *    - Foundation of computational biology
 * 
 * 4. OPTIMIZATION VARIANTS:
 *    - Damerau-Levenshtein distance (includes transposition)
 *    - Weighted edit distance (different costs for operations)
 *    - Approximate string matching
 * 
 * 5. COMPLEXITY ANALYSIS:
 *    - No known algorithm faster than O(mn) for general case
 *    - Special cases can be optimized (e.g., when k is small)
 *    - Space can always be reduced to O(min(m,n))
 * 
 * 6. INTERVIEW FOLLOW-UP QUESTIONS:
 *    Q: What if insertions cost 2 and deletions cost 1?
 *    A: Modify the +1 in recurrence to +2 or +1 based on operation
 *    
 *    Q: How would you handle very long strings?
 *    A: Space optimization, or if k (max edits) is bounded, use diagonal DP
 *    
 *    Q: Can you make this work for Unicode strings?
 *    A: Yes, Java handles this automatically with charAt()
 *    
 *    Q: What if we only care if distance <= k?
 *    A: Can optimize by only computing diagonal band of width 2k+1
 * 
 * ============================================================================
 */


class EditDistanceMemo {

    private static Map<String, Integer> memo = new HashMap<>();

    public static int minDistance(String word1, String word2) {
        memo.clear();
        return helper(word1, word2, word1.length(), word2.length());
    }

    private static int helper(String w1, String w2, int i, int j) {

        // Base cases
        if (i == 0) return j; // insert all remaining chars
        if (j == 0) return i; // delete all remaining chars

        String key = i + "|" + j;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }

        int result;

        if (w1.charAt(i - 1) == w2.charAt(j - 1)) {
            // No operation needed
            result = helper(w1, w2, i - 1, j - 1);
        } else {
            // Try all 3 operations
            result = 1 + Math.min(
                    helper(w1, w2, i - 1, j),     // delete
                    Math.min(
                        helper(w1, w2, i, j - 1), // insert
                        helper(w1, w2, i - 1, j - 1) // replace
                    )
            );
        }

        memo.put(key, result);
        return result;
    }

    // Test
    public static void main(String[] args) {
        System.out.println(minDistance("horse", "ros"));       // 3
        System.out.println(minDistance("intention", "execution")); // 5
    }
}
