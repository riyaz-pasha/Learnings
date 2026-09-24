/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n binary matrix (containing only 0s and 1s).
 * We are allowed to flip ANY number of columns. Flipping a column means changing 
 * all the 0s to 1s and 1s to 0s in that specific column.
 * Our goal is to find the maximum number of rows that can have entirely identical 
 * values (i.e., a row of all 0s OR a row of all 1s) at the same time after these flips.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can we flip zero columns? 
 *    (Yes, "any number of columns" includes 0. If some rows are already all 0s 
 *     or all 1s, we can just leave the matrix as is).
 * 2. Are the rows considered valid if one row is all 0s and another is all 1s?
 *    (Yes! The problem states "all values become identical" per row. So Row A 
 *     can be [0,0,0] and Row B can be [1,1,1] at the same time, and both count).
 * 3. Can the matrix be a single column or single row?
 *    (Yes, constraints say 1 <= m, n <= 50. If n=1, any row is already identical 
 *     to itself, so the answer is m).
 * 4. Can we modify the matrix in place?
 *    (We don't actually need to mutate it to solve the problem, reading is enough).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. Single Column Matrix: [[0], [1], [0]] -> Output: 3. All rows are already valid.
 * 2. Already Uniform Rows: [[0,0], [1,1]] -> Output: 2. Flip 0 columns.
 * 3. Completely Unique Rows: [[0,1], [0,0]] -> Output: 1. If we flip col 1, row 0 
 *    becomes [0,0] (valid), but row 1 becomes [0,1] (invalid). Max is 1.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. The "Aha!" Observation (Pattern Matching):
 *    - Let's look at a row: [0, 1, 0]. If we flip column 1, it becomes [0, 0, 0].
 *    - What happens to a row that is its EXACT OPPOSITE: [1, 0, 1]? If we flip 
 *      column 1, it becomes [1, 1, 1]. Both rows become uniform at the same time!
 *    - Therefore, two rows can be made uniform simultaneously IF AND ONLY IF they 
 *      are exactly the same originally, OR they are exact bitwise complements of each other.
 * 
 * 2. Hashing / Normalization (The Optimal Approach):
 *    - We need a way to group rows that are either identical or exact opposites.
 *    - We can "normalize" every row so that it always starts with a 0. 
 *    - How? By checking the first element of the row. 
 *      If it's 1, we flip every bit in our string representation.
 *      If it's 0, we leave it as is.
 *    - More elegantly: we just store a pattern of "Differences from the first element".
 *      If `matrix[i][j] == matrix[i][0]`, we record a '0' (or 'S' for Same).
 *      If `matrix[i][j] != matrix[i][0]`, we record a '1' (or 'D' for Different).
 *    - Example: [0, 1, 0] -> Starts with 0. Pattern: 0 1 0.
 *    - Example: [1, 0, 1] -> Starts with 1. Difference from start: 0 1 0. 
 *      They yield the EXACT same pattern!
 *    - We simply tally the frequencies of these patterns using a HashMap and return 
 *      the maximum frequency.
 *    - Time: O(M * N), Space: O(M * N).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Hashing Approach)
 * ============================================================================
 * Matrix:
 * [0, 1, 0]  <- Row 0
 * [1, 0, 1]  <- Row 1
 * [1, 1, 1]  <- Row 2
 * 
 * Processing Row 0: [0, 1, 0]
 * - Base = 0.
 * - j=0: 0 ^ 0 = 0
 * - j=1: 1 ^ 0 = 1
 * - j=2: 0 ^ 0 = 0
 * Pattern: "010". Map: {"010": 1}
 * 
 * Processing Row 1: [1, 0, 1]
 * - Base = 1.
 * - j=0: 1 ^ 1 = 0
 * - j=1: 0 ^ 1 = 1
 * - j=2: 1 ^ 1 = 0
 * Pattern: "010". Map: {"010": 2}
 * 
 * Processing Row 2: [1, 1, 1]
 * - Base = 1.
 * - j=0: 1 ^ 1 = 0
 * - j=1: 1 ^ 1 = 0
 * - j=2: 1 ^ 1 = 0
 * Pattern: "000". Map: {"010": 2, "000": 1}
 * 
 * Max frequency in Map is 2. Return 2.
 */

import java.util.HashMap;
import java.util.Map;

public class MaxEqualRowsAfterFlips {

    public static void main(String[] args) {
        int[][] matrix1 = {
            {0, 1},
            {1, 0}
        };
        
        int[][] matrix2 = {
            {0, 0, 0},
            {0, 0, 1},
            {1, 1, 0}
        };
        
        System.out.println("--- Test Case 1 ---");
        System.out.println("Output: " + maxEqualRowsAfterFlips(matrix1)); // Expected: 2
        
        System.out.println("\n--- Test Case 2 ---");
        System.out.println("Output: " + maxEqualRowsAfterFlips(matrix2)); // Expected: 2
    }

    /**
     * SOLUTION: HashMap Normalization (Optimal)
     * 
     * Idea: Two rows can become identical after column flips if they share the 
     * same relative pattern. We normalize each row by checking if elements differ 
     * from the first element of that row. We hash this pattern and find the max.
     * 
     * Time Complexity: O(M * N) - We visit every element exactly once to build patterns.
     * Space Complexity: O(M * N) - We store up to M strings of length N in the HashMap.
     */
    public static int maxEqualRowsAfterFlips(int[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        // Map to store the frequency of each normalized row pattern
        Map<String, Integer> patternCount = new HashMap<>();
        int maxRows = 0;
        
        for (int[] row : matrix) {
            StringBuilder pattern = new StringBuilder();
            
            // The first element of the row acts as our baseline for comparison.
            int baseElement = row[0];
            
            for (int val : row) {
                // If the current value is the same as the baseElement, XOR yields 0.
                // If it is different, XOR yields 1.
                // This perfectly normalizes both a row and its exact complement 
                // into the exact same binary string.
                pattern.append(val ^ baseElement);
            }
            
            // Convert to string to use as a HashMap key
            String normalizedStr = pattern.toString();
            
            // Update the frequency map
            int count = patternCount.getOrDefault(normalizedStr, 0) + 1;
            patternCount.put(normalizedStr, count);
            
            // Track the maximum frequency found so far
            maxRows = Math.max(maxRows, count);
        }
        
        return maxRows;
    }
    
    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if N (columns) is very large, and M (rows) is huge? 
     *    Is there a way to avoid creating thousands of String objects?
     *    Answer: Yes! Instead of `StringBuilder` and `String`, we could use a Trie 
     *    (Prefix Tree). Each node in the Trie would represent a '0' or '1'. We insert 
     *    each normalized row into the Trie. The leaf nodes would maintain a `count`. 
     *    This avoids allocating millions of String objects, heavily reducing GC 
     *    (Garbage Collection) overhead, while keeping the Time Complexity at O(M * N).
     * 
     * 2. What if N <= 64?
     *    Answer: If columns are less than or equal to 64, we can represent the 
     *    normalized pattern as a single 64-bit `long` primitive instead of a String! 
     *    We can use bitwise shifting (`pattern = (pattern << 1) | (val ^ baseElement);`). 
     *    Then we just use a `Map<Long, Integer>`, which is drastically faster and 
     *    uses virtually no memory overhead compared to Strings.
     */
}
