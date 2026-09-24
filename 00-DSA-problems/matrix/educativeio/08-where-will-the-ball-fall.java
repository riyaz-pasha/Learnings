/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We have a 2D grid representing a box. We drop `n` balls, one at the top of 
 * each column. 
 * Each cell in the grid contains either:
 *  1: A board slanting from top-left to bottom-right (redirects ball RIGHT).
 * -1: A board slanting from top-right to bottom-left (redirects ball LEFT).
 * 
 * A ball gets stuck if:
 * 1. It is redirected into the left or right wall of the box.
 * 2. It falls into a "V-shape" (e.g., cell is 1 but its right neighbor is -1).
 * 
 * We need to return an array where the i-th element is the column index where 
 * the i-th ball falls out at the bottom, or -1 if the ball gets stuck.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the grid be a single column wide?
 *    (Yes, if it's 1 column wide, any ball dropped will instantly hit a wall 
 *     and get stuck. The answer array will just be [-1]).
 * 2. Can the grid be a single row high?
 *    (Yes, the ball will just make one lateral movement and fall out, or get stuck).
 * 3. Does the ball drop down one row per step, or can it move multiple columns 
 *    in the same row?
 *    (A ball drops exactly one row at a time. It moves one column left or right, 
 *     then immediately enters the row below).
 * 4. Are we guaranteed the input will only contain 1 and -1?
 *    (Yes, constraints specify this).
 * 5. Can we modify the grid?
 *    (No need, we only need to read from it to trace the paths).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. Single Column: [[1]] -> Ball tries to go right, hits wall -> [-1].
 * 2. Perfect V-Shape: [[1, -1]] -> Ball 0 goes right, ball 1 goes left, they 
 *    meet in the middle and get stuck. -> [-1, -1].
 * 3. All Right: [[1, 1, 1], [1, 1, 1]] 
 *    - Ball 0 -> Col 1 -> Col 2 (Exits at 2)
 *    - Ball 1 -> Col 2 -> Wall (Stuck at -1)
 *    - Ball 2 -> Wall (Stuck at -1)
 *    -> [2, -1, -1]
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Simulation / Brute Force (The Optimal Approach):
 *    - We drop `n` balls. The path of each ball is completely independent.
 *    - Therefore, we can just simulate the drop for each ball one by one.
 *    - For a single ball dropped at `currCol`, it drops row by row.
 *    - The direction it wants to go is determined by `grid[row][currCol]`.
 *      - If it's `1`, it wants to go to `currCol + 1`.
 *      - If it's `-1`, it wants to go to `currCol - 1`.
 *      Let's call this `nextCol`.
 *    - The ball gets stuck if:
 *      a) `nextCol` is out of bounds (hits a wall).
 *      b) `grid[row][nextCol]` is opposite to `grid[row][currCol]` (forms a V).
 *    - If it doesn't get stuck, `currCol` becomes `nextCol` and we move to 
 *      the next row.
 *    - Time: O(M * N) since there are N balls and each travels M rows. With M,N <= 100, 
 *      M*N = 10,000 operations, which is incredibly fast and optimal.
 *    - Space: O(1) auxiliary.
 * 
 * 2. Is DFS/Memoization useful here?
 *    - Since the ball never moves UP, and always goes exactly one row DOWN, 
 *      paths never truly overlap in a way that saves computation time for multiple 
 *      steps. Each start column maps to exactly one deterministic path. 
 *    - A simple loop simulation is much more memory efficient than recursion.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Simulation)
 * ============================================================================
 * Grid:
 * [ 1,  1,  1, -1, -1]
 * [ 1,  1,  1, -1, -1]
 * [-1, -1, -1,  1,  1]
 * [ 1,  1,  1,  1, -1]
 * [-1, -1, -1, -1, -1]
 * 
 * Tracing Ball 0 (Starts at col 0):
 * Row 0: grid[0][0] = 1. wants to go right. nextCol = 1. 
 *        grid[0][1] is also 1. (Match). Moves to col 1.
 * Row 1: grid[1][1] = 1. wants to go right. nextCol = 2. 
 *        grid[1][2] is also 1. (Match). Moves to col 2.
 * Row 2: grid[2][2] = -1. wants to go left. nextCol = 1. 
 *        grid[2][1] is also -1. (Match). Moves to col 1.
 * Row 3: grid[3][1] = 1. wants to go right. nextCol = 2. 
 *        grid[3][2] is also 1. (Match). Moves to col 2.
 * Row 4: grid[4][2] = -1. wants to go left. nextCol = 1. 
 *        grid[4][1] is also -1. (Match). Moves to col 1.
 * Ball 0 survives and exits at col 1!
 * 
 * Tracing Ball 2 (Starts at col 2):
 * Row 0: grid[0][2] = 1. wants to go right. nextCol = 3. 
 *        grid[0][3] is -1. (V-Shape!). Ball gets stuck! Break early. -> -1.
 */

import java.util.Arrays;

public class WhereWillTheBallFall {

    public static void main(String[] args) {
        int[][] grid1 = {
            { 1,  1,  1, -1, -1},
            { 1,  1,  1, -1, -1},
            {-1, -1, -1,  1,  1},
            { 1,  1,  1,  1, -1},
            {-1, -1, -1, -1, -1}
        };

        int[][] grid2 = {
            {-1}
        };

        int[][] grid3 = {
            { 1,  1,  1,  1,  1,  1},
            {-1, -1, -1, -1, -1, -1},
            { 1,  1,  1,  1,  1,  1},
            {-1, -1, -1, -1, -1, -1}
        };

        System.out.println("--- Test Case 1 ---");
        System.out.println("Output: " + Arrays.toString(findBall(grid1)));

        System.out.println("\n--- Test Case 2 (Single Column) ---");
        System.out.println("Output: " + Arrays.toString(findBall(grid2)));

        System.out.println("\n--- Test Case 3 (Zig-Zag) ---");
        System.out.println("Output: " + Arrays.toString(findBall(grid3)));
    }

    /**
     * SOLUTION: Iterative Simulation (Optimal)
     * 
     * Idea: Loop over every column and drop a ball. For each ball, walk it row 
     * by row. Use a clever trick combining the wall check and V-shape check into 
     * one simple comparison.
     * 
     * Time Complexity: O(M * N) where M is rows and N is cols. We visit at most 
     *                  M cells for each of the N balls.
     * Space Complexity: O(1) auxiliary space (excluding the returned array).
     */
    public static int[] findBall(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        int[] result = new int[n];
        
        // Drop a ball at every column
        for (int startCol = 0; startCol < n; startCol++) {
            int currCol = startCol;
            
            // Simulate the ball dropping row by row
            for (int row = 0; row < m; row++) {
                
                // The direction the board is slanting determines the next column.
                // If 1 (right), we add 1. If -1 (left), we add -1. 
                int nextCol = currCol + grid[row][currCol];
                
                // Check if the ball gets stuck. 
                // A ball gets stuck if:
                // 1. nextCol is < 0 (hits left wall)
                // 2. nextCol is >= n (hits right wall)
                // 3. The cell it's trying to enter slants in the OPPOSITE direction 
                //    (grid[row][currCol] != grid[row][nextCol]), forming a V-shape.
                if (nextCol < 0 || nextCol >= n || grid[row][currCol] != grid[row][nextCol]) {
                    currCol = -1; // Mark as stuck
                    break;        // No need to check further rows for this ball
                }
                
                // If it didn't get stuck, successfully move it to the next column
                currCol = nextCol;
            }
            
            // Record the exit column (or -1 if it got stuck)
            result[startCol] = currCol;
        }
        
        return result;
    }

    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if you needed to know the full path the ball took?
     *    Answer: Instead of just returning `currCol` at the end, we would maintain 
     *    a `List<int[]>` for each ball representing the `[row, col]` coordinates 
     *    at each step, and return a List of Lists.
     * 
     * 2. What if multiple balls are dropped simultaneously and balls can collide 
     *    and stop each other?
     *    Answer: You would need to simulate row by row for ALL balls simultaneously, 
     *    rather than one ball top-to-bottom at a time. After calculating `nextCol` 
     *    for all balls in the current row, you'd check for collisions 
     *    (if ball A and ball B have the same `nextCol`), mark them as stuck, 
     *    and proceed to the next row with the remaining active balls.
     */
}
