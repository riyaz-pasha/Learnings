/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n matrix representing a grid for the "Game of Life".
 * - 1 represents a live cell.
 * - 0 represents a dead cell.
 * 
 * We must update the board to its next generation based on 4 rules:
 * 1. Underpopulation: A live cell with < 2 live neighbors dies.
 * 2. Survival: A live cell with 2 or 3 live neighbors lives.
 * 3. Overpopulation: A live cell with > 3 live neighbors dies.
 * 4. Reproduction: A dead cell with exactly 3 live neighbors becomes alive.
 * 
 * Crucial constraint: The board must be updated SIMULTANEOUSLY. We cannot 
 * update a cell to its new state and let that new state affect the calculation 
 * of its neighbors. We must do this in-place (modifying the given grid).
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. What is the boundary of the board? Do cells outside count as dead or wrap around?
 *    (They count as dead. No wrap-around / toroidal grid).
 * 2. What does "simultaneous update" mean for our algorithm?
 *    (It means we cannot simply overwrite `board[i][j]` with its new value, because 
 *    the cell to its right still needs to read `board[i][j]`'s ORIGINAL state).
 * 3. Does "in-place" strictly mean O(1) auxiliary space?
 *    (Yes, creating a full copy of the board would be O(M*N) space, which is 
 *    often considered the brute-force approach for this specific problem).
 * 4. Can the board be 1x1 or 1xN?
 *    (Yes, constraints say 1 <= m, n <= 25. Small grids must be handled safely).
 * 5. Are the matrix values strictly 0 and 1 initially?
 *    (Yes, but we can potentially use other integer values temporarily to store state).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. 1x1 Matrix: [[1]] -> Has 0 neighbors. Dies of underpopulation. Output: [[0]].
 * 2. Stable Shapes (Block):
 *    [[1, 1],
 *     [1, 1]] -> Every cell has exactly 3 neighbors. All survive. Remains unchanged.
 * 3. Oscillators (Blinker):
 *    [[0, 1, 0],
 *     [0, 1, 0],
 *     [0, 1, 0]] -> The middle 1 has 2 neighbors (lives). The top/bottom 1s have 1 neighbor 
 *                   (die). The middle-left and middle-right 0s have 3 neighbors (reproduce).
 *    Result:
 *    [[0, 0, 0],
 *     [1, 1, 1],
 *     [0, 0, 0]]
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (O(M*N) Space):
 *    - Create an exact clone of the input board.
 *    - Iterate through the original board. Count neighbors by looking at the original board.
 *    - Write the result (0 or 1) to the cloned board.
 *    - Finally, copy the cloned board back into the original board.
 *    - Time: O(M*N), Space: O(M*N). Good to mention, but immediately strive for better.
 * 
 * 2. In-Place State Encoding via Arbitrary Integers (Better):
 *    - Since cells are only 0 or 1, we can invent new states:
 *      - `2`: Was alive, now dead (Over/Under population).
 *      - `3`: Was dead, now alive (Reproduction).
 *    - When counting neighbors, if a cell is `1` or `2`, it means it WAS alive originally.
 *    - Time: O(M*N), Space: O(1).
 * 
 * 3. In-Place State Encoding via Bit Manipulation (Most Optimal/Elegant):
 *    - An integer has 32 bits. We are only using the 0th bit (least significant bit).
 *    - We can use the 1st bit to store the NEXT state!
 *    - State format: `[Next State, Current State]`
 *      - 00 (0 in decimal): Was dead, stays dead.
 *      - 01 (1 in decimal): Was alive, stays alive (Wait, if it stays alive, we make it 11, which is 3).
 *      - 10 (2 in decimal): Was dead, becomes alive.
 *      - 11 (3 in decimal): Was alive, stays alive.
 *    - To read the current state: `board[i][j] & 1` (Extracts 0th bit).
 *    - To set the next state to alive: `board[i][j] |= 2` (Sets the 1st bit to 1).
 *    - At the very end, we just shift all elements right by 1 bit (`board[i][j] >>= 1`). 
 *      This throws away the old state and brings the new state into the 0th bit position!
 *    - Time: O(M*N), Space: O(1).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Bit Manipulation Approach)
 * ============================================================================
 * Cell is `0`. Has 3 live neighbors. (Rule 4: Reproduction).
 * - Current binary: `00`
 * - Action: `board[i][j] |= 2` (which is `10` in binary).
 * - New binary: `00 | 10 = 10` (Decimal 2).
 * - When checking this cell's original state for its neighbors: `10 & 01 = 00` (Original was dead, correct).
 * - Second pass (shift right): `10 >> 1 = 01`. It is now `1` (Alive!).
 * 
 * Cell is `1`. Has 1 live neighbor. (Rule 1: Underpopulation).
 * - Current binary: `01`
 * - Action: None (it dies, so next state bit should remain 0).
 * - New binary: `01`.
 * - When checking this cell's original state: `01 & 01 = 01` (Original was alive, correct).
 * - Second pass (shift right): `01 >> 1 = 00`. It is now `0` (Dead!).
 */

import java.util.Arrays;

public class GameOfLife {

    public static void main(String[] args) {
        int[][] board = {
            {0, 1, 0},
            {0, 0, 1},
            {1, 1, 1},
            {0, 0, 0}
        };

        System.out.println("Original Board:");
        printBoard(board);

        gameOfLifeOptimal(board);

        System.out.println("\nBoard after 1 Generation:");
        printBoard(board);
    }

    /**
     * SOLUTION: In-Place Bit Manipulation (Optimal)
     * 
     * Idea: Store both the current state and the next state in the same integer 
     * using the 0th and 1st bits respectively.
     * 
     * Time Complexity: O(M * N) - We visit every cell and its 8 neighbors exactly once.
     * Space Complexity: O(1) - No extra matrices allocated.
     */
    public static void gameOfLifeOptimal(int[][] board) {
        if (board == null || board.length == 0) return;
        
        int rows = board.length;
        int cols = board[0].length;
        
        // Direction arrays to easily traverse the 8 neighboring cells
        // (Top-Left, Top, Top-Right, Left, Right, Bottom-Left, Bottom, Bottom-Right)
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};
        
        // Pass 1: Calculate the next state for each cell and store it in the 1st bit
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int liveNeighbors = 0;
                
                // Count live neighbors
                for (int i = 0; i < 8; i++) {
                    int neighborRow = r + dx[i];
                    int neighborCol = c + dy[i];
                    
                    // Check boundaries
                    if (neighborRow >= 0 && neighborRow < rows && neighborCol >= 0 && neighborCol < cols) {
                        // Extract the 0th bit (original state) to check if the neighbor is currently alive.
                        // We use bitwise AND with 1 because the neighbor might have already been processed
                        // and holding its next state in the 1st bit.
                        liveNeighbors += (board[neighborRow][neighborCol] & 1);
                    }
                }
                
                // Apply the rules of Conway's Game of Life
                
                // Rule 2: Any live cell with 2 or 3 live neighbors lives on to the next generation.
                if ((board[r][c] & 1) == 1 && (liveNeighbors == 2 || liveNeighbors == 3)) {
                    // Set the 1st bit to 1 (Binary '10' is decimal 2)
                    // e.g., 01 | 10 = 11 (Decimal 3)
                    board[r][c] |= 2; 
                }
                
                // Rule 4: Any dead cell with exactly 3 live neighbors becomes a live cell.
                if ((board[r][c] & 1) == 0 && liveNeighbors == 3) {
                    // Set the 1st bit to 1
                    // e.g., 00 | 10 = 10 (Decimal 2)
                    board[r][c] |= 2;
                }
                
                // Note: We don't need to do anything for Rule 1 (Underpopulation) 
                // and Rule 3 (Overpopulation) because the 1st bit is already 0 by default,
                // which correctly represents that the cell will die.
            }
        }
        
        // Pass 2: Finalize the state transitions
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Shift the bits to the right by 1.
                // This discards the 0th bit (old state) and shifts the 1st bit (new state)
                // down into the 0th bit position.
                board[r][c] >>= 1;
            }
        }
    }

    /**
     * Helper method to visualize the board
     */
    private static void printBoard(int[][] board) {
        for (int[] row : board) {
            System.out.println(Arrays.toString(row));
        }
    }

    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if the board is infinite?
     *    Answer: A 2D array cannot represent an infinite grid. Instead, we can use 
     *    a `Set<Coordinate>` to store the coordinates of ONLY the ALIVE cells. 
     *    Since dead cells with < 3 live neighbors remain dead, we only ever need to 
     *    evaluate the currently alive cells AND their immediate neighbors.
     *    We would iterate over the Set of live cells, add all their neighbors into 
     *    a map counting how many live neighbors each cell has, and then generate a 
     *    NEW Set of live cells based on the counts.
     * 
     * 2. What if the board is too large to fit in memory (e.g., stored on disk)?
     *    Answer: We can read the matrix sequentially, one chunk/row at a time. 
     *    To compute the next state of a row `i`, we only need access to row `i-1`, 
     *    row `i`, and row `i+1`. We can maintain a "sliding window" of 3 rows in memory.
     *    As we process row `i`, we write its next state to a new file or chunk. 
     *    Once row `i` is done, we discard `i-1`, load `i+2`, and move the window down.
     */
}
