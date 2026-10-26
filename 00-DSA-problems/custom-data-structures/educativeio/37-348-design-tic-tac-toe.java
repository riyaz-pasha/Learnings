/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Assume the following rules are for the tic-tac-toe game on an n x n board:
 * 1. A move is guaranteed to be valid and is placed on an empty block.
 * 2. Once a winning condition is reached, no more moves are allowed.
 * 3. A player who succeeds in placing n of their marks in a horizontal, 
 *    vertical, or diagonal row wins the game.
 * 
 * Implement the TicTacToe class:
 * - TicTacToe(int n): Initializes the object the size of the board n.
 * - int move(int row, int col, int player): Indicates that the player with 
 *   id player (1 or 2) makes a move at the cell (row, col). Returns 0 if no 
 *   one wins, 1 if player 1 wins, and 2 if player 2 wins.
 * 
 * Constraints:
 * - 2 <= n <= 100
 * - player is 1 or 2.
 * - 0 <= row, col < n
 * - At most n^2 calls will be made to move.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Do we need to handle invalid moves (e.g., placing on an already occupied cell)?
 *    (No, the prompt explicitly states moves are guaranteed to be valid).
 * 2. Is it possible for a move to trigger a win on multiple axes simultaneously?
 *    (Yes, placing the final piece in a corner could win a row, column, and diagonal 
 *    all at once. Returning the player ID upon any matching condition is sufficient).
 * 3. Do we need to check for a "draw" condition?
 *    (The prompt only asks to return 1, 2, or 0. If the board fills up and no one 
 *    wins, we just continue returning 0).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Determine if a specific player has occupied an entire 
 *   row, column, or diagonal immediately after their move. The binding constraint 
 *   is making this evaluation instantly without scanning the board.
 * 
 * --- Approach 1: 2D Array Simulation (Brute Force) ---
 * 1. Intuition: Keep an `int[][] board` of size n x n.
 * 2. Try: On every `move(row, col, player)`, update the board. Then, run a `for` 
 *    loop across the entire `row`, the entire `col`, and both diagonals to check 
 *    if all pieces belong to `player`.
 * 3. Why it works: Perfectly simulates the game.
 * 4. Why it's suboptimal: Scanning the row and column takes O(n) time for every move. 
 *    If the board scales to 1000x1000, checking every move is too slow.
 * 5. Bottleneck: Verifying static historical data over and over.
 * 
 * --- Approach 2: O(1) Counter Arrays (Optimal) ---
 * 1. Intuition: To win, a player needs `n` pieces in a row. We don't actually care 
 *    *where* the pieces are in that row, just *how many* pieces the player has in it.
 * 2. Try: We could use two arrays for Player 1 (`p1_rows`, `p1_cols`) and two arrays 
 *    for Player 2. But we can optimize even further using a mathematical trick!
 * 3. The +/- 1 Trick: We use a single array for rows and a single array for columns. 
 *    - When Player 1 makes a move, we ADD 1 to that row and column counter.
 *    - When Player 2 makes a move, we SUBTRACT 1 from that row and column counter.
 *    - If any counter reaches exactly `n`, Player 1 wins!
 *    - If any counter reaches exactly `-n`, Player 2 wins!
 * 4. Which to write in an interview?
 *    Approach 2. It demonstrates the ability to mathematically compress state, reducing 
 *    both Time Complexity to O(1) and Space Complexity from O(n^2) to O(n).
 * 
 * Time Complexity: O(1) per move.
 * Space Complexity: O(n) to store the row and col counter arrays.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Minimum board size (n = 2): Logic holds perfectly. A single move updates a row, 
 *   a column, and potentially a diagonal, reaching a max sum of 1.
 * - Anti-Diagonal Logic: How do we know if a move is on the anti-diagonal (bottom-left 
 *   to top-right)? The mathematical property is `row + col == n - 1`.
 * - Intersecting moves: If Player 1 plays on a row, and Player 2 plays on the same row, 
 *   the counter becomes 0. This perfectly ruins the row for BOTH players, which is exactly 
 *   how Tic-Tac-Toe works. Neither player can ever reach `n` or `-n` on that row again.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "State Cancellation." 
 *   Since a single cell can only be occupied by one player, and a mixed row can 
 *   never result in a win for anyone, adding 1 for P1 and subtracting 1 for P2 
 *   permanently prevents that row's absolute sum from ever reaching 'n'. 
 * 
 * ASCII DRY RUN (n = 3)
 * P1 plays (0, 0)
 * P2 plays (0, 2)
 * P1 plays (1, 1)
 * P1 plays (2, 2)
 * 
 * Internals:
 * rows = [0, 0, 0], cols = [0, 0, 0], diag = 0, antiDiag = 0
 * 
 * 1. P1(0, 0) -> +1
 *    rows[0]=1, cols[0]=1, diag=1
 * 2. P2(0, 2) -> -1
 *    rows[0]=0, cols[2]=-1, antiDiag=-1 (since 0+2 == 3-1)
 *    *Notice rows[0] is 0! Neither player can win Row 0 now.*
 * 3. P1(1, 1) -> +1
 *    rows[1]=1, cols[1]=1, diag=2, antiDiag=0
 * 4. P1(2, 2) -> +1
 *    rows[2]=1, cols[2]=1, diag=3!
 *    
 * Evaluation: Math.abs(diag) == 3. Player 1 wins!
 * 
 * Pitfalls:
 * - Mixing up `diag` and `antiDiag` condition checks.
 *   - Main Diagonal (top-left to bottom-right): `row == col`.
 *   - Anti-Diagonal (top-right to bottom-left): `row + col == n - 1`.
 * 
 * Pattern Recognition: "Grid-based win condition / Voting" -> "+1 / -1 Counters".
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if `n` is incredibly large (e.g., 10^9) but the game ends in relatively 
 *     few moves?
 * A1: Allocating `new int[10^9]` for rows and columns would cause a Memory Limit 
 *     Exceeded. We would switch from standard arrays to `HashMap<Integer, Integer>`. 
 *     This changes space complexity to O(M) where M is the number of moves made, 
 *     while keeping time complexity at O(1).
 * 
 * Q2: How would you add an `undo()` method?
 * A2: The `+1 / -1` trick makes undo operations strictly O(1) and effortless. We 
 *     would just pass the row, col, and player to the undo method, and apply the 
 *     INVERSE logic (if player == 1, subtract 1. If player == 2, add 1). No board 
 *     scanning is necessary to revert the state!
 */

public class TicTacToeStudy {

    static class TicTacToe {
        
        // Counter array tracking the net pieces for each row.
        private final int[] rows;
        
        // Counter array tracking the net pieces for each column.
        private final int[] cols;
        
        // Single counter tracking the net pieces on the top-left to bottom-right diagonal.
        private int diagonal;
        
        // Single counter tracking the net pieces on the top-right to bottom-left diagonal.
        private int antiDiagonal;
        
        // The size of the board (to determine the winning threshold).
        private final int n;

        /**
         * Initializes the object the size of the board n.
         * Time: O(n) to allocate arrays.
         */
        public TicTacToe(int n) {
            this.n = n;
            this.rows = new int[n];
            this.cols = new int[n];
            this.diagonal = 0;
            this.antiDiagonal = 0;
        }

        /**
         * Indicates that a player makes a move at the cell (row, col).
         * Time Complexity: O(1)
         * Space Complexity: O(1) auxiliary per call.
         */
        public int move(int row, int col, int player) {
            // Player 1 pushes the counter towards +n
            // Player 2 pushes the counter towards -n
            int point = (player == 1) ? 1 : -1;
            
            // 1. Update the row and column corresponding to the move
            rows[row] += point;
            cols[col] += point;
            
            // 2. Update the main diagonal if the move falls on it
            if (row == col) {
                diagonal += point;
            }
            
            // 3. Update the anti-diagonal if the move falls on it
            // The anti-diagonal exists where the coordinates sum to exactly n - 1
            if (row + col == n - 1) {
                antiDiagonal += point;
            }
            
            // 4. Check for a win condition instantly
            // If any of the counters reach +n or -n, someone just won.
            // Using Math.abs() allows us to check both players simultaneously.
            if (Math.abs(rows[row]) == n ||
                Math.abs(cols[col]) == n ||
                Math.abs(diagonal) == n ||
                Math.abs(antiDiagonal) == n) {
                
                return player;
            }
            
            // If no win condition is met, the game continues
            return 0;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        // Initialize a 3x3 board
        TicTacToe ticTacToe = new TicTacToe(3);

        System.out.println("P1 plays (0,0): " + ticTacToe.move(0, 0, 1)); // Expected: 0
        System.out.println("P2 plays (0,2): " + ticTacToe.move(0, 2, 2)); // Expected: 0
        System.out.println("P1 plays (2,2): " + ticTacToe.move(2, 2, 1)); // Expected: 0
        System.out.println("P2 plays (1,1): " + ticTacToe.move(1, 1, 2)); // Expected: 0
        System.out.println("P1 plays (2,0): " + ticTacToe.move(2, 0, 1)); // Expected: 0
        System.out.println("P2 plays (1,0): " + ticTacToe.move(1, 0, 2)); // Expected: 0
        
        // P1 plays (2,1) completing the bottom row!
        // Row 2 was: (2,0) by P1, (2,2) by P1. Placing (2,1) gives P1 exactly 3 points.
        System.out.println("P1 plays (2,1): " + ticTacToe.move(2, 1, 1)); // Expected: 1
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Mathematical State Compression (+1 / -1 Counters).
 * Key Observation: In games where lines are purely contested (Tic-Tac-Toe), tracking 
 * the exact layout of the board is unnecessary. If a line requires 'n' unbroken pieces 
 * to win, assigning +1 to Player A and -1 to Player B guarantees that a line can 
 * only reach an absolute sum of 'n' if a single player dominates it exclusively.
 * What to Memorize: The anti-diagonal condition: `row + col == n - 1`.
 * Most Common Trap: Storing the board in a 2D array and running a loop to check the 
 * rows and columns. While technically correct, it reduces an elegant O(1) math problem 
 * into a brute force O(N) simulation.
 * Mental Trigger: "Determine win condition instantly on a grid" -> "+1 / -1 Row & Col Arrays".
 * ============================================================================
 */











































class TicTacToe {

    private final int n;

    // rows[r] stores the signed sum of all marks in row r.
    // Player 1 contributes +1; player 2 contributes -1.
    private final int[] rows;

    // cols[c] stores the signed sum of all marks in column c.
    private final int[] cols;

    // Sum for the main diagonal, where row == col.
    private int diagonal;

    // Sum for the anti-diagonal, where row + col == n - 1.
    private int antiDiagonal;

    TicTacToe(int n) {
        this.n = n;

        // Zero-initialized counters represent an empty board.
        // We do not need a separate n x n board for winner detection.
        this.rows = new int[n];
        this.cols = new int[n];

        // Java initializes diagonal and antiDiagonal to zero automatically.
    }

    int move(int row, int col, int player) {

        // Convert the player ID into a signed contribution.
        // Player 1 adds +1, while player 2 adds -1.
        // This lets one counter represent both players without separate arrays.
        int contribution = (player == 1) ? 1 : -1;

        // Every move belongs to exactly one row and one column.
        rows[row] += contribution;
        cols[col] += contribution;

        // A move on the main diagonal contributes to that diagonal's sum.
        if (row == col) {
            diagonal += contribution;
        }

        // A move on the anti-diagonal contributes to its sum.
        // This is an independent condition: the center cell of an odd-sized
        // board belongs to both diagonals and must update both counters.
        if (row + col == n - 1) {
            antiDiagonal += contribution;
        }

        // A line can newly become a winning line only if it contains
        // the latest move. Therefore, checking these four counters is enough.
        //
        // A sum of +n means every cell belongs to player 1.
        // A sum of -n means every cell belongs to player 2.
        // Mixed lines cannot reach either value because +1 and -1 cancel.
        //
        // We check the current row, current column, and both diagonal sums.
        // Checking diagonal sums even for a non-diagonal move is harmless:
        // those sums have not changed and cannot newly become winning.
        if (Math.abs(rows[row]) == n
                || Math.abs(cols[col]) == n
                || Math.abs(diagonal) == n
                || Math.abs(antiDiagonal) == n) {

            // A valid winning line must belong to the player who just moved.
            // Return the original player ID, not the signed contribution.
            return player;
        }

        // No winning line was completed by this move.
        return 0;
    }

    /*
     * TIME COMPLEXITY:
     * - Constructor: O(n), because the two counter arrays contain n elements.
     * - Each move: O(1), because it performs a fixed number of array accesses,
     *   additions, comparisons, and at most two diagonal updates.
     *
     * SPACE COMPLEXITY:
     * - O(n) auxiliary space for the two arrays.
     * - O(1) additional scalar state.
     * - Total auxiliary space: O(n).
     *
     * CORRECTNESS:
     * Each counter is exactly the signed sum of the corresponding line's
     * marks. A line containing n marks from one player has absolute sum n.
     * A mixed line has a smaller absolute sum because opposing marks cancel.
     *
     * Every new winning line must contain the latest move because no other
     * cells changed. Thus, checking the affected counters detects every win.
     *
     * IMPORTANT ASSUMPTION:
     * This implementation relies on the problem guarantee that moves are
     * valid and that no moves are made after a winner is found. If those
     * guarantees are removed, extra state and validation are required.
     */
}











































class TicTacToeScanAffectedLines {

    private final int n;
    private final int[][] board;

    TicTacToeScanAffectedLines(int n) {
        this.n = n;
        this.board = new int[n][n];
    }

    int move(int row, int col, int player) {
        // Store the move so that we can inspect the affected lines.
        board[row][col] = player;

        // A new winning row must contain the cell just played.
        boolean rowComplete = true;

        for (int c = 0; c < n; c++) {
            if (board[row][c] != player) {
                rowComplete = false;
                break;
            }
        }

        if (rowComplete) {
            return player;
        }

        // A new winning column must also contain the cell just played.
        boolean colComplete = true;

        for (int r = 0; r < n; r++) {
            if (board[r][col] != player) {
                colComplete = false;
                break;
            }
        }

        if (colComplete) {
            return player;
        }

        // Only inspect the main diagonal if this move belongs to it.
        if (row == col) {
            boolean complete = true;

            for (int i = 0; i < n; i++) {
                if (board[i][i] != player) {
                    complete = false;
                    break;
                }
            }

            if (complete) {
                return player;
            }
        }

        // Only inspect the anti-diagonal if this move belongs to it.
        if (row + col == n - 1) {
            boolean complete = true;

            for (int i = 0; i < n; i++) {
                if (board[i][n - 1 - i] != player) {
                    complete = false;
                    break;
                }
            }

            if (complete) {
                return player;
            }
        }

        return 0;
    }

    /*
     * TIME COMPLEXITY:
     * - Constructor: O(n^2) to allocate the board.
     * - Each move: O(n) worst case, because we inspect at most four
     *   lines, and each line contains n cells.
     *
     * SPACE COMPLEXITY:
     * - O(n^2) for the board.
     *
     * WHY IT IS BETTER:
     * We avoid inspecting unrelated rows and columns.
     *
     * WHAT IS STILL WASTEFUL:
     * Suppose a row already contains two marks from player 1. We scan that
     * row again to rediscover those two marks on the next move.
     *
     * NEXT OPTIMIZATION:
     * Remember the counts instead of recounting the cells.
     */
}
