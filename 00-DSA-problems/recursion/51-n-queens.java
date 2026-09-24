import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * N-Queens Problem - All Solution Variants
 * 
 * Problem: Place n queens on an n×n chessboard so no two queens attack each
 * other.
 * Queens attack horizontally, vertically, and diagonally.
 */
class NQueensAllSolutions {

    // ==================== SOLUTION 1: HashSet Tracking (Most Efficient)
    // ====================
    /**
     * Approach: Use HashSets to track occupied columns and diagonals for O(1)
     * conflict checking
     * 
     * Time Complexity: O(n!) - We try placing queens with decreasing valid
     * positions per row
     * Space Complexity: O(n²) - Board storage + O(n) for recursion stack and sets
     * 
     * Pros: Fastest conflict checking with O(1) lookups
     * Cons: Extra space for three HashSets
     */
    public List<List<String>> solveNQueens1(int n) {
        List<List<String>> result = new ArrayList<>();
        char[][] board = new char[n][n];

        // Initialize board with empty spaces
        for (int i = 0; i < n; i++) {
            Arrays.fill(board[i], '.');
        }

        // Track occupied columns and diagonals
        Set<Integer> cols = new HashSet<>(); // Columns with queens
        Set<Integer> diag1 = new HashSet<>(); // Main diagonals (row - col is constant)
        Set<Integer> diag2 = new HashSet<>(); // Anti-diagonals (row + col is constant)

        backtrack1(0, n, board, result, cols, diag1, diag2);
        return result;
    }

    private void backtrack1(int row, int n, char[][] board, List<List<String>> result,
            Set<Integer> cols, Set<Integer> diag1, Set<Integer> diag2) {
        if (row == n) {
            result.add(constructBoard(board));
            return;
        }

        for (int col = 0; col < n; col++) {
            // O(1) conflict check using sets
            if (cols.contains(col) ||
                    diag1.contains(row - col) ||
                    diag2.contains(row + col)) {
                continue;
            }

            // Place queen and mark as occupied
            board[row][col] = 'Q';
            cols.add(col);
            diag1.add(row - col);
            diag2.add(row + col);

            backtrack1(row + 1, n, board, result, cols, diag1, diag2);

            // Backtrack: remove queen and unmark
            board[row][col] = '.';
            cols.remove(col);
            diag1.remove(row - col);
            diag2.remove(row + col);
        }
    }

    // ==================== SOLUTION 2: Boolean Arrays (Memory Efficient)
    // ====================
    /**
     * Approach: Use boolean arrays instead of HashSets for tracking
     * 
     * Time Complexity: O(n!) - Same backtracking approach
     * Space Complexity: O(n²) - Board + O(n) for boolean arrays and recursion
     * 
     * Pros: Slightly more memory efficient than HashSets, still O(1) lookups
     * Cons: Need to handle array indexing carefully for diagonals
     */
    public List<List<String>> solveNQueens2(int n) {
        List<List<String>> result = new ArrayList<>();
        char[][] board = new char[n][n];

        for (int i = 0; i < n; i++) {
            Arrays.fill(board[i], '.');
        }

        boolean[] cols = new boolean[n];
        boolean[] diag1 = new boolean[2 * n - 1]; // row - col + (n-1) to avoid negative index
        boolean[] diag2 = new boolean[2 * n - 1]; // row + col

        backtrack2(0, n, board, result, cols, diag1, diag2);
        return result;
    }

    private void backtrack2(int row, int n, char[][] board, List<List<String>> result,
            boolean[] cols, boolean[] diag1, boolean[] diag2) {
        if (row == n) {
            result.add(constructBoard(board));
            return;
        }

        for (int col = 0; col < n; col++) {
            int d1 = row - col + (n - 1); // Normalize to positive index
            int d2 = row + col;

            if (cols[col] || diag1[d1] || diag2[d2]) {
                continue;
            }

            board[row][col] = 'Q';
            cols[col] = diag1[d1] = diag2[d2] = true;

            backtrack2(row + 1, n, board, result, cols, diag1, diag2);

            board[row][col] = '.';
            cols[col] = diag1[d1] = diag2[d2] = false;
        }
    }

    // ==================== SOLUTION 3: Bitwise Operations (Most Compact)
    // ====================
    /**
     * Approach: Use bit manipulation for space-efficient tracking
     * 
     * Time Complexity: O(n!) - Same backtracking, but with bit operations
     * Space Complexity: O(n²) - Board + O(n) for recursion (minimal tracking
     * overhead)
     * 
     * Pros: Most space-efficient, elegant bit manipulation
     * Cons: Works only for n <= 31 (integer bit limit), harder to understand
     */
    public List<List<String>> solveNQueens3(int n) {
        List<List<String>> result = new ArrayList<>();
        char[][] board = new char[n][n];

        for (int i = 0; i < n; i++) {
            Arrays.fill(board[i], '.');
        }

        backtrack3(0, n, board, result, 0, 0, 0);
        return result;
    }

    private void backtrack3(int row, int n, char[][] board, List<List<String>> result,
            int cols, int diag1, int diag2) {
        if (row == n) {
            result.add(constructBoard(board));
            return;
        }

        // Available positions: positions not under attack
        int availablePositions = ((1 << n) - 1) & ~(cols | diag1 | diag2);

        while (availablePositions != 0) {
            // Get rightmost available position
            int position = availablePositions & -availablePositions;
            availablePositions -= position; // Remove this position

            int col = Integer.bitCount(position - 1); // Get column index

            board[row][col] = 'Q';
            backtrack3(row + 1, n, board, result,
                    cols | position, // Mark column
                    (diag1 | position) << 1, // Shift diagonal down-left
                    (diag2 | position) >> 1); // Shift diagonal down-right
            board[row][col] = '.';
        }
    }

    // ==================== SOLUTION 4: Simple Brute Force with isSafe Check
    // ====================
    /**
     * Approach: Check all previous rows for conflicts each time
     * 
     * Time Complexity: O(n! × n²) - For each placement, we check O(n) positions
     * Space Complexity: O(n²) - Only board and recursion stack
     * 
     * Pros: Simplest to understand, no extra data structures
     * Cons: Slowest due to O(n) conflict checks per placement
     */
    public List<List<String>> solveNQueens4(int n) {
        List<List<String>> result = new ArrayList<>();
        char[][] board = new char[n][n];

        for (int i = 0; i < n; i++) {
            Arrays.fill(board[i], '.');
        }

        backtrack4(0, n, board, result);
        return result;
    }

    private void backtrack4(int row, int n, char[][] board, List<List<String>> result) {
        if (row == n) {
            result.add(constructBoard(board));
            return;
        }

        for (int col = 0; col < n; col++) {
            if (isSafe(board, row, col, n)) {
                board[row][col] = 'Q';
                backtrack4(row + 1, n, board, result);
                board[row][col] = '.';
            }
        }
    }

    // Check if placing queen at (row, col) is safe
    private boolean isSafe(char[][] board, int row, int col, int n) {
        // Check column above
        for (int i = 0; i < row; i++) {
            if (board[i][col] == 'Q')
                return false;
        }

        // Check upper-left diagonal
        for (int i = row - 1, j = col - 1; i >= 0 && j >= 0; i--, j--) {
            if (board[i][j] == 'Q')
                return false;
        }

        // Check upper-right diagonal
        for (int i = row - 1, j = col + 1; i >= 0 && j < n; i--, j++) {
            if (board[i][j] == 'Q')
                return false;
        }

        return true;
    }

    // ==================== SOLUTION 5: Column Array Tracking (Space Optimized)
    // ====================
    /**
     * Approach: Store queen column positions in an array instead of 2D board
     * 
     * Time Complexity: O(n! × n) - O(n) for conflict checking per placement
     * Space Complexity: O(n) - Only 1D array + recursion stack
     * 
     * Pros: Minimal space usage, easy to understand
     * Cons: Need to convert to board format at the end
     */
    public List<List<String>> solveNQueens5(int n) {
        List<List<String>> result = new ArrayList<>();
        int[] queens = new int[n]; // queens[i] = column position of queen in row i
        Arrays.fill(queens, -1);

        backtrack5(0, n, queens, result);
        return result;
    }

    private void backtrack5(int row, int n, int[] queens, List<List<String>> result) {
        if (row == n) {
            result.add(constructBoardFromArray(queens, n));
            return;
        }

        for (int col = 0; col < n; col++) {
            if (isValidPlacement(queens, row, col)) {
                queens[row] = col;
                backtrack5(row + 1, n, queens, result);
                queens[row] = -1;
            }
        }
    }

    private boolean isValidPlacement(int[] queens, int row, int col) {
        for (int i = 0; i < row; i++) {
            int placedCol = queens[i];
            // Check same column or diagonal
            if (placedCol == col ||
                    Math.abs(row - i) == Math.abs(col - placedCol)) {
                return false;
            }
        }
        return true;
    }

    private List<String> constructBoardFromArray(int[] queens, int n) {
        List<String> board = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            char[] row = new char[n];
            Arrays.fill(row, '.');
            row[queens[i]] = 'Q';
            board.add(new String(row));
        }
        return board;
    }

    // ==================== Helper Method ====================
    private List<String> constructBoard(char[][] board) {
        List<String> solution = new ArrayList<>();
        for (char[] row : board) {
            solution.add(new String(row));
        }
        return solution;
    }

    // ==================== Test All Solutions ====================
    public static void main(String[] args) {
        NQueensAllSolutions solver = new NQueensAllSolutions();

        System.out.println("=".repeat(70));
        System.out.println("N-QUEENS PROBLEM - ALL SOLUTION VARIANTS");
        System.out.println("=".repeat(70));

        int[] testCases = { 1, 4, 8 };

        for (int n : testCases) {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("Testing with n = " + n);
            System.out.println("=".repeat(70));

            // Test Solution 1: HashSet
            long start = System.nanoTime();
            List<List<String>> result1 = solver.solveNQueens1(n);
            long end = System.nanoTime();
            System.out.println("\nSolution 1 (HashSet): " + result1.size() + " solutions");
            System.out.println("Time: " + (end - start) / 1_000_000.0 + " ms");

            // Test Solution 2: Boolean Arrays
            start = System.nanoTime();
            List<List<String>> result2 = solver.solveNQueens2(n);
            end = System.nanoTime();
            System.out.println("\nSolution 2 (Boolean Arrays): " + result2.size() + " solutions");
            System.out.println("Time: " + (end - start) / 1_000_000.0 + " ms");

            // Test Solution 3: Bitwise
            start = System.nanoTime();
            List<List<String>> result3 = solver.solveNQueens3(n);
            end = System.nanoTime();
            System.out.println("\nSolution 3 (Bitwise): " + result3.size() + " solutions");
            System.out.println("Time: " + (end - start) / 1_000_000.0 + " ms");

            // Test Solution 4: Brute Force
            start = System.nanoTime();
            List<List<String>> result4 = solver.solveNQueens4(n);
            end = System.nanoTime();
            System.out.println("\nSolution 4 (Brute Force): " + result4.size() + " solutions");
            System.out.println("Time: " + (end - start) / 1_000_000.0 + " ms");

            // Test Solution 5: Array Tracking
            start = System.nanoTime();
            List<List<String>> result5 = solver.solveNQueens5(n);
            end = System.nanoTime();
            System.out.println("\nSolution 5 (Array Tracking): " + result5.size() + " solutions");
            System.out.println("Time: " + (end - start) / 1_000_000.0 + " ms");

            // Display first solution for smaller cases
            if (n <= 4 && !result1.isEmpty()) {
                System.out.println("\nFirst solution:");
                for (String row : result1.get(0)) {
                    System.out.println(row);
                }
            }
        }

        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPARISON SUMMARY");
        System.out.println("=".repeat(70));
        System.out.println("Solution 1 (HashSet):        Fastest, O(1) lookups, uses extra space");
        System.out.println("Solution 2 (Boolean Arrays): Memory efficient, O(1) lookups");
        System.out.println("Solution 3 (Bitwise):        Most compact, works only for n <= 31");
        System.out.println("Solution 4 (Brute Force):    Simplest, slowest O(n²) per check");
        System.out.println("Solution 5 (Array Tracking): Minimal space O(n), moderate speed");
        System.out.println("=".repeat(70));
    }
}
