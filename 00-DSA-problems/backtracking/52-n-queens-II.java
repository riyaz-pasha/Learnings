import java.util.*;
/*
 * The n-queens puzzle is the problem of placing n queens on an n x n chessboard
 * such that no two queens attack each other.
 * 
 * Given an integer n, return the number of distinct solutions to the n-queens
 * puzzle.
 * 
 * Example 1:
 * Input: n = 4
 * Output: 2
 * Explanation: There are two distinct solutions to the 4-queens puzzle as
 * shown.
 * 
 * Example 2:
 * Input: n = 1
 * Output: 1
 */

// Solution 1: Basic Backtracking
// Time: O(N!) Space: O(N)
class Solution1 {
    public int solveNQueens(int n) {
        int[] queens = new int[n]; // queens[i] = column position of queen in row i
        return backtrack(queens, 0, n);
    }

    private int backtrack(int[] queens, int row, int n) {
        if (row == n) {
            return 1; // Found a valid solution
        }

        int count = 0;
        for (int col = 0; col < n; col++) {
            if (isValid(queens, row, col)) {
                queens[row] = col;
                count += backtrack(queens, row + 1, n);
            }
        }
        return count;
    }

    private boolean isValid(int[] queens, int row, int col) {
        for (int i = 0; i < row; i++) {
            // Check column conflict
            if (queens[i] == col) {
                return false;
            }
            // Check diagonal conflicts
            if (Math.abs(queens[i] - col) == Math.abs(i - row)) {
                return false;
            }
        }
        return true;
    }
}

// Solution 2: Optimized with Sets for O(1) conflict checking
class Solution2 {
    public int solveNQueens(int n) {
        return backtrack(0, new HashSet<>(), new HashSet<>(), new HashSet<>(), n);
    }

    private int backtrack(int row, Set<Integer> cols, Set<Integer> diag1, Set<Integer> diag2, int n) {
        if (row == n) {
            return 1;
        }

        int count = 0;
        for (int col = 0; col < n; col++) {
            int d1 = row - col; // Main diagonal
            int d2 = row + col; // Anti-diagonal

            if (cols.contains(col) || diag1.contains(d1) || diag2.contains(d2)) {
                continue;
            }

            // Place queen
            cols.add(col);
            diag1.add(d1);
            diag2.add(d2);

            count += backtrack(row + 1, cols, diag1, diag2, n);

            // Remove queen (backtrack)
            cols.remove(col);
            diag1.remove(d1);
            diag2.remove(d2);
        }
        return count;
    }
}

// Solution 3: Most Optimized with boolean arrays
class Solution3 {
    public int solveNQueens(int n) {
        boolean[] cols = new boolean[n];
        boolean[] diag1 = new boolean[2 * n - 1]; // Main diagonals
        boolean[] diag2 = new boolean[2 * n - 1]; // Anti-diagonals
        return backtrack(0, cols, diag1, diag2, n);
    }

    private int backtrack(int row, boolean[] cols, boolean[] diag1, boolean[] diag2, int n) {
        if (row == n) {
            return 1;
        }

        int count = 0;
        for (int col = 0; col < n; col++) {
            int d1 = row - col + n - 1; // Offset to make index positive
            int d2 = row + col;

            if (cols[col] || diag1[d1] || diag2[d2]) {
                continue;
            }

            // Place queen
            cols[col] = diag1[d1] = diag2[d2] = true;

            count += backtrack(row + 1, cols, diag1, diag2, n);

            // Remove queen (backtrack)
            cols[col] = diag1[d1] = diag2[d2] = false;
        }
        return count;
    }
}

// Solution 4: Bit Manipulation (Most Space Efficient)
class Solution4 {
    public int solveNQueens(int n) {
        return backtrack(0, 0, 0, 0, n);
    }

    private int backtrack(int row, int cols, int diag1, int diag2, int n) {
        if (row == n) {
            return 1;
        }

        int count = 0;
        int available = ((1 << n) - 1) & ~(cols | diag1 | diag2);

        while (available != 0) {
            int pos = available & -available; // Get rightmost set bit
            available -= pos; // Remove this bit

            count += backtrack(row + 1,
                    cols | pos,
                    (diag1 | pos) << 1,
                    (diag2 | pos) >> 1,
                    n);
        }
        return count;
    }
}

// Test class to demonstrate all solutions
class NQueensTest {

    public static void main(String[] args) {
        Solution1 sol1 = new Solution1();
        Solution2 sol2 = new Solution2();
        Solution3 sol3 = new Solution3();
        Solution4 sol4 = new Solution4();

        int[] testCases = { 1, 4, 8 };

        for (int n : testCases) {
            System.out.println("n = " + n + ":");
            System.out.println("Solution 1: " + sol1.solveNQueens(n));
            System.out.println("Solution 2: " + sol2.solveNQueens(n));
            System.out.println("Solution 3: " + sol3.solveNQueens(n));
            System.out.println("Solution 4: " + sol4.solveNQueens(n));
            System.out.println();
        }
    }

}
