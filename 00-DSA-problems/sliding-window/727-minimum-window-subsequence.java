import java.util.Arrays;

/**
 * MINIMUM WINDOW SUBSEQUENCE (LeetCode 727) - COMPREHENSIVE GUIDE
 * 
 * Problem: Given strings s1 and s2, return the minimum contiguous substring of s1 
 * such that s2 is a SUBSEQUENCE of that substring.
 * 
 * KEY DIFFERENCE FROM "MINIMUM WINDOW SUBSTRING":
 * - Subsequence: Characters must appear IN ORDER (but not necessarily consecutive)
 * - Substring: All characters must be present (order doesn't matter, duplicates counted)
 * 
 * EXAMPLE:
 * s1 = "abcdebdde", s2 = "bde"
 * Output: "bcde"
 * Explanation: "bcde" contains 'b', 'd', 'e' in order. "bdde" also works but "bcde" comes first.
 * Note: "deb" is NOT valid because characters aren't in order.
 * 
 * DIFFICULTY: Hard
 * OPTIMAL TIME: O(m * n) where m = |s1|, n = |s2|
 * OPTIMAL SPACE: O(m * n) or O(n) with space optimization
 * 
 * KEY PATTERNS:
 * 1. Dynamic Programming (2D DP)
 * 2. Two-Pointer Technique (Forward-Backward Scan)
 * 3. Space Optimization (1D DP)
 * 
 * COMPANIES: Amazon, Google, Microsoft, Facebook
 * 
 * EDGE CASES:
 * 1. s2 longer than s1 → return ""
 * 2. s2 not a subsequence of s1 → return ""
 * 3. Multiple minimum windows → return leftmost
 * 4. Single character strings
 * 5. s2 = s1
 */
class MinimumWindowSubsequence {

    // ========================================================================
    // APPROACH 1: DYNAMIC PROGRAMMING 2D (MOST INTUITIVE)
    // ========================================================================
    /**
     * This is the most straightforward DP approach for understanding the problem.
     * 
     * DP STATE DEFINITION:
     * dp[i][j] = starting index of the minimum window ending at s1[i-1] that 
     *            contains s2[0...j-1] as a subsequence.
     * dp[i][j] = -1 if no such window exists.
     * 
     * RECURRENCE RELATION:
     * If s1[i-1] == s2[j-1]:
     *     If j == 1: dp[i][j] = i (found first char, window starts at current position)
     *     If j > 1:  dp[i][j] = dp[i-1][j-1] (extend previous match)
     * Else:
     *     dp[i][j] = dp[i-1][j] (carry forward previous window)
     * 
     * TIME: O(m * n) where m = len(s1), n = len(s2)
     * SPACE: O(m * n)
     * 
     * INTERVIEW TIPS:
     * - Draw a small DP table to explain the transitions
     * - Show example: s1="abc", s2="bc" step by step
     * - Mention that this is the foundation before optimizing
     */
    public String minWindowDP2D(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        
        // Edge case: s2 cannot be longer than s1
        if (m < n) {
            return "";
        }
        
        // dp[i][j] = starting index of minimum window ending at s1[i-1]
        // that contains s2[0..j-1] as subsequence
        int[][] dp = new int[m + 1][n + 1];
        
        // Initialize with -1 (no valid window)
        for (int i = 0; i <= m; i++) {
            Arrays.fill(dp[i], -1);
        }
        
        // Base case: empty s2 means window starts right after current position
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        
        // Fill DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                // CRITICAL: Can't match more characters than we've seen
                if (j > i) {
                    dp[i][j] = -1;
                    continue;
                }
                
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    // Characters match
                    if (j == 1) {
                        // First character of s2 found, window starts here
                        dp[i][j] = i - 1; // 0-indexed start position
                    } else {
                        // Extend from previous match
                        dp[i][j] = dp[i - 1][j - 1];
                    }
                } else {
                    // Characters don't match, carry forward previous window
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }
        
        // Find minimum window from all valid windows
        int minLen = Integer.MAX_VALUE;
        int startIdx = -1;
        
        // Check last column (where we've matched all of s2)
        for (int i = 1; i <= m; i++) {
            if (dp[i][n] != -1) {
                int start = dp[i][n];
                int length = i - start; // window length
                
                // Update if this is shorter (or same length but earlier)
                if (length < minLen) {
                    minLen = length;
                    startIdx = start;
                }
            }
        }
        
        return startIdx == -1 ? "" : s1.substring(startIdx, startIdx + minLen);
    }

    // ========================================================================
    // APPROACH 2: SPACE-OPTIMIZED DP (1D ARRAY)
    // ========================================================================
    /**
     * Optimizes space from O(m*n) to O(n) by using rolling array technique.
     * 
     * KEY INSIGHT:
     * We only need previous row (i-1) to compute current row (i).
     * Use two arrays: prev and curr, swap them after each row.
     * 
     * TIME: O(m * n)
     * SPACE: O(n) - only store current and previous row
     * 
     * WHEN TO USE:
     * - When interviewer asks about space optimization
     * - When memory is constrained
     * - To show advanced DP skills
     * 
     * INTERVIEW TIP:
     * First solve with 2D DP, then optimize. Explain the rolling array technique.
     */
    public String minWindowDP1D(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        
        if (m < n) {
            return "";
        }
        
        // Only need current and previous row
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        
        Arrays.fill(prev, -1);
        Arrays.fill(curr, -1);
        prev[0] = 0; // Base case for empty s2
        
        int minLen = Integer.MAX_VALUE;
        int startIdx = -1;
        
        for (int i = 1; i <= m; i++) {
            curr[0] = i; // Base case for empty s2
            
            for (int j = 1; j <= n && j <= i; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    if (j == 1) {
                        curr[j] = i - 1;
                    } else {
                        curr[j] = prev[j - 1];
                    }
                } else {
                    curr[j] = prev[j];
                }
            }
            
            // Check if we have a valid window ending at position i
            if (curr[n] != -1) {
                int start = curr[n];
                int length = i - start;
                
                if (length < minLen) {
                    minLen = length;
                    startIdx = start;
                }
            }
            
            // Swap arrays for next iteration
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return startIdx == -1 ? "" : s1.substring(startIdx, startIdx + minLen);
    }

    // ========================================================================
    // APPROACH 3: TWO POINTER (FORWARD-BACKWARD SCAN) - OPTIMAL
    // ========================================================================
    /**
     * Most elegant solution using two-pointer technique.
     * 
     * ALGORITHM:
     * 1. Forward scan: Find end of a window containing s2 as subsequence
     * 2. Backward scan: Shrink window from the end to find minimum start
     * 3. Repeat from next position after minimum window
     * 
     * INTUITION:
     * - We don't need to check every possible window
     * - Once we find a valid window, we can shrink it backwards
     * - Then continue searching from the next position
     * 
     * TIME: O(m * n) - each character in s1 visited at most 2n times
     * SPACE: O(1) - only using pointers
     * 
     * PROS:
     * + Best space complexity O(1)
     * + More intuitive once you understand the pattern
     * + No DP table overhead
     * 
     * INTERVIEW FAVORITE:
     * This is often the expected solution for experienced candidates.
     * Shows strong problem-solving skills and pattern recognition.
     */
    public String minWindowTwoPointer(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        
        if (m < n) {
            return "";
        }
        
        int minLen = Integer.MAX_VALUE;
        int minStart = -1;
        
        // Start from each position in s1
        int i = 0;
        
        while (i < m) {
            // Phase 1: Forward scan - find end of window containing s2
            int j = 0; // pointer for s2
            int end = i; // end of current window
            
            // Match all characters of s2 in order
            while (end < m && j < n) {
                if (s1.charAt(end) == s2.charAt(j)) {
                    j++;
                }
                if (j < n) { // Don't increment if we've matched all of s2
                    end++;
                }
            }
            
            // If we couldn't match all of s2, no more windows possible
            if (j < n) {
                break;
            }
            
            // Phase 2: Backward scan - shrink window from end
            int start = end;
            j = n - 1; // Start from last character of s2
            
            // Find the minimum start position
            while (j >= 0) {
                if (s1.charAt(start) == s2.charAt(j)) {
                    j--;
                }
                if (j >= 0) { // Don't decrement if we've matched all
                    start--;
                }
            }
            
            // Update minimum window if this one is smaller
            int windowLen = end - start + 1;
            if (windowLen < minLen) {
                minLen = windowLen;
                minStart = start;
            }
            
            // Continue search from next position after window start
            // This ensures we don't miss any shorter windows
            i = start + 1;
        }
        
        return minStart == -1 ? "" : s1.substring(minStart, minStart + minLen);
    }

    // ========================================================================
    // APPROACH 4: OPTIMIZED TWO POINTER (CLEANER IMPLEMENTATION)
    // ========================================================================
    /**
     * Cleaner version of two-pointer approach.
     * Recommended for interviews due to code clarity.
     * 
     * TIME: O(m * n)
     * SPACE: O(1)
     */
    public String minWindowOptimized(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        
        if (m < n || n == 0) {
            return "";
        }
        
        int minLen = Integer.MAX_VALUE;
        String result = "";
        
        int i = 0; // Pointer for s1
        
        while (i < m) {
            // Step 1: Find a window - match all characters of s2
            int j = 0; // Pointer for s2
            int start = i;
            
            // Forward scan: match s2 in s1
            while (i < m) {
                if (s1.charAt(i) == s2.charAt(j)) {
                    j++;
                    if (j == n) {
                        // Found all characters of s2
                        break;
                    }
                }
                i++;
            }
            
            // If we couldn't match all of s2, no solution
            if (j < n) {
                break;
            }
            
            // Step 2: Shrink window - find minimum start
            int end = i; // Current position is the end
            j = n - 1; // Start from last char of s2
            
            // Backward scan: find leftmost start
            while (j >= 0) {
                if (s1.charAt(i) == s2.charAt(j)) {
                    j--;
                }
                i--;
            }
            
            start = i + 1; // Adjust start position
            
            // Step 3: Update result if this window is smaller
            int windowLen = end - start + 1;
            if (windowLen < minLen) {
                minLen = windowLen;
                result = s1.substring(start, end + 1);
            }
            
            // Step 4: Move to next possible start
            i = start + 1;
        }
        
        return result;
    }

    // ========================================================================
    // APPROACH 5: BRUTE FORCE (FOR UNDERSTANDING ONLY)
    // ========================================================================
    /**
     * Check all substrings to see if they contain s2 as subsequence.
     * 
     * TIME: O(m² * n) - m² substrings, each takes O(n) to check
     * SPACE: O(1)
     * 
     * USE ONLY TO:
     * - Understand the problem
     * - Show why optimization is needed
     * 
     * NEVER use in actual interview unless asked to start simple.
     */
    public String minWindowBruteForce(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        
        if (m < n) {
            return "";
        }
        
        int minLen = Integer.MAX_VALUE;
        String result = "";
        
        // Try all possible substrings
        for (int i = 0; i < m; i++) {
            for (int j = i + n; j <= m; j++) {
                String window = s1.substring(i, j);
                
                // Check if s2 is a subsequence of window
                if (isSubsequence(s2, window)) {
                    if (window.length() < minLen) {
                        minLen = window.length();
                        result = window;
                    }
                }
            }
        }
        
        return result;
    }
    
    // Helper: Check if s is a subsequence of t
    private boolean isSubsequence(String s, String t) {
        int i = 0; // pointer for s
        int j = 0; // pointer for t
        
        while (i < s.length() && j < t.length()) {
            if (s.charAt(i) == t.charAt(j)) {
                i++;
            }
            j++;
        }
        
        return i == s.length();
    }

    // ========================================================================
    // COMMON MISTAKES & DEBUGGING TIPS
    // ========================================================================
    /**
     * COMMON MISTAKES:
     * 
     * 1. WRONG: Confusing subsequence with substring
     *    RIGHT: Subsequence maintains ORDER but characters can be non-contiguous
     *    Example: "ace" is subsequence of "abcde" but not substring
     * 
     * 2. WRONG: Not handling the case where multiple minimum windows exist
     *    RIGHT: Return the LEFTMOST one (earliest starting position)
     * 
     * 3. WRONG: In DP, not checking j > i constraint
     *    RIGHT: Can't match j characters if we've only seen i characters
     * 
     * 4. WRONG: In two-pointer, incrementing i before checking end condition
     *    RIGHT: Check if j == n BEFORE incrementing end pointer
     * 
     * 5. WRONG: Off-by-one errors in substring extraction
     *    RIGHT: s.substring(start, end + 1) includes character at 'end'
     * 
     * 6. WRONG: Not moving to start+1 after finding a window
     *    RIGHT: Continue from start+1 to find potentially shorter windows
     * 
     * 7. WRONG: Backward scan not properly decrementing indices
     *    RIGHT: Careful with loop conditions and pointer management
     * 
     * 8. WRONG: Assuming s1 always contains s2 as subsequence
     *    RIGHT: Always check if a valid window was found
     */

    // ========================================================================
    // INTERVIEW COMMUNICATION GUIDE
    // ========================================================================
    /**
     * HOW TO APPROACH IN INTERVIEW (30-35 minutes):
     * 
     * PHASE 1: CLARIFICATION (2-3 min)
     * Questions to ask:
     * - "Should I return any minimum window or the leftmost?" (LEFTMOST)
     * - "Are all characters lowercase?" (Usually YES)
     * - "Can s2 be empty?" (Edge case to handle)
     * - "What if s2 is not a subsequence of s1?" (Return "")
     * - "Can strings contain spaces or special characters?" (Usually no)
     * 
     * PHASE 2: EXAMPLES (2-3 min)
     * Walk through the example:
     * s1 = "abcdebdde", s2 = "bde"
     * 
     * Valid windows:
     * - "bcdde" (indices 1-5) - length 5
     * - "bdde" (indices 5-8) - length 4
     * - "bcde" (indices 1-4) - length 4 ✓ ANSWER (leftmost)
     * 
     * Invalid:
     * - "deb" - wrong order
     * - "bde" - not contiguous in s1
     * 
     * PHASE 3: APPROACH DISCUSSION (3-5 min)
     * 
     * Start with intuition:
     * "We need to find the shortest contiguous substring where s2 appears in order."
     * 
     * Brute force:
     * "We could check all O(m²) substrings, each taking O(n) time = O(m²*n)"
     * 
     * Optimization path:
     * Option 1: "DP can help track minimum windows ending at each position"
     * Option 2: "Two pointers: expand to find window, contract to minimize it"
     * 
     * Choose approach:
     * "I'll use two-pointer as it's O(1) space and elegant" OR
     * "I'll use DP as it's more systematic and easier to verify"
     * 
     * PHASE 4: COMPLEXITY ANALYSIS (1 min)
     * - Two-pointer: Time O(m*n), Space O(1)
     * - DP 2D: Time O(m*n), Space O(m*n)
     * - DP 1D: Time O(m*n), Space O(n)
     * 
     * PHASE 5: CODING (15-20 min)
     * - Write clean, commented code
     * - Use meaningful variable names
     * - Handle edge cases
     * - Add comments for non-obvious logic
     * 
     * PHASE 6: TESTING (3-5 min)
     * Test cases:
     * 1. Given example
     * 2. No valid window: s1="abc", s2="def" → ""
     * 3. Single character: s1="a", s2="a" → "a"
     * 4. s2 longer: s1="ab", s2="abc" → ""
     * 5. Entire string: s1="abc", s2="abc" → "abc"
     * 6. Multiple windows: verify leftmost is returned
     * 
     * PHASE 7: FOLLOW-UP (if time)
     * - Space optimization (if used 2D DP)
     * - How to find ALL minimum windows
     * - Case-insensitive matching
     * - Handling Unicode characters
     */

    // ========================================================================
    // KEY INSIGHTS & PATTERNS
    // ========================================================================
    /**
     * PATTERN RECOGNITION:
     * 
     * 1. SUBSEQUENCE vs SUBSTRING:
     *    - Subsequence: maintains order, can skip characters
     *    - Substring: contiguous, all characters adjacent
     *    
     * 2. WINDOW OPTIMIZATION:
     *    - Expand to find valid window
     *    - Contract to minimize
     *    - Continue from next position
     *    
     * 3. DP STATE DESIGN:
     *    - Track "starting position" not "existence"
     *    - Helps find minimum window length
     *    
     * 4. TWO-POINTER TECHNIQUE:
     *    - Forward scan: match all of s2
     *    - Backward scan: find minimum start
     *    - Critical: move to start+1, not end+1
     */

    // ========================================================================
    // RELATED PROBLEMS
    // ========================================================================
    /**
     * SIMILAR PROBLEMS:
     * 
     * 1. LeetCode 76 - Minimum Window Substring (Different!)
     *    - Uses frequency count, not order
     *    - Sliding window with HashMap
     *    
     * 2. LeetCode 392 - Is Subsequence
     *    - Simpler version: just check if subsequence exists
     *    
     * 3. LeetCode 524 - Longest Word in Dictionary through Deleting
     *    - Find longest subsequence from list
     *    
     * 4. LeetCode 792 - Number of Matching Subsequences
     *    - Count how many words are subsequences
     *    
     * 5. LeetCode 115 - Distinct Subsequences
     *    - Count number of ways to form subsequence
     * 
     * VARIATIONS:
     * - Find all minimum windows (not just one)
     * - Find longest window containing subsequence
     * - Case-insensitive matching
     * - Allow character substitutions (edit distance)
     */

    // ========================================================================
    // COMPLEXITY COMPARISON
    // ========================================================================
    /**
     * APPROACH COMPARISON:
     * 
     * | Approach      | Time      | Space   | Interview Score | Notes              |
     * |---------------|-----------|---------|-----------------|-------------------|
     * | Brute Force   | O(m²*n)   | O(1)    | ⭐☆☆☆☆          | Never use         |
     * | DP 2D         | O(m*n)    | O(m*n)  | ⭐⭐⭐⭐☆        | Most systematic   |
     * | DP 1D         | O(m*n)    | O(n)    | ⭐⭐⭐⭐⭐       | Space optimized   |
     * | Two Pointer   | O(m*n)    | O(1)    | ⭐⭐⭐⭐⭐       | Most elegant      |
     * 
     * RECOMMENDATION:
     * - Start with DP if not confident with two-pointer
     * - Use two-pointer for best impression
     * - Mention both approaches if time permits
     */

    // ========================================================================
    // TEST CASES
    // ========================================================================
    public static void main(String[] args) {
        MinimumWindowSubsequence solution = new MinimumWindowSubsequence();
        
        System.out.println("=== MINIMUM WINDOW SUBSEQUENCE TEST CASES ===\n");
        
        // Test Case 1: Standard example
        String s1_1 = "abcdebdde";
        String s2_1 = "bde";
        System.out.println("Test 1: Standard Example");
        System.out.println("s1 = \"" + s1_1 + "\", s2 = \"" + s2_1 + "\"");
        System.out.println("Expected: \"bcde\"");
        System.out.println("DP 2D:        " + solution.minWindowDP2D(s1_1, s2_1));
        System.out.println("DP 1D:        " + solution.minWindowDP1D(s1_1, s2_1));
        System.out.println("Two Pointer:  " + solution.minWindowTwoPointer(s1_1, s2_1));
        System.out.println("Optimized:    " + solution.minWindowOptimized(s1_1, s2_1));
        System.out.println("Brute Force:  " + solution.minWindowBruteForce(s1_1, s2_1));
        System.out.println();
        
        // Test Case 2: No valid window
        String s1_2 = "abc";
        String s2_2 = "def";
        System.out.println("Test 2: No Valid Window");
        System.out.println("s1 = \"" + s1_2 + "\", s2 = \"" + s2_2 + "\"");
        System.out.println("Expected: \"\"");
        System.out.println("Result:   \"" + solution.minWindowOptimized(s1_2, s2_2) + "\"");
        System.out.println();
        
        // Test Case 3: Single character
        String s1_3 = "a";
        String s2_3 = "a";
        System.out.println("Test 3: Single Character");
        System.out.println("s1 = \"" + s1_3 + "\", s2 = \"" + s2_3 + "\"");
        System.out.println("Expected: \"a\"");
        System.out.println("Result:   \"" + solution.minWindowOptimized(s1_3, s2_3) + "\"");
        System.out.println();
        
        // Test Case 4: s2 longer than s1
        String s1_4 = "ab";
        String s2_4 = "abc";
        System.out.println("Test 4: s2 Longer Than s1");
        System.out.println("s1 = \"" + s1_4 + "\", s2 = \"" + s2_4 + "\"");
        System.out.println("Expected: \"\"");
        System.out.println("Result:   \"" + solution.minWindowOptimized(s1_4, s2_4) + "\"");
        System.out.println();
        
        // Test Case 5: Entire string is minimum
        String s1_5 = "abc";
        String s2_5 = "abc";
        System.out.println("Test 5: Entire String is Minimum");
        System.out.println("s1 = \"" + s1_5 + "\", s2 = \"" + s2_5 + "\"");
        System.out.println("Expected: \"abc\"");
        System.out.println("Result:   \"" + solution.minWindowOptimized(s1_5, s2_5) + "\"");
        System.out.println();
        
        // Test Case 6: Multiple occurrences
        String s1_6 = "abcdeabcde";
        String s2_6 = "ace";
        System.out.println("Test 6: Multiple Occurrences");
        System.out.println("s1 = \"" + s1_6 + "\", s2 = \"" + s2_6 + "\"");
        System.out.println("Expected: \"abcde\" (first occurrence)");
        System.out.println("Result:   \"" + solution.minWindowOptimized(s1_6, s2_6) + "\"");
        System.out.println();
        
        // Test Case 7: Characters at boundaries
        String s1_7 = "fgrqsqsnodwmxzkzxwqegkndaa";
        String s2_7 = "kzed";
        System.out.println("Test 7: Complex Case");
        System.out.println("s1 = \"" + s1_7 + "\", s2 = \"" + s2_7 + "\"");
        System.out.println("Result:   \"" + solution.minWindowOptimized(s1_7, s2_7) + "\"");
        System.out.println();
        
        // Performance comparison
        System.out.println("=== PERFORMANCE COMPARISON ===");
        String s1_perf = "abcdebddeabcdebddeabcdebdde";
        String s2_perf = "bde";
        
        long start, end;
        
        start = System.nanoTime();
        solution.minWindowDP2D(s1_perf, s2_perf);
        end = System.nanoTime();
        System.out.println("DP 2D:       " + (end - start) + " ns");
        
        start = System.nanoTime();
        solution.minWindowDP1D(s1_perf, s2_perf);
        end = System.nanoTime();
        System.out.println("DP 1D:       " + (end - start) + " ns");
        
        start = System.nanoTime();
        solution.minWindowTwoPointer(s1_perf, s2_perf);
        end = System.nanoTime();
        System.out.println("Two Pointer: " + (end - start) + " ns");
    }
}

/**
 * ============================================================================
 * FINAL INTERVIEW CHECKLIST
 * ============================================================================
 * 
 * BEFORE CODING:
 * □ Clarified subsequence vs substring
 * □ Asked about multiple windows (return leftmost)
 * □ Confirmed character set and case sensitivity
 * □ Discussed approach and got buy-in
 * □ Stated time and space complexity
 * 
 * WHILE CODING:
 * □ Used descriptive variable names (start, end, i, j)
 * □ Added comments for complex logic
 * □ Handled all edge cases
 * □ Proper loop conditions and bounds checking
 * □ Correct substring extraction
 * 
 * AFTER CODING:
 * □ Tested with given example
 * □ Tested edge cases
 * □ Walked through algorithm step-by-step
 * □ Verified time/space complexity
 * □ Discussed optimizations if applicable
 * 
 * KEY TALKING POINTS:
 * ✓ "Subsequence means characters in order, not necessarily contiguous"
 * ✓ "Two-pointer gives us O(1) space with same time complexity"
 * ✓ "We expand to find valid window, contract to minimize"
 * ✓ "Continue from start+1 to find all possible windows"
 * ✓ "DP tracks minimum window starting position at each index"
 * 
 * COMMON PITFALLS TO AVOID:
 * ✗ Confusing with Minimum Window Substring (different problem!)
 * ✗ Not returning leftmost window when multiple exist
 * ✗ Off-by-one errors in substring extraction
 * ✗ Not moving to start+1 after finding window
 * ✗ Incrementing pointers at wrong time
 * 
 * ============================================================================
 */
