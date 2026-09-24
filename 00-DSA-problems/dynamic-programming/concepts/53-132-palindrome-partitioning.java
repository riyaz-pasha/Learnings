/**
 * PALINDROME PARTITIONING II - MINIMUM CUTS
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given: String s
 * Find: Minimum number of cuts to partition s into palindromes
 * 
 * Example: "aab"
 * - Partition 1: ["a","a","b"] → 2 cuts
 * - Partition 2: ["aa","b"] → 1 cut ✓ (minimum)
 * - Partition 3: ["aab"] → not valid (not palindrome)
 * Answer: 1
 * 
 * INTERVIEW THOUGHT PROCESS:
 * =========================
 * 
 * Step 1: BRUTE FORCE THINKING
 * "Let me think about all possible partitions..."
 * - Try cutting at every position
 * - For each cut, check if left part is palindrome
 * - Recursively solve for right part
 * - This is exponential: O(2^n) - too slow!
 * 
 * Step 2: IDENTIFY THE PATTERN
 * Key observation: "This has optimal substructure!"
 * - If we know min cuts for s[0...i-1]
 * - And s[i...j] is a palindrome
 * - Then cuts for s[0...j] = cuts[i-1] + 1
 * 
 * This is DYNAMIC PROGRAMMING!
 * 
 * Step 3: DEFINE THE DP STATE
 * dp[i] = minimum cuts needed for s[0...i]
 * 
 * But wait! We also need to know if s[i...j] is palindrome.
 * Two approaches:
 * A) Check palindrome on-the-fly: O(n) per check
 * B) Pre-compute palindrome table: O(n²) space, O(1) lookup
 * 
 * Step 4: CHOOSE OPTIMAL APPROACH
 * Pre-computing palindromes is better!
 * - One-time O(n²) preprocessing
 * - Then O(1) lookups during DP
 * - Total: O(n²) time, O(n²) space
 * 
 * Step 5: THE RECURRENCE RELATION
 * For each position i:
 *   For each position j from 0 to i:
 *     If s[j...i] is palindrome:
 *       dp[i] = min(dp[i], dp[j-1] + 1)
 * 
 * Special case: If s[0...i] is palindrome, dp[i] = 0 (no cuts needed)
 * 
 * TIME COMPLEXITY: O(n²)
 * - Palindrome preprocessing: O(n²)
 * - DP computation: O(n²)
 * 
 * SPACE COMPLEXITY: O(n²) for palindrome table + O(n) for dp array
 * 
 * INTERVIEW COMMUNICATION TIPS:
 * ============================
 * 1. "I recognize this as a DP problem with optimal substructure"
 * 2. "I'll pre-compute which substrings are palindromes"
 * 3. "Then use DP to find minimum cuts"
 * 4. "Let me trace through 'aab' to verify my approach..."
 */

class PalindromePartitioningII {
    
    /**
     * SOLUTION 1: DP WITH PALINDROME TABLE (RECOMMENDED)
     * 
     * Most clear and efficient approach for interviews.
     * Easy to explain and implement correctly.
     */
    public int minCut(String s) {
        int n = s.length();
        if (n <= 1) return 0;
        
        // Step 1: Build palindrome table
        // isPalin[i][j] = true if s[i...j] is palindrome
        boolean[][] isPalin = buildPalindromeTable(s);
        
        // Step 2: DP for minimum cuts
        // dp[i] = minimum cuts needed for s[0...i]
        int[] dp = new int[n];
        
        for (int i = 0; i < n; i++) {
            // Worst case: cut after every character
            dp[i] = i; // i cuts for i+1 characters
            
            // If entire substring s[0...i] is palindrome, no cuts needed
            if (isPalin[0][i]) {
                dp[i] = 0;
                continue;
            }
            
            // Try all possible last palindrome substrings
            for (int j = 1; j <= i; j++) {
                // If s[j...i] is palindrome
                if (isPalin[j][i]) {
                    //cuts for s[0...i] = cuts for s[0...j-1] + 1 (cut before j)
                    dp[i] = Math.min(dp[i], dp[j - 1] + 1);
                }
            }
        }
        
        return dp[n - 1];
    }
    
    /**
     * BUILD PALINDROME TABLE
     * 
     * Uses "Expand Around Center" approach
     * More efficient than checking every substring individually
     * 
     * Time: O(n²), Space: O(n²)
     */
    private boolean[][] buildPalindromeTable(String s) {
        int n = s.length();
        boolean[][] isPalin = new boolean[n][n];
        
        // Every single character is a palindrome
        for (int i = 0; i < n; i++) {
            isPalin[i][i] = true;
        }
        
        // Check for length 2
        for (int i = 0; i < n - 1; i++) {
            if (s.charAt(i) == s.charAt(i + 1)) {
                isPalin[i][i + 1] = true;
            }
        }
        
        // Check for lengths 3 and above
        // Key insight: s[i...j] is palindrome if:
        // - s[i] == s[j] AND
        // - s[i+1...j-1] is palindrome
        for (int len = 3; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                
                if (s.charAt(i) == s.charAt(j) && isPalin[i + 1][j - 1]) {
                    isPalin[i][j] = true;
                }
            }
        }
        
        return isPalin;
    }
    
    /**
     * SOLUTION 2: OPTIMIZED DP (EXPAND AROUND CENTER)
     * 
     * Instead of building full palindrome table, expand around centers
     * while computing DP. Slightly more complex but same complexity.
     * 
     * This approach saves some space by not storing full table,
     * but is harder to understand and debug in interviews.
     */
    public int minCutOptimized(String s) {
        int n = s.length();
        int[] dp = new int[n];
        
        // Initialize: worst case is i cuts for position i
        for (int i = 0; i < n; i++) {
            dp[i] = i;
        }
        
        // For each center position
        for (int center = 0; center < n; center++) {
            // Expand around single character center (odd length palindromes)
            expandAndUpdate(s, dp, center, center);
            
            // Expand around two character center (even length palindromes)
            expandAndUpdate(s, dp, center, center + 1);
        }
        
        return dp[n - 1];
    }
    
    /**
     * Expand around center and update DP array
     */
    private void expandAndUpdate(String s, int[] dp, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            // s[left...right] is a palindrome
            
            if (left == 0) {
                // Entire substring from start is palindrome
                dp[right] = 0;
            } else {
                // Update: cuts for [0...right] = cuts for [0...left-1] + 1
                dp[right] = Math.min(dp[right], dp[left - 1] + 1);
            }
            
            left--;
            right++;
        }
    }
    
    /**
     * SOLUTION 3: RECURSIVE WITH MEMOIZATION
     * 
     * Good for explaining the thought process, but harder to code.
     * Helps demonstrate the overlapping subproblems.
     */
    public int minCutRecursive(String s) {
        int n = s.length();
        boolean[][] isPalin = buildPalindromeTable(s);
        Integer[] memo = new Integer[n];
        
        return solve(s, n - 1, isPalin, memo);
    }
    
    /**
     * Returns minimum cuts needed for s[0...end]
     */
    private int solve(String s, int end, boolean[][] isPalin, Integer[] memo) {
        // Base case: empty string or single character
        if (end < 0) return -1; // No cuts needed
        if (end == 0) return 0;  // Single character, no cuts
        
        if (memo[end] != null) return memo[end];
        
        // If entire substring is palindrome, no cuts needed
        if (isPalin[0][end]) {
            memo[end] = 0;
            return 0;
        }
        
        int minCuts = Integer.MAX_VALUE;
        
        // Try all possible last palindrome substrings
        for (int i = end; i >= 0; i--) {
            if (isPalin[i][end]) {
                // s[i...end] is palindrome
                // cuts = cuts for [0...i-1] + 1
                int cuts = solve(s, i - 1, isPalin, memo) + 1;
                minCuts = Math.min(minCuts, cuts);
            }
        }
        
        memo[end] = minCuts;
        return minCuts;
    }
    
    /**
     * ALTERNATIVE: SPACE-OPTIMIZED PALINDROME CHECK
     * 
     * If memory is critical, we can check palindromes on-the-fly
     * Time: O(n³), Space: O(n)
     * 
     * NOT recommended for interviews unless space is explicitly constrained
     */
    public int minCutSpaceOptimized(String s) {
        int n = s.length();
        int[] dp = new int[n];
        
        for (int i = 0; i < n; i++) {
            dp[i] = i; // worst case
            
            for (int j = 0; j <= i; j++) {
                if (isPalindrome(s, j, i)) {
                    dp[i] = (j == 0) ? 0 : Math.min(dp[i], dp[j - 1] + 1);
                }
            }
        }
        
        return dp[n - 1];
    }
    
    /**
     * Check if s[i...j] is palindrome - O(n) time
     */
    private boolean isPalindrome(String s, int i, int j) {
        while (i < j) {
            if (s.charAt(i) != s.charAt(j)) return false;
            i++;
            j--;
        }
        return true;
    }
    
    /**
     * COMPREHENSIVE TEST SUITE
     */
    public static void main(String[] args) {
        PalindromePartitioningII solver = new PalindromePartitioningII();
        
        // Test Case 1: Example from problem
        String test1 = "aab";
        assert solver.minCut(test1) == 1 : "Test 1 failed";
        assert solver.minCutOptimized(test1) == 1 : "Test 1 (optimized) failed";
        assert solver.minCutRecursive(test1) == 1 : "Test 1 (recursive) failed";
        System.out.println("✓ Test 1 passed: \"aab\" → 1 cut");
        
        // Test Case 2: Single character
        String test2 = "a";
        assert solver.minCut(test2) == 0 : "Test 2 failed";
        System.out.println("✓ Test 2 passed: \"a\" → 0 cuts");
        
        // Test Case 3: No palindrome possible
        String test3 = "ab";
        assert solver.minCut(test3) == 1 : "Test 3 failed";
        System.out.println("✓ Test 3 passed: \"ab\" → 1 cut");
        
        // Test Case 4: Already a palindrome
        String test4 = "aba";
        assert solver.minCut(test4) == 0 : "Test 4 failed";
        System.out.println("✓ Test 4 passed: \"aba\" → 0 cuts");
        
        // Test Case 5: All same characters
        String test5 = "aaaa";
        assert solver.minCut(test5) == 0 : "Test 5 failed";
        System.out.println("✓ Test 5 passed: \"aaaa\" → 0 cuts");
        
        // Test Case 6: Worst case - no palindromes
        String test6 = "abcdef";
        assert solver.minCut(test6) == 5 : "Test 6 failed";
        System.out.println("✓ Test 6 passed: \"abcdef\" → 5 cuts");
        
        // Test Case 7: Complex case
        String test7 = "abacabad";
        int result7 = solver.minCut(test7);
        System.out.println("✓ Test 7 passed: \"abacabad\" → " + result7 + " cuts");
        
        // Test Case 8: Nested palindromes
        String test8 = "leet";
        assert solver.minCut(test8) == 2 : "Test 8 failed";
        // "l", "ee", "t" → 2 cuts
        System.out.println("✓ Test 8 passed: \"leet\" → 2 cuts");
        
        // Test Case 9: Long palindrome in middle
        String test9 = "aabaa";
        assert solver.minCut(test9) == 0 : "Test 9 failed";
        System.out.println("✓ Test 9 passed: \"aabaa\" → 0 cuts");
        
        // Verify all solutions give same results
        String[] tests = {"aab", "a", "ab", "aba", "aaaa", "abcdef"};
        for (String test : tests) {
            int r1 = solver.minCut(test);
            int r2 = solver.minCutOptimized(test);
            int r3 = solver.minCutRecursive(test);
            int r4 = solver.minCutSpaceOptimized(test);
            assert r1 == r2 && r2 == r3 && r3 == r4 : 
                "Solutions don't match for: " + test;
        }
        
        System.out.println("\n✅ All tests passed!");
        
        // Detailed trace for learning
        traceExample();
    }
    
    /**
     * DETAILED TRACE FOR UNDERSTANDING
     * 
     * Shows step-by-step how "aab" is solved
     */
    private static void traceExample() {
        System.out.println("\n=== DETAILED TRACE: \"aab\" ===\n");
        
        String s = "aab";
        System.out.println("Input: " + s);
        System.out.println("\nStep 1: Build Palindrome Table");
        System.out.println("  isPalin[0][0] = true (\"a\")");
        System.out.println("  isPalin[0][1] = true (\"aa\")");
        System.out.println("  isPalin[0][2] = false (\"aab\")");
        System.out.println("  isPalin[1][1] = true (\"a\")");
        System.out.println("  isPalin[1][2] = false (\"ab\")");
        System.out.println("  isPalin[2][2] = true (\"b\")");
        
        System.out.println("\nStep 2: DP Computation");
        System.out.println("\ni = 0 (considering \"a\"):");
        System.out.println("  s[0...0] = \"a\" is palindrome");
        System.out.println("  dp[0] = 0 (no cuts needed)");
        
        System.out.println("\ni = 1 (considering \"aa\"):");
        System.out.println("  s[0...1] = \"aa\" is palindrome");
        System.out.println("  dp[1] = 0 (no cuts needed)");
        
        System.out.println("\ni = 2 (considering \"aab\"):");
        System.out.println("  s[0...2] = \"aab\" is NOT palindrome");
        System.out.println("  Initialize dp[2] = 2 (worst case)");
        System.out.println("  Try j = 1: s[1...2] = \"ab\" is NOT palindrome");
        System.out.println("  Try j = 2: s[2...2] = \"b\" is palindrome");
        System.out.println("    dp[2] = min(2, dp[1] + 1) = min(2, 0 + 1) = 1");
        System.out.println("  dp[2] = 1 ✓");
        
        System.out.println("\nAnswer: 1 cut needed");
        System.out.println("Partition: [\"aa\", \"b\"]");
    }
}

/**
 * INTERVIEW STRATEGY SUMMARY:
 * ===========================
 * 
 * STEP-BY-STEP APPROACH TO COMMUNICATE IN INTERVIEW:
 * 
 * 1. CLARIFY THE PROBLEM
 *    "So we need to partition into palindromes with minimum cuts?"
 *    "Can I assume ASCII characters? What's the max length?"
 * 
 * 2. DISCUSS BRUTE FORCE
 *    "Brute force would try all possible partitions - that's 2^n"
 *    "Way too slow for n > 20"
 * 
 * 3. IDENTIFY DP PATTERN
 *    "I notice this has optimal substructure"
 *    "If I know min cuts for s[0...i], I can build solution for s[0...j]"
 *    "This is a DP problem!"
 * 
 * 4. DEFINE STATE
 *    "dp[i] = minimum cuts for s[0...i]"
 *    "Also need to know which substrings are palindromes"
 * 
 * 5. EXPLAIN TRANSITION
 *    "For each position i, try all j where s[j...i] is palindrome"
 *    "dp[i] = min(dp[j-1] + 1) for all valid j"
 * 
 * 6. CODE THE SOLUTION
 *    "I'll pre-compute palindrome table first"
 *    "Then run DP"
 * 
 * 7. TRACE AN EXAMPLE
 *    "Let me verify with 'aab'..."
 *    Draw the DP table
 * 
 * 8. ANALYZE COMPLEXITY
 *    "Time: O(n²) for palindrome table + O(n²) for DP = O(n²)"
 *    "Space: O(n²) for palindrome table + O(n) for DP = O(n²)"
 * 
 * 9. DISCUSS OPTIMIZATIONS
 *    "Could expand around centers instead of full table"
 *    "Could save space by checking palindromes on-the-fly (but slower)"
 * 
 * COMMON MISTAKES TO AVOID:
 * ========================
 * ❌ Forgetting the case when s[0...i] is entirely palindrome (0 cuts)
 * ❌ Off-by-one errors in indexing
 * ❌ Not initializing dp array correctly
 * ❌ Building palindrome table incorrectly
 * ❌ Trying to optimize too early (code the clear solution first!)
 * 
 * FOLLOW-UP QUESTIONS YOU MIGHT GET:
 * =================================
 * Q: "Can you return the actual partition, not just the count?"
 * A: Yes, modify DP to store the partition points, then reconstruct.
 * 
 * Q: "What if we want ALL minimum partitions?"
 * A: Use backtracking from DP solution to find all optimal paths.
 * 
 * Q: "How would you handle very long strings (millions of characters)?"
 * A: Consider rolling hash for palindrome checking, suffix arrays,
 *    or approximate solutions if exact isn't needed.
 * 
 * Q: "Can we parallelize this?"
 * A: Palindrome table building can be parallelized. DP has dependencies
 *    but could use diagonal parallelization.
 * 
 * RELATED PROBLEMS:
 * ================
 * - Palindrome Partitioning I (return all partitions)
 * - Longest Palindromic Substring
 * - Matrix Chain Multiplication
 * - Word Break
 * - Partition Equal Subset Sum
 */
