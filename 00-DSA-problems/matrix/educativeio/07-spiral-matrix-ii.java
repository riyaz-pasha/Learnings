/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given a positive integer 'n'. 
 * Our task is to generate an n x n 2D matrix filled with sequential numbers 
 * from 1 up to n^2. The numbers must be placed in a clockwise spiral pattern, 
 * starting from the top-left corner (index 0, 0) and spiraling inwards.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can n be 0 or negative? 
 *    (Constraints say 1 <= n <= 20, so it will always be a valid positive size).
 * 2. Does the spiral always go clockwise, starting by moving to the right?
 *    (Yes, this is the standard definition of a spiral pattern).
 * 3. Should the output be a 2D primitive array or a List of Lists?
 *    (Usually, a 2D primitive array `int[][]` is preferred for matrix generation 
 *     unless otherwise specified).
 * 4. Are there any memory or performance constraints?
 *    (The maximum size is 20x20 = 400 elements. Time and space complexities 
 *     will both be O(n^2), which is perfectly optimal since we must generate 
 *     all n^2 elements).
 * 5. Do we need to handle integer overflow for the values?
 *    (Max value is 20^2 = 400, which easily fits inside a standard integer).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. n = 1: The smallest valid grid.
 *    Output: [[1]]
 * 2. n = 2: An even-sized grid. No center element; boundaries cross perfectly.
 *    Output:
 *    [1, 2]
 *    [4, 3]
 * 3. n = 3: An odd-sized grid. Finishes exactly at a single center element.
 *    Output:
 *    [1, 2, 3]
 *    [8, 9, 4]
 *    [7, 6, 5]
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Directional Simulation / "Turtle" Approach (Alternative):
 *    - Maintain a 2D grid initialized to 0. 
 *    - Keep track of the current position (r, c) and direction (right, down, left, up).
 *    - Place the counter value 1 to n^2. 
 *    - If the next step in the current direction hits a grid boundary OR a cell 
 *      that is not 0, change direction clockwise.
 *    - Time: O(n^2), Space: O(1) beyond the output array.
 * 
 * 2. Boundary Tracking (The Most Optimal & Standard Approach):
 *    - Instead of simulating cell-by-cell and checking neighbors, we can fill 
 *      entire edges at a time.
 *    - Maintain 4 boundaries: `top`, `bottom`, `left`, `right`.
 *    - Fill the TOP row from `left` to `right`, then push the `top` boundary down (`top++`).
 *    - Fill the RIGHT column from `top` to `bottom`, then push the `right` boundary left (`right--`).
 *    - Fill the BOTTOM row from `right` to `left`, then push the `bottom` boundary up (`bottom--`).
 *    - Fill the LEFT column from `bottom` to `top`, then push the `left` boundary right (`left++`).
 *    - Loop this until our counter reaches n^2.
 *    - Since it's a perfect n x n square, we don't even need the strict boundary-crossing 
 *      checks (top <= bottom) inside the loop like we did in Spiral Matrix I, because 
 *      the loop condition `count <= n * n` perfectly handles termination!
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Boundary Tracking Approach)
 * ============================================================================
 * n = 3. Target elements: 1 to 9.
 * Init: top = 0, bottom = 2, left = 0, right = 2, count = 1.
 * Matrix = empty 3x3.
 * 
 * Iteration 1:
 * - Fill TOP (left=0 to right=2):
 *   matrix[0][0]=1, matrix[0][1]=2, matrix[0][2]=3.  count=4.
 *   top++ -> top becomes 1.
 * 
 * - Fill RIGHT (top=1 to bottom=2):
 *   matrix[1][2]=4, matrix[2][2]=5. count=6.
 *   right-- -> right becomes 1.
 * 
 * - Fill BOTTOM (right=1 to left=0):
 *   matrix[2][1]=6, matrix[2][0]=7. count=8.
 *   bottom-- -> bottom becomes 1.
 * 
 * - Fill LEFT (bottom=1 to top=1):
 *   matrix[1][0]=8. count=9.
 *   left++ -> left becomes 1.
 * 
 * Iteration 2:
 * - Fill TOP (left=1 to right=1):
 *   matrix[1][1]=9. count=10.
 *   top++ -> top becomes 2.
 * 
 * Loop condition `count <= 9` becomes FALSE. 
 * Final Matrix is fully generated!
 */

import java.util.Arrays;

public class GenerateSpiralMatrix {

    public static void main(String[] args) {
        System.out.println("--- Test Case 1: n = 1 ---");
        printMatrix(generateMatrixBoundary(1));
        
        System.out.println("\n--- Test Case 2: n = 3 ---");
        printMatrix(generateMatrixBoundary(3));
        
        System.out.println("\n--- Test Case 3: n = 4 (Simulation Approach) ---");
        printMatrix(generateMatrixSimulation(4));
    }

    /**
     * SOLUTION 1: Boundary Tracking (The Most Optimal & Cleanest Approach)
     * 
     * Idea: Constrict 4 boundaries around the matrix and fill the perimeter layer 
     * by layer until the matrix is fully populated.
     * 
     * Time Complexity: O(n^2) - We fill exactly n^2 elements once.
     * Space Complexity: O(1) auxiliary - No extra space used besides the required output matrix.
     */
    public static int[][] generateMatrixBoundary(int n) {
        // Initialize the output matrix of size n x n
        int[][] matrix = new int[n][n];
        
        // Define the 4 boundaries
        int top = 0;
        int bottom = n - 1;
        int left = 0;
        int right = n - 1;
        
        // The value to place in the matrix
        int count = 1;
        int target = n * n;
        
        // Keep looping until we have placed all n^2 elements
        while (count <= target) {
            
            // 1. Fill the TOP row from left to right
            for (int i = left; i <= right; i++) {
                matrix[top][i] = count++;
            }
            top++; // Shrink the top boundary downwards
            
            // 2. Fill the RIGHT column from top to bottom
            for (int i = top; i <= bottom; i++) {
                matrix[i][right] = count++;
            }
            right--; // Shrink the right boundary leftwards
            
            // 3. Fill the BOTTOM row from right to left
            for (int i = right; i >= left; i--) {
                matrix[bottom][i] = count++;
            }
            bottom--; // Shrink the bottom boundary upwards
            
            // 4. Fill the LEFT column from bottom to top
            for (int i = bottom; i >= top; i--) {
                matrix[i][left] = count++;
            }
            left++; // Shrink the left boundary rightwards
        }
        
        return matrix;
    }

    /**
     * SOLUTION 2: Simulation / Direction Vectors
     * 
     * Idea: Imagine walking through the grid starting at (0,0) and facing right.
     * Move forward until you hit an edge or a non-zero cell, then turn 90 degrees 
     * clockwise.
     * 
     * Time Complexity: O(n^2)
     * Space Complexity: O(1) auxiliary
     */
    public static int[][] generateMatrixSimulation(int n) {
        int[][] matrix = new int[n][n];
        
        // Direction vectors: Right, Down, Left, Up
        // Row modifier (dr) and Column modifier (dc)
        int[] dr = {0, 1, 0, -1};
        int[] dc = {1, 0, -1, 0};
        
        int row = 0;
        int col = 0;
        int dir = 0; // Starts facing Right (index 0)
        
        for (int count = 1; count <= n * n; count++) {
            // Place the current number
            matrix[row][col] = count;
            
            // Calculate the next theoretical step
            int nextRow = row + dr[dir];
            int nextCol = col + dc[dir];
            
            // Check if the next step is invalid (out of bounds or already filled)
            if (nextRow < 0 || nextRow >= n || nextCol < 0 || nextCol >= n || matrix[nextRow][nextCol] != 0) {
                // If invalid, turn 90 degrees clockwise
                dir = (dir + 1) % 4;
                
                // Recalculate next step in the new direction
                nextRow = row + dr[dir];
                nextCol = col + dc[dir];
            }
            
            // Move to the next valid cell
            row = nextRow;
            col = nextCol;
        }
        
        return matrix;
    }
    
    /**
     * Helper method to pretty-print a 2D matrix with proper alignment.
     */
    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int val : row) {
                // Print with 3-character width alignment for visual neatness
                System.out.printf("%3d ", val);
            }
            System.out.println();
        }
    }
    
    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if you needed to generate a rectangular matrix of size m x n?
     *    Answer: The logic remains largely the same. However, you MUST re-introduce 
     *    the boundary overlap checks (`if (top <= bottom)` and `if (left <= right)`) 
     *    before filling the bottom and left edges, just like in Spiral Matrix I.
     *    Without them, in a rectangular grid, you might traverse the same center 
     *    row or column backwards, overwriting previously placed numbers.
     * 
     * 2. What if you want to spiral outwards starting from the center?
     *    Answer: You can either:
     *    a) Generate it inwards as normal, but instead of filling 1 to n^2, fill it 
     *       from n^2 down to 1.
     *    b) Mathematically calculate the center (n/2, n/2) and simulate walking 
     *       in an expanding spiral: move right 1, down 1, left 2, up 2, right 3...
     */
}
