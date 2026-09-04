import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Edit Distance (Levenshtein Distance)
 * Given two strings word1 and word2, return the minimum number of operations 
 * needed to transform word1 into word2.
 * 
 * Allowed operations (cost = 1 each):
 * 1. Insert a character
 * 2. Delete a character
 * 3. Replace a character
 * 
 * Constraints:
 * 0 <= word1.length, word2.length <= 500
 * word1 and word2 consist of lowercase English letters.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, recognizing this as the classic "Levenshtein Distance" 
 * problem is expected. Clarify a few edge cases first:
 * 
 * Q: "Can either string be empty?"
 * A: Yes, lengths can be 0. If word1 is empty and word2 has length N, we must 
 *    do N insertions. If word2 is empty, we must do N deletions.
 * 
 * Q: "Do all operations have the exact same cost?"
 * A: Yes, they all count as 1 operation. (In some variations, replace costs 2, 
 *    or operations have weights. Confirming this shows attention to detail).
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We are trying to map word1 to word2 by looking at them from the end backwards.
 * Let's compare character `i` of word1 and character `j` of word2.
 * 
 * SCENARIO A: word1[i] == word2[j]
 * Perfect match! No operation needed. We just move both pointers backward.
 * 
 * SCENARIO B: word1[i] != word2[j]
 * We must force them to match using one of three operations. We will try all 
 * three parallel universes and take the minimum cost:
 * 1. INSERT: We insert a character matching word2[j] into word1. Since word1 now 
 *    matches word2[j], we move word2's pointer backward, but keep word1's pointer 
 *    the same (it still needs to be matched).
 * 2. DELETE: We delete character `i` from word1. It's gone. We move word1's 
 *    pointer backward, but keep word2's pointer the same.
 * 3. REPLACE: We swap word1[i] to become word2[j]. They now match! We move 
 *    BOTH pointers backward.
 * 
 * Because we explore overlapping combinations of these prefixes/suffixes, 
 * this perfectly maps to 2D Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: word1 = "horse", word2 = "ros"
 * 
 * Let's trace from the end:
 * 'e' != 's' -> Mismatch. Try 3 operations:
 *   1. Replace 'e' with 's': word1 becomes "horss". Now ends match. 
 *      Cost = 1 + EditDistance("hors", "ro").
 *   2. Delete 'e': word1 becomes "hors". 
 *      Cost = 1 + EditDistance("hors", "ros").
 *   3. Insert 's': word1 becomes "horses". Now ends match. 
 *      Cost = 1 + EditDistance("horse", "ro").
 * 
 * We compute all branches recursively/iteratively, taking the minimum cost. 
 * Optimal path:
 * "horse" -> "rorse" (replace 'h' with 'r')
 * "rorse" -> "rose"  (delete 'r')
 * "rose"  -> "ros"   (delete 'e')
 * Total = 3 operations.
 */
public class EditDistance {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Evaluate from the end of both strings backwards. Branch into 
     * three paths whenever characters do not match.
     * 
     * Time Complexity: O(3^(m+n)) - Exponential branching (3 choices) at mismatch.
     * Space Complexity: O(m+n) - Maximum depth of the recursion tree.
     */
    public int minDistanceRecursive(String word1, String word2) {
        return solveRecursive(word1, word2, word1.length() - 1, word2.length() - 1);
    }

    private int solveRecursive(String word1, String word2, int i, int j) {
        // BASE CASE REASONING:
        // If word1 is exhausted (i < 0), but word2 still has 'j + 1' characters left,
        // our ONLY option is to INSERT the remaining 'j + 1' characters into word1.
        if (i < 0) {
            return j + 1;
        }
        
        // BASE CASE REASONING:
        // If word2 is exhausted (j < 0), but word1 still has 'i + 1' characters left,
        // our ONLY option is to DELETE the remaining 'i + 1' characters from word1 
        // to make it empty like word2.
        if (j < 0) {
            return i + 1;
        }

        // If characters match, cost is 0. Just evaluate the rest of the strings.
        if (word1.charAt(i) == word2.charAt(j)) {
            return solveRecursive(word1, word2, i - 1, j - 1);
        }

        // If mismatch, try all 3 operations. Each costs 1 operation.
        int insertCost  = 1 + solveRecursive(word1, word2, i, j - 1);
        int deleteCost  = 1 + solveRecursive(word1, word2, i - 1, j);
        int replaceCost = 1 + solveRecursive(word1, word2, i - 1, j - 1);

        return Math.min(insertCost, Math.min(deleteCost, replaceCost));
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the minimum distance for the suffix pairs [i][j].
     * 
     * Time Complexity: O(m * n) - We evaluate every pair of indices once.
     * Space Complexity: O(m * n) - For the 2D memo array + call stack.
     */
    public int minDistanceMemo(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        int[][] memo = new int[m][n];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(word1, word2, m - 1, n - 1, memo);
    }

    private int solveMemo(String word1, String word2, int i, int j, int[][] memo) {
        // BASE CASES (Same physical logic as brute force)
        if (i < 0) return j + 1;
        if (j < 0) return i + 1;

        if (memo[i][j] != -1) {
            return memo[i][j];
        }

        if (word1.charAt(i) == word2.charAt(j)) {
            memo[i][j] = solveMemo(word1, word2, i - 1, j - 1, memo);
        } else {
            int insert  = 1 + solveMemo(word1, word2, i, j - 1, memo);
            int delete  = 1 + solveMemo(word1, word2, i - 1, j, memo);
            int replace = 1 + solveMemo(word1, word2, i - 1, j - 1, memo);
            memo[i][j] = Math.min(insert, Math.min(delete, replace));
        }

        return memo[i][j];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build a 2D grid. dp[i][j] signifies the minimum edit distance 
     * between the first 'i' characters of word1 and the first 'j' characters of word2.
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n)
     */
    public int minDistanceTabulation(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        
        int[][] dp = new int[m + 1][n + 1];

        // BASE CASE REASONING:
        // dp[i][0] (Col 0): Word2 is EMPTY. To transform word1 of length 'i' into 
        // an empty string, we MUST delete all 'i' characters. Cost = i.
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }

        // dp[0][j] (Row 0): Word1 is EMPTY. To transform an empty string into word2 
        // of length 'j', we MUST insert all 'j' characters. Cost = j.
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        // Outer loop (i): Slowly revealing word1 character by character
        for (int i = 1; i <= m; i++) {
            
            char char1 = word1.charAt(i - 1);

            // Inner loop (j): Slowly revealing word2 character by character
            for (int j = 1; j <= n; j++) {
                
                char char2 = word2.charAt(j - 1);

                // PHYSICAL CHECK: Do the two current characters naturally match?
                if (char1 == char2) {
                    
                    // YES! They match. No operation needed for these two letters.
                    // The cost to convert prefix 'i' to prefix 'j' is EXACTLY the same 
                    // as the cost to convert prefix 'i-1' to prefix 'j-1'.
                    // We look DIAGONALLY UP-LEFT.
                    dp[i][j] = dp[i - 1][j - 1];
                    
                } else {
                    
                    // NO MATCH. We must pay a penalty of 1 operation. 
                    // Which operation is cheapest? Let's check all 3 universes:
                    
                    // UNIVERSE 1: REPLACE
                    // Assume we somehow optimally converted word1(0..i-1) to word2(0..j-1).
                    // Now, we just pay 1 operation to swap 'char1' into 'char2'.
                    // We look DIAGONALLY UP-LEFT and add 1.
                    int replace = dp[i - 1][j - 1];
                    
                    // UNIVERSE 2: DELETE
                    // Assume we already converted word1(0..i-1) into the FULL word2(0..j).
                    // Now we have this extra 'char1' hanging off the end. We pay 1 to delete it.
                    // We look DIRECTLY UP and add 1.
                    int delete = dp[i - 1][j];
                    
                    // UNIVERSE 3: INSERT
                    // Assume we already converted the FULL word1(0..i) into word2(0..j-1).
                    // Now we just need to append 'char2' to the end. We pay 1 to insert it.
                    // We look DIRECTLY LEFT and add 1.
                    int insert = dp[i][j - 1];

                    // Greedily pick the cheapest operation path, plus the 1 cost of doing it.
                    dp[i][j] = 1 + Math.min(replace, Math.min(delete, insert));
                }
            }
        }

        return dp[m][n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, to calculate row 'i', we ONLY look at the current row 'i' 
     * (left) and the previous row 'i-1' (up and up-left).
     * The rest of the matrix is dead memory. We can condense this to just TWO arrays.
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(min(m, n)) - By swapping strings so the shortest is the columns.
     */
    public int minDistanceSpaceOptimized(String word1, String word2) {
        // Optimization: Ensure word2 is the shorter string to minimize array size.
        if (word1.length() < word2.length()) {
            return minDistanceSpaceOptimized(word2, word1);
        }

        int m = word1.length();
        int n = word2.length();
        
        // We only need the "previous row" and the "current row"
        int[] prevRow = new int[n + 1];
        int[] currRow = new int[n + 1];

        // BASE CASE SEEDING (Row 0):
        // If word1 is empty (row 0), cost to make word2 of length 'j' is exactly 'j' insertions.
        for (int j = 0; j <= n; j++) {
            prevRow[j] = j;
        }

        for (int i = 1; i <= m; i++) {
            
            // BASE CASE SEEDING (Col 0 inside the loop):
            // If word2 is empty (col 0), the cost to match word1 of length 'i' 
            // is exactly 'i' deletions. So the first element of current row is 'i'.
            currRow[0] = i;

            char char1 = word1.charAt(i - 1);
            
            for (int j = 1; j <= n; j++) {
                char char2 = word2.charAt(j - 1);

                if (char1 == char2) {
                    // Match: Inherit from diagonal (prevRow[j-1])
                    currRow[j] = prevRow[j - 1];
                } else {
                    // Mismatch: 1 + min(Replace(diag), Delete(up), Insert(left))
                    int replace = prevRow[j - 1];
                    int delete = prevRow[j];
                    int insert = currRow[j - 1];
                    
                    currRow[j] = 1 + Math.min(replace, Math.min(delete, insert));
                }
            }
            
            // Swap the arrays. Current row becomes the previous row for next iteration.
            int[] temp = prevRow;
            prevRow = currRow;
            currRow = temp;
        }

        // Because we swapped at the end of the last loop, the final answer sits in prevRow.
        return prevRow[n];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new EditDistance();
        
        record TestCase(String word1, String word2, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("horse", "ros", 3),           // horse->rorse->rose->ros
            new TestCase("intention", "execution", 5), // intention->inention->enention->exention->exection->execution
            new TestCase("", "a", 1),                  // Insert 1
            new TestCase("abc", "", 3),                // Delete 3
            new TestCase("same", "same", 0)            // Match
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Word 1  : " + tc.word1);
            System.out.println("Word 2  : " + tc.word2);
            System.out.println("Expected: " + tc.expected);
            
            // Prevent brute-force recursion from timing out on long strings
            if (tc.word1.length() <= 10 && tc.word2.length() <= 10) {
                System.out.println("Recursive (Brute) : " + solver.minDistanceRecursive(tc.word1, tc.word2));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Too long for O(3^N))");
            }
            
            System.out.println("Memoization       : " + solver.minDistanceMemo(tc.word1, tc.word2));
            System.out.println("Tabulation 2D     : " + solver.minDistanceTabulation(tc.word1, tc.word2));
            System.out.println("Space Optimized   : " + solver.minDistanceSpaceOptimized(tc.word1, tc.word2));
            System.out.println();
        }
    }
}
