/**
 * STANDALONE STUDY NOTE: DESIGN TIC-TAC-TOE
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can we assume that the `player` parameter is always either 1 or 2?" (Yes, per constraints).
 * - "Are moves guaranteed to be valid and placed on empty blocks?" (Yes, input guarantees no overlapping moves).
 * - "Is the board size `n` fixed upon initialization?" (Yes, ranges from 2 to 9).
 * - "Can we achieve better than O(N) or O(N^2) time complexity per move?" (Yes, we can design an O(1) time check per move using running score tracking).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to implement a Tic-Tac-Toe move checker that determines if a player wins instantly after placing a mark. 
 * The binding constraint is that scanning the entire n x n board or traversing rows/columns naively after every move 
 * takes O(N) or O(N^2) time, which can be optimized down to absolute O(1) constant time.
 * 
 * --- APPROACH 1: BRUTE FORCE (Simulating the 2D Board) ---
 * 1. What I'd naturally try: Maintain an `int[n][n]` grid. On every move, place the player's ID, and then scan 
 *    the entire row, column, and both diagonals to check if any line has `n` matching marks.
 * 2. Why it works: It perfectly mirrors the physical board layout and directly tests the winning rules.
 * 3. Why it's too slow/costly: Scanning lines takes O(N) time per move. While fast for small boards, it scales 
 *    poorly if `n` is large.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) per move — because we scan rows, columns, and diagonals of length N.
 *    - Space Complexity: O(N^2) — to store the physical 2D board configuration.
 * 
 * --- APPROACH 2: OPTIMAL (Running Sum / Count Tracking Arrays) ---
 * 1. The Observation: A player wins if and only if they accumulate `n` marks in any single row, any single column, 
 *    the main diagonal, or the anti-diagonal. Instead of storing a full 2D grid, we can track the *net score* of marks 
 *    for each line.
 * 2. How it leads to the next step: 
 *    - Assign Player 1 a value of +1 and Player 2 a value of -1 (or track separate arrays for each player). 
 *      Using a single signed array approach: for Player 1 add +1, for Player 2 subtract -1. 
 *      If any row, column, or diagonal absolute sum reaches `n`, that player wins!
 *    - Alternatively, maintain separate tracking arrays for each player: `rows[player][row]`, `cols[player][col]`, 
 *      `diagonal`, and `antiDiagonal`.
 * 3. Which one I'd write in an interview: The dual tracking array approach or signed-count approach. Let's use 
 *    separate tracking arrays for each player (`rows`, `cols`, `diagonal`, `antiDiagonal`), as it is extremely intuitive, 
 *    clean, and completely avoids sign-confusion bugs under pressure.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(1) per move — because updating row, column, and diagonal counters and checking if they 
 *      equal `n` takes a fixed number of primitive operations regardless of board size `n`.
 *    - Space Complexity: O(N) — because we store counters for rows, columns, and diagonals, which scale linearly with `n`.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Minimum board size ($n = 2$): Works seamlessly; checking diagonal constraints correctly.
 * - Anti-diagonal placement: Moves where `row + col == n - 1` correctly trigger anti-diagonal tracking.
 * - Center cell on odd boards ($n = 3$): Automatically updates both main and anti-diagonals if `row == col` 
 *   and `row + col == n - 1`.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * O(1) State Tracking:
 * 
 * Board size n = 3
 * 
 * Trackers for Player 1:
 * - Rows: `rowCounts[3]`
 * - Cols: `colCounts[3]`
 * - Main Diagonal (`row == col`): `diagCount`
 * - Anti-Diagonal (`row + col == n - 1`): `antiDiagCount`
 * 
 * When Player 1 places at (0, 0):
 * - `rowCounts[0]++` (Becomes 1)
 * - `colCounts[0]++` (Becomes 1)
 * - `diagCount++`    (Becomes 1, since 0 == 0)
 * - Check if any reaches `n` (3). If so, Player 1 wins!
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: n = 3
 * 
 * - Move 1: move(0, 0, 1) [Player 1 at row 0, col 0]
 *   - rowCounts1[0] = 1, colCounts1[0] = 1, diagCount1 = 1. Max is 1 != 3 -> returns 0.
 * - Move 2: move(0, 1, 2) [Player 2 at row 0, col 1]
 *   - rowCounts2[0] = 1, colCounts2[1] = 1. Max is 1 != 3 -> returns 0.
 * - Move 3: move(1, 1, 1) [Player 1 at row 1, col 1]
 *   - rowCounts1[1] = 1, colCounts1[1] = 1, diagCount1 = 2. Max is 2 != 3 -> returns 0.
 * - Move 4: move(2, 1, 2) [Player 2 at row 2, col 1]
 *   - rowCounts2[2] = 1, colCounts2[1] = 2. Max is 2 != 3 -> returns 0.
 * - Move 5: move(2, 2, 1) [Player 1 at row 2, col 2]
 *   - rowCounts1[2] = 1, colCounts1[2] = 1, diagCount1 = 3. 
 *   - diagCount1 == 3 -> Player 1 wins! Returns 1.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To achieve O(1) time complexity per move instead of scanning the board, we can maintain running count arrays 
 * for each player: an array for rows, an array for columns, and two variables for the main and anti-diagonals. 
 * When a player makes a move at `(row, col)`, we increment their count for that specific row, column, and 
 * diagonals if applicable. Then, we simply check if any of those counts equals `n`. If so, that player wins instantly. 
 * This reduces our time complexity per move to O(1) and auxiliary space complexity to O(N)."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: How would you modify this class if the board size could dynamically expand during the game?
 * A: We would replace fixed-size arrays with dynamic data structures like HashMaps or resizable lists, 
 *    though standard Tic-Tac-Toe boards remain statically sized once initialized.
 * 
 * Q: Can this be implemented using a single shared array with positive and negative values for Player 1 and Player 2?
 * A: Yes! We can use `rows[row] += (player == 1 ? 1 : -1)`. If `Math.abs(rows[row]) == n`, Player 1 or Player 2 wins. 
 *    This reduces memory usage further, though separate tracking arrays are equally clear and less prone to edge-case bugs.
 */

public class TicTacToe {
    private final int n;
    
    // Tracking arrays for Player 1 and Player 2
    private final int[] rows1, cols1, rows2, cols2;
    private int diag1, antiDiag1;
    private int diag2, antiDiag2;

    /**
     * CONSTRUCTOR
     * Initializes the TicTacToe game board of size n x n.
     * Time: O(N), Space: O(N)
     */
    public TicTacToe(int n) {
        this.n = n;
        this.rows1 = new int[n];
        this.cols1 = new int[n];
        this.rows2 = new int[n];
        this.cols2 = new int[n];
    }

    /**
     * MOVE METHOD
     * Indicates that the player places their mark on the cell (row, col).
     * Time: O(1), Space: O(1)
     */
    public int move(int row, int col, int player) {
        if (player == 1) {
            // [What it does]: Increment Player 1's tally for this row and column
            rows1[row]++;
            cols1[col]++;

            // Check if move is on the main diagonal (top-left to bottom-right)
            if (row == col) {
                diag1++;
            }

            // Check if move is on the anti-diagonal (top-right to bottom-left)
            if (row + col == n - 1) {
                antiDiag1++;
            }

            // [Why needed]: Check if any line has reached 'n' marks, indicating a win for Player 1
            if (rows1[row] == n || cols1[col] == n || diag1 == n || antiDiag1 == n) {
                return 1;
            }

        } else {
            // [What it does]: Increment Player 2's tally for this row and column
            rows2[row]++;
            cols2[col]++;

            if (row == col) {
                diag2++;
            }

            if (row + col == n - 1) {
                antiDiag2++;
            }

            // [Why needed]: Check if any line has reached 'n' marks, indicating a win for Player 2
            if (rows2[row] == n || cols2[col] == n || diag2 == n || antiDiag2 == n) {
                return 2;
            }
        }

        // No winner yet
        return 0;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Running state counters for rows, columns, and diagonals (O(1) state management).
     * 2. Key observation: You don't need to store a 2D board matrix; tracking line counts instantly reveals wins.
     * 3. Memorize vs Understand: Understand the diagonal conditions: main diagonal is `row == col`, anti-diagonal is `row + col == n - 1`.
     * 4. Most common trap: Forgetting to check both diagonals or mishandling anti-diagonal index math.
     * 5. Mental trigger: "O(1) Tic-Tac-Toe move validation" -> Separate score counters for rows, cols, and diagonals.
     */
    public static void main(String[] args) {
        // Test Case: Simulating a 3x3 Tic-Tac-Toe game
        TicTacToe game = new TicTacToe(3);

        System.out.println("Move 1 (Player 1 at 0,0): " + game.move(0, 0, 1)); // Expected: 0
        System.out.println("Move 2 (Player 2 at 0,2): " + game.move(0, 2, 2)); // Expected: 0
        System.out.println("Move 3 (Player 1 at 1,1): " + game.move(1, 1, 1)); // Expected: 0
        System.out.println("Move 4 (Player 2 at 1,0): " + game.move(1, 0, 2)); // Expected: 0
        System.out.println("Move 5 (Player 1 at 2,2): " + game.move(2, 2, 1)); // Expected: 1 (Player 1 wins on main diagonal!)
    }
}



class TicTacToe {

    private final int n;

    // rows[i] tracks the progress of row i.
    private final int[] rows;

    // cols[j] tracks the progress of column j.
    private final int[] cols;

    // Main diagonal: (0,0), (1,1), ..., (n-1,n-1).
    private int diagonal;

    // Anti-diagonal: (0,n-1), (1,n-2), ..., (n-1,0).
    private int antiDiagonal;

    public TicTacToe(int n) {
        this.n = n;
        this.rows = new int[n];
        this.cols = new int[n];
    }

    public int move(int row, int col, int player) {

        // Encode player 1 as +1 and player 2 as -1.
        int value = (player == 1) ? 1 : -1;

        // Update the row and column containing this cell.
        rows[row] += value;
        cols[col] += value;

        // Update the main diagonal only if this cell belongs to it.
        if (row == col) {
            diagonal += value;
        }

        // Update the anti-diagonal only if this cell belongs to it.
        if (row + col == n - 1) {
            antiDiagonal += value;
        }

        // If any relevant line reaches +n or -n,
        // the current player has filled that line.
        if (Math.abs(rows[row]) == n
                || Math.abs(cols[col]) == n
                || Math.abs(diagonal) == n
                || Math.abs(antiDiagonal) == n) {
            return player;
        }

        // No winning line was completed by this move.
        return 0;
    }
}


