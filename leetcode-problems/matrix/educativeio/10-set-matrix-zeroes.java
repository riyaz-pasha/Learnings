/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n integer matrix. 
 * If any element in the matrix is 0, we must set its entire row and its 
 * entire column to 0.
 * The catch is that we must do this "in-place", meaning we cannot allocate a 
 * new 2D matrix of the same size to help us keep track of the changes.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Does "in-place" mean O(1) auxiliary space, or is O(m + n) space acceptable?
 *    (O(m + n) space using two arrays is a good intermediate solution, but the 
 *     interviewer will likely push for strict O(1) space).
 * 2. If a cell becomes 0 because of another 0 in its row/col, does it also 
 *    trigger its own row/col to become 0?
 *    (No, only cells that were *originally* 0 should trigger the row/col wipe. 
 *     This is exactly why we can't just modify the matrix as we traverse it 
 *     without keeping track of original states).
 * 3. What are the constraints on the elements?
 *    (Elements can range from -2^31 to 2^31 - 1, meaning we cannot use a "magic 
 *     number" like -999999 to mark cells since it might conflict with actual data).
 * 4. Can the matrix be 1D (e.g., 1 row or 1 column)?
 *    (Yes, m or n could be 1, so the solution must handle edge boundaries properly).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. No zeros in the matrix: Output should be identical to the input.
 * 2. All zeros in the matrix: Output should be all zeros.
 * 3. Zero in the first row or first column:
 *    Example:
 *    [0, 1, 2]
 *    [3, 4, 5]
 *    [6, 7, 8]
 *    The 0 at (0,0) must wipe the first row and first column. Our O(1) space 
 *    approach uses the first row and column for tracking, so we must be extremely 
 *    careful not to overwrite or lose this initial state.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (O(M*N) space):
 *    - Clone the original matrix. Iterate through the clone. 
 *    - When a 0 is found, update the corresponding row/col in the original matrix.
 *    - Time: O(M * N), Space: O(M * N). Not "in-place".
 * 
 * 2. Better Approach (O(M + N) space):
 *    - Use two boolean arrays: `rowHasZero` of size M and `colHasZero` of size N.
 *    - Iterate through the matrix. If `matrix[i][j] == 0`, set `rowHasZero[i] = true` 
 *      and `colHasZero[j] = true`.
 *    - Iterate a second time. If `rowHasZero[i]` or `colHasZero[j]` is true, 
 *      set `matrix[i][j] = 0`.
 *    - Time: O(M * N), Space: O(M + N). This is a solid stepping stone.
 * 
 * 3. Most Optimal Approach (O(1) space):
 *    - We can achieve O(1) space by using the matrix's OWN first row and first 
 *      column to act as the `rowHasZero` and `colHasZero` arrays!
 *    - However, doing this overwrites the data in the first row/col. We need to 
 *      know if the first row/col ORIGINALLY contained any zeros.
 *    - Step A: Scan the first row and first col. Set booleans `firstRowZero` 
 *      and `firstColZero` if any zeros are found.
 *    - Step B: Iterate from i=1 and j=1. If `matrix[i][j] == 0`, set 
 *      `matrix[i][0] = 0` and `matrix[0][j] = 0`.
 *    - Step C: Iterate again from i=1 and j=1. If `matrix[i][0] == 0` or 
 *      `matrix[0][j] == 0`, set `matrix[i][j] = 0`.
 *    - Step D: Finally, if `firstRowZero` is true, wipe the first row. If 
 *      `firstColZero` is true, wipe the first col.
 *    - Time: O(M * N), Space: O(1).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (O(1) Space Approach)
 * ============================================================================
 * Initial Matrix:
 * [1, 1, 1]
 * [1, 0, 1]
 * [1, 1, 1]
 * 
 * Step A (Check first row/col):
 * First row has no 0s. (firstRowZero = false)
 * First col has no 0s. (firstColZero = false)
 * 
 * Step B (Mark inner zeros onto first row/col):
 * Start loops at i=1, j=1.
 * i=1, j=1 -> matrix[1][1] is 0!
 * Mark its row and col headers: matrix[1][0] = 0, matrix[0][1] = 0.
 * Matrix now:
 * [1, 0, 1]  <-- top header marked
 * [0, 0, 1]  <-- left header marked
 * [1, 1, 1]
 * 
 * Step C (Zero out inner cells based on headers):
 * i=1, j=1 -> header [1][0] is 0 -> matrix[1][1] = 0
 * i=1, j=2 -> header [1][0] is 0 -> matrix[1][2] = 0
 * i=2, j=1 -> header [0][1] is 0 -> matrix[2][1] = 0
 * Matrix now:
 * [1, 0, 1]
 * [0, 0, 0]
 * [1, 0, 1]
 * 
 * Step D (Handle first row/col):
 * firstRowZero is false -> leave first row as is.
 * firstColZero is false -> leave first col as is.
 * 
 * Final Matrix is correct!
 */

import java.util.Arrays;

public class SetMatrixZeroes {

    public static void main(String[] args) {
        int[][] mat1 = {
            {1, 1, 1},
            {1, 0, 1},
            {1, 1, 1}
        };

        int[][] mat2 = {
            {0, 1, 2, 0},
            {3, 4, 5, 2},
            {1, 3, 1, 5}
        };

        System.out.println("--- Test Case 1 ---");
        setZeroesOptimal(mat1);
        printMatrix(mat1);

        System.out.println("\n--- Test Case 2 ---");
        setZeroesOptimal(mat2);
        printMatrix(mat2);
    }

    /**
     * SOLUTION 1: Intermediate Approach using O(M + N) Space.
     * 
     * Idea: Keep two arrays to flag which rows and columns need to be zeroed.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(M + N) auxiliary space.
     */
    public static void setZeroesBetter(int[][] matrix) {
        if (matrix == null || matrix.length == 0) return;
        
        int rows = matrix.length;
        int cols = matrix[0].length;
        
        boolean[] rowHasZero = new boolean[rows];
        boolean[] colHasZero = new boolean[cols];
        
        // Pass 1: Find all original zeros and flag their rows/cols
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix[i][j] == 0) {
                    rowHasZero[i] = true;
                    colHasZero[j] = true;
                }
            }
        }
        
        // Pass 2: Iterate again and apply the zeros based on flags
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (rowHasZero[i] || colHasZero[j]) {
                    matrix[i][j] = 0;
                }
            }
        }
    }

    /**
     * SOLUTION 2: Optimal Approach using O(1) Space.
     * 
     * Idea: Use the first row and first column of the matrix itself to store 
     * the flags. We just need two extra variables to remember the initial state 
     * of the first row and first column.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(1) auxiliary space (truly in-place).
     */
    public static void setZeroesOptimal(int[][] matrix) {
        if (matrix == null || matrix.length == 0) return;
        
        int rows = matrix.length;
        int cols = matrix[0].length;
        
        boolean firstRowZero = false;
        boolean firstColZero = false;
        
        // Step A1: Determine if the first column originally has a zero
        for (int i = 0; i < rows; i++) {
            if (matrix[i][0] == 0) {
                firstColZero = true;
                break;
            }
        }
        
        // Step A2: Determine if the first row originally has a zero
        for (int j = 0; j < cols; j++) {
            if (matrix[0][j] == 0) {
                firstRowZero = true;
                break;
            }
        }
        
        // Step B: Use first row and first column as our flag arrays.
        // We start from i=1 and j=1 to avoid processing the headers.
        for (int i = 1; i < rows; i++) {
            for (int j = 1; j < cols; j++) {
                if (matrix[i][j] == 0) {
                    matrix[i][0] = 0; // Mark the row header
                    matrix[0][j] = 0; // Mark the col header
                }
            }
        }
        
        // Step C: Iterate the inner matrix again and apply zeros based on headers
        for (int i = 1; i < rows; i++) {
            for (int j = 1; j < cols; j++) {
                // If either the row header or col header is 0, set cell to 0
                if (matrix[i][0] == 0 || matrix[0][j] == 0) {
                    matrix[i][j] = 0;
                }
            }
        }
        
        // Step D1: If the first column originally had a zero, zero it out completely
        if (firstColZero) {
            for (int i = 0; i < rows; i++) {
                matrix[i][0] = 0;
            }
        }
        
        // Step D2: If the first row originally had a zero, zero it out completely
        if (firstRowZero) {
            for (int j = 0; j < cols; j++) {
                matrix[0][j] = 0;
            }
        }
    }
    
    /**
     * Helper method to neatly print a 2D matrix.
     */
    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }
    
    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. Can we do this in one single pass?
     *    Answer: No, because updating a cell to 0 in one pass could falsely 
     *    trigger wiping out rows/cols for cells processed later. We must separate 
     *    the "flagging" phase from the "applying" phase.
     * 
     * 2. What if the matrix is sparse (mostly non-zeros)?
     *    Answer: The current logic works well. If it's extremely sparse with 
     *    hardly any zeros, keeping a list of coordinates of the 0s might be 
     *    faster than iterating over the entire matrix again, but that would use 
     *    O(K) space where K is the number of zeros, violating O(1) space.
     * 
     * 3. What if we use a specific marker value instead of relying on headers?
     *    Answer: The problem allows matrix values to span the entire 32-bit signed 
     *    integer range (-2^31 to 2^31 - 1). Therefore, we have no "safe" unused 
     *    number (like Integer.MAX_VALUE) to serve as a marker. The header technique 
     *    is the only truly safe O(1) space approach.
     */
}
