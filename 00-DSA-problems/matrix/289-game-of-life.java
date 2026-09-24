/*
 * According to Wikipedia's article:
 * "The Game of Life, also known simply as Life, is a cellular automaton devised by the British mathematician John Horton Conway in 1970."
 * 
 * The board is made up of an m x n grid of cells, where each cell has an
 * initial state: live (represented by a 1) or dead (represented by a 0). Each
 * cell interacts with its eight neighbors (horizontal, vertical, diagonal)
 * using the following four rules (taken from the above Wikipedia article):
 * 
 * Any live cell with fewer than two live neighbors dies as if caused by
 * under-population.
 * Any live cell with two or three live neighbors lives on to the next
 * generation.
 * Any live cell with more than three live neighbors dies, as if by
 * over-population.
 * Any dead cell with exactly three live neighbors becomes a live cell, as if by
 * reproduction.
 * The next state of the board is determined by applying the above rules
 * simultaneously to every cell in the current state of the m x n grid board. In
 * this process, births and deaths occur simultaneously.
 * 
 * Given the current state of the board, update the board to reflect its next
 * state.
 * 
 * Note that you do not need to return anything.
 * 
 * Example 1:
 * Input: board = [[0,1,0],[0,0,1],[1,1,1],[0,0,0]]
 * Output: [[0,0,0],[1,0,1],[0,1,1],[0,1,0]]
 * 
 * Example 2:
 * Input: board = [[1,1],[1,0]]
 * Output: [[1,1],[1,1]]
 */

// Solution 1: Using a copy of the board (Space: O(m*n))
// Time Complexity: O(m×n)
// Space Complexity: O(m×n)
class Solution1 {

    public void gameOfLife(int[][] board) {
        int m = board.length;
        int n = board[0].length;

        // Create a copy of the original board
        int[][] copy = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                copy[i][j] = board[i][j];
            }
        }

        // Directions for 8 neighbors (including diagonals)
        int[][] directions = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };

        // Apply rules to each cell
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int liveNeighbors = countLiveNeighbors(copy, i, j, directions);

                if (copy[i][j] == 1) { // Currently alive
                    if (liveNeighbors < 2 || liveNeighbors > 3) {
                        board[i][j] = 0; // Dies
                    }
                    // If 2 or 3 neighbors, stays alive (no change needed)
                } else { // Currently dead
                    if (liveNeighbors == 3) {
                        board[i][j] = 1; // Becomes alive
                    }
                }
            }
        }
    }

    private int countLiveNeighbors(int[][] board, int row, int col, int[][] directions) {
        int count = 0;
        int m = board.length;
        int n = board[0].length;

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n) {
                count += board[newRow][newCol];
            }
        }
        return count;
    }

}

// Solution 2: In-place using bit manipulation (Space: O(1))
// Time Complexity: O(m×n)
class Solution2 {

    public void gameOfLife(int[][] board) {
        int m = board.length;
        int n = board[0].length;

        // Directions for 8 neighbors
        int[][] directions = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };

        // First pass: encode the next state in the second bit
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int liveNeighbors = countLiveNeighbors(board, i, j, directions);

                // Current state is in the first bit (board[i][j] & 1)
                int currentState = board[i][j] & 1;

                if (currentState == 1) { // Currently alive
                    if (liveNeighbors == 2 || liveNeighbors == 3) {
                        // Set second bit to 1 (will stay alive)
                        board[i][j] |= 2;
                    }
                } else { // Currently dead
                    if (liveNeighbors == 3) {
                        // Set second bit to 1 (will become alive)
                        board[i][j] |= 2;
                    }
                }
            }
        }

        // Second pass: extract the next state from the second bit
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                board[i][j] >>= 1; // Right shift to get the next state
            }
        }
    }

    private int countLiveNeighbors(int[][] board, int row, int col, int[][] directions) {
        int count = 0;
        int m = board.length;
        int n = board[0].length;

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n) {
                // Use & 1 to get only the original state (first bit)
                count += board[newRow][newCol] & 1;
            }
        }
        return count;
    }

}

// Solution 3: Clean and readable version
// Time Complexity: O(m×n)
// Space Complexity: O(m×n)
class Solution3 {

    public void gameOfLife(int[][] board) {
        if (board == null || board.length == 0)
            return;

        int rows = board.length;
        int cols = board[0].length;

        // Create a copy to preserve original state
        int[][] original = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(board[i], 0, original[i], 0, cols);
        }

        // Apply rules to each cell
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int neighbors = getLiveNeighbors(original, i, j);
                applyRules(board, i, j, original[i][j], neighbors);
            }
        }
    }

    private int getLiveNeighbors(int[][] board, int row, int col) {
        int count = 0;
        int rows = board.length;
        int cols = board[0].length;

        // Check all 8 directions
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0)
                    continue; // Skip the cell itself

                int newRow = row + i;
                int newCol = col + j;

                if (isValid(newRow, newCol, rows, cols) && board[newRow][newCol] == 1) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isValid(int row, int col, int rows, int cols) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private void applyRules(int[][] board, int row, int col, int currentState, int neighbors) {
        if (currentState == 1) { // Live cell
            if (neighbors < 2) {
                board[row][col] = 0; // Under-population
            } else if (neighbors == 2 || neighbors == 3) {
                board[row][col] = 1; // Survives
            } else {
                board[row][col] = 0; // Over-population
            }
        } else { // Dead cell
            if (neighbors == 3) {
                board[row][col] = 1; // Reproduction
            }
        }
    }

}

// Test class to verify the solutions
class GameOfLifeTest {

    public static void main(String[] args) {
        // Test Example 1
        int[][] board1 = { { 0, 1, 0 }, { 0, 0, 1 }, { 1, 1, 1 }, { 0, 0, 0 } };
        System.out.println("Example 1 - Before:");
        printBoard(board1);

        new Solution1().gameOfLife(board1);
        System.out.println("After:");
        printBoard(board1);

        // Test Example 2
        int[][] board2 = { { 1, 1 }, { 1, 0 } };
        System.out.println("\nExample 2 - Before:");
        printBoard(board2);

        new Solution2().gameOfLife(board2);
        System.out.println("After:");
        printBoard(board2);
    }

    private static void printBoard(int[][] board) {
        for (int[] row : board) {
            for (int cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }

}
