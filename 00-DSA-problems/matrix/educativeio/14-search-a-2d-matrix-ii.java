/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n integer matrix where:
 * 1. Every row is sorted in ascending order from left to right.
 * 2. Every column is sorted in ascending order from top to bottom.
 * 
 * Our task is to determine if a specific 'target' integer exists in this matrix.
 * We just need to return true if it is found, and false otherwise.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the matrix contain duplicate values? 
 *    (Yes. Our algorithm should just return true upon hitting the first match).
 * 2. What should we return if the target is not found? 
 *    (Return false).
 * 3. Can the target be smaller than the top-left element or larger than the bottom-right?
 *    (Yes, those are great early-exit boundary conditions).
 * 4. Can the matrix be a single row or single column?
 *    (Yes, constraints say m, n >= 1).
 * 5. Can I modify the matrix during the search?
 *    (No, reading is sufficient, and we should aim for O(1) auxiliary space).
 * 6. Is this matrix strictly flattened-sorted? (i.e., is the first element of row 2 
 *    guaranteed to be larger than the last element of row 1?)
 *    (No! That is a different problem. Here, rows and columns are sorted independently, 
 *    so elements can overlap between rows).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. Target Out of Bounds: Target is 0, but matrix[0][0] is 5. Target cannot exist.
 * 2. 1x1 Matrix: [[5]], target = 5 (true), target = 2 (false).
 * 3. Target exists multiple times: [[1, 4], [2, 4]], target = 4.
 * 4. The "Overlap" Case:
 *    [1, 4, 7]
 *    [2, 5, 8]
 *    [3, 6, 9]
 *    Target = 5. Note that row 2 starts with 2, which is smaller than the end of row 1 (7).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force:
 *    - Iterate through every single cell using nested loops.
 *    - Time: O(M * N), Space: O(1). Too slow for the optimal solution.
 * 
 * 2. Binary Search on Each Row:
 *    - Since each row is sorted, we can iterate through all M rows, and perform 
 *      a binary search on each row for the target.
 *    - Time: O(M log N), Space: O(1). Better, but doesn't fully utilize the 
 *      column sorting property.
 * 
 * 3. Step-Wise / Saddleback Search (The Optimal Approach):
 *    - Where should we start searching? 
 *      - If we start Top-Left: Both moving Right and Down INCREASES the value. 
 *        If our current value is less than the target, we don't know which way to go.
 *      - If we start Top-Right: Moving Left DECREASES the value. Moving Down INCREASES it.
 *        This gives us decisive logic!
 *    - Algorithm:
 *      1. Start at the Top-Right corner (row = 0, col = n - 1).
 *      2. If matrix[row][col] == target, return true.
 *      3. If matrix[row][col] > target, the entire column below is also strictly 
 *         greater than the target. We can safely eliminate the column by moving LEFT (col--).
 *      4. If matrix[row][col] < target, the entire row to the left is also strictly 
 *         less than the target. We can safely eliminate the row by moving DOWN (row++).
 *    - Time: O(M + N). In the worst case, we move all the way down and all the way left.
 *    - Space: O(1).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Optimal Approach)
 * ============================================================================
 * Matrix:
 * [1,  4,  7, 11]
 * [2,  5,  8, 12]
 * [3,  6,  9, 16]
 * Target: 5
 * 
 * Start at Top-Right: row = 0, col = 3. Value = 11.
 * 1. 11 > 5. Move Left (col = 2). Value = 7.
 * 2. 7 > 5. Move Left (col = 1). Value = 4.
 * 3. 4 < 5. Move Down (row = 1). Value = 5.
 * 4. 5 == 5. Found! Return true.
 */

public class Search2DMatrixII {

    public static void main(String[] args) {
        int[][] matrix = {
            {1,   4,  7, 11, 15},
            {2,   5,  8, 12, 19},
            {3,   6,  9, 16, 22},
            {10, 13, 14, 17, 24},
            {18, 21, 23, 26, 30}
        };
        
        int target1 = 5;
        int target2 = 20;

        System.out.println("--- Solution 1: Binary Search on Rows ---");
        System.out.println("Target " + target1 + " found: " + searchMatrixBinarySearch(matrix, target1));
        System.out.println("Target " + target2 + " found: " + searchMatrixBinarySearch(matrix, target2));

        System.out.println("\n--- Solution 2: Optimal Step-Wise Search ---");
        System.out.println("Target " + target1 + " found: " + searchMatrixOptimal(matrix, target1));
        System.out.println("Target " + target2 + " found: " + searchMatrixOptimal(matrix, target2));
    }

    /**
     * SOLUTION 1: Binary Search on each row
     * 
     * Idea: Iterate vertically row by row. For each row, run a standard binary 
     * search. This only takes advantage of the horizontal sorting.
     * 
     * Time Complexity: O(M log N)
     * Space Complexity: O(1)
     */
    public static boolean searchMatrixBinarySearch(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) return false;
        
        int m = matrix.length;
        int n = matrix[0].length;
        
        for (int i = 0; i < m; i++) {
            // Optimization: If the first element of the row is > target,
            // or the last element is < target, target cannot be in this row.
            if (matrix[i][0] > target || matrix[i][n - 1] < target) {
                continue;
            }
            
            // Standard Binary Search
            int left = 0, right = n - 1;
            while (left <= right) {
                int mid = left + (right - left) / 2;
                if (matrix[i][mid] == target) {
                    return true;
                } else if (matrix[i][mid] < target) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        
        return false;
    }

    /**
     * SOLUTION 2: Optimal Step-Wise Search (Saddleback Search)
     * 
     * Idea: Start at the top-right corner. Use the sorting properties to safely 
     * eliminate entire rows or columns at every step.
     * 
     * Time Complexity: O(M + N) - In the worst case we traverse one full edge.
     * Space Complexity: O(1) - Constant auxiliary space.
     */
    public static boolean searchMatrixOptimal(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) return false;
        
        int m = matrix.length;
        int n = matrix[0].length;
        
        // Quick boundary checks to eliminate impossible targets early
        if (target < matrix[0][0] || target > matrix[m - 1][n - 1]) {
            return false;
        }
        
        // Start from Top-Right corner
        int row = 0;
        int col = n - 1;
        
        // Traverse until we fall off the grid (bottom or left edge)
        while (row < m && col >= 0) {
            if (matrix[row][col] == target) {
                // Target found
                return true;
            } else if (matrix[row][col] > target) {
                // Current value is too large. 
                // Everything below this cell in the same column is even larger.
                // Ergo, we can eliminate this column.
                col--;
            } else {
                // Current value is too small.
                // Everything to the left in this row is even smaller.
                // Ergo, we can eliminate this row.
                row++;
            }
        }
        
        // We fell off the grid without finding the target
        return false;
    }
    
    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if the matrix is strictly sorted? 
     *    (Meaning the first integer of each row is greater than the last integer 
     *     of the previous row).
     *    Answer: That is LeetCode 74 (Search a 2D Matrix). You don't need the 
     *    Step-Wise search. You can treat the entire M x N matrix as a single 1D 
     *    array of length M*N, and run a single Binary Search in O(log(M*N)) time.
     *    Row index = mid / cols, Col index = mid % cols.
     * 
     * 2. Can we start from the Bottom-Left instead of Top-Right?
     *    Answer: Yes! Bottom-Left works identically because moving UP decreases 
     *    the value (eliminates column), and moving RIGHT increases the value 
     *    (eliminates row). Top-Left and Bottom-Right do NOT work.
     * 
     * 3. What if you wanted to count HOW MANY times the target appears?
     *    Answer: You would use the same Step-Wise traversal. If `matrix[row][col] == target`, 
     *    you increment a counter, and then move either left (col--) or down (row++) 
     *    to continue the search, since duplicates might exist.
     */
}
