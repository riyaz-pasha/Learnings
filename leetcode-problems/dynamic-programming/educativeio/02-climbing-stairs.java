import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Climbing Stairs
 * You are climbing a staircase. It takes n steps to reach the top.
 * Each time, you can either climb 1 or 2 steps.
 * In how many distinct ways can you climb to the top?
 * Constraints: 1 <= n <= 45
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In a senior-level interview (L4/L5), before writing any code, you should ask:
 * 
 * Q: "Can n be 0 or negative?"
 * A: Constraint says 1 <= n <= 45. So, strictly positive.
 * 
 * Q: "Will the answer fit in a standard 32-bit signed integer?"
 * A: Yes. The maximum value is for n = 45. The 45th Fibonacci number is 
 *    1,134,903,170, which is comfortably less than Integer.MAX_VALUE (2^31 - 1).
 *    (Calling this out shows extreme attention to system limits/overflows).
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "To reach step n, my very last move could have only been one of two things:
 *  1. I took a 1-step from step (n-1).
 *  2. I took a 2-step from step (n-2).
 * 
 * Therefore, the total distinct ways to reach step n is simply the sum of 
 * the ways to reach (n-1) and the ways to reach (n-2). 
 * This is exactly the Fibonacci sequence recurrence relation."
 *
 * ----------------------------------------------------------------------------
 * 3. KEY OBSERVATIONS & INTUITION
 * ----------------------------------------------------------------------------
 * Base Cases:
 * - n = 1: [1] -> 1 way
 * - n = 2: [1,1], [2] -> 2 ways
 * - n = 3: [1,1,1], [1,2], [2,1] -> 3 ways (which is ways(2) + ways(1))
 * 
 * Recurrence Relation:
 * f(n) = f(n-1) + f(n-2)
 *
 * ----------------------------------------------------------------------------
 * 4. VISUALIZATION & TRACING (For n = 4)
 * ----------------------------------------------------------------------------
 * Tree representation of recursive calls for n = 4:
 * 
 *                         (4)
 *                       /     \
 *                  [-1]         [-2]
 *                  /               \
 *                (3)               (2)
 *               /   \             /   \
 *             (2)   (1)         (1)   (0) -> (0 means exact fit, valid path)
 *            /   \
 *          (1)   (0)
 * 
 * Notice how (2) is computed multiple times? This overlapping subproblem 
 * property tells us we MUST use Dynamic Programming (memoization/tabulation) 
 * to avoid redundant O(2^n) work.
 */
public class ClimbingStairs {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Translate the recurrence relation directly into code.
     * 
     * Time Complexity: O(2^n) - Size of the recursion tree grows exponentially.
     * Space Complexity: O(n) - Maximum depth of the call stack is n.
     * 
     * NOTE: This will throw a Time Limit Exceeded (TLE) error for n = 45 on 
     * most coding platforms, but it is the crucial first step in DP.
     */
    public int climbStairsRecursive(int n) {
        // Base cases
        if (n == 1) return 1;
        if (n == 2) return 2;
        
        // Recurrence relation
        return climbStairsRecursive(n - 1) + climbStairsRecursive(n - 2);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the results of expensive function calls and return the 
     * cached result when the same inputs occur again.
     * 
     * Time Complexity: O(n) - We solve each subproblem exactly once.
     * Space Complexity: O(n) - For the recursion stack + the memoization array.
     */
    public int climbStairsMemo(int n) {
        // Using an array for memoization is faster than a HashMap for primitives.
        // We use n + 1 so we can 1-index our steps for better readability.
        int[] memo = new int[n + 1];
        
        // Initialize array with -1 to denote uncalculated states
        Arrays.fill(memo, -1);
        
        return solveMemo(n, memo);
    }

    private int solveMemo(int n, int[] memo) {
        if (n == 1) return 1;
        if (n == 2) return 2;
        
        // If already calculated, return from cache (pruning the tree)
        if (memo[n] != -1) {
            return memo[n];
        }
        
        // Otherwise, calculate and store in cache before returning
        memo[n] = solveMemo(n - 1, memo) + solveMemo(n - 2, memo);
        return memo[n];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Eliminate recursion entirely. Build the solution iteratively from 
     * the base cases up to n. This prevents StackOverflow issues.
     * 
     * Time Complexity: O(n) - Single loop up to n.
     * Space Complexity: O(n) - For the DP array.
     */
    public int climbStairsTabulation(int n) {
        if (n == 1) return 1;
        
        // dp[i] represents the total distinct ways to reach step i
        int[] dp = new int[n + 1];
        
        // Seed the base cases
        dp[1] = 1;
        dp[2] = 2;
        
        // Iterate and build up
        for (int i = 3; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }
        
        return dp[n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Expectation)
     * ========================================================================
     * Idea: Looking at `dp[i] = dp[i - 1] + dp[i - 2]`, we realize we only ever
     * need the LAST TWO values to compute the current value. We don't need to 
     * keep the entire array in memory.
     * 
     * Time Complexity: O(n) - Single loop up to n.
     * Space Complexity: O(1) - Constant space, just a few integer variables.
     */
    public int climbStairsSpaceOptimized(int n) {
        if (n == 1) return 1;
        if (n == 2) return 2;
        
        // Using 'var' (Java 10+) for cleaner local variable declarations.
        var twoStepsBehind = 1; // equivalent to dp[i-2]
        var oneStepBehind = 2;  // equivalent to dp[i-1]
        var currentWays = 0;    // equivalent to dp[i]
        
        for (int i = 3; i <= n; i++) {
            currentWays = oneStepBehind + twoStepsBehind;
            
            // Shift the window forward for the next iteration
            twoStepsBehind = oneStepBehind;
            oneStepBehind = currentWays;
        }
        
        return currentWays;
    }

    /**
     * ========================================================================
     * APPROACH 5: Matrix Exponentiation (Senior/Staff Level Flex)
     * ========================================================================
     * Idea: The Fibonacci sequence can be represented as a matrix multiplication.
     * [ F(n)   ] = [ 1 1 ] ^ (n-1) * [ F(1) ]
     * [ F(n-1) ]   [ 1 0 ]           [ F(0) ]
     * 
     * By using fast exponentiation (similar to calculating x^n in O(log n)),
     * we can find the nth Fibonacci number in logarithmic time.
     * 
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int climbStairsMatrix(int n) {
        if (n <= 2) return n;
        
        // The transformation matrix for Fibonacci
        int[][] q = {{1, 1}, {1, 0}};
        
        // We raise the matrix to the power of n
        int[][] res = matrixPower(q, n);
        
        // The answer will reside in res[0][0] based on the matrix math
        return res[0][0];
    }
    
    private int[][] matrixPower(int[][] a, int n) {
        int[][] ret = {{1, 0}, {0, 1}}; // Identity matrix
        while (n > 0) {
            // If n is odd, multiply the current result by matrix A
            if ((n & 1) == 1) {
                ret = multiply(ret, a);
            }
            n >>= 1; // Divide by 2
            a = multiply(a, a); // Square the matrix
        }
        return ret;
    }
    
    private int[][] multiply(int[][] a, int[][] b) {
        int[][] c = new int[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                c[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j];
            }
        }
        return c;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new ClimbingStairs();
        
        int[] testCases = {1, 2, 3, 5, 45};
        
        for (int n : testCases) {
            System.out.println("---- Test Case: n = " + n + " ----");
            System.out.println("Recursive (Brute) : " + (n <= 30 ? solver.climbStairsRecursive(n) : "Skipped (TLE)"));
            System.out.println("Memoization       : " + solver.climbStairsMemo(n));
            System.out.println("Tabulation        : " + solver.climbStairsTabulation(n));
            System.out.println("Space Optimized   : " + solver.climbStairsSpaceOptimized(n));
            System.out.println("Matrix Exp (LogN) : " + solver.climbStairsMatrix(n));
            System.out.println();
        }
    }
}
