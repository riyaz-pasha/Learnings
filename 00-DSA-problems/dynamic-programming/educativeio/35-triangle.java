import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Triangle
 * Given a triangle array, return the minimum path sum from top to bottom.
 * At each step from index 'i' in the current row, you may move to index 'i' 
 * or index 'i + 1' in the next row.
 * 
 * Constraints:
 * 1 <= triangle.length <= 200
 * triangle[0].length == 1
 * triangle[i].length == triangle[i - 1].length + 1
 * -10^4 <= triangle[i][j] <= 10^4
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, point out the structural difference between this and 
 * a standard square grid:
 * 
 * Q: "Are the numbers only positive?"
 * A: No, constraints say they can be negative. But since we just want the 
 *    minimum path sum, this doesn't break DP logic (unlike graphs with cycles).
 * 
 * CRITICAL SENIOR INSIGHT - THE DIRECTION OF TRAVERSAL:
 * "If we try to solve this 'Top-Down' (iteratively moving from row 0 down to N), 
 * we will have to constantly deal with annoying boundary conditions on the left 
 * and right edges of the triangle.
 * 
 * However, if we evaluate it 'Bottom-Up' (starting at the base of the triangle 
 * and building our way up to the tip), EVERY single node above the base perfectly 
 * sits on top of exactly two children. Boundary checks vanish entirely. 
 * This is the hallmark of a senior-level elegant solution."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "If I am standing on a number at row 'i' and column 'j', to reach the bottom, 
 * my very next step MUST be to either:
 *  1. The node directly below me: (i + 1, j)
 *  2. The node below and to the right: (i + 1, j + 1)
 * 
 * I want to pick the path that costs the least. So the minimum path sum from 
 * my current node to the bottom is simply:
 * My Value + Min(Best path from below-left, Best path from below-right)
 * 
 * Because multiple paths from the top will eventually funnel through the same 
 * nodes near the bottom, we have overlapping subproblems -> Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example:
 *    [2]
 *   [3,4]
 *  [6,5,7]
 * [4,1,8,3]
 * 
 * Let's trace Bottom-Up (Tabulation):
 * 
 * Row 3 (Base): [4, 1, 8, 3]
 * 
 * Row 2:
 * - '6' sits on '4' and '1'. Min is 1. Cost = 6 + 1 = 7.
 * - '5' sits on '1' and '8'. Min is 1. Cost = 5 + 1 = 6.
 * - '7' sits on '8' and '3'. Min is 3. Cost = 7 + 3 = 10.
 * Row 2 becomes: [7, 6, 10]
 * 
 * Row 1:
 * - '3' sits on '7' and '6'. Min is 6. Cost = 3 + 6 = 9.
 * - '4' sits on '6' and '10'. Min is 6. Cost = 4 + 6 = 10.
 * Row 1 becomes: [9, 10]
 * 
 * Row 0:
 * - '2' sits on '9' and '10'. Min is 9. Cost = 2 + 9 = 11.
 * 
 * Final minimum path sum is 11. (Path: 2 -> 3 -> 5 -> 1).
 */
public class TriangleMinimumPath {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Start at the top (0,0) and branch into the two valid lower nodes.
     * 
     * Time Complexity: O(2^N) - We branch 2 ways at every level (N levels).
     * Space Complexity: O(N) - Maximum depth of the recursion tree.
     */
    public int minimumTotalRecursive(List<List<Integer>> triangle) {
        if (triangle == null || triangle.isEmpty()) return 0;
        return solveRecursive(triangle, 0, 0);
    }

    private int solveRecursive(List<List<Integer>> triangle, int row, int col) {
        // BASE CASE REASONING:
        // If our row index matches the size of the triangle, we have fallen off 
        // the bottom edge. There is no more path to traverse, so it costs 0.
        if (row == triangle.size()) {
            return 0;
        }

        // Universe 1: We step to the node directly below us (same column index)
        int stepDown = solveRecursive(triangle, row + 1, col);

        // Universe 2: We step to the node below and to the right (column + 1)
        int stepDownRight = solveRecursive(triangle, row + 1, col + 1);

        // The minimum path from here is our own value + the cheaper of the two paths.
        return triangle.get(row).get(col) + Math.min(stepDown, stepDownRight);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the minimum path sum from each (row, col) to the bottom.
     * 
     * Time Complexity: O(N^2) - We evaluate each of the N*(N+1)/2 nodes exactly once.
     * Space Complexity: O(N^2) - For the 2D memo array + call stack.
     */
    public int minimumTotalMemo(List<List<Integer>> triangle) {
        if (triangle == null || triangle.isEmpty()) return 0;
        
        int n = triangle.size();
        // Since the triangle has varying row lengths, we allocate a square 
        // array of N x N. The unused upper-right triangle is wasted space, 
        // but perfectly fine for an N=200 constraint.
        Integer[][] memo = new Integer[n][n];
        
        return solveMemo(triangle, 0, 0, memo);
    }

    private int solveMemo(List<List<Integer>> triangle, int row, int col, Integer[][] memo) {
        // BASE CASE (Same physical logic as brute force)
        if (row == triangle.size()) return 0;

        if (memo[row][col] != null) {
            return memo[row][col];
        }

        int stepDown = solveMemo(triangle, row + 1, col, memo);
        int stepDownRight = solveMemo(triangle, row + 1, col + 1, memo);

        memo[row][col] = triangle.get(row).get(col) + Math.min(stepDown, stepDownRight);
        return memo[row][col];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build the solution from the base of the triangle up to the tip.
     * dp[i][j] signifies: "The absolute minimum path sum starting from this 
     * specific cell (i, j) going all the way down to the bottom of the triangle."
     * 
     * Time Complexity: O(N^2)
     * Space Complexity: O(N^2)
     */
    public int minimumTotalTabulation(List<List<Integer>> triangle) {
        int n = triangle.size();
        int[][] dp = new int[n][n];

        // BASE CASE REASONING (Seeding the bottom row):
        // If you are standing on the very bottom row, the "minimum path to the bottom" 
        // is literally just the value of the node you are standing on. You are already there.
        // We initialize the entire bottom row of our DP table with these base values.
        for (int j = 0; j < n; j++) {
            dp[n - 1][j] = triangle.get(n - 1).get(j);
        }

        // Outer loop: We walk BACKWARDS (upwards) from the second-to-last row 
        // all the way up to the top tip of the triangle (row 0).
        for (int i = n - 2; i >= 0; i--) {
            
            // Inner loop: We iterate through every element in the current row 'i'.
            // The number of elements in row 'i' is exactly 'i + 1'.
            for (int j = 0; j <= i; j++) {
                
                // --- DETAILED TABULATION EXPLANATION ---
                
                // 1. Look directly below us.
                // Because we are building upwards, the row below us (i+1) is ALREADY SOLVED!
                // We just ask: "What was the cheapest path to the bottom from the left child?"
                int cheapestPathBelowLeft = dp[i + 1][j];
                
                // 2. Look below and to the right.
                // We ask: "What was the cheapest path to the bottom from the right child?"
                int cheapestPathBelowRight = dp[i + 1][j + 1];
                
                // 3. We are standing on node (i, j). To reach the bottom optimally, 
                // we greedily pick the cheaper of the two paths below us, and then 
                // add the cost of our current node.
                dp[i][j] = triangle.get(i).get(j) + Math.min(cheapestPathBelowLeft, cheapestPathBelowRight);
            }
        }

        // The answer sits at the very top of the triangle.
        return dp[0][0];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: Look closely at the Tabulation loop above. To calculate row `i`, 
     * we ONLY need the values from row `i+1` (the row immediately below it).
     * Any rows below that are completely dead memory.
     * 
     * We can collapse the 2D matrix into a single 1D array of size N (the 
     * width of the base of the triangle). As we move up the triangle, we just 
     * continuously overwrite this single array.
     * 
     * Time Complexity: O(N^2)
     * Space Complexity: O(N) - Massively reduced memory footprint.
     */
    public int minimumTotalSpaceOptimized(List<List<Integer>> triangle) {
        int n = triangle.size();
        
        // This 1D array represents the minimum path sums of the "row below us".
        int[] dp = new int[n];
        
        // BASE CASE REASONING:
        // We seed our single array with the values from the very bottom row of the triangle.
        for (int j = 0; j < n; j++) {
            dp[j] = triangle.get(n - 1).get(j);
        }

        // We walk upwards from the second-to-last row up to the tip.
        for (int i = n - 2; i >= 0; i--) {
            
            // For the current row, we iterate through its columns.
            for (int j = 0; j <= i; j++) {
                
                // MAGIC OF THE 1D ARRAY:
                // We are overwriting our array in-place!
                
                // `dp[j]` on the right side of the equals sign is the old value from 
                // the row below (the left child).
                int leftChildCost = dp[j];
                
                // `dp[j+1]` is the old value from the row below (the right child).
                int rightChildCost = dp[j + 1];
                
                // We calculate our new optimal cost, and overwrite `dp[j]`. 
                // This primes `dp[j]` to act as the "left child" for the row ABOVE us 
                // in the next iteration of the outer loop.
                dp[j] = triangle.get(i).get(j) + Math.min(leftChildCost, rightChildCost);
            }
        }

        // The tip of the triangle collapses into the very first slot of the array.
        return dp[0];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new TriangleMinimumPath();
        
        record TestCase(List<List<Integer>> triangle, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(Arrays.asList(
                Arrays.asList(2),
                Arrays.asList(3, 4),
                Arrays.asList(6, 5, 7),
                Arrays.asList(4, 1, 8, 3)
            ), 11), // 2 -> 3 -> 5 -> 1 = 11
            
            new TestCase(Arrays.asList(
                Arrays.asList(-10)
            ), -10),
            
            new TestCase(Arrays.asList(
                Arrays.asList(-1),
                Arrays.asList(2, 3),
                Arrays.asList(1, -1, -3)
            ), -1) // -1 -> 3 -> -3 = -1
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.minimumTotalRecursive(tc.triangle));
            System.out.println("Memoization       : " + solver.minimumTotalMemo(tc.triangle));
            System.out.println("Tabulation 2D     : " + solver.minimumTotalTabulation(tc.triangle));
            System.out.println("Space Optimized   : " + solver.minimumTotalSpaceOptimized(tc.triangle));
            System.out.println();
        }
    }
}
