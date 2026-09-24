import java.util.*;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Matrix Chain Multiplication (MCM)
 * Given an array 'arr' representing the dimensions of a sequence of matrices, 
 * find the most efficient way to multiply these matrices together. 
 * The problem is not to perform the multiplications, but merely to decide 
 * the sequence of multiplications (where to place parentheses) to MINIMIZE 
 * the total number of scalar multiplications.
 * 
 * Note: Matrix 'i' (1-indexed) has dimensions arr[i-1] x arr[i].
 * 
 * Constraints:
 * 2 <= arr.length <= 100
 * 1 <= arr[i] <= 500
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, MCM is the canonical example of "Interval DP".
 * 
 * Q: "Why does the order of multiplication matter?"
 * A: Matrix multiplication is associative, meaning A*(B*C) == (A*B)*C, but the 
 *    COMPUTATIONAL COST is drastically different based on dimensions.
 *    If A is 10x30, B is 30x5, C is 5x60:
 *    - (AB)C cost: (10*30*5) + (10*5*60) = 1500 + 3000 = 4500 operations.
 *    - A(BC) cost: (30*5*60) + (10*30*60) = 9000 + 18000 = 27000 operations.
 * 
 * Q: "Will the final answer fit in a standard 32-bit integer?"
 * A: For array sizes up to 100 and dimensions up to 500, the maximum possible 
 *    operations could potentially exceed 2.1 billion (Integer.MAX_VALUE). 
 *    In production, returning a `long` or catching overflow is best. However, 
 *    standard coding platforms guarantee the answer fits in an `int` for this problem.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We have a sequence of matrices from index 'i' to 'j'. To multiply them all 
 * together, we MUST make a final matrix multiplication between two chunks. 
 * We can place this final 'split' at some index 'k' (where i <= k < j).
 * 
 * This means we first multiply the left chunk (i to k), then the right chunk 
 * (k+1 to j), and finally multiply the resulting two matrices together.
 * 
 * The total cost for a split at 'k' is:
 * (Cost of left chunk) + (Cost of right chunk) + (Cost to multiply left and right chunks)
 * 
 * To find the absolute minimum cost, we evaluate EVERY possible split point 'k' 
 * between 'i' and 'j', and take the minimum. Because we will repeatedly evaluate 
 * the same smaller matrix chains, this is Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: arr = [10, 20, 30, 40] -> Matrices A(10x20), B(20x30), C(30x40)
 * We want the min cost for interval i=1 to j=3 (Matrices A to C).
 * 
 * Options for split 'k':
 * 
 * Option 1: k = 1. Left: A, Right: (BC). 
 * - Cost of A (i=1, j=1) = 0.
 * - Cost of BC (i=2, j=3) = 20 * 30 * 40 = 24000.
 * - Resulting matrices: A is (10x20), BC is (20x40). 
 * - Final multiply cost = 10 * 20 * 40 = 8000.
 * - Total = 0 + 24000 + 8000 = 32000.
 * 
 * Option 2: k = 2. Left: (AB), Right: C.
 * - Cost of AB (i=1, j=2) = 10 * 20 * 30 = 6000.
 * - Cost of C (i=3, j=3) = 0.
 * - Resulting matrices: AB is (10x30), C is (30x40).
 * - Final multiply cost = 10 * 30 * 40 = 12000.
 * - Total = 6000 + 0 + 12000 = 18000.
 * 
 * Minimum is 18000.
 */
public class MatrixChainMultiplication {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Try every possible partition point 'k' for the given interval [i, j].
     * 
     * Time Complexity: O(2^N) or specifically Catalan Number - Exponential branching.
     * Space Complexity: O(N) - Maximum depth of the recursion tree.
     */
    public int mcmRecursive(int[] arr) {
        if (arr == null || arr.length < 2) return 0;
        // The number of matrices is arr.length - 1. 
        // Matrix 1 starts at i=1, Matrix n starts at i=n-1.
        return solveRecursive(arr, 1, arr.length - 1);
    }

    private int solveRecursive(int[] arr, int i, int j) {
        // BASE CASE REASONING:
        // If i == j, we are looking at a single, isolated matrix.
        // It requires exactly 0 scalar multiplications to multiply a single 
        // matrix by itself (it's already computed). We return 0 cost.
        if (i == j) {
            return 0;
        }

        int minCost = Integer.MAX_VALUE;

        // Try partitioning the chain at every possible index 'k'.
        // The left chunk will be matrices from i to k.
        // The right chunk will be matrices from (k + 1) to j.
        for (int k = i; k < j; k++) {
            
            // 1. Solve the left sub-chain recursively
            int leftCost = solveRecursive(arr, i, k);
            
            // 2. Solve the right sub-chain recursively
            int rightCost = solveRecursive(arr, k + 1, j);
            
            // 3. Cost to multiply the resulting left and right matrices together.
            // The left matrix has dimensions: arr[i-1] x arr[k]
            // The right matrix has dimensions: arr[k] x arr[j]
            // Cost = (rows of left) * (shared dimension) * (cols of right)
            int multiplyCost = arr[i - 1] * arr[k] * arr[j];
            
            int totalCost = leftCost + rightCost + multiplyCost;
            
            minCost = Math.min(minCost, totalCost);
        }

        return minCost;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: We repeatedly evaluate the same sub-chains (e.g., matrices 2 to 4).
     * Cache the results in a 2D memo array.
     * 
     * Time Complexity: O(N^3) - State space is N^2, loop of size N inside.
     * Space Complexity: O(N^2) - For the memo array + call stack.
     */
    public int mcmMemo(int[] arr) {
        if (arr == null || arr.length < 2) return 0;
        
        int n = arr.length;
        int[][] memo = new int[n][n];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(arr, 1, n - 1, memo);
    }

    private int solveMemo(int[] arr, int i, int j, int[][] memo) {
        // BASE CASE (Same physical logic as brute force)
        if (i == j) return 0;

        if (memo[i][j] != -1) {
            return memo[i][j];
        }

        int minCost = Integer.MAX_VALUE;

        for (int k = i; k < j; k++) {
            int left = solveMemo(arr, i, k, memo);
            int right = solveMemo(arr, k + 1, j, memo);
            int multiplyCost = arr[i - 1] * arr[k] * arr[j];
            
            minCost = Math.min(minCost, left + right + multiplyCost);
        }

        memo[i][j] = minCost;
        return memo[i][j];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build an N x N grid. dp[i][j] signifies the minimum cost to multiply 
     * the continuous sequence of matrices from index 'i' to index 'j'.
     * 
     * CRITICAL SENIOR INSIGHT - INTERVAL DP ITERATION:
     * We cannot just loop 'i' from 1 to N and 'j' from 1 to N. Why? 
     * Because to calculate a large chain (like 1 to 4), we MUST ALREADY have 
     * the answers for the smaller chains (like 1 to 2, and 3 to 4).
     * Therefore, the outer loop MUST be the LENGTH of the chain we are evaluating.
     * 
     * Time Complexity: O(N^3)
     * Space Complexity: O(N^2)
     */
    public int mcmTabulation(int[] arr) {
        if (arr == null || arr.length < 2) return 0;
        
        int n = arr.length;
        int[][] dp = new int[n][n];

        // BASE CASE REASONING:
        // A chain of length 1 (a single matrix) costs 0 to multiply.
        // This corresponds to dp[i][i] = 0.
        // Java initializes arrays to 0 by default, so the main diagonal is already 
        // perfectly set to 0.

        // Outer loop: The LENGTH of the chain we are currently evaluating.
        // We start from length 2 (multiplying 2 matrices) up to n-1 (all matrices).
        for (int length = 2; length < n; length++) {
            
            // We slide a window of size 'length' across our array of matrices.
            // 'i' is the starting matrix of the current window.
            for (int i = 1; i <= n - length; i++) {
                
                // 'j' is the ending matrix of the current window.
                int j = i + length - 1;
                
                dp[i][j] = Integer.MAX_VALUE;
                
                // --- DETAILED TABULATION EXPLANATION ---
                // We test every possible split point 'k' strictly inside our window.
                for (int k = i; k < j; k++) {
                    
                    // Look up the optimal cost for the left sub-chain. 
                    // This is guaranteed to be computed because its length is smaller 
                    // than our current 'length'.
                    int leftCost = dp[i][k];
                    
                    // Look up the optimal cost for the right sub-chain.
                    int rightCost = dp[k + 1][j];
                    
                    // Calculate the cost to combine them.
                    int multiplyCost = arr[i - 1] * arr[k] * arr[j];
                    
                    int totalCost = leftCost + rightCost + multiplyCost;
                    
                    // Greedily record the minimum cost found across all split points.
                    dp[i][j] = Math.min(dp[i][j], totalCost);
                }
            }
        }

        // The answer sits in the cell representing the full chain: matrix 1 to n-1.
        return dp[1][n - 1];
    }

    /**
     * ========================================================================
     * APPROACH 4: Note on Space Optimization (L4/L5 Expectation)
     * ========================================================================
     * Just like the "Burst Balloons" problem, Matrix Chain Multiplication is an 
     * "Interval DP" problem. 
     * 
     * To compute the answer for `dp[i][j]`, we must look up `dp[i][k]` and 
     * `dp[k+1][j]` for EVERY 'k' between 'i' and 'j'. 
     * 
     * This means we are querying cells all over the 2D matrix—specifically, 
     * multiple cells in the same row `i`, and multiple cells in the same column `j`. 
     * We CANNOT discard previous rows or columns because they represent the smaller 
     * foundational intervals that make up our larger intervals.
     * 
     * Therefore, O(N^2) space is the strict mathematical lower bound for standard 
     * dynamic programming approaches to MCM. We cannot collapse this into a 1D array.
     */

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new MatrixChainMultiplication();
        
        record TestCase(int[] arr, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{10, 20, 30, 40}, 18000),      // Covered in tracing example
            new TestCase(new int[]{10, 20, 30}, 6000),           // Only two matrices (10x20 * 20x30)
            new TestCase(new int[]{40, 20, 30, 10, 30}, 26000),  // Optimal split: (A(BC))D
            new TestCase(new int[]{10, 30, 5, 60}, 4500),        // A(BC) optimal logic
            new TestCase(new int[]{5, 10}, 0)                    // Only one matrix, 0 cost
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Dimensions : " + Arrays.toString(tc.arr));
            System.out.println("Expected   : " + tc.expected);
            
            // Prevent recursive explosion on large chains during testing
            if (tc.arr.length <= 15) {
                System.out.println("Recursive (Brute) : " + solver.mcmRecursive(tc.arr));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Interval DP is O(2^N) without memo)");
            }
            
            System.out.println("Memoization       : " + solver.mcmMemo(tc.arr));
            System.out.println("Tabulation 2D     : " + solver.mcmTabulation(tc.arr));
            System.out.println();
        }
    }
}
