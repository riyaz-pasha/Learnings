import java.util.*;

class Search2DMatrix {

    /**
     * ============================================================
     * 🔥 APPROACH 1: Treat Matrix as Sorted 1D Array (BEST)
     * ============================================================
     *
     * WHY THIS WORKS?
     * ------------------------------------------------------------
     * Matrix behaves like:
     *   [1, 3, 5, 7, 9, 11 ...]
     *
     * Because:
     * - Row-wise sorted
     * - Row[i][0] > Row[i-1][last]
     *
     * So we can apply Binary Search directly.
     *
     * ------------------------------------------------------------
     * MAPPING TRICK (IMPORTANT)
     * ------------------------------------------------------------
     * Convert 1D index -> 2D index:
     *
     *   row = index / n
     *   col = index % n
     *
     * Example:
     *   n = 3
     *   index = 4 → row = 4/3 = 1, col = 4%3 = 1
     *
     * ------------------------------------------------------------
     * MONOTONIC FUNCTION:
     * ------------------------------------------------------------
     * f(mid) = matrix[mid/n][mid%n]
     *
     * It is strictly increasing → VALID for Binary Search
     *
     * ------------------------------------------------------------
     * GOAL:
     * ------------------------------------------------------------
     * Find if target exists → classic Binary Search
     *
     * ------------------------------------------------------------
     * INTERVIEW THINKING:
     * ------------------------------------------------------------
     * "Can I reduce 2D → 1D using index mapping?"
     *
     */
    public static boolean searchMatrix(int[][] matrix, int target) {

        int m = matrix.length;
        int n = matrix[0].length;

        int low = 0;
        int high = m * n - 1;

        // Explicit answer variable (as per your preference)
        boolean found = false;

        while (low <= high) {

            int mid = low + (high - low) / 2;

            // Convert 1D index to 2D
            int row = mid / n;
            int col = mid % n;

            int value = matrix[row][col];

            if (value == target) {
                found = true; // store answer
                break;
            }
            else if (value < target) {
                low = mid + 1;   // move right
            }
            else {
                high = mid - 1;  // move left
            }
        }

        return found;
    }

    public static void main(String[] args) {
        int[][] matrix = {
            {1, 3, 5},
            {7, 9, 11}
        };

        System.out.println(searchMatrix(matrix, 9)); // true
    }
}

class Search2DMatrixTwoPhase {

    /**
     * ============================================================
     * 🔥 APPROACH 2: Binary Search Row + Binary Search Column
     * ============================================================
     *
     * STEP 1: Find correct row
     *   Use first column to locate row
     *
     * STEP 2: Binary search in that row
     *
     */
    public static boolean searchMatrix(int[][] matrix, int target) {

        int m = matrix.length;
        int n = matrix[0].length;

        int low = 0, high = m - 1;
        int rowAnswer = -1;

        // Find row
        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (matrix[mid][0] <= target &&
                matrix[mid][n - 1] >= target) {

                rowAnswer = mid;
                break;
            }
            else if (matrix[mid][0] > target) {
                high = mid - 1;
            }
            else {
                low = mid + 1;
            }
        }

        if (rowAnswer == -1) return false;

        // Binary search in row
        int l = 0, h = n - 1;
        boolean found = false;

        while (l <= h) {
            int mid = l + (h - l) / 2;

            if (matrix[rowAnswer][mid] == target) {
                found = true;
                break;
            }
            else if (matrix[rowAnswer][mid] < target) {
                l = mid + 1;
            }
            else {
                h = mid - 1;
            }
        }

        return found;
    }
}

/**
 * Problem Statement:
 * Given an m x n 2D integer array `matrix`, determine whether the integer `target` 
 * exists in the matrix.
 * The matrix has the following properties:
 * 1. Each row is sorted in non-decreasing order.
 * 2. The first element of each row is strictly greater than the last element of the previous row.
 * 
 * Constraints:
 * - m == matrix.length
 * - n == matrix[i].length
 * - 1 <= m, n <= 100
 * - -10^4 <= matrix[i][j], target <= 10^4
 */
class Search2DMatrix2 {

    /**
     * SOLUTION 1: Flattened 1D Iterative Binary Search (Optimal)
     * 
     * Time Complexity: O(log(m * n))
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION:
     * Because the matrix rows are strictly sorted and sequential, the entire 2D matrix 
     * can be visualized as a single sorted 1D array.
     * 
     * Matrix (3x4):
     * [ 1,  3,  5,  7 ]
     * [10, 11, 16, 20 ]
     * [23, 30, 34, 60 ]
     * 
     * Flattened conceptually:
     * [ 1, 3, 5, 7, 10, 11, 16, 20, 23, 30, 34, 60 ]
     *   0  1  2  3   4   5   6   7   8   9  10  11  (Indices 0 to 11)
     * 
     * We can map a 1D index `i` to a 2D index `(row, col)` using:
     * row = i / n
     * col = i % n
     * 
     * Example: index 5 (value 11). row = 5 / 4 = 1. col = 5 % 4 = 1. matrix[1][1] = 11.
     */
    public static boolean searchMatrixIterative1D(int[][] matrix, int target) {
        int m = matrix.length;
        int n = matrix[0].length;
        
        int low = 0;
        int high = m * n - 1;
        boolean result = false; // Explicit result variable

        while (low <= high) {
            int mid = low + (high - low) / 2;
            
            // Map 1D index to 2D coordinates
            int row = mid / n;
            int col = mid % n;
            int midValue = matrix[row][col];

            if (midValue == target) {
                result = true; // Match found
                break;
            } else if (midValue < target) {
                low = mid + 1; // Search right half
            } else {
                high = mid - 1; // Search left half
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Two-Phase Binary Search (Optimal)
     * 
     * Time Complexity: O(log m + log n) = O(log(m * n))
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Phase 1: Binary search on the first column to find which row the target might reside in.
     * Phase 2: Standard binary search within that specific row.
     * 
     * This avoids math operations (/, %) inside the loop, which can be marginally faster 
     * at a hardware level, though the asymptotic complexity is identical to Solution 1.
     */
    public static boolean searchMatrixTwoPhase(int[][] matrix, int target) {
        int m = matrix.length;
        int n = matrix[0].length;

        // --- Phase 1: Find the target row ---
        int top = 0;
        int bottom = m - 1;
        int targetRow = -1; // Explicit result variable for the row

        while (top <= bottom) {
            int midRow = top + (bottom - top) / 2;

            // Check if the target is within the bounds of this row
            if (target >= matrix[midRow][0] && target <= matrix[midRow][n - 1]) {
                targetRow = midRow; // We found the row it belongs to
                break;
            } else if (target < matrix[midRow][0]) {
                bottom = midRow - 1; // Target is smaller than row minimum, go up
            } else {
                top = midRow + 1; // Target is larger than row maximum, go down
            }
        }

        // Target doesn't fit in any row's range
        if (targetRow == -1) {
            return false;
        }

        // --- Phase 2: Binary Search in the identified row ---
        int low = 0;
        int high = n - 1;
        boolean finalResult = false; // Explicit result variable for the final answer

        while (low <= high) {
            int midCol = low + (high - low) / 2;
            
            if (matrix[targetRow][midCol] == target) {
                finalResult = true;
                break;
            } else if (matrix[targetRow][midCol] < target) {
                low = midCol + 1;
            } else {
                high = midCol - 1;
            }
        }

        return finalResult;
    }

    /**
     * SOLUTION 3: Flattened 1D Recursive Binary Search (O(log(m * n)))
     * 
     * Time Complexity: O(log(m * n))
     * Space Complexity: O(log(m * n)) - Due to the recursive call stack.
     * 
     * EXPLANATION:
     * Transforms Solution 1 into a recursive format, passing the boolean result
     * back up the call chain.
     */
    public static boolean searchMatrixRecursiveWrapper(int[][] matrix, int target) {
        int m = matrix.length;
        int n = matrix[0].length;
        return searchMatrixRecursive(matrix, target, 0, m * n - 1, false);
    }

    private static boolean searchMatrixRecursive(int[][] matrix, int target, int low, int high, boolean currentResult) {
        boolean result = currentResult; // Explicit result variable
        
        if (low > high) {
            return result; // Base case: search space exhausted
        }

        int m = matrix.length;
        int n = matrix[0].length;
        int mid = low + (high - low) / 2;
        int midValue = matrix[mid / n][mid % n];

        if (midValue == target) {
            result = true; // Found the target
        } else if (midValue < target) {
            // Target is strictly greater, search right half
            result = searchMatrixRecursive(matrix, target, mid + 1, high, result);
        } else {
            // Target is strictly smaller, search left half
            result = searchMatrixRecursive(matrix, target, low, mid - 1, result);
        }

        return result;
    }

    /**
     * SOLUTION 4: Java Streams (Sub-optimal Time, Clean Syntax)
     * 
     * Time Complexity: O(m * n) - Worst case checks every element.
     * Space Complexity: O(1) overhead.
     * 
     * EXPLANATION:
     * This violates the O(log N) expectation for sorted searches but is highly 
     * robust and demonstrates the power of Java functional programming.
     * We convert the 2D array to a flattened IntStream and check if ANY match the target.
     */
    public static boolean searchMatrixStream(int[][] matrix, int target) {
        return Arrays.stream(matrix)          // Stream of int[] (rows)
                .flatMapToInt(Arrays::stream) // Flatten each row into a single IntStream
                .anyMatch(val -> val == target); // Returns true if target exists
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * A Java Record that maps out the input matrix, target, and expected boolean output.
     */
    public record TestCase(int[][] matrix, int target, boolean expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on problem description and edge cases
        TestCase[] testCases = {
            new TestCase(new int[][]{
                {1, 3, 5, 7},
                {10, 11, 16, 20},
                {23, 30, 34, 60}
            }, 3, true),   // Target exists in the first row
            
            new TestCase(new int[][]{
                {1, 3, 5, 7},
                {10, 11, 16, 20},
                {23, 30, 34, 60}
            }, 13, false), // Target falls in a gap between elements in row 2
            
            new TestCase(new int[][]{
                {1, 3, 5, 7},
                {10, 11, 16, 20},
                {23, 30, 34, 60}
            }, 60, true),  // Target is the very last element
            
            new TestCase(new int[][]{
                {1}
            }, 1, true),   // Single element matrix, target exists
            
            new TestCase(new int[][]{
                {1}
            }, 0, false)   // Single element matrix, target does not exist
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            boolean resIterative = searchMatrixIterative1D(tc.matrix(), tc.target());
            boolean resTwoPhase  = searchMatrixTwoPhase(tc.matrix(), tc.target());
            boolean resRecursive = searchMatrixRecursiveWrapper(tc.matrix(), tc.target());
            boolean resStream    = searchMatrixStream(tc.matrix(), tc.target());

            boolean passed = (resIterative == tc.expected()) &&
                             (resTwoPhase == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resStream == tc.expected());

            System.out.printf("Test %d | Target: %-2d | Expected: %-5b | Passed: %b%n",
                    i + 1, tc.target(), tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iterative: %b, TwoPhase: %b, Recursive: %b, Stream: %b%n",
                        resIterative, resTwoPhase, resRecursive, resStream);
            }
        }
    }
}
