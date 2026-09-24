import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Unique Paths
 * Robi, a robot, is located at the top-left corner of an m x n grid.
 * Robi can only move DOWN or RIGHT at any point in time.
 * The goal is to reach the bottom-right corner.
 * How many possible unique paths are there?
 * 
 * Constraints:
 * 1 <= m, n <= 100
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, catching mathematical constraints is a massive signal:
 * 
 * Q: "For m=100 and n=100, the mathematical answer is (198 choose 99). 
 *     This evaluates to a massive 59-digit number that will overflow a standard 
 *     32-bit integer, and even a 64-bit long. Are the test cases guaranteed to 
 *     fit within a 32-bit integer, or do I need to use BigInteger/return modulo?"
 * A: Standard coding platforms guarantee the generated test cases will yield an 
 *    answer less than or equal to 2 * 10^9 (fitting in a signed 32-bit int). 
 *    Calling this out verbally proves you understand system limitations.
 * 
 * Q: "What if m=1 and n=1?"
 * A: The robot is already on the treasure. There is exactly 1 path (doing nothing).
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "To physically step onto a cell located at (row, col), Robi's very last move 
 * could have only come from two possible adjacent cells:
 *  1. From the cell directly ABOVE it (row - 1, col), moving DOWN.
 *  2. From the cell directly LEFT of it (row, col - 1), moving RIGHT.
 * 
 * Therefore, the total number of unique paths to reach (row, col) is simply 
 * the sum of the unique paths to reach the cell above, plus the unique paths 
 * to reach the cell to the left.
 * 
 * Since multiple paths will intersect at the same grid cells (overlapping 
 * subproblems), Dynamic Programming is the optimal approach."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: m = 3 (rows), n = 3 (cols)
 * 
 * Grid of unique paths (Tabulation):
 * 
 *       Col 0   Col 1   Col 2
 * Row 0:  1       1       1    <- Top edge (Can only reach by moving strictly right)
 * Row 1:  1       2       3    <- Left edge (Can only reach by moving strictly down)
 * Row 2:  1       3       6    <- Bottom-right is 6.
 * 
 * Look at cell (1, 1). Paths to reach it = (above: 1) + (left: 1) = 2.
 * Look at cell (2, 2). Paths to reach it = (above: 3) + (left: 3) = 6.
 */
public class UniquePaths {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Start at the top-left (0, 0) and branch recursively DOWN and RIGHT.
     * 
     * Time Complexity: O(2^(m+n)) - Exponential branching at every cell.
     * Space Complexity: O(m+n) - Maximum depth of the recursion tree.
     */
    public int uniquePathsRecursive(int m, int n) {
        return solveRecursive(0, 0, m, n);
    }

    private int solveRecursive(int row, int col, int m, int n) {
        // BASE CASE REASONING:
        // If Robi steps out of the grid bounds, he crashed. 
        // This path is invalid. Return 0.
        if (row >= m || col >= n) {
            return 0;
        }

        // BASE CASE REASONING:
        // If Robi physically lands on the bottom-right cell (m-1, n-1), 
        // the mission is successful! This specific sequence of moves forms 
        // exactly 1 valid unique path. Return 1 to count it.
        if (row == m - 1 && col == n - 1) {
            return 1;
        }

        // Choice 1: Move DOWN (row + 1, col stays the same)
        int downPaths = solveRecursive(row + 1, col, m, n);
        
        // Choice 2: Move RIGHT (col + 1, row stays the same)
        int rightPaths = solveRecursive(row, col + 1, m, n);

        // Total paths from this current cell is the sum of both universes.
        return downPaths + rightPaths;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: We arrive at the same grid cells repeatedly via different routes.
     * Cache the results for [row][col] to evaluate each cell exactly once.
     * 
     * Time Complexity: O(m * n) - We visit each cell exactly once.
     * Space Complexity: O(m * n) - For the 2D memo array + recursion stack.
     */
    public int uniquePathsMemo(int m, int n) {
        int[][] memo = new int[m][n];
        for (int[] r : memo) Arrays.fill(r, -1);
        
        return solveMemo(0, 0, m, n, memo);
    }

    private int solveMemo(int row, int col, int m, int n, int[][] memo) {
        // BASE CASES (Same physical logic as brute force)
        if (row >= m || col >= n) return 0;
        if (row == m - 1 && col == n - 1) return 1;

        if (memo[row][col] != -1) {
            return memo[row][col];
        }

        int down = solveMemo(row + 1, col, m, n, memo);
        int right = solveMemo(row, col + 1, m, n, memo);

        memo[row][col] = down + right;
        return memo[row][col];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build an m x n spreadsheet. dp[i][j] signifies the total number 
     * of unique paths from the start (0, 0) to the specific cell (i, j).
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n) for the 2D array.
     */
    public int uniquePathsTabulation(int m, int n) {
        int[][] dp = new int[m][n];

        // BASE CASE REASONING (The Top Edge):
        // Consider the very first row (row 0). Since Robi cannot move UP, 
        // the ONLY physical way to reach any cell on the top edge is by 
        // moving strictly RIGHT from the start. 
        // Therefore, every cell on the top row has exactly 1 unique path to it.
        for (int j = 0; j < n; j++) {
            dp[0][j] = 1;
        }

        // BASE CASE REASONING (The Left Edge):
        // Consider the very first column (col 0). Since Robi cannot move LEFT, 
        // the ONLY physical way to reach any cell on the left edge is by 
        // moving strictly DOWN from the start.
        // Therefore, every cell on the left column has exactly 1 unique path to it.
        for (int i = 0; i < m; i++) {
            dp[i][0] = 1;
        }

        // We skip the first row (i=0) and first column (j=0) because we already 
        // seeded their base values. We start evaluating the internal grid at (1, 1).
        for (int i = 1; i < m; i++) {
            
            for (int j = 1; j < n; j++) {
                
                // PHYSICAL REALITY OF THE GRID:
                // To land exactly on cell dp[i][j], Robi's very last step MUST 
                // have been taken from either:
                
                // 1. The cell directly ABOVE (dp[i-1][j]), moving one step DOWN.
                int pathsFromAbove = dp[i - 1][j];
                
                // 2. The cell directly LEFT (dp[i][j-1]), moving one step RIGHT.
                int pathsFromLeft = dp[i][j - 1];

                // The total unique routes to reach our current cell is simply 
                // the sum of the routes from our two valid entry points.
                dp[i][j] = pathsFromAbove + pathsFromLeft;
            }
        }

        // The answer to the problem lies in the very last cell of the grid.
        return dp[m - 1][n - 1];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, look closely at how we calculate the current row `i`.
     * To calculate `dp[i][j]`, we ONLY look at `dp[i-1][j]` (the value directly 
     * above it in the previous row) and `dp[i][j-1]` (the value directly to its 
     * left in the current row).
     * 
     * We don't need to keep an entire m x n matrix in memory! We only need ONE 
     * single row array of size 'n' that we continuously overwrite.
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(n) - Massively reduced memory footprint.
     */
    public int uniquePathsSpaceOptimized(int m, int n) {
        // We only maintain a single 1D array representing the columns of a row.
        int[] dp = new int[n];
        
        // BASE CASE REASONING:
        // Initialize our array to represent the very first row (row 0).
        // As established, the top edge has exactly 1 path to every cell.
        Arrays.fill(dp, 1);

        // We iterate through every remaining row (from 1 to m-1).
        for (int i = 1; i < m; i++) {
            
            // We iterate left-to-right across the columns.
            // We start at j = 1 because the first column (j = 0) is the left edge,
            // which always strictly has 1 path. We don't need to overwrite it.
            for (int j = 1; j < n; j++) {
                
                // MAGIC OF THE 1D ARRAY:
                // We overwrite our single array in-place.
                // Before we update it, `dp[j]` holds the value from the PREVIOUS row (pathsFromAbove).
                // `dp[j-1]` holds the value we literally just updated a microsecond ago 
                // for the CURRENT row (pathsFromLeft).
                // We add them together and overwrite `dp[j]`.
                dp[j] = dp[j] + dp[j - 1];
                
            }
        }

        // After iterating through all 'm' rows, the last element in our 1D array 
        // represents the bottom-right corner.
        return dp[n - 1];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new UniquePaths();
        
        record TestCase(int m, int n, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(3, 7, 28),
            new TestCase(3, 2, 3),
            new TestCase(1, 1, 1),      // Start is the end
            new TestCase(10, 1, 1),     // Straight line down
            new TestCase(1, 10, 1)      // Straight line right
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Grid Size: " + tc.m + "x" + tc.n);
            System.out.println("Expected : " + tc.expected);
            
            // Limit recursive call printing to small grids to avoid TLE
            if (tc.m + tc.n <= 15) {
                System.out.println("Recursive (Brute) : " + solver.uniquePathsRecursive(tc.m, tc.n));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Grid too large for O(2^(m+n)))");
            }
            
            System.out.println("Memoization       : " + solver.uniquePathsMemo(tc.m, tc.n));
            System.out.println("Tabulation 2D     : " + solver.uniquePathsTabulation(tc.m, tc.n));
            System.out.println("Space Optimized   : " + solver.uniquePathsSpaceOptimized(tc.m, tc.n));
            System.out.println();
        }
    }
}
