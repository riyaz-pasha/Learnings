import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Minimum Path Sum
 * You are given an m x n grid containing non-negative integers. 
 * Find a path from the top-left cell (0, 0) to the bottom-right cell (m-1, n-1)
 * that MINIMIZES the sum of the values along the path. 
 * You can only move DOWN or RIGHT at any step.
 * 
 * Constraints:
 * 1 <= m, n <= 200
 * 0 <= grid[i][j] <= 200
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, demonstrate system awareness by validating inputs:
 * 
 * Q: "Can the grid have negative numbers?"
 * A: The constraints state 0 <= grid[i][j]. This is important because negative 
 *    costs would mean we might want to take longer, winding paths to accumulate 
 *    negative values, completely breaking standard DP/Dijkstra logic. 
 *    Since they are non-negative, the shortest path physically aligns with the 
 *    lowest cost accumulation.
 * 
 * Q: "Will the maximum path sum fit within a standard 32-bit integer?"
 * A: The maximum path length is (200 + 200 - 1) = 399 cells. 
 *    The maximum value of a cell is 200. 
 *    Maximum possible sum = 399 * 200 = 79,800. 
 *    This easily fits inside a standard integer (up to ~2.1 billion).
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "To physically land on a cell located at (row, col), my very last move 
 * must have been from one of two adjacent cells:
 *  1. From the cell directly ABOVE it (row - 1, col), moving DOWN.
 *  2. From the cell directly LEFT of it (row, col - 1), moving RIGHT.
 * 
 * If I want to minimize my total cost to reach (row, col), I should look at 
 * the cheapest way I got to the cell ABOVE, and the cheapest way I got to the 
 * cell to the LEFT. I will greedily pick the cheaper of those two entry points, 
 * and then simply add the cost of my current cell (row, col).
 * 
 * Because multiple paths converge on the same cells (e.g., reaching (1, 1) by 
 * going right-down vs down-right), we have overlapping subproblems -> DP."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: 
 * Grid = 
 * [1, 3, 1]
 * [1, 5, 1]
 * [4, 2, 1]
 * 
 * Let's trace the Cost Grid (Tabulation):
 * 
 * Row 0: 
 * - (0,0) is just 1.
 * - (0,1) must come from left: 1 + 3 = 4.
 * - (0,2) must come from left: 4 + 1 = 5.
 * Row 0 Cost = [1, 4, 5]
 * 
 * Row 1:
 * - (1,0) must come from above: 1 + 1 = 2.
 * - (1,1) can come from above (cost 4) or left (cost 2). Min is 2. Add cell cost 5 -> 7.
 * - (1,2) can come from above (cost 5) or left (cost 7). Min is 5. Add cell cost 1 -> 6.
 * Row 1 Cost = [2, 7, 6]
 * 
 * Row 2:
 * - (2,0) must come from above: 2 + 4 = 6.
 * - (2,1) min(above 7, left 6) = 6. Add cell cost 2 -> 8.
 * - (2,2) min(above 6, left 8) = 6. Add cell cost 1 -> 7.
 * Row 2 Cost = [6, 8, 7]
 * 
 * Final minimum cost is 7. Path: 1 -> 3 -> 1 -> 1 -> 1.
 */
public class MinimumPathSum {

    // A safe infinity value. If we use Integer.MAX_VALUE and add a cell cost to it, 
    // it will overflow to a massive negative number, ruining Math.min().
    private static final int INF = Integer.MAX_VALUE / 2;

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: We start at the bottom-right destination and recursively ask for 
     * the cheapest path from the cell above and the cell to the left, all the 
     * way back to the start.
     * 
     * Time Complexity: O(2^(m+n)) - Exponential branching.
     * Space Complexity: O(m+n) - Maximum depth of the recursion tree.
     */
    public int minPathSumRecursive(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        return solveRecursive(grid, grid.length - 1, grid[0].length - 1);
    }

    private int solveRecursive(int[][] grid, int row, int col) {
        // BASE CASE REASONING (Start Point):
        // If we have traced our path all the way back to the very first cell (0, 0),
        // the "cost" to reach the starting line from the starting line is simply 
        // the toll we pay to stand on that cell: grid[0][0].
        if (row == 0 && col == 0) {
            return grid[0][0];
        }

        // BASE CASE REASONING (Out of Bounds):
        // If our trace takes us outside the grid (e.g., trying to come from ABOVE 
        // the top row), this physical path is impossible. 
        // We return our "infinity" value to ensure this dead-end is never chosen 
        // as the "minimum" path by the caller.
        if (row < 0 || col < 0) {
            return INF;
        }

        // Parallel Universe 1: What if our last step was moving DOWN from the cell above?
        int costFromAbove = solveRecursive(grid, row - 1, col);

        // Parallel Universe 2: What if our last step was moving RIGHT from the cell to our left?
        int costFromLeft = solveRecursive(grid, row, col - 1);

        // We greedily choose the cheaper historical path, and pay the toll for our current cell.
        return grid[row][col] + Math.min(costFromAbove, costFromLeft);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: We calculate the cost to reach specific grid cells multiple times 
     * from different overlapping paths. We can cache these minimum costs.
     * 
     * Time Complexity: O(m * n) - We evaluate each cell exactly once.
     * Space Complexity: O(m * n) - For the 2D memo array + call stack.
     */
    public int minPathSumMemo(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        
        int m = grid.length;
        int n = grid[0].length;
        
        int[][] memo = new int[m][n];
        for (int[] r : memo) Arrays.fill(r, -1);
        
        return solveMemo(grid, m - 1, n - 1, memo);
    }

    private int solveMemo(int[][] grid, int row, int col, int[][] memo) {
        // BASE CASES (Same physical logic as brute force)
        if (row == 0 && col == 0) return grid[0][0];
        if (row < 0 || col < 0) return INF;

        // Return cached cost if already calculated
        if (memo[row][col] != -1) {
            return memo[row][col];
        }

        int costFromAbove = solveMemo(grid, row - 1, col, memo);
        int costFromLeft = solveMemo(grid, row, col - 1, memo);

        memo[row][col] = grid[row][col] + Math.min(costFromAbove, costFromLeft);
        return memo[row][col];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build an m x n spreadsheet. dp[i][j] signifies the absolute minimum 
     * total cost accumulated from the start (0, 0) up to cell (i, j).
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n) for the 2D array.
     */
    public int minPathSumTabulation(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;

        int m = grid.length;
        int n = grid[0].length;
        int[][] dp = new int[m][n];

        // BASE CASE REASONING (The Starting Cell):
        // The cost to stand on the very first cell is exactly the value inside it.
        dp[0][0] = grid[0][0];

        // BASE CASE REASONING (The Top Edge):
        // Consider the very first row (row 0). Since Robi cannot move UP, 
        // the ONLY physical way to traverse this top edge is by moving strictly RIGHT.
        // Therefore, the cost to reach any cell on the top edge is just the 
        // accumulating sum of all cells to its left.
        for (int j = 1; j < n; j++) {
            dp[0][j] = dp[0][j - 1] + grid[0][j];
        }

        // BASE CASE REASONING (The Left Edge):
        // Consider the very first column (col 0). Since Robi cannot move LEFT, 
        // the ONLY physical way to traverse this left edge is by moving strictly DOWN.
        // Therefore, the cost to reach any cell on the left edge is just the 
        // accumulating sum of all cells directly above it.
        for (int i = 1; i < m; i++) {
            dp[i][0] = dp[i - 1][0] + grid[i][0];
        }

        // Now we process the "inner" cells of the grid, starting at (1, 1).
        for (int i = 1; i < m; i++) {
            
            for (int j = 1; j < n; j++) {
                
                // PHYSICAL REALITY OF THE GRID:
                // To land exactly on this current inner cell, our very last step 
                // MUST have been taken from either:
                
                // 1. The cell directly ABOVE (dp[i-1][j]).
                // We look UP one row in our spreadsheet to see the cheapest way we 
                // reached that above cell.
                int cheapestPathFromAbove = dp[i - 1][j];
                
                // 2. The cell directly LEFT (dp[i][j-1]).
                // We look LEFT one column in our spreadsheet to see the cheapest way 
                // we reached that left cell.
                int cheapestPathFromLeft = dp[i][j - 1];

                // Since we want to MINIMIZE our cost, we greedily pick the cheaper 
                // of those two historical routes, and then pay the toll to step 
                // onto our current cell.
                dp[i][j] = grid[i][j] + Math.min(cheapestPathFromAbove, cheapestPathFromLeft);
            }
        }

        // The answer to the problem lies in the very last cell of the grid.
        return dp[m - 1][n - 1];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: Look closely at the inner loop in Tabulation. To calculate `dp[i][j]`, 
     * we ONLY need the cell directly ABOVE it (which comes from the previous row), 
     * and the cell directly LEFT of it (which comes from the current row we are building).
     * 
     * We don't need to keep an entire m x n matrix in memory! We only need ONE 
     * single row array of size 'n' that we continuously overwrite as we scan downward.
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(n) - We flattened a full 2D grid into a 1D array.
     */
    public int minPathSumSpaceOptimized(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        
        int m = grid.length;
        int n = grid[0].length;
        
        // This single 1D array will act as a sliding window moving row by row.
        int[] dp = new int[n];
        
        // BASE CASE REASONING (Seeding Row 0):
        // Just like in 2D Tabulation, we must calculate the exact costs for the 
        // very first row, because there is no row above it to inherit from.
        // The cost is strictly cumulative from left to right.
        dp[0] = grid[0][0];
        for (int j = 1; j < n; j++) {
            dp[j] = dp[j - 1] + grid[0][j];
        }

        // We iterate through every remaining row (from 1 to m-1).
        for (int i = 1; i < m; i++) {
            
            // BASE CASE REASONING (The Left Edge inside the loop):
            // The very first column (j = 0) can only be reached by moving DOWN 
            // from the cell directly above it. 
            // So, `dp[0]` (the current cost) simply absorbs the new cell's cost 
            // to become the new `dp[0]` for this row.
            dp[0] = dp[0] + grid[i][0];
            
            // We iterate left-to-right across the remaining columns.
            for (int j = 1; j < n; j++) {
                
                // MAGIC OF THE 1D ARRAY:
                // We overwrite our single array in-place!
                
                // Right side of equals: `dp[j]` holds the pristine data from the 
                // PREVIOUS row (it represents moving DOWN from above).
                int costFromAbove = dp[j];
                
                // Right side of equals: `dp[j-1]` was literally just calculated a 
                // microsecond ago in the previous loop iteration. It represents 
                // moving RIGHT from the cell on our left.
                int costFromLeft = dp[j - 1];
                
                // We pick the minimum of those two entry points, add our current 
                // cell's cost, and overwrite `dp[j]` for the next row to use.
                dp[j] = grid[i][j] + Math.min(costFromAbove, costFromLeft);
            }
        }

        // After the outer loop finishes scanning all 'm' rows, the last element 
        // in our 1D array represents the bottom-right corner.
        return dp[n - 1];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new MinimumPathSum();
        
        record TestCase(int[][] grid, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[][]{
                {1, 3, 1},
                {1, 5, 1},
                {4, 2, 1}
            }, 7), // 1 -> 3 -> 1 -> 1 -> 1
            new TestCase(new int[][]{
                {1, 2, 3},
                {4, 5, 6}
            }, 12), // 1 -> 2 -> 3 -> 6
            new TestCase(new int[][]{
                {5}
            }, 5) // Single cell grid
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.minPathSumRecursive(tc.grid));
            System.out.println("Memoization       : " + solver.minPathSumMemo(tc.grid));
            System.out.println("Tabulation 2D     : " + solver.minPathSumTabulation(tc.grid));
            System.out.println("Space Optimized   : " + solver.minPathSumSpaceOptimized(tc.grid));
            System.out.println();
        }
    }
}
