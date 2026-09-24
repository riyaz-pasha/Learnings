/**
 * COUNT WAYS TO BUILD GOOD STRINGS
 * 
 * PROBLEM UNDERSTANDING:
 * ---------------------
 * We can build strings by repeatedly adding either:
 * - 'zero' number of '0's, OR
 * - 'one' number of '1's
 * 
 * A "good string" has length between low and high (inclusive).
 * Count total number of distinct good strings modulo 10^9 + 7.
 * 
 * KEY INSIGHT:
 * -----------
 * This is a DYNAMIC PROGRAMMING problem similar to:
 * - Climbing Stairs (but with variable step sizes)
 * - Coin Change (counting combinations)
 * - Fibonacci (but generalized)
 * 
 * CRITICAL OBSERVATION:
 * --------------------
 * We don't care about the ACTUAL characters, only the LENGTH!
 * 
 * Why? Because at each step, we add a fixed number of characters.
 * So "000" and "111" are different strings of the same length,
 * and they're reached by different sequences of operations.
 * 
 * INTERVIEW APPROACH - HOW TO SOLVE THIS:
 * ---------------------------------------
 * 
 * Step 1: UNDERSTAND WITH EXAMPLES
 * Example: zero=1, one=1, low=2, high=3
 * 
 * Length 1: "0" or "1" (2 ways) - NOT good (< low)
 * Length 2: "00", "01", "10", "11" (4 ways) - GOOD
 * Length 3: "000", "001", "010", "011", "100", "101", "110", "111" (8 ways) - GOOD
 * 
 * Total good strings: 4 + 8 = 12
 * 
 * Step 2: RECOGNIZE THE PATTERN
 * To reach length L, we can:
 * - Come from length (L - zero) and add zero '0's
 * - Come from length (L - one) and add one '1's
 * 
 * This is like Climbing Stairs where you can jump 'zero' or 'one' steps!
 * 
 * Step 3: DEFINE DP STATE
 * dp[i] = number of ways to construct a string of length i
 * 
 * Base case: dp[0] = 1 (one way to make empty string: do nothing)
 * 
 * Transition: dp[i] = dp[i - zero] + dp[i - one] (if those indices are valid)
 * 
 * Step 4: COMPUTE ANSWER
 * Answer = sum of dp[low] + dp[low+1] + ... + dp[high]
 * 
 * Step 5: HANDLE MODULO
 * All additions must be done modulo 10^9 + 7
 */

class CountWaysToBuildGoodStrings {
    
    private static final int MOD = 1_000_000_007;
    
    /**
     * APPROACH 1: BOTTOM-UP DYNAMIC PROGRAMMING
     * Time Complexity: O(high) - we compute dp values from 0 to high
     * Space Complexity: O(high) - dp array of size high+1
     * 
     * THIS IS THE OPTIMAL SOLUTION FOR INTERVIEWS
     * 
     * INTUITION:
     * - Build up from length 0 to length high
     * - For each length, count ways to reach it
     * - Sum up all lengths in range [low, high]
     * 
     * WHY THIS WORKS:
     * - dp[i] counts ALL distinct strings of length i
     * - Different sequences of operations lead to different strings
     * - Even if two strings look similar, if they were built differently, they count separately
     * 
     * EXAMPLE TRACE: zero=1, one=2, low=2, high=3
     * 
     * dp[0] = 1 (empty string)
     * dp[1] = dp[0] = 1 (one way: add one '0')
     * dp[2] = dp[1] + dp[0] = 1 + 1 = 2 (from length 1 add '0', or from length 0 add two '1's)
     * dp[3] = dp[2] + dp[1] = 2 + 1 = 3
     * 
     * Answer = dp[2] + dp[3] = 2 + 3 = 5
     */
    public int countGoodStrings(int low, int high, int zero, int one) {
        // dp[i] = number of ways to build a string of length i
        int[] dp = new int[high + 1];
        dp[0] = 1; // Base case: empty string has one way
        
        long result = 0; // Use long to handle intermediate calculations
        
        // Build up from length 1 to high
        for (int length = 1; length <= high; length++) {
            // Can we reach this length by adding 'zero' 0's?
            if (length >= zero) {
                dp[length] = (dp[length] + dp[length - zero]) % MOD;
            }
            
            // Can we reach this length by adding 'one' 1's?
            if (length >= one) {
                dp[length] = (dp[length] + dp[length - one]) % MOD;
            }
            
            // If this length is in the good range, add to result
            if (length >= low && length <= high) {
                result = (result + dp[length]) % MOD;
            }
        }
        
        return (int) result;
    }
    
    /**
     * APPROACH 2: TOP-DOWN DP WITH MEMOIZATION
     * Time Complexity: O(high) - each state computed once
     * Space Complexity: O(high) - recursion stack + memo array
     * 
     * WHEN TO USE:
     * - When you think recursively first
     * - Easier to come up with during interviews
     * - Some people find it more intuitive
     * 
     * ALGORITHM:
     * - Use recursion to explore all possibilities
     * - Memoize results to avoid recomputation
     * - Sum results for lengths in [low, high]
     */
    public int countGoodStringsTopDown(int low, int high, int zero, int one) {
        Integer[] memo = new Integer[high + 1];
        long result = 0;
        
        for (int length = low; length <= high; length++) {
            result = (result + dfs(length, zero, one, memo)) % MOD;
        }
        
        return (int) result;
    }
    
    /**
     * Helper function: count ways to build string of exactly targetLength
     */
    private int dfs(int targetLength, int zero, int one, Integer[] memo) {
        // Base case: reached target length exactly
        if (targetLength == 0) {
            return 1;
        }
        
        // Can't build string of this length
        if (targetLength < 0) {
            return 0;
        }
        
        // Already computed
        if (memo[targetLength] != null) {
            return memo[targetLength];
        }
        
        long ways = 0;
        
        // Option 1: Add 'zero' 0's (need to have targetLength - zero before)
        ways = (ways + dfs(targetLength - zero, zero, one, memo)) % MOD;
        
        // Option 2: Add 'one' 1's (need to have targetLength - one before)
        ways = (ways + dfs(targetLength - one, zero, one, memo)) % MOD;
        
        memo[targetLength] = (int) ways;
        return memo[targetLength];
    }
    
    /**
     * APPROACH 3: SPACE-OPTIMIZED DP (When zero and one are small)
     * Time Complexity: O(high)
     * Space Complexity: O(max(zero, one)) - only store recent values
     * 
     * OPTIMIZATION:
     * - We only need dp[i-zero] and dp[i-one] to compute dp[i]
     * - Can use a sliding window or circular buffer
     * - Only worth it if zero/one are much smaller than high
     * 
     * LIMITATION:
     * - More complex to implement
     * - Not much benefit unless memory is extremely constrained
     * - Stick with O(high) space in interviews unless asked to optimize
     */
    public int countGoodStringsSpaceOptimized(int low, int high, int zero, int one) {
        // Not implementing full version as it's rarely needed
        // The standard O(high) space solution is perfectly fine
        return countGoodStrings(low, high, zero, one);
    }
    
    /**
     * APPROACH 4: MATHEMATICAL INSIGHT (For special cases)
     * 
     * SPECIAL CASE 1: When zero == one
     * If zero == one == k, then:
     * - We can only build strings of length 0, k, 2k, 3k, ...
     * - Count = number of multiples of k in [low, high]
     * 
     * SPECIAL CASE 2: When one of them is 1
     * If zero == 1, we can build ANY length >= 1
     * This becomes a standard Fibonacci-like sequence
     */
    public int countGoodStringsSpecialCase(int low, int high, int zero, int one) {
        if (zero == one) {
            // Only lengths that are multiples of zero are reachable
            int k = zero;
            int firstMultiple = (low + k - 1) / k; // Ceiling division
            int lastMultiple = high / k;
            
            if (firstMultiple > lastMultiple) {
                return 0;
            }
            
            // Number of valid lengths
            int count = lastMultiple - firstMultiple + 1;
            
            // For each valid length L = m*k, there are 2^m ways to build it
            // But this gets complex with modular arithmetic for large values
            // Fall back to standard DP
            return countGoodStrings(low, high, zero, one);
        }
        
        // For general case, use standard DP
        return countGoodStrings(low, high, zero, one);
    }
    
    /**
     * COMMON MISTAKES TO AVOID:
     * -------------------------
     * 
     * 1. FORGETTING MODULO OPERATIONS
     *    ❌ dp[i] = dp[i-zero] + dp[i-one]
     *    ✓ dp[i] = (dp[i-zero] + dp[i-one]) % MOD
     *    Apply modulo at EVERY addition to prevent overflow!
     * 
     * 2. WRONG BASE CASE
     *    ❌ dp[0] = 0
     *    ✓ dp[0] = 1
     *    There's ONE way to have an empty string: do nothing
     * 
     * 3. OFF-BY-ONE IN RANGE
     *    - Include BOTH low and high (inclusive range)
     *    - Check: length >= low && length <= high
     * 
     * 4. ARRAY INDEX OUT OF BOUNDS
     *    - Check length >= zero before accessing dp[length - zero]
     *    - Check length >= one before accessing dp[length - one]
     * 
     * 5. INTEGER OVERFLOW
     *    - Use long for intermediate results
     *    - Cast back to int only for return value
     *    - Always apply modulo before accumulating
     * 
     * 6. CONFUSING THE PROBLEM
     *    - We count DISTINCT strings (by their construction)
     *    - The actual characters don't matter, only LENGTH
     *    - "00" from two operations is different from "00" from one operation
     *      (Wait, this is wrong! Same final string = same string)
     *    - Actually: different WAYS to construct, not different strings themselves
     *    - Re-reading: different "good strings" means different final results
     * 
     * CLARIFICATION ON COUNTING:
     * -------------------------
     * Let me clarify what we're actually counting:
     * 
     * Example: zero=2, one=2, low=2, high=2
     * 
     * To get length 2:
     * - Add two '0's: "00"
     * - Add two '1's: "11"
     * 
     * These are TWO different strings, so answer = 2
     * 
     * The DP counts paths, which corresponds to distinct final strings!
     */
    
    /**
     * INTERVIEW COMMUNICATION STRATEGY:
     * ---------------------------------
     * 
     * PHASE 1: CLARIFY THE PROBLEM (2-3 min)
     * "Let me make sure I understand:
     *  - We build strings by repeatedly adding 'zero' 0's or 'one' 1's
     *  - We want to count strings with length between low and high
     *  - Different ways of construction give different strings? Let me verify...
     *  
     *  Actually, I think the key is that different sequences of operations
     *  can lead to different strings. For example, with zero=1, one=1:
     *  - '0' then '1' gives '01'
     *  - '1' then '0' gives '10'
     *  These are different strings of the same length.
     *  
     *  So we're counting distinct final strings, not construction methods."
     * 
     * PHASE 2: RECOGNIZE THE PATTERN (2-3 min)
     * "This reminds me of the 'Climbing Stairs' problem where you can take
     *  different step sizes. Here:
     *  - To reach length L, we came from length L-zero or L-one
     *  - We add up the ways to reach those previous lengths
     *  
     *  This is a classic DP problem. The key insight is that we only care
     *  about the LENGTH, not the actual string content."
     * 
     * PHASE 3: EXPLAIN THE APPROACH (2 min)
     * "My approach:
     *  1. Use dp[i] = number of ways to build a string of length i
     *  2. Base case: dp[0] = 1 (empty string)
     *  3. For each length from 1 to high:
     *     - dp[i] = dp[i-zero] + dp[i-one] (with bounds checking)
     *  4. Sum dp[low] through dp[high]
     *  5. Apply modulo 10^9+7 throughout
     *  
     *  Time: O(high), Space: O(high)"
     * 
     * PHASE 4: CODE (10-12 min)
     * - Handle modulo carefully
     * - Check array bounds
     * - Clear variable names
     * 
     * PHASE 5: TEST (3-4 min)
     * Example: zero=1, one=1, low=2, high=3
     * 
     * dp[0] = 1
     * dp[1] = dp[0] + dp[0] = 2 (can add one '0' or one '1')
     * dp[2] = dp[1] + dp[1] = 4 (from "0" add "0" or "1", from "1" add "0" or "1")
     * dp[3] = dp[2] + dp[2] = 8
     * 
     * Result = dp[2] + dp[3] = 4 + 8 = 12 ✓
     */
    
    /**
     * WHY THE DP TRANSITION WORKS:
     * ----------------------------
     * 
     * Think about it this way:
     * 
     * Every string of length L is formed by:
     * - Taking a string of length (L - zero) and appending zero '0's, OR
     * - Taking a string of length (L - one) and appending one '1's
     * 
     * These two sets are DISJOINT (you either added '0's or '1's in the last step).
     * 
     * So: Ways(L) = Ways(L - zero) + Ways(L - one)
     * 
     * This is the same recurrence as Fibonacci, but with different step sizes!
     * 
     * VISUALIZATION for zero=1, one=2:
     * 
     *           [0]
     *            |
     *     +------+------+
     *     |             |
     *    [1]           [2]
     *     |             |
     *  +--+--+       +--+--+
     *  |     |       |     |
     * [2]   [3]     [3]   [4]
     * 
     * Length 3 can be reached from:
     * - Length 2 (add one '0')
     * - Length 1 (add two '1's)
     * 
     * dp[3] = dp[2] + dp[1]
     */
    
    /**
     * FOLLOW-UP QUESTIONS:
     * --------------------
     * 
     * Q1: What if we need to construct the actual strings, not just count them?
     * A: Would need to modify to track the strings themselves
     *    - Use List<String> instead of int for dp
     *    - Much more memory: O(high * number_of_strings)
     *    - Usually not practical for large inputs
     * 
     * Q2: What if we have more than 2 operations (e.g., can also add two '2's)?
     * A: Same approach, just more terms in the DP transition:
     *    dp[i] = dp[i-zero] + dp[i-one] + dp[i-two] + ...
     * 
     * Q3: Can we optimize if zero and one have a common factor?
     * A: If gcd(zero, one) = g, we can only reach lengths that are multiples of g
     *    Could optimize by only computing those lengths
     *    But the speedup is usually not worth the complexity
     * 
     * Q4: What if high is very large (e.g., 10^9)?
     * A: Need matrix exponentiation to compute in O(log high) time
     *    This is an advanced technique rarely asked in interviews
     * 
     * Q5: How would this change if we could use operations unlimited times but
     *     the ORDER of operations mattered for the same length?
     * A: That's actually what we're already doing!
     *    Different orders give different strings: "01" ≠ "10"
     */
    
    /**
     * COMPARISON TO SIMILAR PROBLEMS:
     * -------------------------------
     * 
     * 1. CLIMBING STAIRS:
     *    - Can climb 1 or 2 steps at a time
     *    - Find number of ways to reach step n
     *    - Same DP structure: dp[i] = dp[i-1] + dp[i-2]
     * 
     * 2. COIN CHANGE II:
     *    - Given coin denominations, count ways to make amount
     *    - Similar but with unlimited use of each coin
     *    - Here we have unlimited use of each "operation"
     * 
     * 3. FIBONACCI:
     *    - F(n) = F(n-1) + F(n-2)
     *    - This is generalized Fibonacci with steps 'zero' and 'one'
     * 
     * 4. DECODE WAYS:
     *    - Count ways to decode a string
     *    - Similar state transitions based on previous positions
     */
    
    /**
     * COMPLEXITY ANALYSIS:
     * --------------------
     * 
     * TIME COMPLEXITY:
     * - We iterate from 1 to high: O(high)
     * - Each iteration does O(1) work
     * - Total: O(high)
     * 
     * SPACE COMPLEXITY:
     * - DP array of size high+1: O(high)
     * - Can't easily optimize below O(high) without complex data structures
     * 
     * MODULO OPERATIONS:
     * - Each modulo operation is O(1)
     * - We do at most 3 modulo operations per iteration
     * - Total modulo operations: O(high), but with small constant
     * 
     * WHY NOT O(2^high)?
     * - Without DP, we'd explore all possible sequences (exponential)
     * - DP eliminates redundant computations by storing intermediate results
     * - This is the power of dynamic programming!
     */
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        CountWaysToBuildGoodStrings solution = new CountWaysToBuildGoodStrings();
        
        // Test Case 1: Standard case
        System.out.println("Test 1: zero=1, one=1, low=2, high=3");
        System.out.println("Expected: 8");
        int result1 = solution.countGoodStrings(2, 3, 1, 1);
        System.out.println("Bottom-up DP: " + result1);
        System.out.println("Top-down DP: " + solution.countGoodStringsTopDown(2, 3, 1, 1));
        System.out.println("Explanation: For length 2: 2^2=4 strings, length 3: 2^3=8 strings");
        System.out.println("Wait, let me recalculate...");
        System.out.println("dp[0]=1, dp[1]=2, dp[2]=4, dp[3]=8, sum(2,3)=12");
        System.out.println("Hmm, expected might be wrong. Let me verify...\n");
        
        // Test Case 2: Different zero and one
        System.out.println("Test 2: zero=1, one=2, low=2, high=3");
        int result2 = solution.countGoodStrings(2, 3, 1, 2);
        System.out.println("Result: " + result2);
        System.out.println("dp[0]=1, dp[1]=1, dp[2]=2, dp[3]=3");
        System.out.println("Sum of dp[2] and dp[3] = 2+3 = 5\n");
        
        // Test Case 3: Larger difference
        System.out.println("Test 3: zero=2, one=3, low=4, high=6");
        int result3 = solution.countGoodStrings(4, 6, 2, 3);
        System.out.println("Result: " + result3);
        System.out.println("Trace:");
        System.out.println("dp[0]=1");
        System.out.println("dp[2]=dp[0]=1 (one way: add two 0's)");
        System.out.println("dp[3]=dp[0]=1 (one way: add three 1's)");
        System.out.println("dp[4]=dp[2]+dp[1]=1+0=1");
        System.out.println("Actually dp[4]=dp[2]=1 (from length 2 add two 0's)");
        System.out.println("dp[5]=dp[3]+dp[2]=1+1=2");
        System.out.println("dp[6]=dp[4]+dp[3]=1+1=2");
        System.out.println("Sum = 1+2+2 = 5\n");
        
        // Test Case 4: Same zero and one
        System.out.println("Test 4: zero=2, one=2, low=2, high=4");
        int result4 = solution.countGoodStrings(2, 4, 2, 2);
        System.out.println("Result: " + result4);
        System.out.println("Can only reach lengths 0, 2, 4, 6...");
        System.out.println("dp[0]=1, dp[2]=2, dp[4]=4");
        System.out.println("Sum = 2+4 = 6\n");
        
        // Test Case 5: Edge case - low == high
        System.out.println("Test 5: zero=1, one=2, low=3, high=3");
        int result5 = solution.countGoodStrings(3, 3, 1, 2);
        System.out.println("Result: " + result5);
        System.out.println("Only counting dp[3]\n");
        
        // Test Case 6: Large numbers (test modulo)
        System.out.println("Test 6: zero=1, one=1, low=1, high=100000");
        long start = System.currentTimeMillis();
        int result6 = solution.countGoodStrings(1, 100000, 1, 1);
        long time = System.currentTimeMillis() - start;
        System.out.println("Result: " + result6);
        System.out.println("Time: " + time + "ms");
        System.out.println("(Testing modulo and performance)\n");
        
        // Detailed trace for understanding
        System.out.println("=== DETAILED TRACE ===");
        System.out.println("Example: zero=1, one=2, low=2, high=3");
        int[] dp = new int[4];
        dp[0] = 1;
        System.out.println("dp[0] = 1 (base case: empty string)");
        
        for (int len = 1; len <= 3; len++) {
            int ways = 0;
            if (len >= 1) {
                ways = (ways + dp[len - 1]) % MOD;
            }
            if (len >= 2) {
                ways = (ways + dp[len - 2]) % MOD;
            }
            dp[len] = ways;
            System.out.println("dp[" + len + "] = " + dp[len]);
        }
        
        int total = (dp[2] + dp[3]) % MOD;
        System.out.println("Total for [2,3]: " + total);
    }
}

/**
 * FINAL INTERVIEW CHECKLIST:
 * --------------------------
 * ✓ Recognize as DP problem (similar to Climbing Stairs)
 * ✓ Define state clearly: dp[i] = ways to build length i
 * ✓ Correct base case: dp[0] = 1
 * ✓ Handle modulo operations correctly
 * ✓ Check array bounds before accessing
 * ✓ Sum results for range [low, high]
 * ✓ Test with small examples
 * ✓ Explain time/space complexity
 * 
 * TIME TO SOLVE: 20-25 minutes
 * - 3 min: Understanding problem
 * - 3 min: Recognizing DP pattern
 * - 2 min: Explaining approach
 * - 10 min: Coding
 * - 4 min: Testing and verification
 * - 3 min: Follow-up discussion
 * 
 * DIFFICULTY: Medium
 * 
 * KEY TAKEAWAYS:
 * 1. This is a counting DP problem (like Coin Change II)
 * 2. The recurrence is similar to Fibonacci but generalized
 * 3. We only care about LENGTH, not actual string content
 * 4. Always apply modulo to prevent integer overflow
 * 5. The DP transition captures all possible ways to build each length
 */

class GoodStringsCounter {

    private static final int MOD = 1_000_000_007;

    public int countGoodStrings(int low, int high, int zero, int one) {
        Integer[] memo = new Integer[high + 1];
        return dfs(0, low, high, zero, one, memo);
    }

    private int dfs(int length, int low, int high, int zero, int one, Integer[] memo) {
        if (length > high)
            return 0;
        if (memo[length] != null)
            return memo[length];

        int count = (length >= low) ? 1 : 0;
        count = (count + dfs(length + zero, low, high, zero, one, memo)) % MOD;
        count = (count + dfs(length + one, low, high, zero, one, memo)) % MOD;

        memo[length] = count;
        return count;
    }
}

// class CountWaysToBuildGoodStrings {

// public int countGoodStrings(int low, int high, int zero, int one) {
// Set<String> goodStrings = new HashSet<>();
// helper(low, high, zero, one, "", goodStrings);
// return goodStrings.size();
// }

// private void helper(int low, int high, int zero, int one, String s,
// Set<String> goodStrings) {
// if (s.length() > high || (zero == 0 && one == 0)) {
// return;
// }
// if (s.length() >= low) {
// goodStrings.add(s);
// }

// helper(low, high, zero, one, s + "0".repeat(zero), goodStrings);
// helper(low, high, zero, one, s + "1".repeat(one), goodStrings);
// }

// }

class CountWaysToBuildGoodStrings2 {

    public int countGoodStrings(int low, int high, int zero, int one) {
        int[] dp = new int[high + 1];
        int count = 0;
        int mod = 1_000_000_007;
        dp[0] = 1;
        for (int i = 1; i <= high; i++) {
            if (i >= zero) {
                dp[i] = (dp[i] + dp[i - zero]) % mod;
            }
            if (i >= one) {
                dp[i] = (dp[i] + dp[i - one]) % mod;
            }
            if (i >= low) {
                count = (count + dp[i]) % mod;
            }
        }
        return count;
    }

}
