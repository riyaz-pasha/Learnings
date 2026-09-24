import java.util.Arrays;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Counting Bits
 * Given a positive integer n, return an array of length n + 1 where result[x] 
 * is the count of 1s in the binary representation of x (0 <= x <= n).
 * 
 * Constraints:
 * 0 <= n <= 10^4
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, you want to acknowledge the trivial solution first 
 * before diving into the optimal Dynamic Programming (DP) bit-manipulation approach.
 * 
 * Q: "Can I just use the built-in language method like Integer.bitCount()?"
 * A: An interviewer will usually say yes for a brute-force baseline, but will 
 *    immediately ask you to solve it in O(n) time without built-in functions. 
 *    (A loop calling a bit-counting function on every number is O(n log n) 
 *    because counting bits takes time proportional to the number of bits).
 * 
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We need to find a relationship between the bit count of a current number 'i' 
 * and the bit count of a smaller number we have ALREADY evaluated. 
 * 
 * There are two highly elegant ways to map this to DP:
 * 
 * STRATEGY A (Right Shift): 
 * If we take any binary number and shift it right by 1 (divide by 2), we drop 
 * its Least Significant Bit (LSB). 
 * So, the number of 1s in 'i' is exactly equal to the number of 1s in (i >> 1), 
 * PLUS whatever bit we just dropped (which is 1 if 'i' is odd, 0 if 'i' is even).
 * Equation: dp[i] = dp[i >> 1] + (i & 1)
 * 
 * STRATEGY B (Brian Kernighan's Algorithm):
 * The operation `i & (i - 1)` drops the lowest set '1' bit from a number.
 * By definition, `i & (i - 1)` results in a smaller number that we have already 
 * computed. So, the count for 'i' is simply 1 plus the count for that smaller number.
 * Equation: dp[i] = dp[i & (i - 1)] + 1
 * 
 * Both strategies give us a beautiful O(n) time and O(n) space solution."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Let's trace STRATEGY A (Right Shift) for n = 5:
 * dp array initialized to 0s.
 * 
 * i = 0: 000 -> 0 bits (Base case)
 * i = 1: 001 -> dp[1 >> 1] + (1 & 1) -> dp[0] + 1 = 0 + 1 = 1.
 * i = 2: 010 -> dp[2 >> 1] + (2 & 1) -> dp[1] + 0 = 1 + 0 = 1.
 * i = 3: 011 -> dp[3 >> 1] + (3 & 1) -> dp[1] + 1 = 1 + 1 = 2.
 * i = 4: 100 -> dp[4 >> 1] + (4 & 1) -> dp[2] + 0 = 1 + 0 = 1.
 * i = 5: 101 -> dp[5 >> 1] + (5 & 1) -> dp[2] + 1 = 1 + 1 = 2.
 * 
 * Result: [0, 1, 1, 2, 1, 2]
 */
public class CountingBits {

    /**
     * ========================================================================
     * APPROACH 1: Brute Force
     * ========================================================================
     * Idea: Loop through every number from 0 to n. For each number, count 
     * its bits individually using a while loop.
     * 
     * Time Complexity: O(n log n) - For each of the n numbers, we iterate through its bits.
     * Space Complexity: O(1) auxiliary - Only the required return array.
     */
    public int[] countBitsBruteForce(int n) {
        int[] result = new int[n + 1];
        
        for (int i = 0; i <= n; i++) {
            int count = 0;
            int num = i;
            while (num > 0) {
                count += (num & 1); // Add 1 if the lowest bit is set
                num >>= 1;          // Shift right to check the next bit
            }
            result[i] = count;
        }
        
        return result;
    }

    /**
     * ========================================================================
     * APPROACH 2: Dynamic Programming (Right Shift / Division by 2)
     * ========================================================================
     * Idea: Build the array iteratively. For any number i, its bit count is 
     * the bit count of (i / 2) plus 1 if 'i' is odd.
     * 
     * Time Complexity: O(n) - Single pass, constant time bitwise operations.
     * Space Complexity: O(1) auxiliary - Only the required return array.
     */
    public int[] countBitsDPShift(int n) {
        int[] dp = new int[n + 1];
        
        // BASE CASE REASONING:
        // The number 0 in binary is '0'. It has zero 1s. 
        // This anchors our entire dynamic programming lookup sequence.
        // Java initializes arrays to 0, so dp[0] = 0 is handled automatically,
        // but it is conceptually the foundation of the loop below.
        
        for (int i = 1; i <= n; i++) {
            // dp[i >> 1] fetches the bit count of the number shifted right.
            // (i & 1) extracts the Least Significant Bit that was dropped by the shift.
            dp[i] = dp[i >> 1] + (i & 1);
        }
        
        return dp;
    }

    /**
     * ========================================================================
     * APPROACH 3: Dynamic Programming (Brian Kernighan's / L4-L5 Target)
     * ========================================================================
     * Idea: `i & (i - 1)` mathematically drops the rightmost '1' bit of any integer.
     * Therefore, the number of bits in 'i' is exactly 1 MORE than the number 
     * of bits in `i & (i - 1)`. 
     * Since `i & (i - 1)` is always strictly less than 'i', we are guaranteed 
     * to have already calculated its value in our DP array.
     * 
     * Time Complexity: O(n) - Single pass, constant time bitwise operations.
     * Space Complexity: O(1) auxiliary - Only the required return array.
     */
    public int[] countBitsDPBrianKernighan(int n) {
        int[] dp = new int[n + 1];
        
        // BASE CASE REASONING:
        // Same as above. The number 0 has exactly 0 bits set.
        dp[0] = 0;
        
        for (int i = 1; i <= n; i++) {
            // We strip the lowest set bit from 'i'.
            // The resulting number is smaller, so we just look up its bit count 
            // and add 1 for the bit we just stripped off.
            dp[i] = dp[i & (i - 1)] + 1;
        }
        
        return dp;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new CountingBits();
        
        int[] testCases = {2, 5, 10, 0};
        
        for (int i = 0; i < testCases.length; i++) {
            int n = testCases[i];
            System.out.println("---- Test Case " + (i + 1) + ": n = " + n + " ----");
            
            System.out.println("Brute Force          : " + Arrays.toString(solver.countBitsBruteForce(n)));
            System.out.println("DP (Right Shift)     : " + Arrays.toString(solver.countBitsDPShift(n)));
            System.out.println("DP (Brian Kernighan) : " + Arrays.toString(solver.countBitsDPBrianKernighan(n)));
            System.out.println();
        }
    }
}
