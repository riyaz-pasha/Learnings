import java.util.*;
/*
 * You are given an n x n 2D matrix representing an image, rotate the image by
 * 90 degrees (clockwise).
 * 
 * You have to rotate the image in-place, which means you have to modify the
 * input 2D matrix directly. DO NOT allocate another 2D matrix and do the
 * rotation.
 * 
 * Example 1:
 * Input: matrix = [[1,2,3],[4,5,6],[7,8,9]]
 * Output: [[7,4,1],[8,5,2],[9,6,3]]
 * 
 * Example 2:
 * Input: matrix = [[5,1,9,11],[2,4,8,10],[13,3,6,7],[15,14,12,16]]
 * Output: [[15,13,2,5],[14,3,4,1],[12,6,8,9],[16,7,10,11]]
 */

class RotateMatrix {

    /**
     * Mathematical Solution: Transpose + Reverse Rows
     * Time Complexity: O(n²)
     * Space Complexity: O(1)
     * 
     * Key insight: 90° clockwise = Transpose + Reverse each row
     */
    public void rotate(int[][] matrix) {
        int n = matrix.length;

        // Step 1: Transpose the matrix (swap matrix[i][j] with matrix[j][i])
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[j][i];
                matrix[j][i] = temp;
            }
        }

        // Step 2: Reverse each row
        for (int i = 0; i < n; i++) {
            reverseRow(matrix[i]);
        }
    }

    private void reverseRow(int[] row) {
        int left = 0, right = row.length - 1;
        while (left < right) {
            int temp = row[left];
            row[left] = row[right];
            row[right] = temp;
            left++;
            right--;
        }
    }

    /**
     * Layer-by-Layer Rotation (Ring by Ring)
     * Time Complexity: O(n²)
     * Space Complexity: O(1)
     * 
     * Rotates each concentric ring from outside to inside
     */
    public void rotateLayered(int[][] matrix) {
        int n = matrix.length;

        // Process each layer (ring)
        for (int layer = 0; layer < n / 2; layer++) {
            int first = layer;
            int last = n - 1 - layer;

            // Rotate elements in current layer
            for (int i = first; i < last; i++) {
                int offset = i - first;

                // Save top element
                int top = matrix[first][i];

                // Top = Left
                matrix[first][i] = matrix[last - offset][first];

                // Left = Bottom
                matrix[last - offset][first] = matrix[last][last - offset];

                // Bottom = Right
                matrix[last][last - offset] = matrix[i][last];

                // Right = Top (saved)
                matrix[i][last] = top;
            }
        }
    }

    /**
     * Four-Point Cycle Solution
     * Time Complexity: O(n²)
     * Space Complexity: O(1)
     * 
     * Uses the mathematical transformation: (i,j) -> (j, n-1-i)
     */
    public void rotateFourPoint(int[][] matrix) {
        int n = matrix.length;

        for (int i = 0; i < (n + 1) / 2; i++) {
            for (int j = 0; j < n / 2; j++) {
                // Four-way swap using the rotation formula
                // (i,j) -> (j, n-1-i) -> (n-1-i, n-1-j) -> (n-1-j, i) -> (i,j)

                int temp = matrix[i][j];
                matrix[i][j] = matrix[n - 1 - j][i];
                matrix[n - 1 - j][i] = matrix[n - 1 - i][n - 1 - j];
                matrix[n - 1 - i][n - 1 - j] = matrix[j][n - 1 - i];
                matrix[j][n - 1 - i] = temp;
            }
        }
    }

    /**
     * Alternative Mathematical Solution: Reverse Rows + Transpose
     * Time Complexity: O(n²)
     * Space Complexity: O(1)
     * 
     * Alternative approach: Reverse rows first, then transpose
     */
    public void rotateAlternative(int[][] matrix) {
        int n = matrix.length;

        // Step 1: Reverse each row
        for (int i = 0; i < n; i++) {
            reverseRow(matrix[i]);
        }

        // Step 2: Transpose the matrix
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[j][i];
                matrix[j][i] = temp;
            }
        }
    }

    /**
     * Recursive Solution
     * Time Complexity: O(n²)
     * Space Complexity: O(log n) due to recursion stack
     */
    public void rotateRecursive(int[][] matrix) {
        rotateRecursiveHelper(matrix, 0, matrix.length - 1);
    }

    private void rotateRecursiveHelper(int[][] matrix, int start, int end) {
        if (start >= end)
            return;

        // Rotate current ring
        for (int i = start; i < end; i++) {
            int offset = i - start;

            // Save top
            int top = matrix[start][i];

            // Top = Left
            matrix[start][i] = matrix[end - offset][start];

            // Left = Bottom
            matrix[end - offset][start] = matrix[end][end - offset];

            // Bottom = Right
            matrix[end][end - offset] = matrix[i][end];

            // Right = Top
            matrix[i][end] = top;
        }

        // Recurse for inner ring
        rotateRecursiveHelper(matrix, start + 1, end - 1);
    }

    /**
     * Step-by-step Visual Solution (for understanding)
     * Shows the transformation process clearly
     */
    public void rotateVisual(int[][] matrix) {
        int n = matrix.length;
        System.out.println("Original matrix:");
        printMatrix(matrix);

        // Step 1: Transpose
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[j][i];
                matrix[j][i] = temp;
            }
        }

        System.out.println("\nAfter transpose:");
        printMatrix(matrix);

        // Step 2: Reverse rows
        for (int i = 0; i < n; i++) {
            reverseRow(matrix[i]);
        }

        System.out.println("\nAfter reversing rows (final result):");
        printMatrix(matrix);
    }

    // Helper methods
    private void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }

    private int[][] copyMatrix(int[][] matrix) {
        int n = matrix.length;
        int[][] copy = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                copy[i][j] = matrix[i][j];
            }
        }
        return copy;
    }

    private boolean matricesEqual(int[][] m1, int[][] m2) {
        if (m1.length != m2.length)
            return false;
        for (int i = 0; i < m1.length; i++) {
            if (!Arrays.equals(m1[i], m2[i]))
                return false;
        }
        return true;
    }

    // Test cases
    public static void main(String[] args) {
        RotateMatrix solution = new RotateMatrix();

        // Test case 1: 3x3 matrix
        int[][] matrix1 = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        int[][] expected1 = { { 7, 4, 1 }, { 8, 5, 2 }, { 9, 6, 3 } };

        System.out.println("=== Test Case 1: 3x3 Matrix ===");
        System.out.println("Input:");
        solution.printMatrix(matrix1);

        int[][] test1 = solution.copyMatrix(matrix1);
        solution.rotate(test1);

        System.out.println("\nOutput:");
        solution.printMatrix(test1);
        System.out.println("Expected: " + Arrays.deepToString(expected1));
        System.out.println("Correct: " + solution.matricesEqual(test1, expected1) + "\n");

        // Test case 2: 4x4 matrix
        int[][] matrix2 = { { 5, 1, 9, 11 }, { 2, 4, 8, 10 }, { 13, 3, 6, 7 }, { 15, 14, 12, 16 } };
        int[][] expected2 = { { 15, 13, 2, 5 }, { 14, 3, 4, 1 }, { 12, 6, 8, 9 }, { 16, 7, 10, 11 } };

        System.out.println("=== Test Case 2: 4x4 Matrix ===");
        System.out.println("Input:");
        solution.printMatrix(matrix2);

        int[][] test2 = solution.copyMatrix(matrix2);
        solution.rotate(test2);

        System.out.println("\nOutput:");
        solution.printMatrix(test2);
        System.out.println("Expected: " + Arrays.deepToString(expected2));
        System.out.println("Correct: " + solution.matricesEqual(test2, expected2) + "\n");

        // Test case 3: 1x1 matrix
        int[][] matrix3 = { { 1 } };
        System.out.println("=== Test Case 3: 1x1 Matrix ===");
        System.out.println("Input: " + Arrays.deepToString(matrix3));
        solution.rotate(matrix3);
        System.out.println("Output: " + Arrays.deepToString(matrix3));
        System.out.println("Expected: [[1]] (unchanged)\n");

        // Test case 4: 2x2 matrix
        int[][] matrix4 = { { 1, 2 }, { 3, 4 } };
        int[][] expected4 = { { 3, 1 }, { 4, 2 } };
        System.out.println("=== Test Case 4: 2x2 Matrix ===");
        System.out.println("Input: " + Arrays.deepToString(matrix4));
        solution.rotate(matrix4);
        System.out.println("Output: " + Arrays.deepToString(matrix4));
        System.out.println("Expected: " + Arrays.deepToString(expected4));
        System.out.println("Correct: " + solution.matricesEqual(matrix4, expected4) + "\n");

        // Compare all solutions with a test matrix
        System.out.println("=== Comparing All Solutions ===");
        int[][] original = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        int[][] test_transpose = solution.copyMatrix(original);
        int[][] test_layered = solution.copyMatrix(original);
        int[][] test_fourpoint = solution.copyMatrix(original);
        int[][] test_alternative = solution.copyMatrix(original);
        int[][] test_recursive = solution.copyMatrix(original);

        solution.rotate(test_transpose);
        solution.rotateLayered(test_layered);
        solution.rotateFourPoint(test_fourpoint);
        solution.rotateAlternative(test_alternative);
        solution.rotateRecursive(test_recursive);

        System.out.println("Transpose + Reverse: " + Arrays.deepToString(test_transpose));
        System.out.println("Layer-by-layer: " + Arrays.deepToString(test_layered));
        System.out.println("Four-point cycle: " + Arrays.deepToString(test_fourpoint));
        System.out.println("Alternative: " + Arrays.deepToString(test_alternative));
        System.out.println("Recursive: " + Arrays.deepToString(test_recursive));

        // Visual demonstration
        System.out.println("\n=== Visual Step-by-Step Demo ===");
        int[][] demo = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        solution.rotateVisual(demo);
    }

}
