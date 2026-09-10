/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given a 2D integer array (matrix) of dimensions m x n.
 * We need to find its transpose. 
 * The transpose of a matrix is formed by turning all the rows of the original 
 * matrix into columns in the new matrix, and all the columns into rows.
 * If the original matrix is of size m x n, the transposed matrix will be of 
 * size n x m.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the matrix be empty (e.g., 0 rows or 0 columns)? 
 *    (Constraints say 1 <= m, n <= 100, so it will always have at least 1 element).
 * 2. Is the matrix guaranteed to be perfectly rectangular? 
 *    (Yes, typically in such problems all rows have the exact same length 'n').
 * 3. Are we allowed to modify the input matrix in place?
 *    (If m != n, we cannot easily do it in-place because the dimensions of the 
 *     underlying arrays must change. We must allocate a new 2D array).
 * 4. What if the matrix is a square (m == n)? Can we optimize space?
 *    (Yes, if it's a square matrix, we can transpose it in-place using O(1) 
 *     extra space by swapping elements across the main diagonal).
 * 5. Are there any strict performance or memory constraints beyond standard limits?
 *    (Standard constraints imply O(M*N) time and O(M*N) space is perfectly fine).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. The Standard Intuitive Approach (Out-of-place):
 *    - Create a new 2D array `transposed` of size n x m.
 *    - Use two nested loops: outer loop for the original rows (0 to m-1), 
 *      inner loop for the original columns (0 to n-1).
 *    - Map each element using the formula: transposed[j][i] = matrix[i][j].
 *    - Time Complexity: O(m * n). Space Complexity: O(m * n).
 *    - This is the universally correct and most standard approach.
 * 
 * 2. The In-Place Approach (Special Case for Square Matrices):
 *    - If the interviewer asks to optimize space and the matrix is n x n:
 *    - Iterate `i` from 0 to n-1. Iterate `j` from `i+1` to n-1.
 *    - Swap `matrix[i][j]` with `matrix[j][i]`.
 *    - Time Complexity: O(n^2). Space Complexity: O(1).
 * 
 * 3. The Modern Java Stream Approach:
 *    - Using Java 8+ Streams to build the transposed matrix.
 *    - Iterate over the columns using IntStream, and for each column, extract 
 *      the corresponding elements from all rows.
 *    - Good to show off language knowledge, though slightly less performant 
 *      due to stream overhead.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING
 * ============================================================================
 * Example: matrix = [
 *   [1, 2, 3],
 *   [4, 5, 6]
 * ]
 * 
 * Dimensions: m = 2 (rows), n = 3 (columns)
 * New matrix dimensions: 3 x 2
 * 
 * Step-by-step mapping:
 * - i=0, j=0 (val=1): transposed[0][0] = 1
 * - i=0, j=1 (val=2): transposed[1][0] = 2
 * - i=0, j=2 (val=3): transposed[2][0] = 3
 * - i=1, j=0 (val=4): transposed[0][1] = 4
 * - i=1, j=1 (val=5): transposed[1][1] = 5
 * - i=1, j=2 (val=6): transposed[2][1] = 6
 * 
 * Result: [
 *   [1, 4],
 *   [2, 5],
 *   [3, 6]
 * ]
 */

import java.util.Arrays;
import java.util.stream.IntStream;

public class MatrixTranspose {

    public static void main(String[] args) {
        int[][] matrix1 = {
            {1, 2, 3},
            {4, 5, 6}
        };
        
        int[][] matrixSquare = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };

        System.out.println("Original Matrix 2x3:");
        printMatrix(matrix1);

        System.out.println("\nSolution 1 (Standard Iteration):");
        printMatrix(transposeStandard(matrix1));

        System.out.println("\nSolution 2 (Java Streams):");
        printMatrix(transposeStreams(matrix1));

        System.out.println("\nOriginal Square Matrix 3x3:");
        printMatrix(matrixSquare);

        System.out.println("\nSolution 3 (In-Place for Square Matrix):");
        transposeInPlace(matrixSquare);
        printMatrix(matrixSquare);
    }

    /**
     * SOLUTION 1: Standard Iterative Approach (Most Optimal & Standard)
     * 
     * Idea: Allocate a new matrix with swapped dimensions. Traverse the original 
     * matrix and place each element at its swapped coordinates.
     * 
     * Time Complexity: O(m * n) - where m is rows and n is columns.
     * Space Complexity: O(m * n) - to hold the new transposed matrix.
     */
    public static int[][] transposeStandard(int[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            return new int[0][0];
        }

        int m = matrix.length;
        int n = matrix[0].length;
        
        // Create the transposed matrix with dimensions n x m
        int[][] transposed = new int[n][m];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }

        return transposed;
    }

    /**
     * SOLUTION 2: Modern Java Streams
     * 
     * Idea: Use IntStream to generate the column indices (0 to n-1). 
     * For each column index 'j', map it to an array containing matrix[0..m-1][j].
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n)
     * 
     * Note: While functional and clean, streams have slightly more overhead 
     * than primitive for-loops. It's a great demonstration of Java API knowledge.
     */
    public static int[][] transposeStreams(int[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            return new int[0][0];
        }

        int m = matrix.length;
        int n = matrix[0].length;

        // Iterate over the columns of the original matrix
        return IntStream.range(0, n)
                .mapToObj(j -> 
                    // For each column j, extract the elements from all rows i
                    IntStream.range(0, m)
                             .map(i -> matrix[i][j])
                             .toArray()
                )
                .toArray(int[][]::new);
    }

    /**
     * SOLUTION 3: In-Place Transposition (Only for Square Matrices)
     * 
     * Idea: If m == n (a square matrix), we don't need to allocate a new matrix. 
     * We can just swap the elements across the main diagonal (where i == j).
     * 
     * Time Complexity: O(n^2) - specifically, it iterates roughly (n^2)/2 times.
     * Space Complexity: O(1) - no extra matrix is created.
     */
    public static void transposeInPlace(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix.length != matrix[0].length) {
            throw new IllegalArgumentException("Matrix must be a square (m == n) for in-place transposition.");
        }

        int n = matrix.length;

        // Iterate only over the upper triangle of the matrix to avoid double-swapping
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // Swap matrix[i][j] with matrix[j][i]
                int temp = matrix[i][j];
                matrix[i][j] = matrix[j][i];
                matrix[j][i] = temp;
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
}
