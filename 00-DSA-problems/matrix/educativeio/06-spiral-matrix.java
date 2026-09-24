/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n 2D array (matrix). 
 * We need to traverse the matrix in a spiral order, starting from the top-left 
 * cell, moving clockwise (right, down, left, up), and return the elements 
 * as a 1D list in the order we visited them.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the matrix be empty (0 rows or 0 columns)? 
 *    (Constraints say 1 <= m, n <= 10, so it will always have at least 1 element, 
 *    but it's a great habit to ask).
 * 2. Is the matrix guaranteed to be a perfect rectangle (all rows have the same length)? 
 *    (Yes, standard matrix rules apply).
 * 3. Does the spiral always go clockwise, starting by moving to the right?
 *    (Yes, the standard definition of spiral order implies clockwise).
 * 4. What should be the return type? An array or a List?
 *    (A dynamic List is usually preferred since we append elements, but if an array 
 *    is required, we can easily convert the list or pre-allocate an array of size M*N).
 * 5. Can the matrix be mutated?
 *    (For a "Visited" approach, mutating the matrix to mark visited cells could save 
 *    space, but usually, it's better not to destroy the input data).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. Single Element: [[5]] -> Output: [5]
 * 2. Single Row (1 x N): [[1, 2, 3]] -> Output: [1, 2, 3] 
 *    (Watch out for going right, then trying to go left on the same row).
 * 3. Single Column (M x 1): [[1], [2], [3]] -> Output: [1, 2, 3]
 *    (Watch out for going down, then trying to go up on the same column).
 * 4. Rectangular Matrix (e.g., 3 x 4 or 4 x 3): 
 *    The center might be a single row or column, not a single point.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Simulation with Visited Matrix (The "Turtle" Approach):
 *    - Imagine a turtle walking on the matrix. It starts facing Right.
 *    - It keeps walking forward until it hits a wall (grid boundary) or a 
 *      cell it has already visited. 
 *    - When blocked, it turns 90 degrees clockwise (Right -> Down -> Left -> Up).
 *    - Time: O(M * N), Space: O(M * N) for the visited array.
 *    - This is very intuitive but uses extra memory.
 * 
 * 2. Boundary Tracking (The Optimal Approach):
 *    - We don't need a visited array. We can just maintain 4 boundaries:
 *      `top`, `bottom`, `left`, `right`.
 *    - Move Right: Traverse from `left` to `right` along the `top` boundary. 
 *      Then push the `top` boundary down (`top++`).
 *    - Move Down: Traverse from `top` to `bottom` along the `right` boundary. 
 *      Then push the `right` boundary left (`right--`).
 *    - Move Left: Traverse from `right` to `left` along the `bottom` boundary. 
 *      Then push the `bottom` boundary up (`bottom--`).
 *    - Move Up: Traverse from `bottom` to `top` along the `left` boundary. 
 *      Then push the `left` boundary right (`left++`).
 *    - CRITICAL CATCH: Before traversing left or up, we MUST check if `top <= bottom` 
 *      and `left <= right`. Otherwise, for 1D shapes (like 1xN or Nx1), we will 
 *      traverse the same row/col backward and duplicate elements!
 *    - Time: O(M * N), Space: O(1) (excluding the output list).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Boundary Tracking Approach)
 * ============================================================================
 * Matrix:
 * [1, 2, 3]
 * [4, 5, 6]
 * [7, 8, 9]
 * 
 * Init: top = 0, bottom = 2, left = 0, right = 2. List = []
 * 
 * Iteration 1:
 * - Traverse Right: (top=0). Cols left(0) to right(2) -> 1, 2, 3. 
 *   List = [1, 2, 3]. top++ -> 1
 * - Traverse Down: (right=2). Rows top(1) to bottom(2) -> 6, 9. 
 *   List = [1, 2, 3, 6, 9]. right-- -> 1
 * - Traverse Left: (bottom=2). top(1) <= bottom(2) is TRUE. 
 *   Cols right(1) to left(0) -> 8, 7. 
 *   List = [1, 2, 3, 6, 9, 8, 7]. bottom-- -> 1
 * - Traverse Up: (left=0). left(0) <= right(1) is TRUE.
 *   Rows bottom(1) to top(1) -> 4.
 *   List = [1, 2, 3, 6, 9, 8, 7, 4]. left++ -> 1
 * 
 * Iteration 2:
 * - top = 1, bottom = 1, left = 1, right = 1.
 * - Traverse Right: (top=1). Cols left(1) to right(1) -> 5.
 *   List = [1, 2, 3, 6, 9, 8, 7, 4, 5]. top++ -> 2
 * - Traverse Down: (right=1). Rows top(2) to bottom(1) -> loop doesn't run.
 *   right-- -> 0
 * - Traverse Left: top(2) <= bottom(1) is FALSE. Skips.
 * - Traverse Up: left(1) <= right(0) is FALSE. Skips.
 * 
 * Loop ends (top > bottom). 
 * Final List: [1, 2, 3, 6, 9, 8, 7, 4, 5]
 */

import java.util.ArrayList;
import java.util.List;

public class SpiralMatrix {

    public static void main(String[] args) {
        int[][] matrix1 = {
            { 1, 2, 3 },
            { 4, 5, 6 },
            { 7, 8, 9 }
        };

        int[][] matrix2 = {
            { 1, 2, 3, 4 },
            { 5, 6, 7, 8 },
            { 9,10,11,12 }
        };
        
        int[][] matrix3 = {
            { 7 },
            { 9 },
            { 6 }
        };

        System.out.println("--- Solution 1: Visited Matrix Simulation ---");
        System.out.println("Matrix 1: " + spiralOrderVisited(matrix1));
        System.out.println("Matrix 2: " + spiralOrderVisited(matrix2));
        
        System.out.println("\n--- Solution 2: Boundary Tracking (Optimal) ---");
        System.out.println("Matrix 1: " + spiralOrderOptimal(matrix1));
        System.out.println("Matrix 2: " + spiralOrderOptimal(matrix2));
        System.out.println("Matrix 3 (Edge Case 1 Col): " + spiralOrderOptimal(matrix3));
    }

    /**
     * SOLUTION 1: Visited Matrix Simulation (The Turtle Approach)
     * 
     * Idea: Keep a 2D boolean array to track visited cells. Use a direction array 
     * for Right, Down, Left, Up. Move forward until you hit a boundary or a visited 
     * cell, then turn 90 degrees clockwise.
     * 
     * Time Complexity: O(M * N) - Every cell visited exactly once.
     * Space Complexity: O(M * N) - For the boolean visited array.
     */
    public static List<Integer> spiralOrderVisited(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        if (matrix == null || matrix.length == 0) return result;

        int m = matrix.length;
        int n = matrix[0].length;
        boolean[][] visited = new boolean[m][n];

        // Direction arrays: Right, Down, Left, Up
        // Right: row stays same (0), col increases (+1)
        int[] rowDir = {0, 1, 0, -1};
        int[] colDir = {1, 0, -1, 0};

        int r = 0, c = 0; // Starting position
        int dirIndex = 0; // 0 = Right, 1 = Down, 2 = Left, 3 = Up

        for (int i = 0; i < m * n; i++) {
            result.add(matrix[r][c]);
            visited[r][c] = true;

            // Calculate the next potential cell based on current direction
            int nextR = r + rowDir[dirIndex];
            int nextC = c + colDir[dirIndex];

            // Check if the next cell is out of bounds OR already visited
            if (nextR < 0 || nextR >= m || nextC < 0 || nextC >= n || visited[nextR][nextC]) {
                // If invalid, turn 90 degrees clockwise
                dirIndex = (dirIndex + 1) % 4; 
                // Recalculate next cell with the new direction
                nextR = r + rowDir[dirIndex];
                nextC = c + colDir[dirIndex];
            }

            // Move to the next cell
            r = nextR;
            c = nextC;
        }

        return result;
    }

    /**
     * SOLUTION 2: Boundary Tracking (The Most Optimal & Standard Approach)
     * 
     * Idea: Maintain 4 boundaries (top, bottom, left, right). Traverse the perimeter, 
     * then shrink the boundaries inward.
     * 
     * Time Complexity: O(M * N) - Every cell visited exactly once.
     * Space Complexity: O(1) - Only pointers are used, no extra grid needed.
     */
    public static List<Integer> spiralOrderOptimal(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        if (matrix == null || matrix.length == 0) return result;

        int top = 0;
        int bottom = matrix.length - 1;
        int left = 0;
        int right = matrix[0].length - 1;

        // Loop as long as boundaries haven't crossed
        while (top <= bottom && left <= right) {

            // 1. Traverse TOP boundary from Left to Right
            for (int i = left; i <= right; i++) {
                result.add(matrix[top][i]);
            }
            top++; // Shrink top boundary down

            // 2. Traverse RIGHT boundary from Top to Bottom
            for (int i = top; i <= bottom; i++) {
                result.add(matrix[i][right]);
            }
            right--; // Shrink right boundary left

            // 3. Traverse BOTTOM boundary from Right to Left
            // CRITICAL CHECK: We must ensure the top boundary didn't cross the bottom 
            // boundary during step 1. This happens for single-row matrices (1xN).
            if (top <= bottom) {
                for (int i = right; i >= left; i--) {
                    result.add(matrix[bottom][i]);
                }
                bottom--; // Shrink bottom boundary up
            }

            // 4. Traverse LEFT boundary from Bottom to Top
            // CRITICAL CHECK: Ensure left boundary didn't cross the right boundary 
            // during step 2. This happens for single-column matrices (Mx1).
            if (left <= right) {
                for (int i = bottom; i >= top; i--) {
                    result.add(matrix[i][left]);
                }
                left++; // Shrink left boundary right
            }
        }

        return result;
    }

    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if the requirement is to generate an NxN spiral matrix from 1 to N^2?
     *    (This is LeetCode 59: Spiral Matrix II. The logic is identical to Solution 2. 
     *     Instead of reading from a matrix and appending to a list, you start a 
     *     counter at 1, and assign `matrix[row][col] = counter++` as you traverse 
     *     the boundaries).
     * 
     * 2. What if we need to traverse Anti-Clockwise?
     *    (Simply change the order of operations in the while loop:
     *     Traverse Left (top row), Traverse Down (left col), Traverse Right (bottom row), 
     *     Traverse Up (right col). Adjust boundary shrinking accordingly).
     * 
     * 3. How would you solve this if the matrix is huge and stored on disk, but 
     *    you can only read contiguous chunks of rows?
     *    (This is a system design trick. You would read the top row sequentially, 
     *     which is fast. For the right column, you'd need to seek to the end of each 
     *     row on disk, which is slow. To optimize, you could read chunks of rows, 
     *     extract the right/left column boundaries for the chunk, cache them, and 
     *     construct the spiral output in memory).
     */
}

