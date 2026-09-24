import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Unique Paths II (Grid with Obstacles)
 * Given an m x n grid where 1 represents an obstacle and 0 represents an 
 * empty space, find the number of distinct paths from the top-left corner 
 * to the bottom-right corner. You can only move DOWN or RIGHT.
 * 
 * Constraints:
 * 1 <= m, n <= 100
 * obstacleGrid[i][j] is 0 or 1
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, immediately identify the edge cases that differentiate 
 * this from the standard "Unique Paths I" problem:
 * 
 * Q: "Can the starting cell (0,0) or the destination cell (m-1,n-1) be blocked?"
 * A: Yes! If the start or end is an obstacle (1), it is physically impossible 
 *    to start or complete the journey. The answer must immediately be 0.
 * 
 * Q: "Will the number of paths fit in a standard 32-bit integer?"
 * A: Standard LeetCode/interview constraints for this problem guarantee the 
 *    answer fits in a signed 32-bit integer.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "The core movement logic remains exactly the same as Unique Paths I: 
 * to reach cell (row, col), I must have come from either (row - 1, col) 
 * or (row, col - 1). 
 * 
 * The only difference is the physical barrier rule:
 * If a cell contains an obstacle (1), the number of valid paths flowing 
 * THROUGH that cell is instantly and permanently 0. It acts as a black hole 
 * for path counts.
 * 
 * We have overlapping subproblems (multiple ways to reach adjacent open cells), 
 * meaning Dynamic Programming is the optimal approach."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: 
 * Grid = 
 * [0, 0, 0]
 * [0, 1, 0]
 * [0, 0, 0]
 * 
 * Let's trace the Tabulation (Bottom-Up) grid:
 * Row 0:
 * - (0,0) = 1 path.
 * - (0,1) = 1 path (came from left).
 * - (0,2) = 1 path (came from left).
 * 
 * Row 1:
 * - (1,0) = 1 path (came from above).
 * - (1,1) is an OBSTACLE. Paths = 0.
 * - (1,2) looks above (1) and left (0). Paths = 1 + 0 = 1.
 * 
 * Row 2:
 * - (2,0) looks above (1). Paths = 1.
 * - (2,1) looks above (0) and left (1). Paths = 0 + 1 = 1.
 * - (2,2) looks above (1) and left (1). Paths = 1 + 1 = 2.
 * 
 * Final unique paths = 2.
 */
public class UniquePathsII {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Start at (0,0) and branch Down and Right. If we hit an obstacle 
     * or go out of bounds, that branch dies (returns 0).
     * 
     * Time Complexity: O(2^(m+n)) - Exponential branching at every open cell.
     * Space Complexity: O(m+n) - Maximum depth of the recursion tree.
     */
    public int uniquePathsWithObstaclesRecursive(int[][] obstacleGrid) {
        if (obstacleGrid == null || obstacleGrid.length == 0) return 0;
        return solveRecursive(obstacleGrid, 0, 0);
    }

    private int solveRecursive(int[][] grid, int row, int col) {
        int m = grid.length;
        int n = grid[0].length;

        // BASE CASE 1: Out of bounds OR hit an obstacle.
        // If we step out of the grid, or we hit a rock (1), this specific 
        // path is completely dead. It contributes 0 valid ways.
        if (row >= m || col >= n || grid[row][col] == 1) {
            return 0;
        }

        // BASE CASE 2: Reached the destination.
        // If we successfully land on the bottom-right cell, we found exactly 
        // 1 valid path through the maze.
        if (row == m - 1 && col == n - 1) {
            return 1;
        }

        // Branch 1: Try moving DOWN
        int down = solveRecursive(grid, row + 1, col);
        
        // Branch 2: Try moving RIGHT
        int right = solveRecursive(grid, row, col + 1);

        // The total valid routes from this cell is the sum of both universes.
        return down + right;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: We arrive at the same open grid cells repeatedly via different routes.
     * Cache the results for [row][col] to evaluate each open cell exactly once.
     * 
     * Time Complexity: O(m * n) - Evaluate each cell at most once.
     * Space Complexity: O(m * n) - For the 2D memo array + recursion stack.
     */
    public int uniquePathsWithObstaclesMemo(int[][] obstacleGrid) {
        if (obstacleGrid == null || obstacleGrid.length == 0) return 0;
        
        int m = obstacleGrid.length;
        int n = obstacleGrid[0].length;
        
        int[][] memo = new int[m][n];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(obstacleGrid, 0, 0, memo);
    }

    private int solveMemo(int[][] grid, int row, int col, int[][] memo) {
        int m = grid.length;
        int n = grid[0].length;

        // BASE CASES (Same physical logic as brute force)
        if (row >= m || col >= n || grid[row][col] == 1) {
            return 0;
        }
        if (row == m - 1 && col == n - 1) {
            return 1;
        }

        // Return cached result if already computed
        if (memo[row][col] != -1) {
            return memo[row][col];
        }

        int down = solveMemo(grid, row + 1, col, memo);
        int right = solveMemo(grid, row, col + 1, memo);

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
    public int uniquePathsWithObstaclesTabulation(int[][] obstacleGrid) {
        if (obstacleGrid == null || obstacleGrid.length == 0) return 0;

        int m = obstacleGrid.length;
        int n = obstacleGrid[0].length;

        // Immediate Short-Circuit: If the starting line is blocked, game over.
        if (obstacleGrid[0][0] == 1) return 0;

        int[][] dp = new int[m][n];

        // BASE CASE REASONING (The Starting Cell):
        // We stand on the starting cell. There is 1 way to be here (doing nothing).
        dp[0][0] = 1;

        // BASE CASE REASONING (The Top Edge):
        // For the very first row, we can only reach cells by moving strictly RIGHT.
        // However, if we encounter an obstacle, EVERY cell to the right of it 
        // becomes completely unreachable, because we cannot go around it.
        for (int j = 1; j < n; j++) {
            if (obstacleGrid[0][j] == 0 && dp[0][j - 1] == 1) {
                dp[0][j] = 1;
            } else {
                dp[0][j] = 0; // Blocked or behind a block
            }
        }

        // BASE CASE REASONING (The Left Edge):
        // Similarly, for the very first column, we can only reach cells by moving DOWN.
        // An obstacle blocks the rest of the column underneath it.
        for (int i = 1; i < m; i++) {
            if (obstacleGrid[i][0] == 0 && dp[i - 1][0] == 1) {
                dp[i][0] = 1;
            } else {
                dp[i][0] = 0;
            }
        }

        // Now we process the "inner" cells of the grid, starting at (1, 1).
        for (int i = 1; i < m; i++) {
            
            for (int j = 1; j < n; j++) {
                
                // --- DETAILED TABULATION EXPLANATION ---
                // PHYSICAL CHECK: Is there a massive rock sitting on this cell?
                if (obstacleGrid[i][j] == 1) {
                    // YES. It is impossible to walk through this cell. 
                    // We lock the number of paths passing through it to exactly 0.
                    dp[i][j] = 0;
                } else {
                    // NO. The cell is clear. To land exactly here, our last step 
                    // MUST have been from either ABOVE or LEFT.
                    // (Note: If an obstacle was above or left, its dp value is 
                    // already 0, so it naturally contributes nothing to our sum!)
                    int pathsFromAbove = dp[i - 1][j];
                    int pathsFromLeft = dp[i][j - 1];

                    // Total unique routes is the sum of both valid entry points.
                    dp[i][j] = pathsFromAbove + pathsFromLeft;
                }
            }
        }

        // The total paths to the finish line sit at the bottom-right corner.
        return dp[m - 1][n - 1];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, to calculate row `i`, we ONLY look at `dp[i-1][j]` 
     * (the row directly above it) and `dp[i][j-1]` (the cell directly to its left).
     * 
     * We can collapse the m x n grid into a single 1D array of size 'n' representing 
     * the columns. As we scan downward row by row, we overwrite the array in-place.
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(n) - Massively reduced memory footprint.
     */
    public int uniquePathsWithObstaclesSpaceOptimized(int[][] obstacleGrid) {
        if (obstacleGrid == null || obstacleGrid.length == 0) return 0;
        
        int m = obstacleGrid.length;
        int n = obstacleGrid[0].length;
        
        // This single 1D array will act as a sliding window moving row by row.
        int[] dp = new int[n];
        
        // BASE CASE REASONING (Seeding the starting position):
        // If the start is blocked, dp[0] is 0. Else, 1.
        dp[0] = obstacleGrid[0][0] == 1 ? 0 : 1;

        // We iterate through every single row in the grid.
        for (int i = 0; i < m; i++) {
            
            // We iterate left-to-right across the columns.
            for (int j = 0; j < n; j++) {
                
                // MAGIC OF THE 1D ARRAY:
                // Is this specific cell an obstacle?
                if (obstacleGrid[i][j] == 1) {
                    
                    // Yes! This cell is a dead end. We MUST explicitly set dp[j] to 0. 
                    // This overwrites whatever valid paths were above it, preventing 
                    // the obstacle from "leaking" paths into the row below it.
                    dp[j] = 0;
                    
                } else if (j > 0) {
                    
                    // No obstacle! We overwrite our single array in-place.
                    // Right side of equals: `dp[j]` holds the pristine data from the 
                    // PREVIOUS row (representing paths moving DOWN from above).
                    // `dp[j-1]` holds the value we literally just updated a microsecond 
                    // ago (representing paths moving RIGHT from the left).
                    dp[j] = dp[j] + dp[j - 1];
                    
                    // Note: If j == 0, it means we are at the left edge. 
                    // Since we can't come from the left, we only inherit paths from 
                    // above. `dp[0]` just naturally keeps its previous value `dp[0]`, 
                    // unless it hit the obstacle condition above!
                }
            }
        }

        // After scanning all 'm' rows, the final cell holds our answer.
        return dp[n - 1];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new UniquePathsII();
        
        record TestCase(int[][] grid, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[][]{
                {0, 0, 0},
                {0, 1, 0},
                {0, 0, 0}
            }, 2), // Traced in comments
            
            new TestCase(new int[][]{
                {0, 1},
                {0, 0}
            }, 1), // Only 1 path: down then right
            
            new TestCase(new int[][]{
                {1, 0},
                {0, 0}
            }, 0), // Blocked at the start
            
            new TestCase(new int[][]{
                {0, 0},
                {1, 1},
                {0, 0}
            }, 0) // Full wall blocking the path
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Expected Paths: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.uniquePathsWithObstaclesRecursive(tc.grid));
            System.out.println("Memoization       : " + solver.uniquePathsWithObstaclesMemo(tc.grid));
            System.out.println("Tabulation 2D     : " + solver.uniquePathsWithObstaclesTabulation(tc.grid));
            System.out.println("Space Optimized   : " + solver.uniquePathsWithObstaclesSpaceOptimized(tc.grid));
            System.out.println();
        }
    }
}
