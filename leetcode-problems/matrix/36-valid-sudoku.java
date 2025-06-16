import java.util.*;
/*
 * Determine if a 9 x 9 Sudoku board is valid. Only the filled cells need to be
 * validated according to the following rules:
 * 
 * Each row must contain the digits 1-9 without repetition.
 * Each column must contain the digits 1-9 without repetition.
 * Each of the nine 3 x 3 sub-boxes of the grid must contain the digits 1-9
 * without repetition.
 * Note:
 * 
 * A Sudoku board (partially filled) could be valid but is not necessarily
 * solvable.
 * Only the filled cells need to be validated according to the mentioned rules.
 * 
 * 
 * Example 1:
 * Input: board =[
 * ["5","3",".",".","7",".",".",".","."],
 * ["6",".",".","1","9","5",".",".","."],
 * [".","9","8",".",".",".",".","6","."],
 * ["8",".",".",".","6",".",".",".","3"],
 * ["4",".",".","8",".","3",".",".","1"],
 * ["7",".",".",".","2",".",".",".","6"],
 * [".","6",".",".",".",".","2","8","."],
 * [".",".",".","4","1","9",".",".","5"],
 * [".",".",".",".","8",".",".","7","9"]]
 * Output: true
 *
 * Example 2:
 * Input: board =
 * ["8","3",".",".","7",".",".",".","."],
 * ["6",".",".","1","9","5",".",".","."],
 * [".","9","8",".",".",".",".","6","."],
 * ["8",".",".",".","6",".",".",".","3"],
 * ["4",".",".","8",".","3",".",".","1"],
 * ["7",".",".",".","2",".",".",".","6"],
 * [".","6",".",".",".",".","2","8","."],
 * [".",".",".","4","1","9",".",".","5"],
 * [".",".",".",".","8",".",".","7","9"]]
 * Output: false
 * Explanation: Same as Example 1, except with the 5 in the top left corner
 * being modified to 8. Since there are two 8's in the top left 3x3 sub-box, it
 * is invalid.
 */

class ValidSudoku {

    /**
     * Clean and Readable Solution using HashSets
     * Time Complexity: O(1) - since board is fixed 9x9
     * Space Complexity: O(1) - maximum 27 sets with 9 elements each
     */
    public boolean isValidSudoku(char[][] board) {
        // Use sets to track seen numbers
        Set<Character>[] rows = new HashSet[9];
        Set<Character>[] cols = new HashSet[9];
        Set<Character>[] boxes = new HashSet[9];

        // Initialize sets
        for (int i = 0; i < 9; i++) {
            rows[i] = new HashSet<>();
            cols[i] = new HashSet<>();
            boxes[i] = new HashSet<>();
        }

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                char num = board[i][j];

                if (num != '.') {
                    // Calculate box index: (row/3)*3 + col/3
                    int boxIndex = (i / 3) * 3 + j / 3;

                    // Check if number already exists
                    if (rows[i].contains(num) ||
                            cols[j].contains(num) ||
                            boxes[boxIndex].contains(num)) {
                        return false;
                    }

                    // Add number to respective sets
                    rows[i].add(num);
                    cols[j].add(num);
                    boxes[boxIndex].add(num);
                }
            }
        }

        return true;
    }

    /**
     * One-Pass Solution using String concatenation for unique keys
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     */
    public boolean isValidSudokuOnePass(char[][] board) {
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                char num = board[i][j];

                if (num != '.') {
                    // Create unique identifiers for each constraint
                    String rowKey = num + " in row " + i;
                    String colKey = num + " in col " + j;
                    String boxKey = num + " in box " + i / 3 + "-" + j / 3;

                    if (!seen.add(rowKey) || !seen.add(colKey) || !seen.add(boxKey)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Optimized Solution using boolean arrays (most memory efficient)
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     */
    public boolean isValidSudokuOptimized(char[][] board) {
        // Boolean arrays to track if digit is seen
        boolean[][] rows = new boolean[9][9]; // rows[i][num-1] = digit (num) seen in row i
        boolean[][] cols = new boolean[9][9]; // cols[j][num-1] = digit (num) seen in col j
        boolean[][] boxes = new boolean[9][9]; // boxes[boxIdx][num-1] = digit (num) seen in box boxIdx

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] != '.') {
                    int num = board[i][j] - '1'; // Convert '1'-'9' to 0-8
                    int boxIndex = (i / 3) * 3 + j / 3;

                    // Check if already seen
                    if (rows[i][num] || cols[j][num] || boxes[boxIndex][num]) {
                        return false;
                    }

                    // Mark as seen
                    rows[i][num] = true;
                    cols[j][num] = true;
                    boxes[boxIndex][num] = true;
                }
            }
        }

        return true;
    }

    /**
     * Bit Manipulation Solution (most space efficient)
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     */
    public boolean isValidSudokuBitwise(char[][] board) {
        int[] rows = new int[9]; // Bitmask for each row
        int[] cols = new int[9]; // Bitmask for each column
        int[] boxes = new int[9]; // Bitmask for each box

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] != '.') {
                    int digit = board[i][j] - '1'; // Convert to 0-8
                    int mask = 1 << digit; // Create bitmask for this digit
                    int boxIndex = (i / 3) * 3 + j / 3;

                    // Check if bit is already set (digit already seen)
                    if ((rows[i] & mask) != 0 ||
                            (cols[j] & mask) != 0 ||
                            (boxes[boxIndex] & mask) != 0) {
                        return false;
                    }

                    // Set the bit
                    rows[i] |= mask;
                    cols[j] |= mask;
                    boxes[boxIndex] |= mask;
                }
            }
        }

        return true;
    }

    /**
     * Three-Pass Solution (separate validation for each rule)
     * More readable but less efficient
     */
    public boolean isValidSudokuThreePass(char[][] board) {
        return validateRows(board) && validateCols(board) && validateBoxes(board);
    }

    private boolean validateRows(char[][] board) {
        for (int i = 0; i < 9; i++) {
            Set<Character> seen = new HashSet<>();
            for (int j = 0; j < 9; j++) {
                if (board[i][j] != '.' && !seen.add(board[i][j])) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateCols(char[][] board) {
        for (int j = 0; j < 9; j++) {
            Set<Character> seen = new HashSet<>();
            for (int i = 0; i < 9; i++) {
                if (board[i][j] != '.' && !seen.add(board[i][j])) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateBoxes(char[][] board) {
        for (int box = 0; box < 9; box++) {
            Set<Character> seen = new HashSet<>();
            int startRow = (box / 3) * 3;
            int startCol = (box % 3) * 3;

            for (int i = startRow; i < startRow + 3; i++) {
                for (int j = startCol; j < startCol + 3; j++) {
                    if (board[i][j] != '.' && !seen.add(board[i][j])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Test cases
    public static void main(String[] args) {
        ValidSudoku solution = new ValidSudoku();

        // Test case 1 - Valid board
        char[][] board1 = {
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

        System.out.println("Test case 1 (Valid board):");
        System.out.println("Result: " + solution.isValidSudoku(board1));
        System.out.println("Expected: true\n");

        // Test case 2 - Invalid board (duplicate 8 in top-left box)
        char[][] board2 = {
                { '8', '3', '.', '.', '7', '.', '.', '.', '.' },
                { '6', '.', '.', '1', '9', '5', '.', '.', '.' },
                { '.', '9', '8', '.', '.', '.', '.', '6', '.' },
                { '8', '.', '.', '.', '6', '.', '.', '.', '3' },
                { '4', '.', '.', '8', '.', '3', '.', '.', '1' },
                { '7', '.', '.', '.', '2', '.', '.', '.', '6' },
                { '.', '6', '.', '.', '.', '.', '2', '8', '.' },
                { '.', '.', '.', '4', '1', '9', '.', '.', '5' },
                { '.', '.', '.', '.', '8', '.', '.', '7', '9' }
        };

        System.out.println("Test case 2 (Invalid board):");
        System.out.println("Result: " + solution.isValidSudoku(board2));
        System.out.println("Expected: false\n");

        // Test all solutions with both boards
        System.out.println("Testing all solutions:");
        System.out.println("HashSet solution - Board1: " + solution.isValidSudoku(board1) + ", Board2: "
                + solution.isValidSudoku(board2));
        System.out.println("OnePass solution - Board1: " + solution.isValidSudokuOnePass(board1) + ", Board2: "
                + solution.isValidSudokuOnePass(board2));
        System.out.println("Optimized solution - Board1: " + solution.isValidSudokuOptimized(board1) + ", Board2: "
                + solution.isValidSudokuOptimized(board2));
        System.out.println("Bitwise solution - Board1: " + solution.isValidSudokuBitwise(board1) + ", Board2: "
                + solution.isValidSudokuBitwise(board2));
        System.out.println("ThreePass solution - Board1: " + solution.isValidSudokuThreePass(board1) + ", Board2: "
                + solution.isValidSudokuThreePass(board2));
    }

}
