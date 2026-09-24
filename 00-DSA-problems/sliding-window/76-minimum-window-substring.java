import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
 * Given two strings s and t of lengths m and n respectively, return the minimum
 * window substring of s such that every character in t (including duplicates)
 * is included in the window. If there is no such substring, return the empty
 * string "".
 * 
 * The testcases will be generated such that the answer is unique.
 * 
 * Example 1:
 * Input: s = "ADOBECODEBANC", t = "ABC"
 * Output: "BANC"
 * Explanation: The minimum window substring "BANC" includes 'A', 'B', and 'C'
 * from string t.
 * 
 * Example 2:
 * Input: s = "a", t = "a"
 * Output: "a"
 * Explanation: The entire string s is the minimum window.
 * 
 * Example 3:
 * Input: s = "a", t = "aa"
 * Output: ""
 * Explanation: Both 'a's from t must be included in the window.
 * Since the largest window of s only has one 'a', return empty string.
 */

class MinimumWindowSubstring {

    /**
     * Optimized Sliding Window Solution
     * Time Complexity: O(|s| + |t|)
     * Space Complexity: O(|s| + |t|)
     */
    public String minWindow(String s, String t) {
        if (s == null || t == null || s.length() < t.length()) {
            return "";
        }

        // Count characters in t
        Map<Character, Integer> tCount = new HashMap<>();
        for (char c : t.toCharArray()) {
            tCount.put(c, tCount.getOrDefault(c, 0) + 1);
        }

        int left = 0, right = 0;
        int minLen = Integer.MAX_VALUE;
        int minStart = 0;
        int formed = 0; // Number of unique characters in current window with desired frequency
        int required = tCount.size(); // Number of unique characters in t

        // Window character count
        Map<Character, Integer> windowCount = new HashMap<>();

        while (right < s.length()) {
            // Expand window by including character at right
            char rightChar = s.charAt(right);
            windowCount.put(rightChar, windowCount.getOrDefault(rightChar, 0) + 1);

            // Check if current character's frequency matches desired frequency in t
            if (tCount.containsKey(rightChar) &&
                    windowCount.get(rightChar).intValue() == tCount.get(rightChar).intValue()) {
                formed++;
            }

            // Try to contract window from left
            while (left <= right && formed == required) {
                // Update minimum window if current is smaller
                if (right - left + 1 < minLen) {
                    minLen = right - left + 1;
                    minStart = left;
                }

                // Remove leftmost character from window
                char leftChar = s.charAt(left);
                windowCount.put(leftChar, windowCount.get(leftChar) - 1);

                if (tCount.containsKey(leftChar) &&
                        windowCount.get(leftChar).intValue() < tCount.get(leftChar).intValue()) {
                    formed--;
                }

                left++;
            }

            right++;
        }

        return minLen == Integer.MAX_VALUE ? "" : s.substring(minStart, minStart + minLen);
    }

    /**
     * Alternative Solution using Array for ASCII characters (more memory efficient
     * for ASCII)
     * Time Complexity: O(|s| + |t|)
     * Space Complexity: O(1) - fixed size arrays
     */
    public String minWindowArray(String s, String t) {
        if (s == null || t == null || s.length() < t.length()) {
            return "";
        }

        // Count characters in t (assuming ASCII)
        int[] tCount = new int[128];
        int uniqueChars = 0;

        for (char c : t.toCharArray()) {
            if (tCount[c] == 0) {
                uniqueChars++;
            }
            tCount[c]++;
        }

        int left = 0, right = 0;
        int minLen = Integer.MAX_VALUE;
        int minStart = 0;
        int formed = 0;
        int[] windowCount = new int[128];

        while (right < s.length()) {
            char rightChar = s.charAt(right);
            windowCount[rightChar]++;

            if (tCount[rightChar] > 0 && windowCount[rightChar] == tCount[rightChar]) {
                formed++;
            }

            while (left <= right && formed == uniqueChars) {
                if (right - left + 1 < minLen) {
                    minLen = right - left + 1;
                    minStart = left;
                }

                char leftChar = s.charAt(left);
                windowCount[leftChar]--;

                if (tCount[leftChar] > 0 && windowCount[leftChar] < tCount[leftChar]) {
                    formed--;
                }

                left++;
            }

            right++;
        }

        return minLen == Integer.MAX_VALUE ? "" : s.substring(minStart, minStart + minLen);
    }

    /**
     * Brute Force Solution (for comparison - not recommended for large inputs)
     * Time Complexity: O(|s|^2 * |t|)
     * Space Complexity: O(|t|)
     */
    public String minWindowBruteForce(String s, String t) {
        if (s == null || t == null || s.length() < t.length()) {
            return "";
        }

        String minWindow = "";
        int minLen = Integer.MAX_VALUE;

        for (int i = 0; i < s.length(); i++) {
            for (int j = i + t.length(); j <= s.length(); j++) {
                String window = s.substring(i, j);
                if (containsAll(window, t) && window.length() < minLen) {
                    minLen = window.length();
                    minWindow = window;
                }
            }
        }

        return minWindow;
    }

    private boolean containsAll(String window, String t) {
        Map<Character, Integer> tCount = new HashMap<>();
        for (char c : t.toCharArray()) {
            tCount.put(c, tCount.getOrDefault(c, 0) + 1);
        }

        for (char c : window.toCharArray()) {
            if (tCount.containsKey(c)) {
                tCount.put(c, tCount.get(c) - 1);
                if (tCount.get(c) == 0) {
                    tCount.remove(c);
                }
            }
        }

        return tCount.isEmpty();
    }

    // Test cases
    public static void main(String[] args) {
        MinimumWindowSubstring solution = new MinimumWindowSubstring();

        // Test case 1
        String s1 = "ADOBECODEBANC";
        String t1 = "ABC";
        System.out.println("Input: s = \"" + s1 + "\", t = \"" + t1 + "\"");
        System.out.println("Output: \"" + solution.minWindow(s1, t1) + "\"");
        System.out.println("Expected: \"BANC\"\n");

        // Test case 2
        String s2 = "a";
        String t2 = "a";
        System.out.println("Input: s = \"" + s2 + "\", t = \"" + t2 + "\"");
        System.out.println("Output: \"" + solution.minWindow(s2, t2) + "\"");
        System.out.println("Expected: \"a\"\n");

        // Test case 3
        String s3 = "a";
        String t3 = "aa";
        System.out.println("Input: s = \"" + s3 + "\", t = \"" + t3 + "\"");
        System.out.println("Output: \"" + solution.minWindow(s3, t3) + "\"");
        System.out.println("Expected: \"\"\n");

        // Additional test case
        String s4 = "ADOBECODEBANC";
        String t4 = "AABC";
        System.out.println("Input: s = \"" + s4 + "\", t = \"" + t4 + "\"");
        System.out.println("Output: \"" + solution.minWindow(s4, t4) + "\"");
        System.out.println("Expected: \"ADOBEC\" or similar valid window");
    }

}

/**
 * MINIMUM WINDOW SUBSTRING - COMPREHENSIVE GUIDE
 * 
 * Problem: Find the minimum window substring of s that contains all characters from t
 * (including duplicates).
 * 
 * DIFFICULTY: Hard
 * TIME COMPLEXITY: O(m + n) where m = length of s, n = length of t
 * SPACE COMPLEXITY: O(k) where k = number of unique characters
 * 
 * KEY PATTERNS & CONCEPTS:
 * 1. Sliding Window Pattern (Two Pointers)
 * 2. HashMap/Array for frequency counting
 * 3. Contract and Expand technique
 * 4. Counter optimization
 * 
 * COMMON INTERVIEW FOLLOW-UPS:
 * - What if we need to find all minimum windows?
 * - What if we need to find the longest window?
 * - What if characters can be used multiple times?
 * - How to handle Unicode characters?
 * 
 * EDGE CASES TO CONSIDER:
 * 1. t is longer than s → return ""
 * 2. t contains characters not in s → return ""
 * 3. s and t are equal → return s
 * 4. t contains duplicate characters
 * 5. Empty strings
 * 6. Single character strings
 */
class MinimumWindowSubstring2 {

    // ========================================================================
    // APPROACH 1: OPTIMIZED SLIDING WINDOW WITH HASHMAP (MOST RECOMMENDED)
    // ========================================================================
    /**
     * This is the BEST approach for interviews - clean, efficient, and handles all cases.
     * 
     * ALGORITHM:
     * 1. Build frequency map of characters in t
     * 2. Use two pointers (left, right) to create a sliding window
     * 3. Expand window by moving right pointer
     * 4. When window is valid (contains all chars), try to contract from left
     * 5. Track the minimum valid window found
     * 
     * TIME: O(m + n) - each character visited at most twice
     * SPACE: O(k) - k unique characters in t
     * 
     * INTERVIEW TIPS:
     * - Start by explaining the sliding window concept
     * - Draw a diagram showing how window expands and contracts
     * - Mention the "formed" counter optimization
     * - Discuss why we use HashMap instead of array (Unicode support)
     */
    public String minWindowOptimized(String s, String t) {
        // Edge case: if t is longer than s, no solution exists
        if (s.length() < t.length()) {
            return "";
        }
        
        // Step 1: Build frequency map for target string t
        // This tells us how many of each character we need
        Map<Character, Integer> targetMap = new HashMap<>();
        for (char c : t.toCharArray()) {
            targetMap.put(c, targetMap.getOrDefault(c, 0) + 1);
        }
        
        // Step 2: Initialize sliding window variables
        int left = 0, right = 0;
        int required = targetMap.size(); // Number of unique characters in t
        int formed = 0; // Number of unique chars in current window with desired frequency
        
        // Window character frequency map
        Map<Character, Integer> windowMap = new HashMap<>();
        
        // Result: [window length, left, right]
        // Using array to avoid creating new objects
        int[] result = {-1, 0, 0}; // -1 indicates no valid window found yet
        
        // Step 3: Expand window by moving right pointer
        while (right < s.length()) {
            // Add character from right to the window
            char rightChar = s.charAt(right);
            windowMap.put(rightChar, windowMap.getOrDefault(rightChar, 0) + 1);
            
            // Check if frequency of current character matches the desired count in t
            // IMPORTANT: Use Integer.equals() not == to compare Integer objects
            if (targetMap.containsKey(rightChar) && 
                windowMap.get(rightChar).intValue() == targetMap.get(rightChar).intValue()) {
                formed++;
            }
            
            // Step 4: Contract window from left while it's valid
            // Try to minimize the window while keeping it valid
            while (left <= right && formed == required) {
                // Update result if this window is smaller
                if (result[0] == -1 || right - left + 1 < result[0]) {
                    result[0] = right - left + 1;
                    result[1] = left;
                    result[2] = right;
                }
                
                // Remove character from left of window
                char leftChar = s.charAt(left);
                windowMap.put(leftChar, windowMap.get(leftChar) - 1);
                
                // Check if removing this character breaks the validity
                if (targetMap.containsKey(leftChar) && 
                    windowMap.get(leftChar).intValue() < targetMap.get(leftChar).intValue()) {
                    formed--;
                }
                
                // Move left pointer forward to contract window
                left++;
            }
            
            // Expand window by moving right pointer
            right++;
        }
        
        // Return the minimum window or empty string if none found
        return result[0] == -1 ? "" : s.substring(result[1], result[2] + 1);
    }

    // ========================================================================
    // APPROACH 2: SLIDING WINDOW WITH ARRAY (FASTER FOR ASCII)
    // ========================================================================
    /**
     * Optimized for ASCII characters only.
     * Uses array instead of HashMap for O(1) access.
     * 
     * WHEN TO USE THIS:
     * - Interviewer confirms ASCII-only input
     * - Performance is critical
     * - Want to show optimization skills
     * 
     * TIME: O(m + n)
     * SPACE: O(1) - fixed size array of 128
     * 
     * PROS:
     * + Faster than HashMap (no hashing overhead)
     * + O(1) lookup time guaranteed
     * 
     * CONS:
     * - Only works for ASCII (or extended ASCII with 256)
     * - Uses more space if character set is small
     */
    public String minWindowArray(String s, String t) {
        if (s.length() < t.length()) {
            return "";
        }
        
        // Frequency arrays for ASCII characters (0-127)
        int[] targetFreq = new int[128];
        int[] windowFreq = new int[128];
        
        // Count required characters
        int required = 0;
        for (char c : t.toCharArray()) {
            if (targetFreq[c] == 0) {
                required++; // Count unique characters
            }
            targetFreq[c]++;
        }
        
        int left = 0, right = 0;
        int formed = 0;
        int minLen = Integer.MAX_VALUE;
        int minLeft = 0;
        
        while (right < s.length()) {
            // Expand window
            char rightChar = s.charAt(right);
            windowFreq[rightChar]++;
            
            // Check if this character's count matches requirement
            if (targetFreq[rightChar] > 0 && 
                windowFreq[rightChar] == targetFreq[rightChar]) {
                formed++;
            }
            
            // Contract window
            while (left <= right && formed == required) {
                // Update minimum window
                if (right - left + 1 < minLen) {
                    minLen = right - left + 1;
                    minLeft = left;
                }
                
                // Remove from left
                char leftChar = s.charAt(left);
                windowFreq[leftChar]--;
                
                if (targetFreq[leftChar] > 0 && 
                    windowFreq[leftChar] < targetFreq[leftChar]) {
                    formed--;
                }
                
                left++;
            }
            
            right++;
        }
        
        return minLen == Integer.MAX_VALUE ? "" : s.substring(minLeft, minLeft + minLen);
    }

    // ========================================================================
    // APPROACH 3: SLIDING WINDOW WITH FILTERED STRING (OPTIMIZED FOR SPARSE)
    // ========================================================================
    /**
     * Optimization when t has very few unique characters compared to s.
     * Pre-filter s to only include characters that are in t.
     * 
     * WHEN TO USE:
     * - s is very large and t has few characters
     * - Many characters in s are irrelevant
     * 
     * EXAMPLE: s = "ADOBECODEBANC", t = "ABC"
     * Filtered: [(0,'A'), (3,'B'), (5,'C'), (9,'B'), (10,'A'), (12,'C')]
     * 
     * TIME: O(m + n) but with better constants
     * SPACE: O(m) in worst case for filtered list
     * 
     * INTERVIEW TIP: Mention this as an optimization for follow-up discussion
     */
    public String minWindowFiltered(String s, String t) {
        if (s.length() < t.length()) {
            return "";
        }
        
        // Build target frequency map
        Map<Character, Integer> targetMap = new HashMap<>();
        for (char c : t.toCharArray()) {
            targetMap.put(c, targetMap.getOrDefault(c, 0) + 1);
        }
        
        // Filter s: only keep characters that are in t
        // Store as (index, character) pairs
        List<Pair> filtered = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (targetMap.containsKey(c)) {
                filtered.add(new Pair(i, c));
            }
        }
        
        // If no characters from t found in s
        if (filtered.isEmpty()) {
            return "";
        }
        
        // Apply sliding window on filtered list
        int left = 0, right = 0;
        int required = targetMap.size();
        int formed = 0;
        Map<Character, Integer> windowMap = new HashMap<>();
        
        int[] result = {-1, 0, 0};
        
        while (right < filtered.size()) {
            char c = filtered.get(right).ch;
            windowMap.put(c, windowMap.getOrDefault(c, 0) + 1);
            
            if (windowMap.get(c).intValue() == targetMap.get(c).intValue()) {
                formed++;
            }
            
            while (left <= right && formed == required) {
                // Get actual indices from original string
                int start = filtered.get(left).index;
                int end = filtered.get(right).index;
                
                if (result[0] == -1 || end - start + 1 < result[0]) {
                    result[0] = end - start + 1;
                    result[1] = start;
                    result[2] = end;
                }
                
                char leftChar = filtered.get(left).ch;
                windowMap.put(leftChar, windowMap.get(leftChar) - 1);
                
                if (windowMap.get(leftChar).intValue() < targetMap.get(leftChar).intValue()) {
                    formed--;
                }
                
                left++;
            }
            
            right++;
        }
        
        return result[0] == -1 ? "" : s.substring(result[1], result[2] + 1);
    }
    
    // Helper class for filtered approach
    static class Pair {
        int index;
        char ch;
        Pair(int index, char ch) {
            this.index = index;
            this.ch = ch;
        }
    }

    // ========================================================================
    // APPROACH 4: BRUTE FORCE (FOR UNDERSTANDING - NOT FOR INTERVIEWS)
    // ========================================================================
    /**
     * Checks all possible substrings.
     * 
     * TIME: O(m² * n) - m² substrings, each takes O(n) to validate
     * SPACE: O(n)
     * 
     * USE ONLY TO:
     * - Explain why sliding window is better
     * - Understand the problem initially
     * 
     * NEVER IMPLEMENT THIS IN AN ACTUAL INTERVIEW unless asked to start simple.
     */
    public String minWindowBruteForce(String s, String t) {
        if (s.length() < t.length()) {
            return "";
        }
        
        String minWindow = "";
        int minLen = Integer.MAX_VALUE;
        
        // Try all possible substrings
        for (int i = 0; i < s.length(); i++) {
            for (int j = i + t.length(); j <= s.length(); j++) {
                String window = s.substring(i, j);
                
                // Check if this window contains all characters from t
                if (containsAll(window, t)) {
                    if (window.length() < minLen) {
                        minLen = window.length();
                        minWindow = window;
                    }
                }
            }
        }
        
        return minWindow;
    }
    
    // Helper method for brute force
    private boolean containsAll(String window, String t) {
        Map<Character, Integer> tMap = new HashMap<>();
        for (char c : t.toCharArray()) {
            tMap.put(c, tMap.getOrDefault(c, 0) + 1);
        }
        
        for (char c : window.toCharArray()) {
            if (tMap.containsKey(c)) {
                tMap.put(c, tMap.get(c) - 1);
                if (tMap.get(c) == 0) {
                    tMap.remove(c);
                }
            }
        }
        
        return tMap.isEmpty();
    }

    // ========================================================================
    // COMMON MISTAKES & DEBUGGING TIPS
    // ========================================================================
    /**
     * COMMON MISTAKES:
     * 
     * 1. WRONG: Using == to compare Integer objects
     *    RIGHT: Use .equals() or .intValue() == 
     *    Example: windowMap.get(c).intValue() == targetMap.get(c).intValue()
     * 
     * 2. WRONG: Not checking if character exists in target map before incrementing formed
     *    RIGHT: Always check: if (targetMap.containsKey(c) && ...)
     * 
     * 3. WRONG: Forgetting that formed tracks UNIQUE characters, not total count
     *    RIGHT: Only increment formed when exact frequency is matched
     * 
     * 4. WRONG: Moving left pointer before checking window validity
     *    RIGHT: Check validity, update result, THEN move left
     * 
     * 5. WRONG: Not handling the case where no valid window exists
     *    RIGHT: Initialize result with sentinel value (like -1)
     * 
     * 6. WRONG: Off-by-one errors in substring
     *    RIGHT: s.substring(left, right + 1) not s.substring(left, right)
     * 
     * 7. WRONG: Not decrementing formed when removing character from left
     *    RIGHT: Check if count drops below required before moving left
     * 
     * 8. WRONG: Creating new HashMap for each window
     *    RIGHT: Reuse single HashMap and update counts
     */

    // ========================================================================
    // INTERVIEW COMMUNICATION TIPS
    // ========================================================================
    /**
     * HOW TO APPROACH IN AN INTERVIEW:
     * 
     * 1. CLARIFY REQUIREMENTS (2 minutes):
     *    - "Can t contain duplicate characters?" (YES)
     *    - "Should I consider case sensitivity?" (Usually YES)
     *    - "What character set? ASCII or Unicode?" (Affects implementation)
     *    - "What if multiple minimum windows exist?" (Return any one)
     * 
     * 2. DISCUSS APPROACH (3 minutes):
     *    - Start with brute force idea (all substrings)
     *    - Identify inefficiency: checking same characters multiple times
     *    - Introduce sliding window: expand when invalid, contract when valid
     *    - Explain "formed" counter optimization
     * 
     * 3. DISCUSS COMPLEXITY (1 minute):
     *    - Time: O(m + n) - each character visited at most twice
     *    - Space: O(k) where k = unique characters in t
     * 
     * 4. CODE (15 minutes):
     *    - Write clean code with meaningful variable names
     *    - Add comments for complex logic
     *    - Handle edge cases
     * 
     * 5. TEST (5 minutes):
     *    - Test with provided examples
     *    - Test edge cases: empty string, no solution, single character
     *    - Walk through algorithm with a simple example
     * 
     * 6. OPTIMIZE (if time):
     *    - Discuss array vs HashMap tradeoff
     *    - Mention filtered string optimization for sparse cases
     */

    // ========================================================================
    // VARIATIONS & RELATED PROBLEMS
    // ========================================================================
    /**
     * VARIATION 1: Find ALL minimum windows
     * Solution: Store all results with minimum length instead of just one
     * 
     * VARIATION 2: Find LONGEST window containing all characters
     * Solution: Maximize instead of minimize, only contract when necessary
     * 
     * VARIATION 3: Characters can be reused unlimited times
     * Solution: Only need to check presence, not count
     * 
     * VARIATION 4: Return count of minimum windows
     * Solution: Count instead of storing actual substrings
     * 
     * VARIATION 5: Case-insensitive matching
     * Solution: Convert both strings to lowercase first
     * 
     * RELATED PROBLEMS:
     * - Longest Substring Without Repeating Characters
     * - Longest Substring with At Most K Distinct Characters
     * - Substring with Concatenation of All Words
     * - Find All Anagrams in a String
     * - Permutation in String
     */

    // ========================================================================
    // TEST CASES
    // ========================================================================
    public static void main(String[] args) {
        MinimumWindowSubstring2 solution = new MinimumWindowSubstring2();
        
        // Test Case 1: Standard example
        String s1 = "ADOBECODEBANC";
        String t1 = "ABC";
        System.out.println("Test 1: " + solution.minWindowOptimized(s1, t1)); 
        // Expected: "BANC"
        
        // Test Case 2: Single character
        String s2 = "a";
        String t2 = "a";
        System.out.println("Test 2: " + solution.minWindowOptimized(s2, t2)); 
        // Expected: "a"
        
        // Test Case 3: No valid window
        String s3 = "a";
        String t3 = "aa";
        System.out.println("Test 3: " + solution.minWindowOptimized(s3, t3)); 
        // Expected: ""
        
        // Test Case 4: Duplicate characters in t
        String s4 = "ADOBECODEBANC";
        String t4 = "AABC";
        System.out.println("Test 4: " + solution.minWindowOptimized(s4, t4)); 
        // Expected: "ADOBEC" or similar
        
        // Test Case 5: Entire string is minimum window
        String s5 = "ABC";
        String t5 = "ABC";
        System.out.println("Test 5: " + solution.minWindowOptimized(s5, t5)); 
        // Expected: "ABC"
        
        // Test Case 6: t longer than s
        String s6 = "A";
        String t6 = "ABC";
        System.out.println("Test 6: " + solution.minWindowOptimized(s6, t6)); 
        // Expected: ""
        
        // Test Case 7: Characters at edges
        String s7 = "BANC";
        String t7 = "ABC";
        System.out.println("Test 7: " + solution.minWindowOptimized(s7, t7)); 
        // Expected: "BANC"
        
        // Test Case 8: Multiple same characters
        String s8 = "aaaaaaaaaaaabbbbbcdd";
        String t8 = "abcdd";
        System.out.println("Test 8: " + solution.minWindowOptimized(s8, t8)); 
        // Expected: "abbbbbcdd"
        
        // Compare all approaches
        System.out.println("\n=== Comparing All Approaches ===");
        System.out.println("Optimized: " + solution.minWindowOptimized(s1, t1));
        System.out.println("Array: " + solution.minWindowArray(s1, t1));
        System.out.println("Filtered: " + solution.minWindowFiltered(s1, t1));
        System.out.println("Brute Force: " + solution.minWindowBruteForce(s1, t1));
    }
}

/**
 * ============================================================================
 * FINAL INTERVIEW CHECKLIST
 * ============================================================================
 * 
 * BEFORE YOU START CODING:
 * □ Clarified all requirements and constraints
 * □ Discussed approach and got interviewer buy-in
 * □ Mentioned time and space complexity
 * □ Asked about character set (ASCII vs Unicode)
 * 
 * WHILE CODING:
 * □ Used meaningful variable names (left, right, formed, required)
 * □ Added comments for non-obvious logic
 * □ Handled edge cases (empty strings, no solution)
 * □ Used correct comparison for Integer objects
 * □ Properly initialized result tracking
 * 
 * AFTER CODING:
 * □ Walked through code with an example
 * □ Tested edge cases
 * □ Discussed time/space complexity
 * □ Mentioned potential optimizations
 * □ Asked about follow-up questions or variations
 * 
 * KEY TALKING POINTS:
 * ✓ "Sliding window is ideal because we can avoid redundant checks"
 * ✓ "The formed counter helps us know when window is valid in O(1)"
 * ✓ "We expand to find valid window, contract to minimize it"
 * ✓ "For ASCII-only, array would be faster than HashMap"
 * ✓ "If t has few chars, we could filter s first for optimization"
 * 
 * ============================================================================
 * COMPLEXITY ANALYSIS SUMMARY
 * ============================================================================
 * 
 * Approach 1 (Optimized HashMap):
 *   Time: O(m + n) where m = |s|, n = |t|
 *   Space: O(k) where k = unique chars in t
 *   Best for: General case, Unicode support
 * 
 * Approach 2 (Array):
 *   Time: O(m + n)
 *   Space: O(1) - fixed array size
 *   Best for: ASCII-only, performance critical
 * 
 * Approach 3 (Filtered):
 *   Time: O(m + n) with better constants
 *   Space: O(m) worst case
 *   Best for: Sparse characters, large s
 * 
 * Approach 4 (Brute Force):
 *   Time: O(m² * n)
 *   Space: O(n)
 *   Best for: Learning only, never in interviews
 * 
 * ============================================================================
 */

