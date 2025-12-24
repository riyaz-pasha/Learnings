import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sudoku Solver - All Solution Variants
 * 
 * Problem: Fill a 9x9 Sudoku grid following standard Sudoku rules.
 * Rules: Each row, column, and 3x3 box must contain digits 1-9 exactly once.
 */
class SudokuSolverAllSolutions {

    // ==================== SOLUTION 1: Backtracking with HashSet (Clean &
    // Efficient) ====================
    /**
     * Approach: Backtracking with HashSets to track used numbers
     * 
     * Time Complexity: O(9^m) where m = number of empty cells (worst case ~9^81)
     * In practice much faster due to constraint propagation
     * Space Complexity: O(1) - Fixed size sets (27 sets of max size 9)
     * 
     * Pros: Clean code, O(1) validity checks, easy to understand
     * Cons: Extra space for HashSets (though still O(1) since fixed size)
     */
    public void solveSudoku1(char[][] board) {
        // Initialize tracking sets
        List<Set<Character>> rows = new ArrayList<>();
        List<Set<Character>> cols = new ArrayList<>();
        List<Set<Character>> boxes = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            rows.add(new HashSet<>());
            cols.add(new HashSet<>());
            boxes.add(new HashSet<>());
        }

        // Fill sets with existing numbers
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != '.') {
                    char num = board[r][c];
                    rows.get(r).add(num);
                    cols.get(c).add(num);
                    boxes.get(getBoxIndex(r, c)).add(num);
                }
            }
        }

        backtrack1(board, 0, 0, rows, cols, boxes);
    }

    private boolean backtrack1(char[][] board, int row, int col,
            List<Set<Character>> rows,
            List<Set<Character>> cols,
            List<Set<Character>> boxes) {
        // Move to next row if reached end of current row
        if (col == 9) {
            return backtrack1(board, row + 1, 0, rows, cols, boxes);
        }

        // Solved if processed all rows
        if (row == 9) {
            return true;
        }

        // Skip filled cells
        if (board[row][col] != '.') {
            return backtrack1(board, row, col + 1, rows, cols, boxes);
        }

        int boxIdx = getBoxIndex(row, col);

        // Try each digit 1-9
        for (char num = '1'; num <= '9'; num++) {
            // Check if number can be placed (O(1) HashSet lookup)
            if (rows.get(row).contains(num) ||
                    cols.get(col).contains(num) ||
                    boxes.get(boxIdx).contains(num)) {
                continue;
            }

            // Place number
            board[row][col] = num;
            rows.get(row).add(num);
            cols.get(col).add(num);
            boxes.get(boxIdx).add(num);

            // Recurse
            if (backtrack1(board, row, col + 1, rows, cols, boxes)) {
                return true;
            }

            // Backtrack
            board[row][col] = '.';
            rows.get(row).remove(num);
            cols.get(col).remove(num);
            boxes.get(boxIdx).remove(num);
        }

        return false;
    }

    private int getBoxIndex(int row, int col) {
        return (row / 3) * 3 + (col / 3);
    }

    // ==================== SOLUTION 2: Backtracking with Boolean Arrays (Most
    // Efficient) ====================
    /**
     * Approach: Use boolean arrays instead of HashSets for tracking
     * 
     * Time Complexity: O(9^m) where m = empty cells
     * Space Complexity: O(1) - Fixed size boolean arrays (243 booleans total)
     * 
     * Pros: Fastest in practice, minimal memory overhead
     * Cons: Slightly more complex indexing
     */
    public void solveSudoku2(char[][] board) {
        boolean[][] rows = new boolean[9][9]; // rows[i][num-1] = true if num in row i
        boolean[][] cols = new boolean[9][9]; // cols[j][num-1] = true if num in col j
        boolean[][] boxes = new boolean[9][9]; // boxes[k][num-1] = true if num in box k

        // Initialize with existing numbers
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != '.') {
                    int num = board[r][c] - '1';
                    rows[r][num] = true;
                    cols[c][num] = true;
                    boxes[getBoxIndex(r, c)][num] = true;
                }
            }
        }

        backtrack2(board, 0, 0, rows, cols, boxes);
    }

    private boolean backtrack2(char[][] board, int row, int col,
            boolean[][] rows, boolean[][] cols, boolean[][] boxes) {
        if (col == 9) {
            return backtrack2(board, row + 1, 0, rows, cols, boxes);
        }
        if (row == 9) {
            return true;
        }
        if (board[row][col] != '.') {
            return backtrack2(board, row, col + 1, rows, cols, boxes);
        }

        int boxIdx = getBoxIndex(row, col);

        for (int num = 0; num < 9; num++) {
            // Check if number can be placed
            if (rows[row][num] || cols[col][num] || boxes[boxIdx][num]) {
                continue;
            }

            // Place number
            board[row][col] = (char) ('1' + num);
            rows[row][num] = cols[col][num] = boxes[boxIdx][num] = true;

            if (backtrack2(board, row, col + 1, rows, cols, boxes)) {
                return true;
            }

            // Backtrack
            board[row][col] = '.';
            rows[row][num] = cols[col][num] = boxes[boxIdx][num] = false;
        }

        return false;
    }

    // ==================== SOLUTION 3: Simple Backtracking with isValid Check
    // ====================
    /**
     * Approach: Check validity on-demand without tracking structures
     * 
     * Time Complexity: O(9^m × 27) - Each placement requires checking row, col, box
     * (27 cells)
     * Space Complexity: O(1) - No extra data structures
     * 
     * Pros: Simplest code, easiest to understand
     * Cons: Slower due to repeated validity checks
     */
    public void solveSudoku3(char[][] board) {
        backtrack3(board, 0, 0);
    }

    private boolean backtrack3(char[][] board, int row, int col) {
        if (col == 9) {
            return backtrack3(board, row + 1, 0);
        }
        if (row == 9) {
            return true;
        }
        if (board[row][col] != '.') {
            return backtrack3(board, row, col + 1);
        }

        for (char num = '1'; num <= '9'; num++) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num;

                if (backtrack3(board, row, col + 1)) {
                    return true;
                }

                board[row][col] = '.';
            }
        }

        return false;
    }

    private boolean isValid(char[][] board, int row, int col, char num) {
        // Check row
        for (int c = 0; c < 9; c++) {
            if (board[row][c] == num)
                return false;
        }

        // Check column
        for (int r = 0; r < 9; r++) {
            if (board[r][col] == num)
                return false;
        }

        // Check 3x3 box
        int boxRow = (row / 3) * 3;
        int boxCol = (col / 3) * 3;
        for (int r = boxRow; r < boxRow + 3; r++) {
            for (int c = boxCol; c < boxCol + 3; c++) {
                if (board[r][c] == num)
                    return false;
            }
        }

        return true;
    }

    // ==================== SOLUTION 4: Optimized with Empty Cell List
    // ====================
    /**
     * Approach: Pre-collect empty cells to avoid scanning filled cells
     * 
     * Time Complexity: O(9^m) where m = empty cells
     * Space Complexity: O(m) for empty cell list + O(1) for tracking arrays
     * 
     * Pros: Faster - only processes empty cells, no unnecessary checks
     * Cons: Extra space for cell list
     */
    public void solveSudoku4(char[][] board) {
        // Collect all empty cells
        List<int[]> emptyCells = new ArrayList<>();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == '.') {
                    emptyCells.add(new int[] { r, c });
                }
            }
        }

        boolean[][] rows = new boolean[9][9];
        boolean[][] cols = new boolean[9][9];
        boolean[][] boxes = new boolean[9][9];

        // Initialize tracking arrays
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != '.') {
                    int num = board[r][c] - '1';
                    rows[r][num] = true;
                    cols[c][num] = true;
                    boxes[getBoxIndex(r, c)][num] = true;
                }
            }
        }

        backtrack4(board, emptyCells, 0, rows, cols, boxes);
    }

    private boolean backtrack4(char[][] board, List<int[]> emptyCells, int idx,
            boolean[][] rows, boolean[][] cols, boolean[][] boxes) {
        if (idx == emptyCells.size()) {
            return true; // All cells filled
        }

        int[] cell = emptyCells.get(idx);
        int row = cell[0], col = cell[1];
        int boxIdx = getBoxIndex(row, col);

        for (int num = 0; num < 9; num++) {
            if (rows[row][num] || cols[col][num] || boxes[boxIdx][num]) {
                continue;
            }

            board[row][col] = (char) ('1' + num);
            rows[row][num] = cols[col][num] = boxes[boxIdx][num] = true;

            if (backtrack4(board, emptyCells, idx + 1, rows, cols, boxes)) {
                return true;
            }

            board[row][col] = '.';
            rows[row][num] = cols[col][num] = boxes[boxIdx][num] = false;
        }

        return false;
    }

    // ==================== SOLUTION 5: Bitmasking (Most Compact)
    // ====================
    /**
     * Approach: Use bitmasks to track used numbers (each bit represents a digit)
     * 
     * Time Complexity: O(9^m) where m = empty cells
     * Space Complexity: O(1) - Only 27 integers for tracking
     * 
     * Pros: Most memory efficient, elegant bit operations
     * Cons: Less readable, bit manipulation complexity
     */
    public void solveSudoku5(char[][] board) {
        int[] rows = new int[9]; // Bitmask for each row
        int[] cols = new int[9]; // Bitmask for each column
        int[] boxes = new int[9]; // Bitmask for each box

        // Initialize bitmasks (bit i set means digit i+1 is used)
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != '.') {
                    int num = board[r][c] - '1';
                    int mask = 1 << num;
                    rows[r] |= mask;
                    cols[c] |= mask;
                    boxes[getBoxIndex(r, c)] |= mask;
                }
            }
        }

        backtrack5(board, 0, 0, rows, cols, boxes);
    }

    private boolean backtrack5(char[][] board, int row, int col,
            int[] rows, int[] cols, int[] boxes) {
        if (col == 9) {
            return backtrack5(board, row + 1, 0, rows, cols, boxes);
        }
        if (row == 9) {
            return true;
        }
        if (board[row][col] != '.') {
            return backtrack5(board, row, col + 1, rows, cols, boxes);
        }

        int boxIdx = getBoxIndex(row, col);

        for (int num = 0; num < 9; num++) {
            int mask = 1 << num;

            // Check if number can be placed using bitwise AND
            if ((rows[row] & mask) != 0 ||
                    (cols[col] & mask) != 0 ||
                    (boxes[boxIdx] & mask) != 0) {
                continue;
            }

            // Place number by setting bits
            board[row][col] = (char) ('1' + num);
            rows[row] |= mask;
            cols[col] |= mask;
            boxes[boxIdx] |= mask;

            if (backtrack5(board, row, col + 1, rows, cols, boxes)) {
                return true;
            }

            // Backtrack by clearing bits
            board[row][col] = '.';
            rows[row] &= ~mask;
            cols[col] &= ~mask;
            boxes[boxIdx] &= ~mask;
        }

        return false;
    }

    // ==================== Helper Methods ====================

    private void printBoard(char[][] board) {
        System.out.println("╔═══════╤═══════╤═══════╗");
        for (int i = 0; i < 9; i++) {
            if (i == 3 || i == 6) {
                System.out.println("╟───────┼───────┼───────╢");
            }
            System.out.print("║ ");
            for (int j = 0; j < 9; j++) {
                System.out.print(board[i][j] + " ");
                if (j == 2 || j == 5) {
                    System.out.print("│ ");
                }
            }
            System.out.println("║");
        }
        System.out.println("╚═══════╧═══════╧═══════╝");
    }

    private char[][] copyBoard(char[][] board) {
        char[][] copy = new char[9][9];
        for (int i = 0; i < 9; i++) {
            copy[i] = board[i].clone();
        }
        return copy;
    }

    private boolean boardsEqual(char[][] b1, char[][] b2) {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (b1[i][j] != b2[i][j])
                    return false;
            }
        }
        return true;
    }

    // ==================== Test All Solutions ====================
    public static void main(String[] args) {
        SudokuSolverAllSolutions solver = new SudokuSolverAllSolutions();

        System.out.println("=".repeat(80));
        System.out.println("SUDOKU SOLVER - ALL SOLUTION VARIANTS");
        System.out.println("=".repeat(80));

        // Test case from problem
        char[][] testBoard = {
                { '5', '3', '.', '.', '7', '.', '.', '.', '.' },
                { '6', '.', '.', '1', '9', '5', '.', '.', '.' },
                { '.', '9', '8', '.', '.', '.', '.', '6', '.' },
                { '8', '.', '.', '.', '6', '.', '.', '.', '3' },
                { '4', '.', '.', '8', '.', '3', '.', '.', '1' },
                { '7', '.', '.', '.', '2', '.', '.', '.', '6' },
                { '.', '6', '.', '.', '.', '.', '2', '8', '.' },
                { '.', '.', '.', '4', '1', '9', '.', '.', '5' },
                { '.', '.', '.', '.', '8', '.', '.', '7', '9' }
        };

        System.out.println("\nOriginal Puzzle:");
        solver.printBoard(testBoard);

        // Test all solutions
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Testing All Solutions:");
        System.out.println("=".repeat(80));

        // Solution 1: HashSet
        char[][] board1 = solver.copyBoard(testBoard);
        long start = System.nanoTime();
        solver.solveSudoku1(board1);
        long end = System.nanoTime();
        System.out.println("\nSolution 1 (HashSet Tracking):");
        System.out.printf("Time: %.4f ms%n", (end - start) / 1_000_000.0);
        solver.printBoard(board1);

        // Solution 2: Boolean Arrays
        char[][] board2 = solver.copyBoard(testBoard);
        start = System.nanoTime();
        solver.solveSudoku2(board2);
        end = System.nanoTime();
        System.out.println("\nSolution 2 (Boolean Arrays):");
        System.out.printf("Time: %.4f ms%n", (end - start) / 1_000_000.0);
        System.out.println("Match with Solution 1: " + solver.boardsEqual(board1, board2));

        // Solution 3: Simple with isValid
        char[][] board3 = solver.copyBoard(testBoard);
        start = System.nanoTime();
        solver.solveSudoku3(board3);
        end = System.nanoTime();
        System.out.println("\nSolution 3 (Simple with isValid):");
        System.out.printf("Time: %.4f ms%n", (end - start) / 1_000_000.0);
        System.out.println("Match with Solution 1: " + solver.boardsEqual(board1, board3));

        // Solution 4: Empty Cell List
        char[][] board4 = solver.copyBoard(testBoard);
        start = System.nanoTime();
        solver.solveSudoku4(board4);
        end = System.nanoTime();
        System.out.println("\nSolution 4 (Empty Cell List):");
        System.out.printf("Time: %.4f ms%n", (end - start) / 1_000_000.0);
        System.out.println("Match with Solution 1: " + solver.boardsEqual(board1, board4));

        // Solution 5: Bitmasking
        char[][] board5 = solver.copyBoard(testBoard);
        start = System.nanoTime();
        solver.solveSudoku5(board5);
        end = System.nanoTime();
        System.out.println("\nSolution 5 (Bitmasking):");
        System.out.printf("Time: %.4f ms%n", (end - start) / 1_000_000.0);
        System.out.println("Match with Solution 1: " + solver.boardsEqual(board1, board5));

        // Test with harder puzzle
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Testing with Harder Puzzle:");
        System.out.println("=".repeat(80));

    }
}
