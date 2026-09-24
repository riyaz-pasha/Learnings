/**
 * ============================================================================
 * N-QUEENS - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We have an n x n chessboard. We need to place exactly n chess Queens on it. 
 * A Queen can attack anything in her row, column, or both diagonals. 
 * Our goal is to find every single possible way to place these n queens such 
 * that no two queens are threatening each other. We must return each valid 
 * board layout as a list of strings, where 'Q' is a queen and '.' is an empty 
 * space.
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Is it guaranteed that a solution exists for any n?
 * A: No. For n = 2 or n = 3, there are no valid arrangements. Our algorithm 
 *    should simply return an empty list for those cases.
 * 
 * Q: What are the constraints on n?
 * A: The problem specifies 1 <= n <= 9. This constraint is the biggest clue! 
 *    Placing n queens is a classic factorial time complexity problem, O(N!). 
 *    Because 9! is 362,880, a Backtracking approach will easily execute 
 *    within the time limit.
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - ROW-BY-ROW PLACEMENT: We know that no two queens can share the same row. 
 *   Therefore, we can drastically simplify the problem: we just need to place 
 *   exactly ONE queen in row 0, exactly ONE queen in row 1, and so on.
 * - CONSTANT TIME ATTACK CHECKS: To check if a cell (row, col) is safe, we 
 *   *could* scan the entire board. But that's slow. Instead, we can use three 
 *   boolean arrays to act as lookup tables:
 *      1. Is there a queen in this Column?
 *      2. Is there a queen on this Main Diagonal (\)?
 *      3. Is there a queen on this Anti-Diagonal (/)?
 * - DIAGONAL MATH: 
 *   - For any cell on a specific Main Diagonal (\), the difference between its 
 *     row and column (row - col) is constant.
 *   - For any cell on a specific Anti-Diagonal (/), the sum of its row and 
 *     column (row + col) is constant.
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Explain the naive approach (checking every cell) and immediately 
 *   pivot to the row-by-row Backtracking approach.
 * - Step 2: Draw a small 4x4 grid and show the math behind the diagonals. This 
 *   proves you understand the constant-time lookup optimization.
 * - Step 3: Write the DFS/Backtracking code. Emphasize the "Choose, Explore, 
 *   Unchoose" pipeline to show a structured thinking process.
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * n = 4
 * 
 * Diagonals Math for 4x4:
 * Anti-Diagonal (row + col):    Main Diagonal (row - col + n - 1):
 * [0, 1, 2, 3]                  [3, 4, 5, 6]
 * [1, 2, 3, 4]                  [2, 3, 4, 5]
 * [2, 3, 4, 5]                  [1, 2, 3, 4]
 * [3, 4, 5, 6]                  [0, 1, 2, 3]
 * 
 * Valid 4x4 solution:
 * . Q . .   (row 0, col 1)
 * . . . Q   (row 1, col 3)
 * Q . . .   (row 2, col 0)
 * . . Q .   (row 3, col 2)
 */

import java.util.*;

public class NQueens {

    public List<List<String>> solveNQueens(int n) {
        List<List<String>> result = new ArrayList<>();
        
        // Initialize an empty board filled with '.'
        char[][] board = new char[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                board[i][j] = '.';
            }
        }
        
        /*
         * We need to keep track of columns and diagonals that are already 
         * occupied by queens.
         * 
         * cols: size n (tracks which of the n columns have a queen)
         * diag1: size 2n - 1 (Main diagonals \ where row - col is constant)
         * diag2: size 2n - 1 (Anti-diagonals / where row + col is constant)
         */
        boolean[] cols = new boolean[n];
        boolean[] diag1 = new boolean[2 * n - 1]; 
        boolean[] diag2 = new boolean[2 * n - 1];
        
        // Start placing queens from row 0
        backtrack(0, board, cols, diag1, diag2, result);
        
        return result;
    }

    private void backtrack(int row, char[][] board, boolean[] cols, 
                           boolean[] diag1, boolean[] diag2, List<List<String>> result) {
        
        int n = board.length;

        /*
         * ============================================================
         * BASE CASE
         * ============================================================
         * 
         * If we have successfully placed a queen in every row, the 'row' 
         * variable will equal 'n' (since we started at 0).
         * This means we have a complete and valid board!
         * We convert the char[][] to a List<String> and add it to results.
         */
        if (row == n) {
            result.add(constructBoard(board));
            return;
        }

        /*
         * ============================================================
         * THE CORE DECISION LOOP
         * ============================================================
         * 
         * For the current 'row', we must decide WHICH column 'col' to 
         * place the queen in. We loop through all possible columns.
         */
        for (int col = 0; col < n; col++) {
            
            /*
             * ------------------------------------------------------------
             * PRUNING (ATTACK CHECKS)
             * ------------------------------------------------------------
             * 
             * Before placing the queen, check if the cell is under attack.
             * 
             * Main Diagonal formula: row - col. 
             * To prevent negative indices in our array, we add (n - 1).
             * Resulting formula: row - col + n - 1
             * 
             * Anti-Diagonal formula: row + col
             * 
             * If ANY of these three lines are occupied, we CANNOT place 
             * a queen here. Skip to the next column.
             */
            int d1 = row - col + n - 1;
            int d2 = row + col;
            
            if (cols[col] || diag1[d1] || diag2[d2]) {
                continue;
            }

            /*
             * ============================================================
             * CHOOSE
             * ============================================================
             * 
             * The cell is safe! 
             * We are thinking: "Let's TRY placing the queen here."
             * We mark the board, the column, and both diagonals as occupied.
             */
            board[row][col] = 'Q';
            cols[col] = true;
            diag1[d1] = true;
            diag2[d2] = true;

            /*
             * ============================================================
             * EXPLORE
             * ============================================================
             * 
             * Now that a queen is placed in this row, we move down to the 
             * next row to place the next queen.
             */
            backtrack(row + 1, board, cols, diag1, diag2, result);

            /*
             * ============================================================
             * UNCHOOSE (BACKTRACK)
             * ============================================================
             * 
             * The exploration of this path is completely finished (whether 
             * it succeeded or failed). 
             * 
             * We must UNDO our choice so we can try the NEXT column in 
             * the current row. This restores the state exactly as it was 
             * before we made the decision.
             */
            board[row][col] = '.';
            cols[col] = false;
            diag1[d1] = false;
            diag2[d2] = false;
        }
    }

    /*
     * UTILITY: Converts the char[][] board into the required List<String> format.
     */
    private List<String> constructBoard(char[][] board) {
        List<String> validBoard = new ArrayList<>();
        for (int i = 0; i < board.length; i++) {
            validBoard.add(new String(board[i]));
        }
        return validBoard;
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        NQueens solver = new NQueens();
        
        System.out.println("--- Test Case: n = 4 ---");
        List<List<String>> solutions4 = solver.solveNQueens(4);
        System.out.println("Total solutions: " + solutions4.size());
        for (int i = 0; i < solutions4.size(); i++) {
            System.out.println("Solution " + (i + 1) + ":");
            for (String row : solutions4.get(i)) {
                System.out.println(row);
            }
            System.out.println();
        }

        System.out.println("--- Test Case: n = 1 ---");
        List<List<String>> solutions1 = solver.solveNQueens(1);
        System.out.println("Total solutions: " + solutions1.size());
        for (String row : solutions1.get(0)) {
            System.out.println(row);
        }
    }
}


/**
 * ============================================================
 * 🔥 N-Queens — BACKTRACKING MASTER TEMPLATE
 * ============================================================
 *
 * IDEA:
 * -----
 * We place queens ROW BY ROW.
 *
 * At each row:
 *   -> Try placing queen in each column
 *   -> Check if it's SAFE
 *   -> If safe → place it and go to next row
 *   -> After recursion → REMOVE (backtrack)
 *
 * WHY ROW BY ROW?
 * ---------------
 * Because:
 *   - Each row must have exactly ONE queen
 *   - So we eliminate row conflicts automatically
 *
 * WHAT WE NEED TO TRACK:
 * ---------------------
 * 1. Columns → no two queens in same column
 * 2. Diagonal → (row + col)
 * 3. Anti-diagonal → (row - col + n - 1)
 *
 * COMPLEXITY:
 * -----------
 * Time  : O(N!) (worst case)
 * Space : O(N^2) for board + O(N) recursion
 *
 * INTERVIEW TIP:
 * --------------
 * Think:
 *   "At each row, I have N choices → branching → backtracking tree"
 *
 * ============================================================
 */

public class NQueens {

    public static List<List<String>> solveNQueens(int n) {

        // Final result
        List<List<String>> result = new ArrayList<>();

        // Board representation
        char[][] board = new char[n][n];

        // Initialize board with '.'
        for (char[] row : board) {
            Arrays.fill(row, '.');
        }

        // These arrays help us check conflicts in O(1)
        boolean[] colUsed = new boolean[n];
        boolean[] diagUsed = new boolean[2 * n];       // row + col
        boolean[] antiDiagUsed = new boolean[2 * n];   // row - col + (n-1)

        // Start backtracking from row 0
        backtrack(0, board, result, colUsed, diagUsed, antiDiagUsed, n);

        return result;
    }

    private static void backtrack(
            int row,
            char[][] board,
            List<List<String>> result,
            boolean[] colUsed,
            boolean[] diagUsed,
            boolean[] antiDiagUsed,
            int n
    ) {

        /**
         * BASE CASE:
         * ----------
         * If we placed queens in all rows → valid solution
         */
        if (row == n) {
            result.add(construct(board));
            return;
        }

        /**
         * TRY ALL COLUMNS FOR CURRENT ROW
         */
        for (int col = 0; col < n; col++) {

            int diag = row + col;
            int antiDiag = row - col + (n - 1);

            /**
             * CHECK IF SAFE:
             * --------------
             * - Column not used
             * - Diagonal not used
             * - Anti-diagonal not used
             */
            if (colUsed[col] || diagUsed[diag] || antiDiagUsed[antiDiag]) {
                continue; // Not safe → skip
            }

            /**
             * PLACE QUEEN
             */
            board[row][col] = 'Q';
            colUsed[col] = true;
            diagUsed[diag] = true;
            antiDiagUsed[antiDiag] = true;

            /**
             * RECURSE FOR NEXT ROW
             */
            backtrack(row + 1, board, result, colUsed, diagUsed, antiDiagUsed, n);

            /**
             * BACKTRACK (UNDO)
             * ----------------
             * This is the MOST IMPORTANT step
             */
            board[row][col] = '.';
            colUsed[col] = false;
            diagUsed[diag] = false;
            antiDiagUsed[antiDiag] = false;
        }
    }

    /**
     * Convert board to required output format
     */
    private static List<String> construct(char[][] board) {
        List<String> solution = new ArrayList<>();

        for (char[] row : board) {
            solution.add(new String(row));
        }

        return solution;
    }

    public static void main(String[] args) {
        int n = 4;
        List<List<String>> res = solveNQueens(n);

        for (List<String> board : res) {
            System.out.println(board);
            System.out.println();
        }
    }
}
