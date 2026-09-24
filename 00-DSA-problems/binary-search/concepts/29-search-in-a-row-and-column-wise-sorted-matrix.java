/*
 * Problem Statement: You have been given a 2-D array 'mat' of size 'N x M'
 * where 'N' and 'M' denote the number of rows and columns, respectively. The
 * elements of each row and each column are sorted in non-decreasing order.
 * But, the first element of a row is not necessarily greater than the last
 * element of the previous row (if it exists).
 * You are given an integer ‘target’, and your task is to find if it exists in
 * the given 'mat' or not.
 */

import java.util.List;

class SearchInARowAndColumnWiseSortedMatrix {

    public boolean searchElement(List<List<Integer>> matrix, int target) {
        int rows = matrix.size(), cols = matrix.getFirst().size();
        int row = 0, col = cols - 1;
        while (row < rows && col >= 0) {
            int val = matrix.get(row).get(col);
            if (val == target) {
                return true;
            } else if (val < target) {
                row++;
            } else {
                col--;
            }
        }
        return false;
    }
}

class SearchInMatrix {

    // Solution 1: Optimal Approach - Start from top-right corner
    // Time: O(N + M), Space: O(1)
    public static boolean searchMatrix1(int[][] mat, int target) {
        if (mat == null || mat.length == 0 || mat[0].length == 0) {
            return false;
        }

        int n = mat.length;
        int m = mat[0].length;

        // Start from top-right corner
        int row = 0;
        int col = m - 1;

        while (row < n && col >= 0) {
            if (mat[row][col] == target) {
                return true;
            } else if (mat[row][col] > target) {
                // Current element is greater, move left
                col--;
            } else {
                // Current element is smaller, move down
                row++;
            }
        }

        return false;
    }

    // Solution 2: Alternative Optimal - Start from bottom-left corner
    // Time: O(N + M), Space: O(1)
    public static boolean searchMatrix2(int[][] mat, int target) {
        if (mat == null || mat.length == 0 || mat[0].length == 0) {
            return false;
        }

        int n = mat.length;
        int m = mat[0].length;

        // Start from bottom-left corner
        int row = n - 1;
        int col = 0;

        while (row >= 0 && col < m) {
            if (mat[row][col] == target) {
                return true;
            } else if (mat[row][col] > target) {
                // Current element is greater, move up
                row--;
            } else {
                // Current element is smaller, move right
                col++;
            }
        }

        return false;
    }

    // Solution 3: Binary Search on each row
    // Time: O(N * log M), Space: O(1)
    public static boolean searchMatrix3(int[][] mat, int target) {
        if (mat == null || mat.length == 0 || mat[0].length == 0) {
            return false;
        }

        for (int[] row : mat) {
            if (binarySearch(row, target)) {
                return true;
            }
        }

        return false;
    }

    private static boolean binarySearch(int[] arr, int target) {
        int left = 0, right = arr.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (arr[mid] == target) {
                return true;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }

    // Solution 4: Brute Force
    // Time: O(N * M), Space: O(1)
    public static boolean searchMatrix4(int[][] mat, int target) {
        if (mat == null || mat.length == 0 || mat[0].length == 0) {
            return false;
        }

        for (int i = 0; i < mat.length; i++) {
            for (int j = 0; j < mat[0].length; j++) {
                if (mat[i][j] == target) {
                    return true;
                }
            }
        }

        return false;
    }

    // Solution 5: Using divide and conquer (recursive)
    // Time: O(N + M), Space: O(log(N + M)) due to recursion
    public static boolean searchMatrix5(int[][] mat, int target) {
        if (mat == null || mat.length == 0 || mat[0].length == 0) {
            return false;
        }

        return searchRecursive(mat, target, 0, mat[0].length - 1, 0, mat.length - 1);
    }

    private static boolean searchRecursive(int[][] mat, int target, int left, int right, int top, int bottom) {
        if (left > right || top > bottom) {
            return false;
        }

        if (target < mat[top][left] || target > mat[bottom][right]) {
            return false;
        }

        int mid = left + (right - left) / 2;
        int row = top;

        // Find the row where mat[row][mid] <= target < mat[row+1][mid]
        while (row <= bottom && mat[row][mid] <= target) {
            if (mat[row][mid] == target) {
                return true;
            }
            row++;
        }

        // Search in left-bottom and right-top partitions
        return searchRecursive(mat, target, left, mid - 1, row, bottom) ||
                searchRecursive(mat, target, mid + 1, right, top, row - 1);
    }

    // Test method
    public static void main(String[] args) {
        // Test case 1
        int[][] mat1 = {
                { 1, 4, 7, 11 },
                { 2, 5, 8, 12 },
                { 3, 6, 9, 16 },
                { 10, 13, 14, 17 }
        };

        System.out.println("Matrix 1:");
        printMatrix(mat1);

        int target1 = 5;
        System.out.println("Target: " + target1);
        System.out.println("Solution 1 (Top-right): " + searchMatrix1(mat1, target1));
        System.out.println("Solution 2 (Bottom-left): " + searchMatrix2(mat1, target1));
        System.out.println("Solution 3 (Binary search): " + searchMatrix3(mat1, target1));
        System.out.println("Solution 4 (Brute force): " + searchMatrix4(mat1, target1));
        System.out.println("Solution 5 (Divide & conquer): " + searchMatrix5(mat1, target1));

        System.out.println();

        // Test case 2
        int[][] mat2 = {
                { 1, 3, 5 },
                { 2, 4, 6 },
                { 7, 8, 9 }
        };

        System.out.println("Matrix 2:");
        printMatrix(mat2);

        int target2 = 6;
        System.out.println("Target: " + target2);
        System.out.println("Found: " + searchMatrix1(mat2, target2));

        int target3 = 10;
        System.out.println("Target: " + target3);
        System.out.println("Found: " + searchMatrix1(mat2, target3));
    }

    private static void printMatrix(int[][] mat) {
        for (int[] row : mat) {
            System.out.println(Arrays.toString(row));
        }
    }
}

/*
 * Algorithm Explanations:
 * 
 * 1. OPTIMAL SOLUTION (Top-right corner approach):
 * - Start from top-right corner (0, m-1)
 * - If current element equals target, return true
 * - If current element > target, move left (eliminate column)
 * - If current element < target, move down (eliminate row)
 * - Time: O(N + M), Space: O(1)
 * 
 * 2. ALTERNATIVE OPTIMAL (Bottom-left corner approach):
 * - Start from bottom-left corner (n-1, 0)
 * - Similar logic but move up when current > target, right when current <
 * target
 * - Time: O(N + M), Space: O(1)
 * 
 * 3. BINARY SEARCH APPROACH:
 * - Apply binary search on each row
 * - Time: O(N * log M), Space: O(1)
 * 
 * 4. BRUTE FORCE:
 * - Check every element
 * - Time: O(N * M), Space: O(1)
 * 
 * 5. DIVIDE AND CONQUER:
 * - Recursively partition the matrix
 * - Time: O(N + M), Space: O(log(N + M))
 * 
 * Key Insights:
 * - The top-right and bottom-left approaches are most efficient
 * - They work because of the sorted property in both dimensions
 * - Each step eliminates either a complete row or column
 * - The algorithm never needs to backtrack
 */
