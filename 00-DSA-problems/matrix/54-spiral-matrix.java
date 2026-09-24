import java.util.*;
/*
 * Given an m x n matrix, return all elements of the matrix in spiral order.
 * 
 * Example 1:
 * Input: matrix = [[1,2,3],[4,5,6],[7,8,9]]
 * Output: [1,2,3,6,9,8,7,4,5]
 * 
 * Example 2:
 * Input: matrix = [[1,2,3,4],[5,6,7,8],[9,10,11,12]]
 * Output: [1,2,3,4,8,12,11,10,9,5,6,7]
 */

class SpiralMatrix {

    /**
     * Boundary-based Solution (Most Intuitive)
     * Time Complexity: O(m * n)
     * Space Complexity: O(1) - excluding result array
     */
    public List<Integer> spiralOrder(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return result;
        }

        int top = 0, bottom = matrix.length - 1;
        int left = 0, right = matrix[0].length - 1;

        while (top <= bottom && left <= right) {
            // Traverse right along top row
            for (int col = left; col <= right; col++) {
                result.add(matrix[top][col]);
            }
            top++; // Move top boundary down

            // Traverse down along right column
            for (int row = top; row <= bottom; row++) {
                result.add(matrix[row][right]);
            }
            right--; // Move right boundary left

            // Traverse left along bottom row (if we still have rows)
            if (top <= bottom) {
                for (int col = right; col >= left; col--) {
                    result.add(matrix[bottom][col]);
                }
                bottom--; // Move bottom boundary up
            }

            // Traverse up along left column (if we still have columns)
            if (left <= right) {
                for (int row = bottom; row >= top; row--) {
                    result.add(matrix[row][left]);
                }
                left++; // Move left boundary right
            }
        }

        return result;
    }

    /**
     * Direction-based Solution
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n) - for visited array
     */
    public List<Integer> spiralOrderDirection(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return result;
        }

        int m = matrix.length, n = matrix[0].length;
        boolean[][] visited = new boolean[m][n];

        // Direction vectors: right, down, left, up
        int[] dr = { 0, 1, 0, -1 };
        int[] dc = { 1, 0, -1, 0 };

        int r = 0, c = 0, di = 0; // Start at (0,0) facing right

        for (int i = 0; i < m * n; i++) {
            result.add(matrix[r][c]);
            visited[r][c] = true;

            // Calculate next position
            int nextR = r + dr[di];
            int nextC = c + dc[di];

            // Check if we need to turn (hit boundary or visited cell)
            if (nextR < 0 || nextR >= m || nextC < 0 || nextC >= n || visited[nextR][nextC]) {
                di = (di + 1) % 4; // Turn clockwise
                nextR = r + dr[di];
                nextC = c + dc[di];
            }

            r = nextR;
            c = nextC;
        }

        return result;
    }

    /**
     * Layer-by-Layer Solution
     * Time Complexity: O(m * n)
     * Space Complexity: O(1) - excluding result array
     */
    public List<Integer> spiralOrderLayers(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return result;
        }

        int m = matrix.length, n = matrix[0].length;
        int layers = Math.min((m + 1) / 2, (n + 1) / 2);

        for (int layer = 0; layer < layers; layer++) {
            // Define boundaries for current layer
            int firstRow = layer;
            int lastRow = m - 1 - layer;
            int firstCol = layer;
            int lastCol = n - 1 - layer;

            // Single row
            if (firstRow == lastRow) {
                for (int col = firstCol; col <= lastCol; col++) {
                    result.add(matrix[firstRow][col]);
                }
            }
            // Single column
            else if (firstCol == lastCol) {
                for (int row = firstRow; row <= lastRow; row++) {
                    result.add(matrix[row][firstCol]);
                }
            }
            // Full layer
            else {
                // Top row (left to right)
                for (int col = firstCol; col < lastCol; col++) {
                    result.add(matrix[firstRow][col]);
                }

                // Right column (top to bottom)
                for (int row = firstRow; row < lastRow; row++) {
                    result.add(matrix[row][lastCol]);
                }

                // Bottom row (right to left)
                for (int col = lastCol; col > firstCol; col--) {
                    result.add(matrix[lastRow][col]);
                }

                // Left column (bottom to top)
                for (int row = lastRow; row > firstRow; row--) {
                    result.add(matrix[row][firstCol]);
                }
            }
        }

        return result;
    }

    /**
     * Recursive Solution
     * Time Complexity: O(m * n)
     * Space Complexity: O(min(m, n)) - recursion stack
     */
    public List<Integer> spiralOrderRecursive(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return result;
        }

        spiralHelper(matrix, 0, matrix.length - 1, 0, matrix[0].length - 1, result);
        return result;
    }

    private void spiralHelper(int[][] matrix, int top, int bottom, int left, int right, List<Integer> result) {
        if (top > bottom || left > right) {
            return;
        }

        // Traverse top row
        for (int col = left; col <= right; col++) {
            result.add(matrix[top][col]);
        }

        // Traverse right column
        for (int row = top + 1; row <= bottom; row++) {
            result.add(matrix[row][right]);
        }

        // Traverse bottom row (if exists)
        if (top < bottom) {
            for (int col = right - 1; col >= left; col--) {
                result.add(matrix[bottom][col]);
            }
        }

        // Traverse left column (if exists)
        if (left < right) {
            for (int row = bottom - 1; row > top; row--) {
                result.add(matrix[row][left]);
            }
        }

        // Recurse for inner matrix
        spiralHelper(matrix, top + 1, bottom - 1, left + 1, right - 1, result);
    }

    /**
     * Optimized Single Pass Solution
     * Time Complexity: O(m * n)
     * Space Complexity: O(1) - excluding result array
     */
    public List<Integer> spiralOrderOptimized(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return result;
        }

        int m = matrix.length, n = matrix[0].length;
        int totalElements = m * n;
        int top = 0, bottom = m - 1, left = 0, right = n - 1;

        while (result.size() < totalElements) {
            // Right
            for (int col = left; col <= right && result.size() < totalElements; col++) {
                result.add(matrix[top][col]);
            }
            top++;

            // Down
            for (int row = top; row <= bottom && result.size() < totalElements; row++) {
                result.add(matrix[row][right]);
            }
            right--;

            // Left
            for (int col = right; col >= left && result.size() < totalElements; col--) {
                result.add(matrix[bottom][col]);
            }
            bottom--;

            // Up
            for (int row = bottom; row >= top && result.size() < totalElements; row--) {
                result.add(matrix[row][left]);
            }
            left++;
        }

        return result;
    }

    // Helper method to print matrix
    private void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }

    // Test cases
    public static void main(String[] args) {
        SpiralMatrix solution = new SpiralMatrix();

        // Test case 1: 3x3 matrix
        int[][] matrix1 = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        System.out.println("Test case 1 - 3x3 matrix:");
        solution.printMatrix(matrix1);
        System.out.println("Spiral order: " + solution.spiralOrder(matrix1));
        System.out.println("Expected: [1,2,3,6,9,8,7,4,5]\n");

        // Test case 2: 3x4 matrix
        int[][] matrix2 = { { 1, 2, 3, 4 }, { 5, 6, 7, 8 }, { 9, 10, 11, 12 } };
        System.out.println("Test case 2 - 3x4 matrix:");
        solution.printMatrix(matrix2);
        System.out.println("Spiral order: " + solution.spiralOrder(matrix2));
        System.out.println("Expected: [1,2,3,4,8,12,11,10,9,5,6,7]\n");

        // Test case 3: Single row
        int[][] matrix3 = { { 1, 2, 3, 4, 5 } };
        System.out.println("Test case 3 - Single row:");
        solution.printMatrix(matrix3);
        System.out.println("Spiral order: " + solution.spiralOrder(matrix3));
        System.out.println("Expected: [1,2,3,4,5]\n");

        // Test case 4: Single column
        int[][] matrix4 = { { 1 }, { 2 }, { 3 }, { 4 } };
        System.out.println("Test case 4 - Single column:");
        solution.printMatrix(matrix4);
        System.out.println("Spiral order: " + solution.spiralOrder(matrix4));
        System.out.println("Expected: [1,2,3,4]\n");

        // Test case 5: Single element
        int[][] matrix5 = { { 42 } };
        System.out.println("Test case 5 - Single element:");
        solution.printMatrix(matrix5);
        System.out.println("Spiral order: " + solution.spiralOrder(matrix5));
        System.out.println("Expected: [42]\n");

        // Compare all solutions with matrix1
        System.out.println("Comparing all solutions with 3x3 matrix:");
        System.out.println("Boundary-based: " + solution.spiralOrder(matrix1));
        System.out.println("Direction-based: " + solution.spiralOrderDirection(matrix1));
        System.out.println("Layer-by-layer: " + solution.spiralOrderLayers(matrix1));
        System.out.println("Recursive: " + solution.spiralOrderRecursive(matrix1));
        System.out.println("Optimized: " + solution.spiralOrderOptimized(matrix1));
    }

}
