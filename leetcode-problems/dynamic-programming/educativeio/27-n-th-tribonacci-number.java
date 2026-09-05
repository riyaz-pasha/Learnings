import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: N-th Tribonacci Number
 * The Tribonacci sequence Tn is defined as:
 * T0 = 0, T1 = 1, T2 = 1, and Tn+3 = Tn + Tn+1 + Tn+2 for n >= 0.
 * Given a number n, calculate the corresponding Tribonacci number.
 * 
 * Constraints:
 * 0 <= n <= 37
 * The answer is guaranteed to fit within a 32-bit integer.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, this is considered a "warm-up" Dynamic Programming 
 * or Recursion problem. Before jumping in, confirm constraints:
 * 
 * Q: "Can n be zero?"
 * A: Yes, the constraint specifies 0 <= n <= 37. We must handle n = 0 safely.
 * 
 * Q: "Will I need to worry about Integer overflow?"
 * A: No, the prompt guarantees the answer for n = 37 fits within a standard 
 *    32-bit signed integer (up to ~2.1 billion). We don't need 'long'.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "The problem strictly defines a recurrence relation for us. To find the value 
 * at any step 'n', I just need to sum the values of the EXACT three preceding 
 * steps: (n-1), (n-2), and (n-3).
 * 
 * Because calculating (n-1) will require calculating (n-2) and (n-3) all over 
 * again, a pure recursive tree will have massive overlapping subproblems. 
 * This perfectly dictates a Dynamic Programming approach where we cache or 
 * iteratively build up our previous results."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Let's trace the sequence iteratively for n = 5:
 * T0 = 0
 * T1 = 1
 * T2 = 1
 * T3 = T2 + T1 + T0 = 1 + 1 + 0 = 2
 * T4 = T3 + T2 + T1 = 2 + 1 + 1 = 4
 * T5 = T4 + T3 + T2 = 4 + 2 + 1 = 7
 * 
 * Result for n=5 is 7.
 */
public class TribonacciNumber {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Translate the mathematical formula directly into a recursive function.
     * 
     * Time Complexity: O(3^n) - The recursion tree branches 3 ways at every step.
     * Space Complexity: O(n) - Maximum depth of the call stack is n.
     * 
     * NOTE: This will likely Time Limit Exceed (TLE) for n = 37 on coding platforms,
     * but it establishes the mathematical baseline.
     */
    public int tribonacciRecursive(int n) {
        // BASE CASE REASONING:
        // The problem mathematically defines the absolute start of the sequence.
        // Before step 3, the sequence is hardcoded to 0, 1, and 1.
        // If n is 0, we simply return the hardcoded T0 value (0).
        if (n == 0) return 0;
        
        // If n is 1 or 2, we return their hardcoded values (1).
        if (n == 1 || n == 2) return 1;
        
        // RECURRENCE RELATION:
        // We branch out into 3 parallel universes to fetch the previous 3 answers.
        return tribonacciRecursive(n - 1) + 
               tribonacciRecursive(n - 2) + 
               tribonacciRecursive(n - 3);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the results of our recursive calls in an array so we never 
     * compute the same 'n' twice.
     * 
     * Time Complexity: O(n) - We evaluate each state exactly once.
     * Space Complexity: O(n) - For the memoization array + recursion stack.
     */
    public int tribonacciMemo(int n) {
        if (n == 0) return 0;
        if (n == 1 || n == 2) return 1;
        
        int[] memo = new int[n + 1];
        Arrays.fill(memo, -1);
        
        return solveMemo(n, memo);
    }

    private int solveMemo(int n, int[] memo) {
        // BASE CASES (Same mathematical constraints as above)
        if (n == 0) return 0;
        if (n == 1 || n == 2) return 1;
        
        // Pruning: Return cached result if we've already computed it
        if (memo[n] != -1) {
            return memo[n];
        }
        
        // Calculate and cache before returning
        memo[n] = solveMemo(n - 1, memo) + solveMemo(n - 2, memo) + solveMemo(n - 3, memo);
        return memo[n];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Eliminate recursion entirely. Build the sequence iteratively from 
     * the ground up in a 1D array.
     * 
     * Time Complexity: O(n) - Single loop up to n.
     * Space Complexity: O(n) - For the DP array.
     */
    public int tribonacciTabulation(int n) {
        // Handle edge cases immediately so we don't go out of bounds 
        // when initializing our DP array.
        if (n == 0) return 0;
        if (n == 1 || n == 2) return 1;
        
        // dp[i] represents the Tribonacci number exactly at sequence index 'i'
        int[] dp = new int[n + 1];
        
        // BASE CASE REASONING:
        // We literally seed the DP array with the hardcoded start values 
        // provided by the problem prompt. These are the anchors for the rest 
        // of our iterative math.
        dp[0] = 0;
        dp[1] = 1;
        dp[2] = 1;
        
        // We start iterating at index 3, because indices 0, 1, and 2 are 
        // already solved by our base cases.
        for (int i = 3; i <= n; i++) {
            
            // --- DETAILED TABULATION EXPLANATION ---
            // To find the value for our current step 'i', we must look at our history.
            // Since this is a 1D array, we are simply looking backwards in a straight line.
            
            // Look directly 1 step behind us.
            int oneStepBack = dp[i - 1];
            
            // Look 2 steps behind us.
            int twoStepsBack = dp[i - 2];
            
            // Look 3 steps behind us.
            int threeStepsBack = dp[i - 3];
            
            // By the laws of the Tribonacci sequence, the current value is strictly 
            // the sum of the last three values. We add them together and store 
            // the result in our current slot `dp[i]`.
            dp[i] = oneStepBack + twoStepsBack + threeStepsBack;
        }
        
        // After the loop finishes, the final answer sits at the very end of the array.
        return dp[n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: Look closely at the Tabulation loop above. To calculate `dp[i]`, 
     * we ONLY need the values from `dp[i-1]`, `dp[i-2]`, and `dp[i-3]`. 
     * We don't need `dp[i-4]` or anything before it. The array history is dead memory.
     * We can reduce our space to O(1) by keeping just a sliding window of 3 variables.
     * 
     * Time Complexity: O(n) - Single loop up to n.
     * Space Complexity: O(1) - Constant auxiliary space.
     */
    public int tribonacciSpaceOptimized(int n) {
        if (n == 0) return 0;
        if (n == 1 || n == 2) return 1;
        
        // BASE CASE REASONING:
        // These variables represent our sliding window. 
        // Before we take our first mathematical step at i=3, we initialize our 
        // window to perfectly mirror the hardcoded base cases: T0, T1, and T2.
        var t0 = 0; // 3 steps back
        var t1 = 1; // 2 steps back
        var t2 = 1; // 1 step back
        
        var current = 0;
        
        for (int i = 3; i <= n; i++) {
            // Calculate the current step by summing our 3-variable window
            current = t0 + t1 + t2;
            
            // Shift the sliding window forward for the next iteration!
            // The old 't1' becomes the new 't0' (it falls further back in time).
            t0 = t1;
            
            // The old 't2' becomes the new 't1'.
            t1 = t2;
            
            // The newly calculated 'current' becomes the new 't2' (the most recent past).
            t2 = current;
        }
        
        return current;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new TribonacciNumber();
        
        // Helper record for clean and maintainable test cases
        record TestCase(int n, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(4, 4),    // T3 = 1+1+0=2, T4 = 2+1+1=4
            new TestCase(5, 7),    // T5 = 4+2+1=7
            new TestCase(25, 1389537),
            new TestCase(37, 2082876103), // Maximum constraint, checks for integer overflow issues
            new TestCase(0, 0),    // Base case edge check
            new TestCase(1, 1)     // Base case edge check
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + ": n = " + tc.n + " ----");
            System.out.println("Expected: " + tc.expected);
            
            // Throttle brute force recursion to avoid locking up during test execution
            if (tc.n <= 25) {
                System.out.println("Recursive (Brute) : " + solver.tribonacciRecursive(tc.n));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Too slow for n > 25)");
            }
            
            System.out.println("Memoization       : " + solver.tribonacciMemo(tc.n));
            System.out.println("Tabulation        : " + solver.tribonacciTabulation(tc.n));
            System.out.println("Space Optimized   : " + solver.tribonacciSpaceOptimized(tc.n));
            System.out.println();
        }
    }
}
