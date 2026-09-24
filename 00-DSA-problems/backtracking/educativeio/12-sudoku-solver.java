/**
 * ============================================================================
 * SUDOKU SOLVER - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We are given a classic 9x9 Sudoku puzzle. Some cells are filled with digits 
 * ('1'-'9'), and empty cells are marked with a dot ('.'). We need to fill in 
 * the blanks such that every row, every column, and every 3x3 sub-box contains 
 * the digits 1 through 9 exactly once. We are guaranteed there is exactly 
 * one unique solution.
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Can I modify the input board directly?
 * A: Yes, solving it in-place is the expected approach to save space.
 * 
 * Q: Do I need to validate the initially given board?
 * A: The problem guarantees the input has exactly one valid solution, so we 
 *    can safely assume the starting state is valid.
 * 
 * Q: What should the function return?
 * A: Typically, the main function modifies the board in-place and returns void. 
 *    However, our recursive helper function should return a boolean to signal 
 *    when the puzzle is completely solved.
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - BACKTRACKING: This is the ultimate "guess and check" problem. We find an 
 *   empty cell, guess a number (1-9), check if it's allowed, and if so, move 
 *   on to the next empty cell. If we get stuck later, we undo our guess and 
 *   try the next number.
 * - HALTING EARLY: Unlike the N-Queens problem where we wanted to find ALL 
 *   solutions, here we only want ONE. Because of this, our recursive function 
 *   must return `true` as soon as it finishes, cascading that `true` all the 
 *   way up the call stack to immediately stop exploring other branches.
 * - THE 3x3 BOX MATH: The trickiest part is checking the 3x3 sub-box. For any 
 *   given cell at `(row, col)`, the top-left corner of its 3x3 box is at 
 *   `row - (row % 3)` and `col - (col % 3)`, or simply `(row / 3) * 3` and 
 *   `(col / 3) * 3` using integer division.
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Write the main skeleton. Explain that you will write a recursive 
 *   backtracking function that returns a boolean.
 * - Step 2: Implement the `isValid` helper function first. Explain the math 
 *   behind checking the 3x3 sub-box using a single loop from 0 to 8. This 
 *   shows strong algorithmic thinking.
 * - Step 3: Write the recursive DFS. Heavily emphasize the "Choose, Explore, 
 *   Unchoose" pipeline and explain WHY you return `false` at the end of the 1-9 loop.
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Checking 3x3 box for cell (5, 4):
 * row / 3 = 5 / 3 = 1. Start row for box = 1 * 3 = 3.
 * col / 3 = 4 / 3 = 1. Start col for box = 1 * 3 = 3.
 * 
 * Using a 1D loop `i` from 0 to 8 to traverse a 3x3 block:
 * i = 0 -> (3 + 0/3, 3 + 0%3) -> (3, 3)
 * i = 1 -> (3 + 1/3, 3 + 1%3) -> (3, 4)
 * ...
 * i = 8 -> (3 + 8/3, 3 + 8%3) -> (5, 5)
 */

import java.util.*;

public class SudokuSolver {

    public void solveSudoku(char[][] board) {
        /*
         * Start the recursive backtracking process.
         * The helper returns a boolean to stop execution the moment 
         * the one guaranteed solution is found.
         */
        solve(board);
    }

    private boolean solve(char[][] board) {
        
        /*
         * ============================================================
         * FINDING THE NEXT DECISION
         * ============================================================
         * 
         * We iterate through the entire board to find the FIRST empty 
         * cell (marked with '.').
         * 
         * Once we find an empty cell, we must make a decision: 
         * "Which digit from 1 to 9 should go here?"
         */
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                
                // We found an empty cell! We must fill it.
                if (board[r][c] == '.') {
                    
                    /*
                     * ============================================================
                     * THE DECISION LOOP
                     * ============================================================
                     * 
                     * We try every possible valid digit from '1' to '9'.
                     */
                    for (char digit = '1'; digit <= '9'; digit++) {
                        
                        // Check if placing 'digit' at (r, c) violates Sudoku rules
                        if (isValid(board, r, c, digit)) {
                            
                            /*
                             * ----------------------------------------------------
                             * CHOOSE
                             * ----------------------------------------------------
                             * The digit is valid so far. Place it on the board.
                             */
                            board[r][c] = digit;
                            
                            /*
                             * ----------------------------------------------------
                             * EXPLORE
                             * ----------------------------------------------------
                             * Recursively attempt to solve the REST of the board.
                             * 
                             * If solve() returns true, it means this current path 
                             * successfully filled the entire board! We immediately 
                             * return true to cascade this success up the call stack, 
                             * avoiding any further unnecessary exploration.
                             */
                            if (solve(board)) {
                                return true; 
                            }
                            
                            /*
                             * ----------------------------------------------------
                             * UNCHOOSE (BACKTRACK)
                             * ----------------------------------------------------
                             * If solve() returned false, it means choosing this 
                             * 'digit' eventually led to a dead end. 
                             * 
                             * We must UNDO our choice, resetting the cell to '.', 
                             * so the loop can try the NEXT digit.
                             */
                            board[r][c] = '.';
                        }
                    }
                    
                    /*
                     * ============================================================
                     * DEAD END REACHED
                     * ============================================================
                     * 
                     * If we tried all digits from 1 to 9 in this empty cell and 
                     * NONE of them led to a solution, it means a previous guess 
                     * (higher up in the call stack) was WRONG. 
                     * 
                     * We must return false to trigger backtracking in the parent call.
                     */
                    return false;
                }
            }
        }
        
        /*
         * ============================================================
         * BASE CASE (SUCCESS)
         * ============================================================
         * 
         * If the nested loops finish without finding ANY '.' characters, 
         * it means the board is completely full and completely valid.
         * We have solved the puzzle!
         */
        return true;
    }

    /**
     * Checks if placing 'char c' at board[row][col] is valid.
     */
    private boolean isValid(char[][] board, int row, int col, char c) {
        
        /*
         * ============================================================
         * O(1) TIME - 1D LOOP TRICK
         * ============================================================
         * 
         * Instead of writing three separate loops for the row, column, 
         * and 3x3 block, we can do it all in a single loop from 0 to 8.
         */
        for (int i = 0; i < 9; i++) {
            
            // 1. Check the Column
            // Is 'c' already present anywhere in this specific column?
            if (board[i][col] == c) return false;
            
            // 2. Check the Row
            // Is 'c' already present anywhere in this specific row?
            if (board[row][i] == c) return false;
            
            // 3. Check the 3x3 Block
            // (row / 3) * 3 gives the starting row index of the 3x3 block.
            // i / 3 acts as the row offset inside the block (0,0,0, 1,1,1, 2,2,2)
            // (col / 3) * 3 gives the starting col index of the 3x3 block.
            // i % 3 acts as the col offset inside the block (0,1,2, 0,1,2, 0,1,2)
            int blockRow = 3 * (row / 3) + (i / 3);
            int blockCol = 3 * (col / 3) + (i % 3);
            
            if (board[blockRow][blockCol] == c) return false;
        }
        
        return true; // The digit 'c' is safe to place!
    }

    /**
     * UTILITY: Print the board beautifully.
     */
    private static void printBoard(char[][] board) {
        for (int r = 0; r < 9; r++) {
            if (r % 3 == 0 && r != 0) {
                System.out.println("---------------------");
            }
            for (int c = 0; c < 9; c++) {
                if (c % 3 == 0 && c != 0) {
                    System.out.print("| ");
                }
                System.out.print(board[r][c] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        SudokuSolver solver = new SudokuSolver();
        
        char[][] board = {
            {'5','3','.','.','7','.','.','.','.'},
            {'6','.','.','1','9','5','.','.','.'},
            {'.','9','8','.','.','.','.','6','.'},
            {'8','.','.','.','6','.','.','.','3'},
            {'4','.','.','8','.','3','.','.','1'},
            {'7','.','.','.','2','.','.','.','6'},
            {'.','6','.','.','.','.','2','8','.'},
            {'.','.','.','4','1','9','.','.','5'},
            {'.','.','.','.','8','.','.','7','9'}
        };

        System.out.println("--- Initial Board ---");
        printBoard(board);

        solver.solveSudoku(board);

        System.out.println("--- Solved Board ---");
        printBoard(board);
    }
}

public class SudokuSolver {

    /**
     * Entry function — modifies board in-place
     */
    public static void solveSudoku(char[][] board) {

        // ============================================================
        // 🧠 IDEA:
        // Instead of checking row/col/box every time (O(9)),
        // we precompute and store used numbers → O(1) checks
        // ============================================================

        // row[r][num] = true → number already used in row r
        boolean[][] row = new boolean[9][10];

        // col[c][num] = true → number already used in column c
        boolean[][] col = new boolean[9][10];

        // box[b][num] = true → number already used in 3x3 box b
        boolean[][] box = new boolean[9][10];

        // ============================================================
        // STEP 1: Pre-fill constraints from given board
        // ============================================================
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {

                if (board[r][c] != '.') {

                    int num = board[r][c] - '0';

                    // 🧩 Compute which 3x3 box this cell belongs to
                    // Formula explanation:
                    // - (r / 3) → which row-block (0,1,2)
                    // - (c / 3) → which col-block (0,1,2)
                    // Combine → unique box index from 0 to 8
                    int boxIndex = (r / 3) * 3 + (c / 3);

                    // Mark this number as used
                    row[r][num] = true;
                    col[c][num] = true;
                    box[boxIndex][num] = true;
                }
            }
        }

        // ============================================================
        // STEP 2: Start backtracking
        // ============================================================
        solve(board, row, col, box);
    }

    /**
     * Backtracking function
     *
     * Returns true if solution is found
     */
    private static boolean solve(char[][] board,
                                 boolean[][] row,
                                 boolean[][] col,
                                 boolean[][] box) {

        // Traverse entire board
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {

                // ====================================================
                // 🔍 Find EMPTY CELL
                // ====================================================
                if (board[r][c] == '.') {

                    // ====================================================
                    // 🎯 Try placing numbers from 1 → 9
                    // ====================================================
                    for (int num = 1; num <= 9; num++) {

                        int boxIndex = (r / 3) * 3 + (c / 3);

                        // ====================================================
                        // ✅ VALIDATION (O(1))
                        // Check if num is unused in:
                        // - row
                        // - column
                        // - 3x3 box
                        // ====================================================
                        if (!row[r][num] && !col[c][num] && !box[boxIndex][num]) {

                            // ====================================================
                            // 🟢 PLACE NUMBER
                            // ====================================================
                            board[r][c] = (char) (num + '0');

                            row[r][num] = true;
                            col[c][num] = true;
                            box[boxIndex][num] = true;

                            // ====================================================
                            // 🔁 RECURSIVE CALL
                            // Solve rest of board
                            // ====================================================
                            if (solve(board, row, col, box)) {
                                return true; // ✅ Solution found → stop further work
                            }

                            // ====================================================
                            // 🔴 BACKTRACK (UNDO)
                            // If placing num didn't lead to solution,
                            // revert all changes
                            // ====================================================
                            board[r][c] = '.';

                            row[r][num] = false;
                            col[c][num] = false;
                            box[boxIndex][num] = false;
                        }
                    }

                    // ====================================================
                    // ❌ DEAD END
                    // No number fits here → backtrack to previous cell
                    // ====================================================
                    return false;
                }
            }
        }

        // ============================================================
        // ✅ BASE CASE
        // No empty cells left → board solved
        // ============================================================
        return true;
    }
}
