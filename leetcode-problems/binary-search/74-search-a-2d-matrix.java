/*
 * You are given an m x n integer matrix matrix with the following two
 * properties:
 * 
 * Each row is sorted in non-decreasing order.
 * The first integer of each row is greater than the last integer of the
 * previous row.
 * Given an integer target, return true if target is in matrix or false
 * otherwise.
 * 
 * You must write a solution in O(log(m * n)) time complexity.
 * 
 * Example 1:
 * Input: matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 3
 * Output: true
 * 
 * Example 2:
 * Input: matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 13
 * Output: false
 */

class Solution {

    // APPROACH 1: Treat as 1D Array (Optimal) ⭐
    // Time: O(log(m*n)), Space: O(1)
    public boolean searchMatrix(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        int m = matrix.length;
        int n = matrix[0].length;

        int left = 0;
        int right = m * n - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int midValue = matrix[mid / n][mid % n];

            if (midValue == target) {
                return true;
            } else if (midValue < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }

}

// APPROACH 2: Two Binary Searches
// Time: O(log m + log n) = O(log(m*n)), Space: O(1)
class SolutionTwoBinarySearches {

    public boolean searchMatrix(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        int m = matrix.length;
        int n = matrix[0].length;

        // First binary search: find the correct row
        int row = findRow(matrix, target);
        if (row == -1)
            return false;

        // Second binary search: search within the row
        return binarySearchInRow(matrix[row], target);
    }

    private int findRow(int[][] matrix, int target) {
        int left = 0;
        int right = matrix.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            // Check if target is in this row
            if (matrix[mid][0] <= target && target <= matrix[mid][matrix[mid].length - 1]) {
                return mid;
            } else if (matrix[mid][0] > target) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return -1; // Row not found
    }

    private boolean binarySearchInRow(int[] row, int target) {
        int left = 0;
        int right = row.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (row[mid] == target) {
                return true;
            } else if (row[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }

}

// APPROACH 3: Start from Top-Right Corner
// Time: O(m + n), Space: O(1)
// Note: This doesn't meet O(log(m*n)) requirement but is elegant
class SolutionTopRight {

    public boolean searchMatrix(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        int row = 0;
        int col = matrix[0].length - 1;

        while (row < matrix.length && col >= 0) {
            if (matrix[row][col] == target) {
                return true;
            } else if (matrix[row][col] > target) {
                col--; // Move left
            } else {
                row++; // Move down
            }
        }

        return false;
    }

}

// APPROACH 4: Recursive Binary Search
// Time: O(log(m*n)), Space: O(log(m*n)) due to recursion
class SolutionRecursive {

    public boolean searchMatrix(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        int m = matrix.length;
        int n = matrix[0].length;

        return searchRecursive(matrix, target, 0, m * n - 1, n);
    }

    private boolean searchRecursive(int[][] matrix, int target, int left, int right, int cols) {
        if (left > right) {
            return false;
        }

        int mid = left + (right - left) / 2;
        int midValue = matrix[mid / cols][mid % cols];

        if (midValue == target) {
            return true;
        } else if (midValue < target) {
            return searchRecursive(matrix, target, mid + 1, right, cols);
        } else {
            return searchRecursive(matrix, target, left, mid - 1, cols);
        }
    }

}

// APPROACH 5: Binary Search with Row Detection
// Time: O(log(m*n)), Space: O(1)
class SolutionRowDetection {

    public boolean searchMatrix(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        int m = matrix.length;
        int n = matrix[0].length;

        // Binary search to find the target row
        int targetRow = -1;
        int left = 0, right = m - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (matrix[mid][0] <= target && target <= matrix[mid][n - 1]) {
                targetRow = mid;
                break;
            } else if (matrix[mid][0] > target) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        if (targetRow == -1)
            return false;

        // Binary search within the target row
        left = 0;
        right = n - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (matrix[targetRow][mid] == target) {
                return true;
            } else if (matrix[targetRow][mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }

}

// APPROACH 6: Linear Search (for comparison - not optimal)
// Time: O(m*n), Space: O(1)
class SolutionLinear {

    public boolean searchMatrix(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] == target) {
                    return true;
                }
                // Early termination since rows are sorted
                if (matrix[i][j] > target) {
                    break;
                }
            }
        }

        return false;
    }
}

// Test class to demonstrate all approaches
class TestSearch2DMatrix {
    public static void main(String[] args) {
        Solution solution = new Solution();
        SolutionTwoBinarySearches twoBinary = new SolutionTwoBinarySearches();
        SolutionTopRight topRight = new SolutionTopRight();

        // Test cases
        int[][][] matrices = {
                { { 1, 3, 5, 7 }, { 10, 11, 16, 20 }, { 23, 30, 34, 60 } },
                { { 1, 3, 5, 7 }, { 10, 11, 16, 20 }, { 23, 30, 34, 60 } },
                { { 1, 4, 7, 11 }, { 2, 5, 8, 12 }, { 3, 6, 9, 16 } }, // Different structure
                { { 1 } },
                { { 1, 1 } } // Edge case with duplicates
        };

        int[] targets = { 3, 13, 5, 1, 1 };
        boolean[] expected = { true, false, true, true, true };

        for (int i = 0; i < matrices.length; i++) {
            int[][] matrix = matrices[i];
            int target = targets[i];

            System.out.println("\nTest Case " + (i + 1) + ":");
            System.out.println("Matrix:");
            printMatrix(matrix);
            System.out.println("Target: " + target);
            System.out.println("Expected: " + expected[i]);

            boolean result1 = solution.searchMatrix(matrix, target);
            boolean result2 = twoBinary.searchMatrix(matrix, target);
            boolean result3 = topRight.searchMatrix(matrix, target);

            System.out.println("1D Array approach: " + result1);
            System.out.println("Two Binary Searches: " + result2);
            System.out.println("Top-Right approach: " + result3);

            // Verify results
            if (result1 != expected[i] || result2 != expected[i] || result3 != expected[i]) {
                System.out.println("⚠️  Some results don't match expected!");
            }
        }

        // Demonstrate the coordinate conversion
        demonstrateCoordinateConversion();

        // Performance comparison
        performanceTest();
    }

    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            System.out.println(java.util.Arrays.toString(row));
        }
    }

    private static void demonstrateCoordinateConversion() {
        System.out.println("\n--- Coordinate Conversion Demonstration ---");
        int m = 3, n = 4; // 3x4 matrix

        System.out.println("Matrix dimensions: " + m + " x " + n);
        System.out.println("Total elements: " + (m * n));
        System.out.println("\n1D Index -> 2D Coordinates:");

        for (int i = 0; i < m * n; i++) {
            int row = i / n;
            int col = i % n;
            System.out.println("Index " + i + " -> (" + row + ", " + col + ")");
        }

        System.out.println("\n2D Coordinates -> 1D Index:");
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int index = i * n + j;
                System.out.println("(" + i + ", " + j + ") -> Index " + index);
            }
        }
    }

    private static void performanceTest() {
        System.out.println("\n--- Performance Test ---");

        // Create large matrix
        int m = 1000, n = 1000;
        int[][] largeMatrix = new int[m][n];

        int value = 1;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                largeMatrix[i][j] = value++;
            }
        }

        Solution binarySearch = new Solution();
        SolutionLinear linearSearch = new SolutionLinear();

        int target = 500000; // Middle element

        // Test binary search
        long start = System.nanoTime();
        boolean binaryResult = binarySearch.searchMatrix(largeMatrix, target);
        long binaryTime = System.nanoTime() - start;

        // Test linear search
        start = System.nanoTime();
        boolean linearResult = linearSearch.searchMatrix(largeMatrix, target);
        long linearTime = System.nanoTime() - start;

        System.out.println("Matrix size: " + m + " x " + n + " = " + (m * n) + " elements");
        System.out.println("Target: " + target);
        System.out.println("Binary Search: " + binaryResult + " (Time: " + binaryTime / 1000 + " μs)");
        System.out.println("Linear Search: " + linearResult + " (Time: " + linearTime / 1000 + " μs)");

        if (binaryTime > 0) {
            System.out.println("Speed improvement: " + (linearTime / binaryTime) + "x faster");
        }
    }

}

// Utility class for matrix operations
class MatrixUtils {

    // Convert 1D index to 2D coordinates
    public static int[] indexToCoordinates(int index, int cols) {
        return new int[] { index / cols, index % cols };
    }

    // Convert 2D coordinates to 1D index
    public static int coordinatesToIndex(int row, int col, int cols) {
        return row * cols + col;
    }

    // Check if matrix satisfies the problem constraints
    public static boolean isValidMatrix(int[][] matrix) {
        if (matrix == null || matrix.length == 0)
            return true;

        int m = matrix.length;
        int n = matrix[0].length;

        // Check if each row is sorted
        for (int i = 0; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (matrix[i][j] < matrix[i][j - 1]) {
                    return false;
                }
            }
        }

        // Check if first element of each row > last element of previous row
        for (int i = 1; i < m; i++) {
            if (matrix[i][0] <= matrix[i - 1][n - 1]) {
                return false;
            }
        }

        return true;
    }

    // Generate a valid test matrix
    public static int[][] generateTestMatrix(int m, int n) {
        int[][] matrix = new int[m][n];
        int value = 1;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = value;
                value += (int) (Math.random() * 3) + 1; // Increment by 1-3
            }
        }

        return matrix;
    }

}
