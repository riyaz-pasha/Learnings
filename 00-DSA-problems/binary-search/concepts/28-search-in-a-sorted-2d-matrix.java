/*
 * Problem Statement: You have been given a 2-D array 'mat' of size 'N x M'
 * where 'N' and 'M' denote the number of rows and columns, respectively. The
 * elements of each row are sorted in non-decreasing order. Moreover, the first
 * element of a row is greater than the last element of the previous row (if it
 * exists). You are given an integer ‘target’, and your task is to find if it
 * exists in the given 'mat' or not.
 * 
 * Examples
 * 
 * Example 1:
 * Input Format: N = 3, M = 4, target = 8,
 * mat[] =
 * 1 2 3 4
 * 5 6 7 8
 * 9 10 11 12
 * Result: true
 * Explanation: The ‘target’ = 8 exists in the 'mat' at index (1, 3).
 * 
 * Example 2:
 * Input Format: N = 3, M = 3, target = 78,
 * mat[] =
 * 1 2 4
 * 6 7 8
 * 9 10 34
 * Result: false
 * Explanation: The ‘target' = 78 does not exist in the 'mat'. Therefore in the
 * output, we see 'false'.
 */

import java.util.List;

class SearchInSorted2dMatrix {

    public boolean searchMatrix(List<List<Integer>> matrix, int target) {
        int rows = matrix.size(), cols = matrix.getFirst().size();
        int low = 0, high = rows * cols;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int midValue = matrix.get(mid / rows).get(mid % rows);
            if (midValue == target) {
                return true;
            } else if (midValue > target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return false;
    }

}

class Search2DMatrix {

    /**
     * BRUTE FORCE APPROACH - Linear Search
     * Algorithm:
     * 1. Iterate through each element in the matrix
     * 2. Check if current element equals target
     * 3. Return true if found, false if not found after checking all elements
     * 
     * Time Complexity: O(N * M) - check every element
     * Space Complexity: O(1) - only using variables
     */
    public static boolean searchMatrixBruteForce(int[][] matrix, int target) {
        int n = matrix.length;
        int m = matrix[0].length;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (matrix[i][j] == target) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * BETTER APPROACH - Binary Search on Each Row
     * Algorithm:
     * 1. For each row, check if target can be in that row
     * 2. Target can be in row i if: matrix[i][0] <= target <= matrix[i][m-1]
     * 3. If possible, perform binary search on that row
     * 4. Return true if found in any row
     * 
     * Time Complexity: O(N + log M) in best case, O(N * log M) in worst case
     * Space Complexity: O(1) - only using variables
     */
    public static boolean searchMatrixBetter(int[][] matrix, int target) {
        int n = matrix.length;
        int m = matrix[0].length;

        for (int i = 0; i < n; i++) {
            // Check if target can be in this row
            if (matrix[i][0] <= target && target <= matrix[i][m - 1]) {
                // Perform binary search on this row
                if (binarySearchRow(matrix[i], target)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Helper method for binary search in a single row
     */
    private static boolean binarySearchRow(int[] row, int target) {
        int left = 0, right = row.length - 1;

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

    /**
     * OPTIMAL APPROACH 1 - Treat Matrix as 1D Array
     * Algorithm:
     * 1. Key insight: Since rows are sorted and first element of each row > last
     * element of previous row,
     * the entire matrix can be viewed as a single sorted array
     * 2. Use binary search on this conceptual 1D array
     * 3. Convert 1D index to 2D coordinates: row = index / m, col = index % m
     * 4. Convert 2D coordinates to 1D index: index = row * m + col
     * 
     * Time Complexity: O(log(N * M)) - single binary search
     * Space Complexity: O(1) - only using variables
     */
    public static boolean searchMatrixOptimal1(int[][] matrix, int target) {
        int n = matrix.length;
        int m = matrix[0].length;

        int left = 0;
        int right = n * m - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            // Convert 1D index to 2D coordinates
            int row = mid / m;
            int col = mid % m;
            int midElement = matrix[row][col];

            if (midElement == target) {
                return true;
            } else if (midElement < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }

    /**
     * OPTIMAL APPROACH 2 - Two-Step Binary Search
     * Algorithm:
     * 1. First, use binary search to find the correct row
     * 2. Then, use binary search within that row to find the target
     * 3. Row selection: find row where matrix[row][0] <= target <= matrix[row][m-1]
     * 
     * Time Complexity: O(log N + log M) - two separate binary searches
     * Space Complexity: O(1) - only using variables
     */
    public static boolean searchMatrixOptimal2(int[][] matrix, int target) {
        int n = matrix.length;
        int m = matrix[0].length;

        // Step 1: Find the correct row using binary search
        int targetRow = findTargetRow(matrix, target);

        if (targetRow == -1) {
            return false; // Target cannot be in any row
        }

        // Step 2: Binary search within the found row
        return binarySearchRow(matrix[targetRow], target);
    }

    /**
     * Helper method to find which row might contain the target
     */
    private static int findTargetRow(int[][] matrix, int target) {
        int n = matrix.length;
        int m = matrix[0].length;

        int left = 0, right = n - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (matrix[mid][0] <= target && target <= matrix[mid][m - 1]) {
                return mid; // Found the row that might contain target
            } else if (matrix[mid][0] > target) {
                right = mid - 1; // Target is in upper rows
            } else {
                left = mid + 1; // Target is in lower rows
            }
        }

        return -1; // Target cannot be in any row
    }

    /**
     * OPTIMAL APPROACH 3 - Staircase Search (Alternative for different constraint)
     * Note: This approach works for matrices where rows and columns are sorted,
     * but not necessarily with the constraint that first element of row > last
     * element of previous row.
     * Including it for completeness and comparison.
     * 
     * Algorithm:
     * 1. Start from top-right corner (or bottom-left)
     * 2. If current element == target, return true
     * 3. If current element > target, move left
     * 4. If current element < target, move down
     * 
     * Time Complexity: O(N + M) - at most N+M moves
     * Space Complexity: O(1) - only using variables
     */
    public static boolean searchMatrixStaircase(int[][] matrix, int target) {
        int n = matrix.length;
        int m = matrix[0].length;

        // Start from top-right corner
        int row = 0, col = m - 1;

        while (row < n && col >= 0) {
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

    // Helper method to demonstrate the 1D array concept
    public static void demonstrateMatrixAs1D(int[][] matrix) {
        int n = matrix.length;
        int m = matrix[0].length;

        System.out.println("=== Matrix as 1D Array Demonstration ===");
        System.out.println("Original Matrix:");
        printMatrix(matrix);

        System.out.println("Conceptual 1D Array (index -> value):");
        for (int i = 0; i < n * m; i++) {
            int row = i / m;
            int col = i % m;
            System.out.printf("Index %2d -> matrix[%d][%d] = %2d\n",
                    i, row, col, matrix[row][col]);
        }

        System.out.println("\nConversion Formulas:");
        System.out.println("1D index to 2D: row = index / m, col = index % m");
        System.out.println("2D coordinates to 1D: index = row * m + col");
        System.out.println();
    }

    // Helper method to demonstrate binary search steps
    public static void demonstrateBinarySearch(int[][] matrix, int target) {
        System.out.println("=== Binary Search Steps Demonstration ===");
        System.out.printf("Searching for target: %d\n", target);

        int n = matrix.length;
        int m = matrix[0].length;
        int left = 0, right = n * m - 1;
        int step = 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int row = mid / m;
            int col = mid % m;
            int midElement = matrix[row][col];

            System.out.printf("Step %d: left=%d, right=%d, mid=%d\n", step++, left, right, mid);
            System.out.printf("        mid index %d -> matrix[%d][%d] = %d\n",
                    mid, row, col, midElement);

            if (midElement == target) {
                System.out.println("        Target found!");
                return;
            } else if (midElement < target) {
                System.out.println("        midElement < target, search right half");
                left = mid + 1;
            } else {
                System.out.println("        midElement > target, search left half");
                right = mid - 1;
            }
            System.out.println();
        }

        System.out.println("Target not found after binary search");
        System.out.println();
    }

    // Helper method to print matrix
    private static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                System.out.printf("%3d ", matrix[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("=== Search in 2D Matrix ===\n");

        // Test case 1: Target exists
        int[][] matrix1 = {
                { 1, 2, 3, 4 },
                { 5, 6, 7, 8 },
                { 9, 10, 11, 12 }
        };
        int target1 = 8;

        System.out.println("Test Case 1 (Target exists):");
        printMatrix(matrix1);
        System.out.println("Target: " + target1);
        System.out.println("Expected: true");

        boolean result1_brute = searchMatrixBruteForce(matrix1, target1);
        boolean result1_better = searchMatrixBetter(matrix1, target1);
        boolean result1_optimal1 = searchMatrixOptimal1(matrix1, target1);
        boolean result1_optimal2 = searchMatrixOptimal2(matrix1, target1);
        boolean result1_staircase = searchMatrixStaircase(matrix1, target1);

        System.out.println("Brute Force: " + result1_brute);
        System.out.println("Better (Row-wise Binary Search): " + result1_better);
        System.out.println("Optimal 1 (1D Binary Search): " + result1_optimal1);
        System.out.println("Optimal 2 (Two-step Binary Search): " + result1_optimal2);
        System.out.println("Staircase Search: " + result1_staircase);
        System.out.println();

        demonstrateMatrixAs1D(matrix1);
        demonstrateBinarySearch(matrix1, target1);

        // Test case 2: Target doesn't exist
        int[][] matrix2 = {
                { 1, 2, 4 },
                { 6, 7, 8 },
                { 9, 10, 34 }
        };
        int target2 = 78;

        System.out.println("Test Case 2 (Target doesn't exist):");
        printMatrix(matrix2);
        System.out.println("Target: " + target2);
        System.out.println("Expected: false");

        boolean result2_optimal1 = searchMatrixOptimal1(matrix2, target2);
        System.out.println("Optimal 1 (1D Binary Search): " + result2_optimal1);
        System.out.println();

        // Test case 3: Edge cases
        int[][] matrix3 = { { 1 } };
        int target3a = 1;
        int target3b = 2;

        System.out.println("Test Case 3 (Single element matrix):");
        printMatrix(matrix3);

        System.out.println("Target: " + target3a + " -> " + searchMatrixOptimal1(matrix3, target3a));
        System.out.println("Target: " + target3b + " -> " + searchMatrixOptimal1(matrix3, target3b));
        System.out.println();

        // Performance comparison
        System.out.println("=== Time Complexity Analysis ===");
        System.out.println("Brute Force: O(N * M) - check every element");
        System.out.println("Better: O(N + log M) best case, O(N * log M) worst case");
        System.out.println("Optimal 1 (1D Binary Search): O(log(N * M)) - most efficient");
        System.out.println("Optimal 2 (Two-step): O(log N + log M) - equivalent to log(N * M)");
        System.out.println("Staircase: O(N + M) - good for different constraints");
        System.out.println();

        System.out.println("=== Key Insights ===");
        System.out.println("1. The constraint 'first element of row > last element of previous row'");
        System.out.println("   makes the entire matrix equivalent to a sorted 1D array");
        System.out.println("2. We can use binary search with index conversion:");
        System.out.println("   - 1D to 2D: row = index / m, col = index % m");
        System.out.println("   - 2D to 1D: index = row * m + col");
        System.out.println("3. This gives us O(log(N*M)) time complexity");
        System.out.println("4. Alternative: two separate binary searches (row finding + element finding)");
        System.out.println("5. Staircase search works but is less efficient for this specific constraint");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM UNDERSTANDING:
 * - Given: N x M matrix where each row is sorted in non-decreasing order
 * - Key constraint: First element of each row > last element of previous row
 * - This constraint makes the entire matrix equivalent to a single sorted array
 * - Task: Find if target exists in the matrix
 * 
 * BRUTE FORCE APPROACH:
 * - Linear search through every element
 * - Simple but inefficient
 * - Time: O(N*M), Space: O(1)
 * 
 * BETTER APPROACH:
 * - For each row, check if target can be in that row
 * - If possible, do binary search on that row
 * - Time: O(N + log M) best case, O(N * log M) worst case
 * - Space: O(1)
 * 
 * OPTIMAL APPROACH 1 (1D Binary Search):
 * - Key insight: The matrix constraint makes it equivalent to a sorted 1D array
 * - Use single binary search with index conversion
 * - Conversion formulas:
 * 1D index to 2D: row = index / m, col = index % m
 * 2D coordinates to 1D: index = row * m + col
 * - Time: O(log(N*M)), Space: O(1)
 * 
 * OPTIMAL APPROACH 2 (Two-step Binary Search):
 * - Step 1: Binary search to find correct row
 * - Step 2: Binary search within that row
 * - Time: O(log N + log M) = O(log(N*M)), Space: O(1)
 * 
 * STAIRCASE SEARCH:
 * - Start from top-right corner
 * - Move left if current > target, down if current < target
 * - Works for general sorted matrix problem
 * - Time: O(N+M), Space: O(1)
 * - Less efficient for this specific constraint
 * 
 * KEY INSIGHT:
 * The constraint "first element of row > last element of previous row" is
 * crucial.
 * It transforms the 2D problem into a 1D binary search problem.
 * 
 * Example: Matrix [1,2,3,4; 5,6,7,8; 9,10,11,12] becomes array
 * [1,2,3,4,5,6,7,8,9,10,11,12]
 * 
 * INDEX CONVERSION EXAMPLES:
 * - Index 5 -> row = 5/4 = 1, col = 5%4 = 1 -> matrix[1][1] = 6
 * - matrix[2][1] -> index = 2*4 + 1 = 9 -> array[9] = 10
 * 
 * The 1D binary search approach is optimal and most elegant for this problem!
 */
