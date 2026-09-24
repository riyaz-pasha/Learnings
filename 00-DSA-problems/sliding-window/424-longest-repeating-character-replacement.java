class LongestRepeatingCharacterReplacement {
    
    /**
     * APPROACH 1: Sliding Window with Frequency Array (Optimal)
     * 
     * INTUITION:
     * - In any valid window, we keep the most frequent character and replace others
     * - If window_size - max_frequency > k, we need too many replacements
     * - Use sliding window to maintain valid windows
     * 
     * REASONING:
     * - Let maxFreq = count of most frequent character in window
     * - Characters to replace = window_size - maxFreq
     * - Valid window when: (right - left + 1) - maxFreq <= k
     * - Track frequency of each character (A-Z) using array
     * 
     * KEY INSIGHT:
     * After k replacements, all characters in window become the same.
     * We want to maximize: (most_frequent_char + replacements)
     * where replacements <= k
     * 
     * TIME COMPLEXITY: O(n) - each character visited at most twice
     * SPACE COMPLEXITY: O(26) = O(1) - fixed size array for A-Z
     */
    public int characterReplacement_Optimal(String s, int k) {
        int[] count = new int[26];  // Frequency of each character A-Z
        int left = 0;
        int maxFreq = 0;            // Max frequency of any char in current window
        int maxLength = 0;
        
        for (int right = 0; right < s.length(); right++) {
            // Add current character to window
            char rightChar = s.charAt(right);
            count[rightChar - 'A']++;
            
            // Update max frequency in current window
            maxFreq = Math.max(maxFreq, count[rightChar - 'A']);
            
            // Check if current window is valid
            // window_size - max_frequency = characters that need replacement
            int windowSize = right - left + 1;
            int replacementsNeeded = windowSize - maxFreq;
            
            // If we need more than k replacements, shrink window
            while (replacementsNeeded > k) {
                char leftChar = s.charAt(left);
                count[leftChar - 'A']--;
                left++;
                
                // Recalculate window properties
                windowSize = right - left + 1;
                
                // Note: We don't update maxFreq here (optimization)
                // maxFreq is the max we've seen, good enough for checking
                replacementsNeeded = windowSize - maxFreq;
            }
            
            maxLength = Math.max(maxLength, windowSize);
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 2: Optimized Sliding Window (No Shrinking)
     * 
     * INTUITION:
     * - We don't need to shrink the window, just slide it forward
     * - Once we find a valid window of size n, we only look for larger ones
     * - If invalid, slide entire window forward (both pointers move)
     * 
     * REASONING:
     * - maxFreq can be "stale" (not recalculated when shrinking)
     * - This is OK! We only care about finding larger valid windows
     * - If current window is invalid with stale maxFreq, any smaller
     *   window with accurate maxFreq would also be invalid or smaller
     * 
     * WHY THIS WORKS:
     * - Say we have valid window of size 10 with maxFreq=7, k=3
     * - Next char makes it size 11, but maxFreq stays 7
     * - Need 11-7=4 replacements, but k=3, so invalid
     * - We slide window: left++, now size=10 again
     * - We'll only expand again if new char increases maxFreq
     * - This ensures we only find larger valid windows
     * 
     * TIME COMPLEXITY: O(n) - single pass
     * SPACE COMPLEXITY: O(1) - fixed array size
     */
    public int characterReplacement_OptimizedWindow(String s, int k) {
        int[] count = new int[26];
        int left = 0;
        int maxFreq = 0;
        
        for (int right = 0; right < s.length(); right++) {
            count[s.charAt(right) - 'A']++;
            maxFreq = Math.max(maxFreq, count[s.charAt(right) - 'A']);
            
            // Use 'if' instead of 'while' - slide window instead of shrink
            int windowSize = right - left + 1;
            if (windowSize - maxFreq > k) {
                count[s.charAt(left) - 'A']--;
                left++;
            }
        }
        
        return s.length() - left;
    }
    
    /**
     * APPROACH 3: Sliding Window with Accurate Max Frequency
     * 
     * INTUITION:
     * - Always maintain accurate maxFreq by recalculating when needed
     * - More intuitive but slightly slower due to recalculation
     * 
     * REASONING:
     * - Each time we shrink window, recalculate maxFreq
     * - This ensures maxFreq is always accurate
     * - Easier to understand and explain in interviews
     * 
     * TIME COMPLEXITY: O(26*n) = O(n) - recalculating maxFreq is O(26)
     * SPACE COMPLEXITY: O(1) - fixed array
     */
    public int characterReplacement_AccurateFreq(String s, int k) {
        int[] count = new int[26];
        int left = 0;
        int maxLength = 0;
        
        for (int right = 0; right < s.length(); right++) {
            count[s.charAt(right) - 'A']++;
            
            // Calculate max frequency in current window
            int maxFreq = getMaxFrequency(count);
            
            // Shrink window if invalid
            while (right - left + 1 - maxFreq > k) {
                count[s.charAt(left) - 'A']--;
                left++;
                maxFreq = getMaxFrequency(count);
            }
            
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    private int getMaxFrequency(int[] count) {
        int max = 0;
        for (int c : count) {
            max = Math.max(max, c);
        }
        return max;
    }
    
    /**
     * APPROACH 4: Try Each Character as Target (Intuitive)
     * 
     * INTUITION:
     * - The final substring will be all one character
     * - Try making substring of all 'A's, all 'B's, ..., all 'Z's
     * - For each target, find longest substring where we need ≤ k replacements
     * 
     * REASONING:
     * - For target character 'X', use sliding window
     * - Count non-X characters in window (these need replacement)
     * - If non-X count > k, shrink window
     * - Return max length across all targets
     * 
     * WHY THIS WORKS:
     * - Every valid answer has all characters the same (after replacements)
     * - By trying each possible target, we find the optimal one
     * 
     * TIME COMPLEXITY: O(26*n) = O(n) - try 26 characters
     * SPACE COMPLEXITY: O(1) - no extra space
     */
    public int characterReplacement_TryEachChar(String s, int k) {
        int maxLength = 0;
        
        // Try each character as the target
        for (char target = 'A'; target <= 'Z'; target++) {
            int left = 0;
            int replacements = 0; // Count of non-target chars in window
            
            for (int right = 0; right < s.length(); right++) {
                // If current char is not target, we need a replacement
                if (s.charAt(right) != target) {
                    replacements++;
                }
                
                // Shrink window if we need too many replacements
                while (replacements > k) {
                    if (s.charAt(left) != target) {
                        replacements--;
                    }
                    left++;
                }
                
                maxLength = Math.max(maxLength, right - left + 1);
            }
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 5: Brute Force (For Understanding)
     * 
     * INTUITION:
     * - Try every possible substring
     * - For each substring, check if it can be made uniform with ≤ k changes
     * 
     * REASONING:
     * - For each substring, find most frequent character
     * - Other characters need to be replaced
     * - If replacements needed ≤ k, this substring is valid
     * 
     * TIME COMPLEXITY: O(n² * 26) = O(n²) - try all substrings
     * SPACE COMPLEXITY: O(26) = O(1) - frequency array
     */
    public int characterReplacement_BruteForce(String s, int k) {
        int maxLength = 0;
        
        // Try every starting position
        for (int start = 0; start < s.length(); start++) {
            int[] freq = new int[26];
            
            // Try every ending position
            for (int end = start; end < s.length(); end++) {
                freq[s.charAt(end) - 'A']++;
                
                int windowSize = end - start + 1;
                int maxFreq = getMaxFrequency(freq);
                int replacementsNeeded = windowSize - maxFreq;
                
                if (replacementsNeeded <= k) {
                    maxLength = Math.max(maxLength, windowSize);
                }
            }
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 6: Detailed Explanation Version
     * 
     * Same as Approach 1 but with extensive comments for learning
     */
    public int characterReplacement_Explained(String s, int k) {
        // Array to count frequency of each character (A-Z)
        int[] count = new int[26];
        
        int left = 0;           // Left boundary of sliding window
        int maxFreq = 0;        // Max frequency of any single char in window
        int maxLength = 0;      // Result: longest valid substring found
        
        // Expand window by moving right pointer
        for (int right = 0; right < s.length(); right++) {
            // STEP 1: Add current character to window
            char currentChar = s.charAt(right);
            count[currentChar - 'A']++;
            
            // STEP 2: Update max frequency
            // This tracks the most frequent character in current window
            maxFreq = Math.max(maxFreq, count[currentChar - 'A']);
            
            // STEP 3: Check if window is valid
            // A window is valid if we can make all chars the same with ≤ k changes
            // 
            // Logic:
            // - Window has (right - left + 1) characters total
            // - Most frequent char appears maxFreq times
            // - We keep all maxFreq chars, replace the rest
            // - Replacements needed = total - maxFreq
            // - Valid if replacements <= k
            
            int windowSize = right - left + 1;
            int replacementsNeeded = windowSize - maxFreq;
            
            // STEP 4: Shrink window if invalid
            while (replacementsNeeded > k) {
                // Remove leftmost character
                char leftChar = s.charAt(left);
                count[leftChar - 'A']--;
                left++;
                
                // Recalculate window size and replacements needed
                windowSize = right - left + 1;
                replacementsNeeded = windowSize - maxFreq;
                
                // Note: We don't recalculate maxFreq here (optimization)
                // Using stale maxFreq is fine - see Approach 2 explanation
            }
            
            // STEP 5: Update result with current valid window size
            maxLength = Math.max(maxLength, windowSize);
        }
        
        return maxLength;
    }
    
    /**
     * Visualization helper to understand the algorithm
     */
    public void visualizeSolution(String s, int k) {
        System.out.println("Input: s = \"" + s + "\", k = " + k);
        System.out.println("Finding longest substring with at most " + k + " replacements...\n");
        
        int[] count = new int[26];
        int left = 0;
        int maxFreq = 0;
        int maxLength = 0;
        int bestLeft = 0, bestRight = -1;
        
        for (int right = 0; right < s.length(); right++) {
            count[s.charAt(right) - 'A']++;
            maxFreq = Math.max(maxFreq, count[s.charAt(right) - 'A']);
            
            while (right - left + 1 - maxFreq > k) {
                count[s.charAt(left) - 'A']--;
                left++;
            }
            
            if (right - left + 1 > maxLength) {
                maxLength = right - left + 1;
                bestLeft = left;
                bestRight = right;
            }
        }
        
        System.out.println("Best window: [" + bestLeft + ", " + bestRight + "]");
        System.out.println("Substring: \"" + s.substring(bestLeft, bestRight + 1) + "\"");
        System.out.println("Length: " + maxLength);
        
        // Show what character dominates
        int[] finalCount = new int[26];
        for (int i = bestLeft; i <= bestRight; i++) {
            finalCount[s.charAt(i) - 'A']++;
        }
        char dominant = 'A';
        int maxCount = 0;
        for (int i = 0; i < 26; i++) {
            if (finalCount[i] > maxCount) {
                maxCount = finalCount[i];
                dominant = (char)('A' + i);
            }
        }
        System.out.println("Most frequent char: '" + dominant + "' appears " + maxCount + " times");
        System.out.println("Replacements needed: " + (maxLength - maxCount));
        System.out.println();
    }
    
    /**
     * Comprehensive test cases
     */
    public static void main(String[] args) {
        LongestRepeatingCharacterReplacement solution = new LongestRepeatingCharacterReplacement();
        
        // Test Case 1
        String s1 = "ABAB";
        int k1 = 2;
        System.out.println("=== Test Case 1 ===");
        solution.visualizeSolution(s1, k1);
        System.out.println("Optimal: " + solution.characterReplacement_Optimal(s1, k1));
        System.out.println("Optimized Window: " + solution.characterReplacement_OptimizedWindow(s1, k1));
        System.out.println("Accurate Freq: " + solution.characterReplacement_AccurateFreq(s1, k1));
        System.out.println("Try Each Char: " + solution.characterReplacement_TryEachChar(s1, k1));
        System.out.println("Brute Force: " + solution.characterReplacement_BruteForce(s1, k1));
        System.out.println();
        
        // Test Case 2
        String s2 = "AABABBA";
        int k2 = 1;
        System.out.println("=== Test Case 2 ===");
        solution.visualizeSolution(s2, k2);
        System.out.println("Optimal: " + solution.characterReplacement_Optimal(s2, k2));
        System.out.println("Optimized Window: " + solution.characterReplacement_OptimizedWindow(s2, k2));
        System.out.println("Accurate Freq: " + solution.characterReplacement_AccurateFreq(s2, k2));
        System.out.println("Try Each Char: " + solution.characterReplacement_TryEachChar(s2, k2));
        System.out.println("Brute Force: " + solution.characterReplacement_BruteForce(s2, k2));
        System.out.println();
        
        // Test Case 3: All same character
        String s3 = "AAAA";
        int k3 = 2;
        System.out.println("=== Test Case 3 (All same) ===");
        solution.visualizeSolution(s3, k3);
        System.out.println("Optimal: " + solution.characterReplacement_Optimal(s3, k3));
        System.out.println("Optimized Window: " + solution.characterReplacement_OptimizedWindow(s3, k3));
        System.out.println("Try Each Char: " + solution.characterReplacement_TryEachChar(s3, k3));
        System.out.println();
        
        // Test Case 4: k = 0
        String s4 = "AABBCC";
        int k4 = 0;
        System.out.println("=== Test Case 4 (k=0) ===");
        solution.visualizeSolution(s4, k4);
        System.out.println("Optimal: " + solution.characterReplacement_Optimal(s4, k4));
        System.out.println("Optimized Window: " + solution.characterReplacement_OptimizedWindow(s4, k4));
        System.out.println("Try Each Char: " + solution.characterReplacement_TryEachChar(s4, k4));
        System.out.println();
        
        // Test Case 5: Complex pattern
        String s5 = "ABCABCABC";
        int k5 = 2;
        System.out.println("=== Test Case 5 (Complex) ===");
        solution.visualizeSolution(s5, k5);
        System.out.println("Optimal: " + solution.characterReplacement_Optimal(s5, k5));
        System.out.println("Optimized Window: " + solution.characterReplacement_OptimizedWindow(s5, k5));
        System.out.println("Try Each Char: " + solution.characterReplacement_TryEachChar(s5, k5));
        System.out.println();
        
        // Test Case 6: Large k
        String s6 = "ABCD";
        int k6 = 3;
        System.out.println("=== Test Case 6 (Large k) ===");
        solution.visualizeSolution(s6, k6);
        System.out.println("Optimal: " + solution.characterReplacement_Optimal(s6, k6));
        System.out.println("Optimized Window: " + solution.characterReplacement_OptimizedWindow(s6, k6));
        System.out.println("Try Each Char: " + solution.characterReplacement_TryEachChar(s6, k6));
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * COMPREHENSIVE ANALYSIS - LONGEST REPEATING CHARACTER REPLACEMENT
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * 1. PROBLEM ESSENCE:
 *    ┌─────────────────────────────────────────────────────────────────────┐
 *    │ Find longest substring where after ≤ k replacements,                │
 *    │ all characters are the same                                          │
 *    └─────────────────────────────────────────────────────────────────────┘
 *    
 *    Key insight: In any valid window, we keep the most frequent character
 *    and replace all others. So we need: window_size - max_freq <= k
 * 
 * 2. THE SLIDING WINDOW MAGIC:
 *    
 *    Valid Window Condition:
 *    ┌─────────────────────────────────────────────────────────────────────┐
 *    │  (right - left + 1) - maxFrequency <= k                             │
 *    │   └──────┬──────┘   └──────┬──────┘    └┬┘                          │
 *    │     window size      chars to keep    allowed                        │
 *    │                                        changes                        │
 *    └─────────────────────────────────────────────────────────────────────┘
 *    
 *    Example: "AABBA" with k=1
 *    - Window size = 5
 *    - Max frequency (A appears 3 times) = 3
 *    - Need to replace = 5 - 3 = 2 characters
 *    - But k = 1, so invalid! (2 > 1)
 *    
 *    Example: "AABBA" first 4 chars with k=1
 *    - Window "AABB" size = 4
 *    - Max frequency = 2 (either A or B)
 *    - Need to replace = 4 - 2 = 2 characters
 *    - Still invalid! (2 > 1)
 *    
 *    Example: "AAB" with k=1
 *    - Window size = 3
 *    - Max frequency (A appears 2 times) = 2
 *    - Need to replace = 3 - 2 = 1 character
 *    - Valid! (1 <= 1) → Can make "AAA" or "BBB"
 * 
 * 3. WHY STALE maxFreq WORKS (Approach 2):
 *    
 *    This is the MOST CONFUSING part - let me explain clearly:
 *    
 *    Scenario:
 *    ┌─────────────────────────────────────────────────────────────────────┐
 *    │ Current: valid window size 10, maxFreq=7, k=3                       │
 *    │ Condition: 10 - 7 = 3 <= 3 ✓ Valid                                  │
 *    │                                                                       │
 *    │ Add new char (not the most frequent):                                │
 *    │ New: size=11, maxFreq=7 (unchanged)                                  │
 *    │ Condition: 11 - 7 = 4 > 3 ✗ Invalid                                  │
 *    │                                                                       │
 *    │ We slide window (left++):                                            │
 *    │ New: size=10, maxFreq=7 (still stale!)                               │
 *    │ Condition: 10 - 7 = 3 <= 3 ✓ "Valid"                                 │
 *    └─────────────────────────────────────────────────────────────────────┘
 *    
 *    But wait! The actual maxFreq in new window might be 6, not 7!
 *    So real condition is: 10 - 6 = 4 > 3, which is INVALID!
 *    
 *    Why is this OK?
 *    → We don't care! We already found a size-10 window.
 *    → We only want to find LARGER valid windows.
 *    → If this window is truly invalid (with accurate maxFreq),
 *      it won't grow larger than size 10 anyway.
 *    → We'll only expand when we get a char that increases maxFreq,
 *      making the window valid again at size 11+.
 *    
 *    The key: maxFreq being stale (overestimate) means we're more
 *    "conservative" about growing. We only grow when we're SURE
 *    the new window is better than anything we've seen.
 * 
 * 4. APPROACH COMPARISON:
 *    
 *    ┌──────────────┬───────────┬─────────┬────────────────────────────┐
 *    │ Approach     │ Time      │ Space   │ Best For                   │
 *    ├──────────────┼───────────┼─────────┼────────────────────────────┤
 *    │ Optimal      │ O(n)      │ O(1)    │ Interviews (clear logic)   │
 *    │ Optimized    │ O(n)      │ O(1)    │ Production (cleanest)      │
 *    │ Accurate Freq│ O(26n)=O(n)│ O(1)   │ Learning (most intuitive)  │
 *    │ Try Each Char│ O(26n)=O(n)│ O(1)   │ Alternative thinking       │
 *    │ Brute Force  │ O(n²)     │ O(1)    │ Understanding only         │
 *    └──────────────┴───────────┴─────────┴────────────────────────────┘
 * 
 * 5. PATTERN FAMILY - All These Problems Use SAME Template:
 *    
 *    ┌─────────────────────────────────────────────────────────────────────┐
 *    │ • Max Consecutive Ones III (replace k zeros)                        │
 *    │ • Longest Subarray After Deleting One (k=1 zero)                    │
 *    │ • Fruit Into Baskets (at most 2 distinct)                           │
 *    │ • THIS PROBLEM (at most k replacements)                             │
 *    │ • Longest Substring with At Most K Distinct Characters              │
 *    └─────────────────────────────────────────────────────────────────────┘
 *    
 *    Universal Template:
 *    ```
 *    left = 0
 *    for right in range(n):
 *        add element at right to window
 *        update window properties
 *        
 *        while window is invalid:
 *            remove element at left from window
 *            left++
 *            update window properties
 *        
 *        update max result
 *    ```
 * 
 * 6. COMMON MISTAKES:
 *    ✗ Forgetting that we want ALL chars the same after replacement
 *    ✗ Not tracking the most frequent character
 *    ✗ Trying to track which specific chars to replace (not needed!)
 *    ✗ Confusing "at most k replacements" with "exactly k replacements"
 *    ✗ Off-by-one errors in window size calculation
 * 
 * 7. INTERVIEW STRATEGY:
 *    
 *    Step 1: Recognize it's a sliding window problem
 *            "longest substring" + constraint → sliding window
 *    
 *    Step 2: Define the window validity condition
 *            Valid when: window_size - max_frequency <= k
 *    
 *    Step 3: Explain the intuition
 *            "We keep the most frequent character and replace others"
 *    
 *    Step 4: Code Approach 1 (Optimal with accurate checking)
 *    
 *    Step 5: If time allows, explain Approach 2 optimization
 *            "We don't need to recalculate maxFreq..."
 *    
 *    Step 6: Complexity analysis
 *            Time: O(n), Space: O(26) = O(1)
 * 
 * 8. EDGE CASES TO TEST:
 *    • All same character: "AAAA", k=2 → 4
 *    • k = 0: Must find longest repeating substring
 *    • k >= length: Entire string can be uniform → length
 *    • Empty string: → 0
 *    • Single character: → 1
 *    • No repeating chars: "ABCD", k=1 → 2
 * 
 * 9. OPTIMIZATION INSIGHTS:
 *    
 *    Why not recalculate maxFreq every time?
 *    → It's O(26) operation, making total O(26n)
 *    → Still O(n) but with higher constant factor
 *    → Stale maxFreq approach avoids this
 *    
 *    Why array instead of HashMap?
 *    → Array access is O(1), HashMap is O(1) average
 *    → Array has better cache locality
 *    → For fixed small set (26 chars), array is faster
 * 
 * 10. RELATED VARIATIONS:
 *     
 *     Harder: "Longest Substring with At Most K Distinct Characters"
 *             Similar but tracks distinct count, not frequency
 *     
 *     Easier: "Max Consecutive Ones II" (k=1)
 *             Simpler case of this problem
 *     
 *     Different: "Minimum Window Substring"
 *                Uses similar template but different validity check
 */
