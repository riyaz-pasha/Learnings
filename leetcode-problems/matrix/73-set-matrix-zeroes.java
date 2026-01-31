import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*
* Given an m x n integer matrix matrix, if an element is 0, set its entire row
* and column to 0's.
* 
* You must do it in place.
* 
* Example 1:
* Input: matrix = [[1,1,1],[1,0,1],[1,1,1]]
* Output: [[1,0,1],[0,0,0],[1,0,1]]
* 
* Example 2:
* Input: matrix = [[0,1,2,0],[3,4,5,2],[1,3,1,5]]
* Output: [[0,0,0,0],[0,4,5,0],[0,3,1,0]]
*/

class SetMatrixZeros {

    /**
     * Optimal Solution: Use first row and column as markers
     * Time Complexity: O(m * n)
     * Space Complexity: O(1)
     * 
     * Uses the matrix itself to store zero markers
     */
    public void setZeroes(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return;
        }

        int m = matrix.length, n = matrix[0].length;
        boolean firstRowZero = false, firstColZero = false;

        // Check if first row should be zeroed
        for (int j = 0; j < n; j++) {
            if (matrix[0][j] == 0) {
                firstRowZero = true;
                break;
            }
        }

        // Check if first column should be zeroed
        for (int i = 0; i < m; i++) {
            if (matrix[i][0] == 0) {
                firstColZero = true;
                break;
            }
        }

        // Use first row and column as markers for zeros
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (matrix[i][j] == 0) {
                    matrix[i][0] = 0; // Mark row
                    matrix[0][j] = 0; // Mark column
                }
            }
        }

        // Set zeros based on markers (excluding first row and column)
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (matrix[i][0] == 0 || matrix[0][j] == 0) {
                    matrix[i][j] = 0;
                }
            }
        }

        // Handle first row
        if (firstRowZero) {
            for (int j = 0; j < n; j++) {
                matrix[0][j] = 0;
            }
        }

        // Handle first column
        if (firstColZero) {
            for (int i = 0; i < m; i++) {
                matrix[i][0] = 0;
            }
        }
    }

    /**
     * Alternative Optimal Solution: Single variable for first column
     * Time Complexity: O(m * n)
     * Space Complexity: O(1)
     * 
     * Uses matrix[0][0] for first row, separate variable for first column
     */
    public void setZeroesAlternative(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return;
        }

        int m = matrix.length, n = matrix[0].length;
        boolean firstColZero = false;

        // Determine if first column should be zeroed
        for (int i = 0; i < m; i++) {
            if (matrix[i][0] == 0) {
                firstColZero = true;
            }

            // Use matrix[0][0] for first row, check from column 1
            for (int j = 1; j < n; j++) {
                if (matrix[i][j] == 0) {
                    matrix[i][0] = 0; // Mark row
                    matrix[0][j] = 0; // Mark column
                }
            }
        }

        // Set zeros based on markers (work backwards to avoid overwriting markers)
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 1; j--) {
                if (matrix[i][0] == 0 || matrix[0][j] == 0) {
                    matrix[i][j] = 0;
                }
            }

            // Handle first column separately
            if (firstColZero) {
                matrix[i][0] = 0;
            }
        }
    }

    /**
     * HashSet Solution: Track zero positions
     * Time Complexity: O(m * n)
     * Space Complexity: O(m + n)
     * 
     * More intuitive but uses extra space
     */
    public void setZeroesHashSet(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return;
        }

        int m = matrix.length, n = matrix[0].length;
        Set<Integer> zeroRows = new HashSet<>();
        Set<Integer> zeroCols = new HashSet<>();

        // Find all zero positions
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == 0) {
                    zeroRows.add(i);
                    zeroCols.add(j);
                }
            }
        }

        // Set entire rows to zero
        for (int row : zeroRows) {
            for (int j = 0; j < n; j++) {
                matrix[row][j] = 0;
            }
        }

        // Set entire columns to zero
        for (int col : zeroCols) {
            for (int i = 0; i < m; i++) {
                matrix[i][col] = 0;
            }
        }
    }

    /**
     * Array-based Solution: Boolean arrays for tracking
     * Time Complexity: O(m * n)
     * Space Complexity: O(m + n)
     */
    public void setZeroesArray(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return;
        }

        int m = matrix.length, n = matrix[0].length;
        boolean[] zeroRows = new boolean[m];
        boolean[] zeroCols = new boolean[n];

        // Mark rows and columns that contain zeros
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == 0) {
                    zeroRows[i] = true;
                    zeroCols[j] = true;
                }
            }
        }

        // Set zeros based on markers
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (zeroRows[i] || zeroCols[j]) {
                    matrix[i][j] = 0;
                }
            }
        }
    }

    /**
     * Brute Force with Extra Matrix (NOT in-place)
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n)
     * 
     * Included for comparison - violates in-place requirement
     */
    public void setZeroesBruteForce(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return;
        }

        int m = matrix.length, n = matrix[0].length;
        int[][] result = new int[m][n];

        // Copy original matrix
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = matrix[i][j];
            }
        }

        // Find zeros and mark entire rows/columns
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == 0) {
                    // Set entire row to zero
                    for (int k = 0; k < n; k++) {
                        result[i][k] = 0;
                    }
                    // Set entire column to zero
                    for (int k = 0; k < m; k++) {
                        result[k][j] = 0;
                    }
                }
            }
        }

        // Copy result back
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = result[i][j];
            }
        }
    }

    /**
     * Step-by-step Solution for Understanding
     * Shows the process clearly with detailed comments
     */
    public void setZeroesDetailed(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return;
        }

        int m = matrix.length, n = matrix[0].length;
        boolean firstRowZero = false, firstColZero = false;

        System.out.println("Original matrix:");
        printMatrix(matrix);

        // Step 1: Check if first row/column need to be zeroed
        System.out.println("\nStep 1: Checking first row and column for zeros...");
        for (int j = 0; j < n; j++) {
            if (matrix[0][j] == 0) {
                firstRowZero = true;
                System.out.println("Found zero in first row at column " + j);
                break;
            }
        }

        for (int i = 0; i < m; i++) {
            if (matrix[i][0] == 0) {
                firstColZero = true;
                System.out.println("Found zero in first column at row " + i);
                break;
            }
        }

        // Step 2: Use first row and column as markers
        System.out.println("\nStep 2: Using first row/column as markers...");
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (matrix[i][j] == 0) {
                    System.out.println("Found zero at (" + i + "," + j + "), marking row " + i + " and column " + j);
                    matrix[i][0] = 0;
                    matrix[0][j] = 0;
                }
            }
        }

        System.out.println("\nMatrix after marking:");
        printMatrix(matrix);

        // Step 3: Set zeros based on markers
        System.out.println("\nStep 3: Setting zeros based on markers...");
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (matrix[i][0] == 0 || matrix[0][j] == 0) {
                    matrix[i][j] = 0;
                }
            }
        }

        // Step 4: Handle first row and column
        if (firstRowZero) {
            System.out.println("Setting first row to zeros...");
            for (int j = 0; j < n; j++) {
                matrix[0][j] = 0;
            }
        }

        if (firstColZero) {
            System.out.println("Setting first column to zeros...");
            for (int i = 0; i < m; i++) {
                matrix[i][0] = 0;
            }
        }

        System.out.println("\nFinal result:");
        printMatrix(matrix);
    }

    // Helper methods
    private void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }

    private int[][] copyMatrix(int[][] matrix) {
        int m = matrix.length, n = matrix[0].length;
        int[][] copy = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                copy[i][j] = matrix[i][j];
            }
        }
        return copy;
    }

    private boolean matricesEqual(int[][] m1, int[][] m2) {
        if (m1.length != m2.length || m1[0].length != m2[0].length)
            return false;
        for (int i = 0; i < m1.length; i++) {
            if (!Arrays.equals(m1[i], m2[i]))
                return false;
        }
        return true;
    }

    // Test cases
    public static void main(String[] args) {
        SetMatrixZeros solution = new SetMatrixZeros();

        // Test case 1: 3x3 matrix with one zero
        int[][] matrix1 = { { 1, 1, 1 }, { 1, 0, 1 }, { 1, 1, 1 } };
        int[][] expected1 = { { 1, 0, 1 }, { 0, 0, 0 }, { 1, 0, 1 } };

        System.out.println("=== Test Case 1: 3x3 Matrix ===");
        System.out.println("Input:");
        solution.printMatrix(matrix1);

        int[][] test1 = solution.copyMatrix(matrix1);
        solution.setZeroes(test1);

        System.out.println("\nOutput:");
        solution.printMatrix(test1);
        System.out.println("Expected: " + Arrays.deepToString(expected1));
        System.out.println("Correct: " + solution.matricesEqual(test1, expected1) + "\n");

        // Test case 2: 3x4 matrix with multiple zeros
        int[][] matrix2 = { { 0, 1, 2, 0 }, { 3, 4, 5, 2 }, { 1, 3, 1, 5 } };
        int[][] expected2 = { { 0, 0, 0, 0 }, { 0, 4, 5, 0 }, { 0, 3, 1, 0 } };

        System.out.println("=== Test Case 2: 3x4 Matrix ===");
        System.out.println("Input:");
        solution.printMatrix(matrix2);

        int[][] test2 = solution.copyMatrix(matrix2);
        solution.setZeroes(test2);

        System.out.println("\nOutput:");
        solution.printMatrix(test2);
        System.out.println("Expected: " + Arrays.deepToString(expected2));
        System.out.println("Correct: " + solution.matricesEqual(test2, expected2) + "\n");

        // Test case 3: Single row
        int[][] matrix3 = { { 1, 0, 3 } };
        System.out.println("=== Test Case 3: Single Row ===");
        System.out.println("Input: " + Arrays.deepToString(matrix3));
        solution.setZeroes(matrix3);
        System.out.println("Output: " + Arrays.deepToString(matrix3));
        System.out.println("Expected: [[0,0,0]]\n");

        // Test case 4: Single column
        int[][] matrix4 = { { 1 }, { 0 }, { 3 } };
        System.out.println("=== Test Case 4: Single Column ===");
        System.out.println("Input: " + Arrays.deepToString(matrix4));
        solution.setZeroes(matrix4);
        System.out.println("Output: " + Arrays.deepToString(matrix4));
        System.out.println("Expected: [[0],[0],[0]]\n");

        // Test case 5: No zeros
        int[][] matrix5 = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[][] original5 = solution.copyMatrix(matrix5);
        System.out.println("=== Test Case 5: No Zeros ===");
        System.out.println("Input: " + Arrays.deepToString(matrix5));
        solution.setZeroes(matrix5);
        System.out.println("Output: " + Arrays.deepToString(matrix5));
        System.out.println("Should remain unchanged: " + solution.matricesEqual(matrix5, original5) + "\n");

        // Compare all solutions
        System.out.println("=== Comparing All Solutions ===");
        int[][] original = { { 0, 1, 2, 0 }, { 3, 4, 5, 2 }, { 1, 3, 1, 5 } };

        int[][] test_optimal = solution.copyMatrix(original);
        int[][] test_alternative = solution.copyMatrix(original);
        int[][] test_hashset = solution.copyMatrix(original);
        int[][] test_array = solution.copyMatrix(original);

        solution.setZeroes(test_optimal);
        solution.setZeroesAlternative(test_alternative);
        solution.setZeroesHashSet(test_hashset);
        solution.setZeroesArray(test_array);

        System.out.println("Optimal (O(1) space): " + Arrays.deepToString(test_optimal));
        System.out.println("Alternative optimal: " + Arrays.deepToString(test_alternative));
        System.out.println("HashSet (O(m+n) space): " + Arrays.deepToString(test_hashset));
        System.out.println("Array (O(m+n) space): " + Arrays.deepToString(test_array));

        // Detailed walkthrough
        System.out.println("\n=== Detailed Step-by-Step Demo ===");
        int[][] demo = { { 1, 1, 1 }, { 1, 0, 1 }, { 1, 1, 1 } };
        solution.setZeroesDetailed(demo);
    }

}
