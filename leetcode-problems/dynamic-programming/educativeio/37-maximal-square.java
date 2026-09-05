import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Maximal Square
 * Given an m x n binary matrix filled with 0's and 1's, find the largest 
 * square composed entirely of 1's and return its area.
 * 
 * Constraints:
 * m == matrix.length
 * n == matrix[i].length
 * 1 <= m, n <= 300
 * matrix[i][j] is '0' or '1'
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, recognizing the geometric nature of the problem is key:
 * 
 * Q: "Can the matrix contain other numbers, or just 0s and 1s?"
 * A: Only 0s and 1s. This is a binary matrix problem, usually represented as 
 *    characters ('0', '1') or integers (0, 1). We will use characters as it is 
 *    the standard representation for this specific problem on coding platforms.
 * 
 * Q: "Is the goal to find a rectangle or strictly a square?"
 * A: Strictly a square. (Note: Mentioning this difference is a great flex, as 
 *    finding the 'Maximal Rectangle' requires an entirely different and more 
 *    complex monotonic stack algorithm. Calling out that 'squares' simplify 
 *    the DP state transitions shows deep algorithmic maturity.)
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "To form a square of side length K whose BOTTOM-RIGHT corner sits at cell (i, j), 
 * three specific conditions must simultaneously be true:
 * 1. The cell directly ABOVE it (i-1, j) must be the bottom-right of a square of size K-1.
 * 2. The cell directly LEFT of it (i, j-1) must be the bottom-right of a square of size K-1.
 * 3. The cell DIAGONALLY ABOVE-LEFT (i-1, j-1) must be the bottom-right of a square of size K-1.
 * 
 * If ANY of these three neighboring squares are smaller than K-1, our current 
 * square is physically constrained by the smallest one among them. 
 * 
 * Therefore, our recurrence relation is:
 * If matrix[i][j] == '1':
 *     dp[i][j] = Math.min(dp[i-1][j], Math.min(dp[i][j-1], dp[i-1][j-1])) + 1
 * Else:
 *     dp[i][j] = 0
 * 
 * Because the calculation for cell (i, j) relies entirely on already-computed 
 * neighboring cells, this perfectly maps to Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example Grid:
 * 1 0 1 0 0
 * 1 0 1 1 1
 * 1 1 1 1 1
 * 1 0 0 1 0
 * 
 * Let's trace the DP array (which tracks the max SIDE LENGTH ending at each cell):
 * Row 0: 1 0 1 0 0
 * Row 1: 1 0 1 1 1
 * Row 2: 1 1 1 2 2  <-- Cell (2,3) looks at (1,3)=1, (2,2)=1, (1,2)=1. Min is 1. +1 = 2!
 * Row 3: 1 0 0 1 0
 * 
 * The maximum side length found anywhere in the DP matrix is 2.
 * The area is 2 * 2 = 4.
 */
public class MaximalSquare {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Traverse every cell. If it's a '1', recursively find the max square 
     * ending at that cell by looking up, left, and diagonal-left.
     * 
     * Time Complexity: O(3^(m*n)) - Massive exponential branching.
     * Space Complexity: O(m+n) - Maximum depth of the recursion tree.
     */
    public int maximalSquareRecursive(char[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int maxSide = 0;
        int m = matrix.length;
        int n = matrix[0].length;
        
        // We must check every single cell to see if it acts as the bottom-right 
        // corner of the largest possible square in the grid.
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                maxSide = Math.max(maxSide, solveRecursive(matrix, i, j));
            }
        }
        
        return maxSide * maxSide; // Area = side * side
    }

    private int solveRecursive(char[][] matrix, int i, int j) {
        // BASE CASE REASONING (Out of Bounds):
        // If we fall off the top or left edges of the grid, there is no square here.
        if (i < 0 || j < 0) {
            return 0;
        }
        
        // BASE CASE REASONING (Obstacle):
        // If the current cell is '0', it physically cannot be the bottom-right 
        // corner of ANY valid square. Its side length contribution is 0.
        if (matrix[i][j] == '0') {
            return 0;
        }
        
        // Recursive Universe: Look UP
        int up = solveRecursive(matrix, i - 1, j);
        
        // Recursive Universe: Look LEFT
        int left = solveRecursive(matrix, i, j - 1);
        
        // Recursive Universe: Look DIAGONAL (Up-Left)
        int diag = solveRecursive(matrix, i - 1, j - 1);
        
        // The square size ending here is constrained by the smallest neighbor.
        return Math.min(up, Math.min(left, diag)) + 1;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the maximum side length calculated for each cell (i, j).
     * 
     * Time Complexity: O(m * n) - We evaluate each cell exactly once.
     * Space Complexity: O(m * n) - For the 2D memo array + call stack.
     */
    public int maximalSquareMemo(char[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int m = matrix.length;
        int n = matrix[0].length;
        int[][] memo = new int[m][n];
        
        for (int[] row : memo) Arrays.fill(row, -1);
        
        int maxSide = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                maxSide = Math.max(maxSide, solveMemo(matrix, i, j, memo));
            }
        }
        
        return maxSide * maxSide;
    }

    private int solveMemo(char[][] matrix, int i, int j, int[][] memo) {
        if (i < 0 || j < 0) return 0;
        if (matrix[i][j] == '0') return 0;
        
        if (memo[i][j] != -1) {
            return memo[i][j];
        }
        
        int up = solveMemo(matrix, i - 1, j, memo);
        int left = solveMemo(matrix, i, j - 1, memo);
        int diag = solveMemo(matrix, i - 1, j - 1, memo);
        
        memo[i][j] = Math.min(up, Math.min(left, diag)) + 1;
        return memo[i][j];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build an m x n spreadsheet. dp[i][j] signifies the maximum side length 
     * of a valid square whose bottom-right corner is exactly at cell (i, j).
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n) for the DP table.
     */
    public int maximalSquareTabulation(char[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int m = matrix.length;
        int n = matrix[0].length;
        
        // We add an extra padding row (0) and column (0) to eliminate boundary checks.
        // This shifts our matrix indices by +1 inside the DP array.
        int[][] dp = new int[m + 1][n + 1];
        int maxSide = 0;

        // Iterate through the grid from top-left to bottom-right
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                
                // PHYSICAL CHECK: Is the cell in the original matrix a '1'?
                if (matrix[i - 1][j - 1] == '1') {
                    
                    // --- DETAILED TABULATION EXPLANATION ---
                    // Look UP (dp[i - 1][j])
                    // Look LEFT (dp[i][j - 1])
                    // Look DIAGONAL (dp[i - 1][j - 1])
                    
                    // The side length of the square ending here is exactly 1 greater 
                    // than the bottleneck (minimum) of its three neighbors.
                    dp[i][j] = Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1])) + 1;
                    
                    // Constantly update our global maximum side length found anywhere in the grid.
                    maxSide = Math.max(maxSide, dp[i][j]);
                }
                // (Implicit Else: If matrix cell is '0', Java already initialized dp[i][j] to 0)
            }
        }

        return maxSide * maxSide;
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, notice that to calculate `dp[i][j]`, we ONLY need 
     * the current row `dp[j-1]` (left) and the previous row `dp[j]` (up).
     * The ONLY tricky part is `dp[i-1][j-1]` (diagonal), which gets overwritten 
     * by `dp[j-1]` right before we need it. 
     * 
     * We can solve this by holding the diagonal value in a single temporary variable `prev`!
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(n) - Massively reduced memory footprint.
     */
    public int maximalSquareSpaceOptimized(char[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int m = matrix.length;
        int n = matrix[0].length;
        
        // This single 1D array represents the "previous row".
        int[] dp = new int[n + 1];
        int maxSide = 0;
        
        // 'prev' will act as our temporal anchor for the `dp[i-1][j-1]` diagonal value.
        int prev = 0;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                
                // Before we potentially overwrite dp[j], we MUST save it.
                // Right now, dp[j] holds the value for the row directly above us. 
                // But in the VERY NEXT iteration (j+1), this current dp[j] will 
                // physically represent the diagonal above-left for that cell.
                int temp = dp[j];
                
                if (matrix[i - 1][j - 1] == '1') {
                    
                    // MAGIC OF THE 1D ARRAY + PREV VARIABLE:
                    // dp[j]     -> Directly UP (from the previous iteration's row)
                    // dp[j-1]   -> Directly LEFT (literally just updated a microsecond ago)
                    // prev      -> DIAGONALLY UP-LEFT (saved securely in our temp variable)
                    
                    dp[j] = Math.min(dp[j], Math.min(dp[j - 1], prev)) + 1;
                    maxSide = Math.max(maxSide, dp[j]);
                    
                } else {
                    // CRITICAL DIFFERENCE FROM 2D TABULATION:
                    // In a 1D array, if we hit a '0', we MUST explicitly reset 
                    // this cell to 0. Otherwise, the old value from the previous 
                    // row will "leak" downward and corrupt the math.
                    dp[j] = 0;
                }
                
                // Advance the diagonal anchor forward for the next cell
                prev = temp;
            }
        }

        return maxSide * maxSide;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new MaximalSquare();
        
        record TestCase(char[][] matrix, int expectedArea) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new char[][]{
                {'1', '0', '1', '0', '0'},
                {'1', '0', '1', '1', '1'},
                {'1', '1', '1', '1', '1'},
                {'1', '0', '0', '1', '0'}
            }, 4), // Traced in comments: 2x2 square -> area 4
            
            new TestCase(new char[][]{
                {'0', '1'},
                {'1', '0'}
            }, 1), // Max square is 1x1
            
            new TestCase(new char[][]{
                {'0'}
            }, 0), // No 1s present
            
            new TestCase(new char[][]{
                {'1', '1', '1'},
                {'1', '1', '1'},
                {'1', '1', '1'}
            }, 9)  // Full 3x3 square
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Expected Area: " + tc.expectedArea);
            
            System.out.println("Recursive (Brute) : " + solver.maximalSquareRecursive(tc.matrix));
            System.out.println("Memoization       : " + solver.maximalSquareMemo(tc.matrix));
            System.out.println("Tabulation 2D     : " + solver.maximalSquareTabulation(tc.matrix));
            System.out.println("Space Optimized   : " + solver.maximalSquareSpaceOptimized(tc.matrix));
            System.out.println();
        }
    }
}
