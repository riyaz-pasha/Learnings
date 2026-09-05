import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Longest Increasing Path in a Matrix
 * Given an m x n matrix of integers, return the length of the longest 
 * strictly increasing path. You can move up, down, left, or right.
 * 
 * Constraints:
 * m == matrix.length
 * n == matrix[i].length
 * 1 <= m, n <= 200
 * 0 <= matrix[i][j] <= 2^31 - 1
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, this problem is a phenomenal test of identifying 
 * hidden graph properties in a matrix.
 * 
 * Q: "Do I need a 'visited' array to prevent infinite loops (cycles)?"
 * A: NO! This is a massive senior insight. Because the path must be 
 *    STRICLY INCREASING, it is physically impossible to travel in a circle 
 *    (e.g., 1 -> 2 -> 3 -> 1 is invalid because 1 is not greater than 3). 
 *    Therefore, this matrix implicitly forms a Directed Acyclic Graph (DAG).
 * 
 * Q: "Can I just use standard 2D Tabulation (like Unique Paths)?"
 * A: Standard nested loops (top-to-bottom, left-to-right) will fail here! 
 *    In standard DP, you know exactly where your dependencies are (e.g., 
 *    always above and to the left). Here, a valid increasing path can snake 
 *    in any of the 4 directions. We do not have a fixed topological order 
 *    just by reading the grid left-to-right.
 * 
 * CRITICAL SENIOR INSIGHT:
 * "Because we lack a rigid directional evaluation order, Top-Down DP 
 * (DFS + Memoization) is the absolute most natural and industry-standard 
 * way to solve this. If forced to use Bottom-Up Tabulation, we must first 
 * artificially create an evaluation order by sorting all the cells by their 
 * values, processing the smallest numbers first."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "Starting from any cell (i, j), the longest increasing path is exactly 1 
 * (the cell itself) PLUS the maximum of the longest increasing paths of its 
 * 4 neighbors—provided those neighbors have a strictly greater value.
 * 
 * Since many paths will inevitably converge on the same high-value 'mountain 
 * peak' cells, we have massive overlapping subproblems. 
 * We will cache the Longest Increasing Path (LIP) starting from each cell."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example Grid:
 * 9  9  4
 * 6  6  8
 * 2  1  1
 * 
 * Let's trace Top-Down Memoization from (2,1) which is the '1' in the middle:
 * 
 * Cell(2,1)=1 looks around for strictly greater neighbors:
 * -> UP: Cell(1,1)=6.
 *    -> Cell(1,1)=6 looks around:
 *       -> UP: Cell(0,1)=9. (Dead end, no greater neighbors. LIP = 1)
 *    -> Cell(1,1) LIP = 1 + (LIP of 9) = 2.
 * 
 * -> RIGHT: Cell(2,2)=1 (Not strictly greater. Ignore).
 * -> LEFT: Cell(2,0)=2.
 *    -> Cell(2,0)=2 looks around:
 *       -> UP: Cell(1,0)=6.
 *          -> UP: Cell(0,0)=9. (LIP = 1)
 *          -> Cell(1,0) LIP = 1 + 1 = 2.
 *       -> Cell(2,0) LIP = 1 + 2 = 3.
 * 
 * Cell(2,1) compares paths: via UP(6) is 2, via LEFT(2) is 3.
 * Max is 3. Therefore, Cell(2,1) LIP = 1 + 3 = 4.
 * The path is [1, 2, 6, 9].
 */
public class LongestIncreasingPath {

    private static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Launch a DFS from every single cell to find the longest path.
     * 
     * Time Complexity: O(4^(m*n)) - Exponential branching from every cell.
     * Space Complexity: O(m*n) - Maximum depth of the recursion tree.
     */
    public int longestIncreasingPathRecursive(int[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int maxPath = 0;
        int m = matrix.length;
        int n = matrix[0].length;
        
        // We must attempt to start a path from every single cell.
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                maxPath = Math.max(maxPath, solveRecursive(matrix, i, j));
            }
        }
        return maxPath;
    }

    private int solveRecursive(int[][] matrix, int row, int col) {
        int maxLength = 1; // The cell itself constitutes a path of length 1
        
        // Explore all 4 parallel universes (Up, Down, Left, Right)
        for (int[] dir : DIRS) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            // PHYSICAL CHECK: 
            // 1. Is the neighbor within the grid bounds?
            // 2. Is the neighbor STRICTLY GREATER than our current cell?
            if (newRow >= 0 && newRow < matrix.length && newCol >= 0 && newCol < matrix[0].length 
                && matrix[newRow][newCol] > matrix[row][col]) {
                
                // If yes, we take a step into that universe and calculate its maximum path.
                int pathLengthFromNeighbor = solveRecursive(matrix, newRow, newCol);
                maxLength = Math.max(maxLength, 1 + pathLengthFromNeighbor);
            }
        }
        
        return maxLength;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: The brute force approach repeatedly calculates the same "mountain peaks". 
     * We cache the longest path originating from `matrix[i][j]` to ensure we 
     * evaluate each cell exactly once.
     * 
     * Time Complexity: O(m * n) - We evaluate each cell exactly once.
     * Space Complexity: O(m * n) - For the 2D memo array + call stack.
     */
    public int longestIncreasingPathMemo(int[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int m = matrix.length;
        int n = matrix[0].length;
        int[][] memo = new int[m][n]; // initialized to 0 by default
        
        int maxPath = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                maxPath = Math.max(maxPath, solveMemo(matrix, i, j, memo));
            }
        }
        return maxPath;
    }

    private int solveMemo(int[][] matrix, int row, int col, int[][] memo) {
        // Return cached result if we've already mapped the path from this cell
        if (memo[row][col] != 0) {
            return memo[row][col];
        }
        
        int maxLength = 1;
        
        for (int[] dir : DIRS) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            if (newRow >= 0 && newRow < matrix.length && newCol >= 0 && newCol < matrix[0].length 
                && matrix[newRow][newCol] > matrix[row][col]) {
                
                maxLength = Math.max(maxLength, 1 + solveMemo(matrix, newRow, newCol, memo));
            }
        }
        
        memo[row][col] = maxLength;
        return maxLength;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation via Sorting)
     * ========================================================================
     * Idea: Standard row-by-row tabulation fails here. To build from the bottom up, 
     * we must process the cells in strictly ascending order of their values.
     * This guarantees that when evaluating cell (i, j), any strictly smaller 
     * neighbors it could have come from have ALREADY been fully processed!
     * 
     * Time Complexity: O(m * n * log(m * n)) - Due to sorting the cells.
     * Space Complexity: O(m * n) - To store the list of cells and the DP array.
     */
    public int longestIncreasingPathTabulation(int[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int m = matrix.length;
        int n = matrix[0].length;
        
        // dp[i][j] signifies: "The length of the longest increasing path ENDING at cell (i, j)"
        int[][] dp = new int[m][n];
        
        // 1. Flatten the grid into a list of cells [value, row, col] so we can sort them.
        int[][] cells = new int[m * n][3];
        int index = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                cells[index++] = new int[]{matrix[i][j], i, j};
                dp[i][j] = 1; // BASE CASE: Every cell represents a path of at least length 1
            }
        }
        
        // 2. Sort the cells by their value in ASCENDING order.
        // This acts as our topological sort. We guarantee we process valleys before mountains.
        Arrays.sort(cells, (a, b) -> Integer.compare(a[0], b[0]));
        
        int maxPath = 1;
        
        // 3. Process each cell from smallest to largest
        for (int[] cell : cells) {
            int val = cell[0];
            int row = cell[1];
            int col = cell[2];
            
            // --- DETAILED TABULATION EXPLANATION ---
            // We look at our 4 neighbors. Since we process in ascending order, 
            // if a neighbor is strictly GREATER than us, it means we can safely 
            // step into it. We push our current path length forward to that neighbor!
            for (int[] dir : DIRS) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                // PHYSICAL CHECK: 
                // Is the neighbor in bounds, and is it strictly GREATER than our current cell?
                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n 
                    && matrix[newRow][newCol] > val) {
                    
                    // The neighbor's DP value becomes the maximum of its current 
                    // recorded path, or our path + 1 (stepping from us to them).
                    dp[newRow][newCol] = Math.max(dp[newRow][newCol], dp[row][col] + 1);
                    
                    // Continually update our global tracker
                    maxPath = Math.max(maxPath, dp[newRow][newCol]);
                }
            }
        }
        
        return maxPath;
    }

    /**
     * ========================================================================
     * APPROACH 4: Note on Space Optimization (L4/L5 Expectation)
     * ========================================================================
     * In standard grid DP problems (like Unique Paths), we optimize 2D space 
     * down to 1D because row `i` ONLY relies on row `i-1`.
     * 
     * CRITICAL INSIGHT:
     * We CANNOT optimize the space down to 1D for this problem. 
     * Because movement is allowed in all 4 directions, a path can snake from 
     * row 0, down to row 199, and spiral all the way back up to row 0. 
     * 
     * Since dependencies are scattered unpredictably across the entire grid, 
     * we must keep the entire O(m * n) DP array in memory at all times. 
     * Stating this boundary limitation explicitly during an interview proves 
     * you truly understand when and why state reduction works!
     */

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new LongestIncreasingPath();
        
        record TestCase(int[][] matrix, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[][]{
                {9, 9, 4},
                {6, 6, 8},
                {2, 1, 1}
            }, 4), // Traced in comments: 1 -> 2 -> 6 -> 9
            
            new TestCase(new int[][]{
                {3, 4, 5},
                {3, 2, 6},
                {2, 2, 1}
            }, 4), // 3 -> 4 -> 5 -> 6
            
            new TestCase(new int[][]{
                {1}
            }, 1), // Single cell
            
            new TestCase(new int[][]{
                {1, 2},
                {4, 3}
            }, 4) // Snake pattern: 1 -> 2 -> 3 -> 4
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.longestIncreasingPathRecursive(tc.matrix));
            System.out.println("Memoization       : " + solver.longestIncreasingPathMemo(tc.matrix));
            System.out.println("Tabulation Sorted : " + solver.longestIncreasingPathTabulation(tc.matrix));
            System.out.println();
        }
    }
}
