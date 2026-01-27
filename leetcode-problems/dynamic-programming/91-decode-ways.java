/**
 * DECODE WAYS
 * 
 * PROBLEM UNDERSTANDING:
 * ---------------------
 * Given a string of digits, decode it to letters where:
 * "1" -> 'A', "2" -> 'B', ..., "26" -> 'Z'
 * 
 * Count the number of ways to decode the string.
 * 
 * VALID CODES:
 * - Single digit: "1" to "9" (NOT "0" - this is crucial!)
 * - Double digit: "10" to "26" only
 * 
 * INVALID CASES:
 * - "0" by itself (no letter maps to 0)
 * - "00", "01", "02", ..., "09" (leading zeros invalid)
 * - "27", "28", ..., "99" (too large, no mapping)
 * 
 * CRITICAL INSIGHT:
 * ----------------
 * This is a DYNAMIC PROGRAMMING problem similar to:
 * - Climbing Stairs (take 1 or 2 steps)
 * - Fibonacci sequence
 * 
 * But with VALIDATION constraints!
 * 
 * KEY CHALLENGE: Handling '0'
 * - '0' cannot stand alone
 * - '0' must be part of "10" or "20"
 * - "30", "40", ... are invalid
 * - "00" is invalid
 * 
 * INTERVIEW APPROACH - HOW TO SOLVE THIS:
 * ---------------------------------------
 * 
 * Step 1: UNDERSTAND THE CONSTRAINTS
 * "Let me trace through the example '11106':
 *  - (1)(1)(10)(6) = AAJF ✓
 *  - (11)(10)(6) = KJF ✓
 *  - (1)(11)(0)(6) = Invalid! '0' can't stand alone
 *  - (1)(1)(1)(06) = Invalid! '06' has leading zero"
 * 
 * Step 2: RECOGNIZE THE PATTERN
 * "At each position, I can either:
 *  1. Take one digit (if it's valid: 1-9)
 *  2. Take two digits (if valid: 10-26)
 *  
 *  This is like Climbing Stairs but with validation!"
 * 
 * Step 3: DEFINE DP STATE
 * dp[i] = number of ways to decode s[0...i-1]
 * 
 * OR (easier to think about):
 * dp[i] = number of ways to decode substring ending at index i
 * 
 * Step 4: HANDLE EDGE CASES
 * - Empty string: 1 way (do nothing)
 * - Starts with '0': 0 ways (invalid)
 * - Contains invalid sequences: 0 ways
 * 
 * Step 5: STATE TRANSITION
 * At position i:
 * - If s[i] is '1'-'9': add dp[i-1] (decode as single digit)
 * - If s[i-1:i] is "10"-"26": add dp[i-2] (decode as double digit)
 * 
 * SPECIAL CASES WITH '0':
 * - If s[i] == '0', can only decode with previous digit
 * - Previous must be '1' or '2' to form "10" or "20"
 * - Otherwise, impossible to decode
 */

class DecodeWays {
    
    /**
     * APPROACH 1: BOTTOM-UP DYNAMIC PROGRAMMING
     * Time Complexity: O(n) where n = length of string
     * Space Complexity: O(n) for dp array
     * 
     * THIS IS THE STANDARD INTERVIEW SOLUTION
     * 
     * INTUITION:
     * - Build up from empty string to full string
     * - At each position, decide if we can decode 1 or 2 digits
     * - Handle '0' carefully (can't stand alone)
     * 
     * DP DEFINITION:
     * dp[i] = number of ways to decode s[0...i-1]
     * dp[0] = 1 (empty string: one way)
     * dp[1] = 1 if s[0] != '0', else 0
     * 
     * TRANSITION:
     * For position i (checking character s[i-1]):
     * 1. Single digit: if s[i-1] is '1'-'9', dp[i] += dp[i-1]
     * 2. Double digit: if s[i-2:i-1] is "10"-"26", dp[i] += dp[i-2]
     */
    public int numDecodings(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) == '0') {
            return 0; // Empty or starts with '0' - invalid
        }
        
        int n = s.length();
        int[] dp = new int[n + 1];
        
        // Base cases
        dp[0] = 1; // Empty string: 1 way to decode (do nothing)
        dp[1] = 1; // First character (already checked it's not '0')
        
        // Fill dp array
        for (int i = 2; i <= n; i++) {
            // Check single digit: s[i-1]
            int oneDigit = s.charAt(i - 1) - '0';
            if (oneDigit >= 1 && oneDigit <= 9) {
                dp[i] += dp[i - 1];
            }
            
            // Check two digits: s[i-2] and s[i-1]
            int twoDigits = (s.charAt(i - 2) - '0') * 10 + (s.charAt(i - 1) - '0');
            if (twoDigits >= 10 && twoDigits <= 26) {
                dp[i] += dp[i - 2];
            }
        }
        
        return dp[n];
    }
    
    /**
     * APPROACH 2: SPACE-OPTIMIZED DP
     * Time Complexity: O(n)
     * Space Complexity: O(1) - only store last two values
     * 
     * OPTIMIZATION:
     * - We only need dp[i-1] and dp[i-2] to compute dp[i]
     * - Use two variables instead of array
     * - This is the Fibonacci-space-optimization pattern
     * 
     * RECOMMENDED FOR FOLLOW-UP: "Can you optimize space?"
     */
    public int numDecodingsSpaceOptimized(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) == '0') {
            return 0;
        }
        
        int n = s.length();
        int prev2 = 1; // dp[i-2]
        int prev1 = 1; // dp[i-1]
        
        for (int i = 2; i <= n; i++) {
            int current = 0;
            
            // Check single digit
            int oneDigit = s.charAt(i - 1) - '0';
            if (oneDigit >= 1 && oneDigit <= 9) {
                current += prev1;
            }
            
            // Check two digits
            int twoDigits = (s.charAt(i - 2) - '0') * 10 + (s.charAt(i - 1) - '0');
            if (twoDigits >= 10 && twoDigits <= 26) {
                current += prev2;
            }
            
            // Shift for next iteration
            prev2 = prev1;
            prev1 = current;
        }
        
        return prev1;
    }
    
    /**
     * APPROACH 3: TOP-DOWN DP WITH MEMOIZATION
     * Time Complexity: O(n)
     * Space Complexity: O(n) - recursion stack + memo
     * 
     * WHEN TO USE:
     * - If you think recursively first
     * - Good for explaining the recurrence relation
     * - Some people find this more intuitive
     */
    public int numDecodingsTopDown(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) == '0') {
            return 0;
        }
        
        Integer[] memo = new Integer[s.length()];
        return decode(s, 0, memo);
    }
    
    /**
     * Recursive helper: count ways to decode from index 'start' to end
     */
    private int decode(String s, int start, Integer[] memo) {
        // Base case: reached end of string
        if (start == s.length()) {
            return 1;
        }
        
        // Invalid: current position is '0'
        if (s.charAt(start) == '0') {
            return 0;
        }
        
        // Already computed
        if (memo[start] != null) {
            return memo[start];
        }
        
        int ways = 0;
        
        // Option 1: Decode one digit
        ways += decode(s, start + 1, memo);
        
        // Option 2: Decode two digits (if possible)
        if (start + 1 < s.length()) {
            int twoDigits = (s.charAt(start) - '0') * 10 + (s.charAt(start + 1) - '0');
            if (twoDigits >= 10 && twoDigits <= 26) {
                ways += decode(s, start + 2, memo);
            }
        }
        
        memo[start] = ways;
        return ways;
    }
    
    /**
     * APPROACH 4: ITERATIVE WITH CLEARER LOGIC
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * PURPOSE:
     * - More explicit validation logic
     * - Easier to understand and debug
     * - Good for interviews when you want clarity
     */
    public int numDecodingsClear(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) == '0') {
            return 0;
        }
        
        int n = s.length();
        int[] dp = new int[n + 1];
        dp[0] = 1;
        dp[1] = 1;
        
        for (int i = 2; i <= n; i++) {
            // Get current and previous character
            char curr = s.charAt(i - 1);
            char prev = s.charAt(i - 2);
            
            // Check if we can decode current character alone
            if (curr != '0') {
                dp[i] += dp[i - 1];
            }
            
            // Check if we can decode previous + current together
            if (prev == '1' || (prev == '2' && curr <= '6')) {
                dp[i] += dp[i - 2];
            }
            
            // If dp[i] is still 0, we can't decode from here
            if (dp[i] == 0) {
                return 0;
            }
        }
        
        return dp[n];
    }
    
    /**
     * COMMON MISTAKES TO AVOID:
     * -------------------------
     * 
     * 1. FORGETTING '0' CAN'T STAND ALONE
     *    ❌ Treating '0' as valid single digit
     *    ✓ '0' must be part of "10" or "20"
     *    Example: "10" has 1 way, not 2!
     * 
     * 2. ALLOWING "00", "01", "02", etc.
     *    ❌ Accepting leading zeros in two-digit codes
     *    ✓ Only "10" through "26" are valid two-digit codes
     *    Example: "30" is INVALID (not "10" or "20")
     * 
     * 3. WRONG RANGE FOR TWO-DIGIT CODES
     *    ❌ Checking twoDigits >= 1 && twoDigits <= 26
     *    ✓ Checking twoDigits >= 10 && twoDigits <= 26
     *    Example: "01" should NOT be valid
     * 
     * 4. BASE CASE ERRORS
     *    ❌ dp[0] = 0
     *    ✓ dp[0] = 1 (empty string has one way: do nothing)
     * 
     * 5. NOT HANDLING STRINGS STARTING WITH '0'
     *    ❌ Proceeding with calculation
     *    ✓ Return 0 immediately
     *    Example: "012" → 0 ways (invalid start)
     * 
     * 6. OFF-BY-ONE INDEXING
     *    - dp[i] represents decoding s[0...i-1]
     *    - Be careful with s.charAt(i-1) vs s.charAt(i)
     * 
     * 7. CHECKING WRONG CONDITION FOR '0'
     *    Wrong: if (curr == '0' && (prev != '1' && prev != '2')) return 0;
     *    Right: If curr == '0', must have valid two-digit with prev
     */
    
    /**
     * INTERVIEW COMMUNICATION STRATEGY:
     * ---------------------------------
     * 
     * PHASE 1: CLARIFY WITH EXAMPLES (3-4 min)
     * "Let me trace through '226':
     *  - (2)(2)(6) → BBF ✓
     *  - (22)(6) → VF ✓
     *  - (2)(26) → BZ ✓
     *  Total: 3 ways
     *  
     *  Now let's check '06':
     *  - (0)(6) → Invalid! '0' can't stand alone
     *  - (06) → Invalid! Leading zero
     *  Total: 0 ways
     *  
     *  And '10':
     *  - (1)(0) → Invalid! '0' can't stand alone
     *  - (10) → J ✓
     *  Total: 1 way (not 2!)"
     * 
     * PHASE 2: RECOGNIZE THE PATTERN (2-3 min)
     * "This reminds me of Climbing Stairs where we can take 1 or 2 steps.
     *  Here, at each position we can decode:
     *  - 1 digit (if it's '1'-'9')
     *  - 2 digits (if they form '10'-'26')
     *  
     *  The key difference: we have validation constraints.
     *  Especially '0' is tricky - it can't stand alone!"
     * 
     * PHASE 3: EXPLAIN THE APPROACH (2 min)
     * "I'll use dynamic programming:
     *  - dp[i] = ways to decode first i characters
     *  - dp[0] = 1 (base case: empty string)
     *  - For each position i:
     *    * If s[i-1] is '1'-'9': dp[i] += dp[i-1]
     *    * If s[i-2:i-1] is '10'-'26': dp[i] += dp[i-2]
     *  
     *  Time: O(n), Space: O(n) (can optimize to O(1))"
     * 
     * PHASE 4: CODE WITH VALIDATION (12-15 min)
     * - Handle edge cases upfront (empty, starts with '0')
     * - Clear validation logic
     * - Comments for tricky parts (especially '0' handling)
     * 
     * PHASE 5: TEST THOROUGHLY (4-5 min)
     * Test cases to walk through:
     * 1. "12" → 2 ways: (1)(2) or (12)
     * 2. "226" → 3 ways
     * 3. "0" → 0 ways (starts with 0)
     * 4. "10" → 1 way (not 2!)
     * 5. "27" → 1 way: (2)(7), NOT (27)
     * 6. "100" → 0 ways: (10)(0) invalid, (1)(00) invalid
     */
    
    /**
     * DETAILED WALKTHROUGH: "226"
     * ---------------------------
     * 
     * String: "226"
     * Indices: 0='2', 1='2', 2='6'
     * 
     * dp[0] = 1 (empty string)
     * 
     * i=1 (character '2'):
     *   oneDigit = 2 (valid: 1-9) → dp[1] += dp[0] = 1
     *   twoDigits = N/A (no previous)
     *   dp[1] = 1
     * 
     * i=2 (character '2'):
     *   oneDigit = 2 (valid: 1-9) → dp[2] += dp[1] = 1
     *   twoDigits = 22 (valid: 10-26) → dp[2] += dp[0] = 1
     *   dp[2] = 1 + 1 = 2
     *   [Decodings: (2)(2) and (22)]
     * 
     * i=3 (character '6'):
     *   oneDigit = 6 (valid: 1-9) → dp[3] += dp[2] = 2
     *   twoDigits = 26 (valid: 10-26) → dp[3] += dp[1] = 1
     *   dp[3] = 2 + 1 = 3
     *   [Decodings: (2)(2)(6), (22)(6), (2)(26)]
     * 
     * Answer: 3 ✓
     */
    
    /**
     * DETAILED WALKTHROUGH: "10"
     * --------------------------
     * 
     * String: "10"
     * This is a TRICKY case!
     * 
     * dp[0] = 1
     * dp[1] = 1 (character '1')
     * 
     * i=2 (character '0'):
     *   oneDigit = 0 (INVALID: must be 1-9) → dp[2] += 0
     *   twoDigits = 10 (valid: 10-26) → dp[2] += dp[0] = 1
     *   dp[2] = 1
     * 
     * Answer: 1 (only "10", NOT "1" + "0") ✓
     */
    
    /**
     * DETAILED WALKTHROUGH: "100"
     * ---------------------------
     * 
     * String: "100"
     * This shows why we need to handle '0' carefully!
     * 
     * dp[0] = 1
     * dp[1] = 1 (character '1')
     * 
     * i=2 (character '0'):
     *   oneDigit = 0 (INVALID) → dp[2] += 0
     *   twoDigits = 10 (valid) → dp[2] += dp[0] = 1
     *   dp[2] = 1
     * 
     * i=3 (character '0'):
     *   oneDigit = 0 (INVALID) → dp[3] += 0
     *   twoDigits = 00 (INVALID: not 10-26) → dp[3] += 0
     *   dp[3] = 0
     * 
     * Answer: 0 (impossible to decode!) ✓
     */
    
    /**
     * FOLLOW-UP QUESTIONS:
     * --------------------
     * 
     * Q1: Can you optimize space to O(1)?
     * A: Yes! We only need the last two dp values (like Fibonacci)
     *    Use two variables: prev1 and prev2
     * 
     * Q2: What if the mapping was different (e.g., 1-27)?
     * A: Just change the validation: twoDigits >= 10 && twoDigits <= 27
     * 
     * Q3: What if we needed to return all possible decodings, not just count?
     * A: Use backtracking or modify DP to store actual strings
     *    Space would be O(n * number_of_decodings) - much more expensive
     * 
     * Q4: How would you handle a very long string?
     * A: The O(1) space optimization becomes important
     *    Also, might need to consider modular arithmetic if counting exceeds int
     * 
     * Q5: What if '*' can represent any digit 1-9?
     * A: This is LeetCode 639 "Decode Ways II" - much harder!
     *    Need to count possibilities for each '*' and multiply
     * 
     * Q6: How do you know when decoding is impossible?
     * A: - String starts with '0'
     *    - Contains '0' that's not part of "10" or "20"
     *    - At any point dp[i] becomes 0 (no valid path)
     */
    
    /**
     * COMPARISON TO SIMILAR PROBLEMS:
     * -------------------------------
     * 
     * 1. CLIMBING STAIRS:
     *    - Simple version: no validation needed
     *    - Just add previous two values
     *    - This problem adds validation constraints
     * 
     * 2. FIBONACCI:
     *    - Base pattern is Fibonacci
     *    - F(n) = F(n-1) + F(n-2)
     *    - We conditionally add based on validity
     * 
     * 3. COUNT WAYS TO BUILD GOOD STRINGS:
     *    - Similar DP structure
     *    - That problem: add fixed number of characters
     *    - This problem: decode existing characters
     * 
     * 4. UNIQUE PATHS:
     *    - Different context (grid) but similar DP
     *    - Count paths by summing previous states
     *    - Both have "ways to reach" counting
     */
    
    /**
     * WHY THIS SOLUTION WORKS:
     * ------------------------
     * 
     * CORRECTNESS:
     * - We consider ALL valid ways to partition the string
     * - At each position, we count ways by adding:
     *   * Ways from one step back (if current digit valid)
     *   * Ways from two steps back (if two-digit combo valid)
     * - This covers all possibilities without double-counting
     * 
     * NO DOUBLE COUNTING:
     * - Each way to decode is uniquely identified by its partition points
     * - (1)(2) and (12) are different partitions
     * - DP ensures we count each unique partition exactly once
     * 
     * HANDLING IMPOSSIBILITY:
     * - If at any position dp[i] = 0, no valid way to reach there
     * - Propagates to end, giving final answer of 0
     * - Early exit when string starts with '0'
     */
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        DecodeWays solution = new DecodeWays();
        
        // Test Case 1: Standard case
        System.out.println("Test 1: \"12\"");
        System.out.println("Expected: 2 (AB or L)");
        System.out.println("Result: " + solution.numDecodings("12"));
        System.out.println("Space Optimized: " + solution.numDecodingsSpaceOptimized("12"));
        System.out.println("Top Down: " + solution.numDecodingsTopDown("12"));
        System.out.println();
        
        // Test Case 2: Multiple possibilities
        System.out.println("Test 2: \"226\"");
        System.out.println("Expected: 3 (BBF, VF, BZ)");
        System.out.println("Result: " + solution.numDecodings("226"));
        System.out.println();
        
        // Test Case 3: Starts with zero
        System.out.println("Test 3: \"06\"");
        System.out.println("Expected: 0 (invalid)");
        System.out.println("Result: " + solution.numDecodings("06"));
        System.out.println();
        
        // Test Case 4: Contains zero (valid)
        System.out.println("Test 4: \"10\"");
        System.out.println("Expected: 1 (only J, not A+?)");
        System.out.println("Result: " + solution.numDecodings("10"));
        System.out.println();
        
        // Test Case 5: Contains zero (invalid)
        System.out.println("Test 5: \"100\"");
        System.out.println("Expected: 0 (impossible to decode)");
        System.out.println("Result: " + solution.numDecodings("100"));
        System.out.println();
        
        // Test Case 6: Large number (>26)
        System.out.println("Test 6: \"27\"");
        System.out.println("Expected: 1 (only BG, not 27)");
        System.out.println("Result: " + solution.numDecodings("27"));
        System.out.println();
        
        // Test Case 7: Complex case
        System.out.println("Test 7: \"11106\"");
        System.out.println("Expected: 2 (AAJF, KJF)");
        System.out.println("Result: " + solution.numDecodings("11106"));
        System.out.println();
        
        // Test Case 8: All valid two-digit
        System.out.println("Test 8: \"111111\"");
        System.out.println("Expected: 13 (like Fibonacci)");
        System.out.println("Result: " + solution.numDecodings("111111"));
        System.out.println();
        
        // Test Case 9: Edge case - single digit
        System.out.println("Test 9: \"1\"");
        System.out.println("Expected: 1");
        System.out.println("Result: " + solution.numDecodings("1"));
        System.out.println();
        
        // Test Case 10: Edge case - "0"
        System.out.println("Test 10: \"0\"");
        System.out.println("Expected: 0");
        System.out.println("Result: " + solution.numDecodings("0"));
        System.out.println();
        
        // Detailed trace
        System.out.println("=== DETAILED TRACE FOR \"226\" ===");
        traceDecoding("226");
        
        System.out.println("\n=== DETAILED TRACE FOR \"10\" ===");
        traceDecoding("10");
        
        System.out.println("\n=== DETAILED TRACE FOR \"100\" ===");
        traceDecoding("100");
    }
    
    /**
     * Helper method to trace the DP execution
     */
    private static void traceDecoding(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) == '0') {
            System.out.println("Invalid: empty or starts with '0'");
            return;
        }
        
        int n = s.length();
        int[] dp = new int[n + 1];
        dp[0] = 1;
        dp[1] = 1;
        
        System.out.println("String: \"" + s + "\"");
        System.out.println("dp[0] = 1 (base case)");
        System.out.println("dp[1] = 1 (first character '" + s.charAt(0) + "')");
        
        for (int i = 2; i <= n; i++) {
            char curr = s.charAt(i - 1);
            char prev = s.charAt(i - 2);
            int oneDigit = curr - '0';
            int twoDigits = (prev - '0') * 10 + (curr - '0');
            
            System.out.println("\ni=" + i + " (character '" + curr + "'):");
            
            if (oneDigit >= 1 && oneDigit <= 9) {
                System.out.println("  One digit " + oneDigit + " is valid");
                System.out.println("  dp[" + i + "] += dp[" + (i-1) + "] = " + dp[i-1]);
                dp[i] += dp[i - 1];
            } else {
                System.out.println("  One digit " + oneDigit + " is INVALID");
            }
            
            if (twoDigits >= 10 && twoDigits <= 26) {
                System.out.println("  Two digits " + twoDigits + " is valid");
                System.out.println("  dp[" + i + "] += dp[" + (i-2) + "] = " + dp[i-2]);
                dp[i] += dp[i - 2];
            } else {
                System.out.println("  Two digits " + twoDigits + " is INVALID");
            }
            
            System.out.println("  dp[" + i + "] = " + dp[i]);
        }
        
        System.out.println("\nFinal answer: " + dp[n]);
    }
}

/**
 * FINAL INTERVIEW CHECKLIST:
 * --------------------------
 * ✓ Understand valid codes (1-9 single, 10-26 double)
 * ✓ Handle '0' correctly (must be part of 10 or 20)
 * ✓ Define DP state clearly
 * ✓ Correct base cases (dp[0]=1, handle first char)
 * ✓ Validate both single and double digit options
 * ✓ Check for invalid strings (starts with '0', etc.)
 * ✓ Test with edge cases (0, 10, 100, 27)
 * ✓ Discuss space optimization
 * 
 * TIME TO SOLVE: 25-30 minutes
 * - 4 min: Understanding problem and constraints
 * - 3 min: Recognizing DP pattern
 * - 2 min: Explaining approach
 * - 15 min: Coding with careful validation
 * - 5 min: Testing thoroughly
 * - 3 min: Follow-up discussion
 * 
 * DIFFICULTY: Medium (but validation makes it tricky!)
 * 
 * KEY TAKEAWAYS:
 * 1. This is Fibonacci/Climbing Stairs with validation
 * 2. The '0' character is the trickiest part - handle carefully!
 * 3. Validation ranges: single (1-9), double (10-26)
 * 4. Early exit for invalid strings (starts with '0')
 * 5. Can optimize space to O(1) (good follow-up answer)
 * 
 * MOST COMMON MISTAKE:
 * Not properly handling '0' - it CANNOT stand alone!
 * "10" has 1 way, not 2!
 */

class DecodeWays2 {

    public int numDecodings(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) == '0')
            return 0;

        int n = s.length();
        int[] dp = new int[n + 1];
        dp[0] = 1; // empty string has one way to decode
        dp[1] = 1; // non-zero single digit has one way to decode

        for (int i = 2; i <= n; i++) {
            // Single digit decode
            if (s.charAt(i - 1) != '0') {
                dp[i] += dp[i - 1];
            }

            // Two digit decode
            int twoDigit = Integer.parseInt(s.substring(i - 2, i));
            if (twoDigit >= 10 && twoDigit <= 26) {
                dp[i] += dp[i - 2];
            }
        }

        return dp[n];
    }
    
}
