/**
 * ============================================================================
 * N-QUEENS II - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * This is the exact same scenario as the classic N-Queens puzzle: we need to 
 * place 'n' non-attacking queens on an 'n x n' board. 
 * HOWEVER, instead of returning the actual visual layouts of the boards, we 
 * ONLY need to return the total COUNT of valid arrangements. 
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Do we need to generate and store the board layouts to count them?
 * A: No! This is the most important realization. Since we only need a number, 
 *    maintaining a `char[][] board` is a complete waste of memory and time.
 * 
 * Q: What are the constraints on n?
 * A: 1 <= n <= 9. This means an exponential/factorial time complexity is 
 *    expected. But more importantly, because n <= 9, it easily fits inside 
 *    a standard 32-bit integer, opening the door for Bitmasking!
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - STRIP AWAY THE BOARD: In N-Queens I, we passed around a 2D array and cloned 
 *   it when a valid state was reached. Here, we just return `1` when we reach 
 *   the end, and sum up all the `1`s.
 * - CONSTANT TIME CHECKS: We still use boolean arrays (`cols`, `diag1`, `diag2`) 
 *   to check if a column or diagonal is under attack in O(1) time.
 * - BITMASKING (THE ULTIMATE OPTIMIZATION): Instead of using boolean arrays 
 *   which take O(N) space, we can use the bits of an integer. An integer has 
 *   32 bits, and we only need 9. We can use bitwise OR (`|`), AND (`&`), and 
 *   shifts (`<<`, `>>`) to track attacks in absolute fastest possible time 
 *   with O(1) auxiliary space!
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Explain that this is identical to N-Queens I, but we can drop the 
 *   2D board to save O(N^2) space per recursive call.
 * - Step 2: Write the standard Boolean Array Backtracking solution. It shows 
 *   solid fundamentals and is usually what the interviewer wants to see first.
 * - Step 3: Stop and say: "Since N is guaranteed to be <= 9, we don't actually 
 *   need arrays. I can optimize this to O(1) space using Bit Manipulation." 
 *   Writing the Bitmask solution will firmly establish you as a top-tier candidate.
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * n = 4
 * 
 * We don't build: 
 * . Q . .
 * . . . Q
 * Q . . .
 * . . Q .
 * 
 * We simply track:
 * row = 4 (Reached the end!) -> return 1
 * 
 * The recursive calls sum up these "1"s from all valid branches.
 * Result for n=4 is 2.
 */

import java.util.*;

public class NQueensII {

    /**
     * SOLUTION 1: Standard Backtracking with Boolean Arrays
     * ------------------------------------------------------------------------
     * Pros: Very readable, clear mapping to the physical chessboard geometry.
     * Cons: Uses O(N) space for the tracking arrays.
     * 
     * Time Complexity: O(N!). We place 1 queen per row, and available columns 
     * decrease as we go down.
     * Space Complexity: O(N) for the boolean arrays and recursion stack.
     */
    public int totalNQueens(int n) {
        
        /*
         * ============================================================
         * HOW TO THINK ABOUT THE STATE
         * ============================================================
         * 
         * We don't need a char[][] board. We just need to know:
         * "Is this column or diagonal currently under attack?"
         */
        boolean[] cols = new boolean[n];
        boolean[] diag1 = new boolean[2 * n - 1]; // Main diagonals (\)
        boolean[] diag2 = new boolean[2 * n - 1]; // Anti diagonals (/)
        
        return backtrack(0, n, cols, diag1, diag2);
    }

    private int backtrack(int row, int n, boolean[] cols, boolean[] diag1, boolean[] diag2) {
        /*
         * ============================================================
         * BASE CASE
         * ============================================================
         * 
         * If 'row' reaches 'n', it means we successfully placed a queen 
         * in rows 0 through (n-1) without any conflicts.
         * We found exactly ONE valid configuration. Return 1 to add to the sum.
         */
        if (row == n) {
            return 1;
        }

        int validConfigurationsCount = 0;

        /*
         * ============================================================
         * THE DECISION LOOP
         * ============================================================
         * 
         * For the current row, try placing a queen in every column 'col'.
         */
        for (int col = 0; col < n; col++) {
            
            // Calculate diagonal indices
            int d1 = row - col + n - 1; // Main diagonal
            int d2 = row + col;         // Anti diagonal
            
            /*
             * ------------------------------------------------------------
             * PRUNING
             * ------------------------------------------------------------
             * If the column or either diagonal is attacked, skip this square.
             */
            if (cols[col] || diag1[d1] || diag2[d2]) {
                continue;
            }

            /*
             * ============================================================
             * CHOOSE
             * ============================================================
             * Claim the column and both diagonals.
             */
            cols[col] = true;
            diag1[d1] = true;
            diag2[d2] = true;

            /*
             * ============================================================
             * EXPLORE
             * ============================================================
             * Move to the next row. Add the returned count of successful 
             * branches to our running total.
             */
            validConfigurationsCount += backtrack(row + 1, n, cols, diag1, diag2);

            /*
             * ============================================================
             * UNCHOOSE / BACKTRACK
             * ============================================================
             * Release the column and diagonals so the loop can try placing 
             * the queen in the NEXT column of this current row.
             */
            cols[col] = false;
            diag1[d1] = false;
            diag2[d2] = false;
        }

        return validConfigurationsCount;
    }


    /**
     * SOLUTION 2: Ultra-Optimized Bitmasking (Advanced)
     * ------------------------------------------------------------------------
     * Pros: Blazing fast. Replaces array lookups with bitwise operations.
     * Eliminates auxiliary array space entirely.
     * Cons: Bit manipulation syntax can be intimidating to read.
     * 
     * Time Complexity: O(N!). Still explores the same tree, but with smaller 
     * constant factors (faster execution time).
     * Space Complexity: O(N) purely for the recursion stack. Auxiliary space 
     * for state tracking is O(1).
     */
    public int totalNQueensBitmask(int n) {
        /*
         * We start at row 0.
         * Initially, no columns, main diagonals, or anti-diagonals are occupied,
         * so their bitmasks are all 0.
         */
        return backtrackBitmask(0, 0, 0, 0, n);
    }

    private int backtrackBitmask(int row, int cols, int diag1, int diag2, int n) {
        if (row == n) {
            return 1;
        }

        int count = 0;

        /*
         * ============================================================
         * THE BITMASK MAGIC
         * ============================================================
         * 
         * (cols | diag1 | diag2) 
         *      -> Gives a bitmask where '1' means the position is ATTACKED.
         * 
         * ~(cols | diag1 | diag2) 
         *      -> Inverts it. Now '1' means the position is SAFE.
         * 
         * ((1 << n) - 1) 
         *      -> Creates a mask of exactly 'n' ones (e.g., for n=4, it's 1111).
         *      -> We bitwise AND this to wipe out any 1s beyond the board size.
         * 
         * availablePositions -> A binary number where every '1' represents 
         * a perfectly safe column to place a queen.
         */
        int availablePositions = ((1 << n) - 1) & ~(cols | diag1 | diag2);

        /*
         * We iterate as long as there is at least one available position ('1' bit)
         */
        while (availablePositions != 0) {
            
            /*
             * ------------------------------------------------------------
             * CHOOSE
             * ------------------------------------------------------------
             * 
             * availablePositions & -availablePositions
             *      -> This is a famous bitwise trick that isolates the 
             *         RIGHTMOST '1' bit. 
             *      -> It essentially says: "Give me the first safe column."
             */
            int position = availablePositions & -availablePositions;

            /*
             * ------------------------------------------------------------
             * EXPLORE
             * ------------------------------------------------------------
             * 
             * Now we recursively call for the next row.
             * 
             * cols | position
             *      -> Mark this specific column as occupied.
             * 
             * (diag1 | position) << 1
             *      -> Mark this diagonal as occupied.
             *      -> WHY SHIFT LEFT? Because as we move down one row, a 
             *         Main Diagonal (\) shifts exactly one column to the left 
             *         relative to the current bitwise perspective!
             * 
             * (diag2 | position) >> 1
             *      -> Mark this anti-diagonal as occupied.
             *      -> WHY SHIFT RIGHT? Because an Anti Diagonal (/) shifts 
             *         exactly one column to the right as we move down a row!
             */
            count += backtrackBitmask(
                row + 1, 
                cols | position, 
                (diag1 | position) << 1, 
                (diag2 | position) >> 1, 
                n
            );

            /*
             * ------------------------------------------------------------
             * UNCHOOSE / BACKTRACK
             * ------------------------------------------------------------
             * 
             * We remove the '1' we just processed from availablePositions.
             * This allows the while loop to move to the NEXT available safe column.
             * 
             * Note: The recursive parameters (cols, diag1, etc.) were passed 
             * by value into the next frame, so they implicitly "backtrack" 
             * when the recursive call returns! We don't need manual un-choosing.
             */
            availablePositions &= (availablePositions - 1); // Clears the lowest set bit
        }

        return count;
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        NQueensII solver = new NQueensII();

        int[] testCases = {1, 4, 8, 9};

        for (int n : testCases) {
            System.out.println("--- Test Case: n = " + n + " ---");
            
            long start1 = System.nanoTime();
            int count1 = solver.totalNQueens(n);
            long end1 = System.nanoTime();
            
            long start2 = System.nanoTime();
            int count2 = solver.totalNQueensBitmask(n);
            long end2 = System.nanoTime();

            System.out.println("Boolean Array DFS Count: " + count1 + " (Took " + (end1 - start1)/1000 + " µs)");
            System.out.println("Bitmask DFS Count:       " + count2 + " (Took " + (end2 - start2)/1000 + " µs)");
            System.out.println();
        }
    }
}


/**
 * ================================================================
 * 🔥 N-Queens II — COUNT SOLUTIONS (INTERVIEW MASTER TEMPLATE)
 * ================================================================
 *
 * GOAL:
 * Count number of ways to place N queens safely.
 *
 * KEY IDEA:
 * - Place queens row by row (1 queen per row)
 * - At each row, try all columns
 * - Use O(1) checks using helper arrays
 *
 * WHY 3 ARRAYS?
 * - colUsed → ensures no two queens share column
 * - diagUsed → ensures no two queens share main diagonal (↘)
 * - antiDiagUsed → ensures no two queens share anti-diagonal (↙)
 *
 * DIAGONAL MAPPING:
 * - main diag index = row + col
 * - anti diag index = row - col + (n - 1)
 *
 * TIME COMPLEXITY:
 * ~ O(N!)  (worst case, heavily pruned)
 *
 * SPACE COMPLEXITY:
 * O(N) recursion stack + O(N) arrays
 *
 * CONSTRAINT: n ≤ 9 → works perfectly
 */
public class NQueensCount {

    public static void main(String[] args) {
        System.out.println(totalNQueens(4)); // Output: 2
    }

    public static int totalNQueens(int n) {

        // Track used columns
        boolean[] colUsed = new boolean[n];

        // Track diagonals (size = 2*n because indices go from 0 → 2n-2)
        boolean[] diagUsed = new boolean[2 * n];       // row + col
        boolean[] antiDiagUsed = new boolean[2 * n];   // row - col + (n-1)

        return backtrack(0, n, colUsed, diagUsed, antiDiagUsed);
    }

    /**
     * BACKTRACK FUNCTION
     *
     * @param row → current row where we place queen
     */
    private static int backtrack(int row, int n,
                                 boolean[] colUsed,
                                 boolean[] diagUsed,
                                 boolean[] antiDiagUsed) {

        // BASE CASE → all queens placed
        if (row == n) {
            return 1; // found valid configuration
        }

        int count = 0;

        // Try placing queen in each column
        for (int col = 0; col < n; col++) {

            int diagIndex = row + col;
            int antiDiagIndex = row - col + (n - 1);

            // Check if position is safe
            if (colUsed[col] || diagUsed[diagIndex] || antiDiagUsed[antiDiagIndex]) {
                continue; // conflict → skip
            }

            // PLACE QUEEN (mark used)
            colUsed[col] = true;
            diagUsed[diagIndex] = true;
            antiDiagUsed[antiDiagIndex] = true;

            // Recurse to next row
            count += backtrack(row + 1, n, colUsed, diagUsed, antiDiagUsed);

            // BACKTRACK (remove queen)
            colUsed[col] = false;
            diagUsed[diagIndex] = false;
            antiDiagUsed[antiDiagIndex] = false;
        }

        return count;
    }
}
