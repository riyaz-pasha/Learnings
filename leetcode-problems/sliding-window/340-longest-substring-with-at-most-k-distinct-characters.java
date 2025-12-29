/**
 * LEETCODE 340: LONGEST SUBSTRING WITH AT MOST K DISTINCT CHARACTERS
 * 
 * PROBLEM STATEMENT:
 * Given a string s and an integer k, return the length of the longest substring 
 * that contains at most k distinct characters.
 * 
 * Examples:
 * Input: s = "eceba", k = 2
 * Output: 3
 * Explanation: The substring is "ece" with length 3 (2 distinct chars: 'e' and 'c')
 * 
 * Input: s = "aa", k = 1
 * Output: 2
 * Explanation: The substring is "aa" with length 2 (1 distinct char: 'a')
 * 
 * KEY INSIGHTS:
 * 1. This is a CLASSIC SLIDING WINDOW problem
 * 2. We need to find the LONGEST valid window (substring)
 * 3. Window is valid when: distinct_chars <= k
 * 4. Window is invalid when: distinct_chars > k
 * 5. When invalid, shrink from left until valid again
 * 
 * SLIDING WINDOW PATTERN RECOGNITION:
 * - "Longest/Shortest substring with condition" -> Sliding Window
 * - "At most K" constraint -> HashMap to track frequency
 * - Need to expand right, shrink left when constraint violated
 * 
 * COMPLEXITY ANALYSIS (Best Approach):
 * Time: O(n) - each character visited at most twice (once by right, once by left)
 * Space: O(k) - HashMap stores at most k distinct characters
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class LongestSubstringKDistinct {
    
    /**
     * ============================================================================
     * APPROACH 1: SLIDING WINDOW WITH HASHMAP (OPTIMAL - BEST FOR INTERVIEWS)
     * ============================================================================
     * 
     * ALGORITHM:
     * 1. Use two pointers: left and right (both start at 0)
     * 2. Use HashMap to track character frequencies in current window
     * 3. Expand window by moving right pointer
     * 4. When distinct chars > k, shrink from left
     * 5. Track maximum length seen
     * 
     * DETAILED WALKTHROUGH:
     * s = "eceba", k = 2
     * 
     * Step 1: right=0, window="e", map={e:1}, distinct=1, max=1
     * Step 2: right=1, window="ec", map={e:1,c:1}, distinct=2, max=2
     * Step 3: right=2, window="ece", map={e:2,c:1}, distinct=2, max=3
     * Step 4: right=3, window="eceb", map={e:2,c:1,b:1}, distinct=3 > k!
     *         Shrink: left=1, window="ceb", map={e:1,c:1,b:1}, distinct=3
     *         Shrink: left=2, window="eb", map={e:1,b:1}, distinct=2
     * Step 5: right=4, window="eba", map={e:1,b:1,a:1}, distinct=3 > k!
     *         Shrink: left=3, window="ba", map={b:1,a:1}, distinct=2
     * 
     * Result: max = 3
     * 
     * WHY HASHMAP?
     * - Need to track character frequencies (not just presence)
     * - When shrinking, decrement count
     * - Remove from map when count reaches 0
     * - map.size() gives us distinct character count
     * 
     * EDGE CASES:
     * - k = 0: Return 0 (can't have any characters)
     * - k >= distinct chars in s: Return s.length()
     * - Empty string: Return 0
     * - All same character: Return s.length()
     * 
     * TIME: O(n) - right pointer moves n times, left pointer moves at most n times
     * SPACE: O(min(n, k)) - HashMap stores at most k characters (or all if k > distinct chars)
     */
    public int lengthOfLongestSubstringKDistinct(String s, int k) {
        // Edge case: k = 0 means we can't have any characters
        if (s == null || s.length() == 0 || k == 0) {
            return 0;
        }
        
        int left = 0;
        int maxLength = 0;
        
        // HashMap to store character frequencies in current window
        // Key: character, Value: frequency count
        Map<Character, Integer> charFrequency = new HashMap<>();
        
        // Expand window by moving right pointer
        for (int right = 0; right < s.length(); right++) {
            char rightChar = s.charAt(right);
            
            // Add right character to window
            charFrequency.put(rightChar, charFrequency.getOrDefault(rightChar, 0) + 1);
            
            // Shrink window if we have more than k distinct characters
            // IMPORTANT: Use 'while' not 'if' because we might need multiple shrinks
            while (charFrequency.size() > k) {
                char leftChar = s.charAt(left);
                
                // Remove left character from window
                charFrequency.put(leftChar, charFrequency.get(leftChar) - 1);
                
                // If frequency becomes 0, remove from map entirely
                // This is crucial for accurate distinct character count
                if (charFrequency.get(leftChar) == 0) {
                    charFrequency.remove(leftChar);
                }
                
                left++; // Shrink window from left
            }
            
            // Update max length (current window is valid: <= k distinct chars)
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    /**
     * ============================================================================
     * APPROACH 2: SLIDING WINDOW WITH ARRAY (FOR ASCII STRINGS)
     * ============================================================================
     * 
     * OPTIMIZATION:
     * - If we know the string contains only ASCII characters (0-127 or 0-255)
     * - We can use an array instead of HashMap for better constant factors
     * - Array access is O(1) and faster than HashMap operations
     * 
     * WHEN TO USE:
     * - String contains only lowercase letters (26 chars)
     * - String contains only ASCII characters (128 or 256 chars)
     * - Know the character set in advance
     * 
     * TRADE-OFF:
     * - Faster in practice due to array access
     * - Uses fixed space O(charset_size) instead of O(k)
     * - Less flexible (assumes character range)
     * 
     * TIME: O(n), SPACE: O(1) or O(charset_size) - constant for fixed charset
     */
    public int lengthOfLongestSubstringKDistinct_Array(String s, int k) {
        if (s == null || s.length() == 0 || k == 0) {
            return 0;
        }
        
        int left = 0;
        int maxLength = 0;
        int distinctCount = 0;
        
        // Assume lowercase letters only (a-z)
        // For full ASCII, use int[128] or int[256]
        int[] charCount = new int[26];
        
        for (int right = 0; right < s.length(); right++) {
            char rightChar = s.charAt(right);
            int rightIndex = rightChar - 'a';
            
            // Add right character
            if (charCount[rightIndex] == 0) {
                distinctCount++; // New distinct character
            }
            charCount[rightIndex]++;
            
            // Shrink window if too many distinct characters
            while (distinctCount > k) {
                char leftChar = s.charAt(left);
                int leftIndex = leftChar - 'a';
                
                charCount[leftIndex]--;
                if (charCount[leftIndex] == 0) {
                    distinctCount--; // Lost a distinct character
                }
                
                left++;
            }
            
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    /**
     * ============================================================================
     * APPROACH 3: SLIDING WINDOW WITH LINKEDHASHMAP (OPTIMAL FOR FOLLOW-UP)
     * ============================================================================
     * 
     * ENHANCEMENT:
     * - LinkedHashMap maintains insertion order
     * - Can efficiently remove the "oldest" character when needed
     * - Useful for follow-up: "Return the actual longest substring"
     * 
     * ADVANTAGE:
     * - When shrinking, we know which character entered first
     * - Can optimize removal in some cases
     * - Good for tracking positions
     * 
     * INTERVIEW TIP:
     * - Mention this as an optimization if asked about variations
     * - Shows deeper understanding of data structures
     * 
     * TIME: O(n), SPACE: O(k)
     */
    public int lengthOfLongestSubstringKDistinct_LinkedHashMap(String s, int k) {
        if (s == null || s.length() == 0 || k == 0) {
            return 0;
        }
        
        int left = 0;
        int maxLength = 0;
        
        // LinkedHashMap maintains insertion order
        Map<Character, Integer> charLastPosition = new LinkedHashMap<>();
        
        for (int right = 0; right < s.length(); right++) {
            char rightChar = s.charAt(right);
            
            // Remove and re-insert to update position (moves to end)
            if (charLastPosition.containsKey(rightChar)) {
                charLastPosition.remove(rightChar);
            }
            charLastPosition.put(rightChar, right);
            
            // Shrink if necessary
            if (charLastPosition.size() > k) {
                // Remove the first (oldest) entry
                Map.Entry<Character, Integer> firstEntry = 
                    charLastPosition.entrySet().iterator().next();
                charLastPosition.remove(firstEntry.getKey());
                left = firstEntry.getValue() + 1;
            }
            
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    /**
     * ============================================================================
     * APPROACH 4: BRUTE FORCE (FOR COMPARISON AND UNDERSTANDING)
     * ============================================================================
     * 
     * ALGORITHM:
     * - Check every possible substring
     * - For each substring, count distinct characters
     * - If <= k, update max length
     * 
     * WHY SHOW THIS:
     * - Helps understand the problem
     * - Shows optimization journey
     * - Good baseline for complexity analysis
     * 
     * PROBLEM:
     * - Too slow for large inputs
     * - Redundant computation
     * - Not suitable for interviews (but good to mention)
     * 
     * TIME: O(n²) - two nested loops
     * SPACE: O(k) - for storing distinct characters
     */
    public int lengthOfLongestSubstringKDistinct_BruteForce(String s, int k) {
        if (s == null || s.length() == 0 || k == 0) {
            return 0;
        }
        
        int maxLength = 0;
        
        // Try all possible starting positions
        for (int i = 0; i < s.length(); i++) {
            Set<Character> distinctChars = new HashSet<>();
            
            // Expand from position i
            for (int j = i; j < s.length(); j++) {
                distinctChars.add(s.charAt(j));
                
                // Check if valid
                if (distinctChars.size() <= k) {
                    maxLength = Math.max(maxLength, j - i + 1);
                } else {
                    break; // No point continuing, will only get more distinct chars
                }
            }
        }
        
        return maxLength;
    }
    
    /**
     * ============================================================================
     * APPROACH 5: OPTIMIZED SLIDING WINDOW (ALTERNATIVE SHRINKING STRATEGY)
     * ============================================================================
     * 
     * VARIATION:
     * - Instead of shrinking one character at a time
     * - We can find the optimal left position directly
     * - Useful when k is very small
     * 
     * CONCEPT:
     * - When we exceed k distinct chars, we know we need to remove oldest char type
     * - Track last position of each character
     * - Jump left pointer to remove that character
     * 
     * TIME: O(n), SPACE: O(k)
     */
    public int lengthOfLongestSubstringKDistinct_OptimizedShrink(String s, int k) {
        if (s == null || s.length() == 0 || k == 0) {
            return 0;
        }
        
        int left = 0;
        int maxLength = 0;
        
        // Track last position of each character
        Map<Character, Integer> charLastIndex = new HashMap<>();
        
        for (int right = 0; right < s.length(); right++) {
            char rightChar = s.charAt(right);
            charLastIndex.put(rightChar, right);
            
            // If we exceed k distinct characters
            if (charLastIndex.size() > k) {
                // Find the character with minimum last position
                int minIndex = Collections.min(charLastIndex.values());
                
                // Remove that character from map
                charLastIndex.values().removeIf(val -> val == minIndex);
                
                // Move left pointer past that character
                left = minIndex + 1;
            }
            
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    /**
     * ============================================================================
     * FOLLOW-UP VARIATION: RETURN THE ACTUAL LONGEST SUBSTRING
     * ============================================================================
     * 
     * MODIFICATION:
     * - Track the start and end positions of the longest substring
     * - Return the actual substring instead of just length
     * 
     * INTERVIEW TIP:
     * - If asked this follow-up, the code changes are minimal
     * - Just track maxStart and maxEnd instead of maxLength
     */
    public String getLongestSubstringKDistinct(String s, int k) {
        if (s == null || s.length() == 0 || k == 0) {
            return "";
        }
        
        int left = 0;
        int maxLength = 0;
        int maxStart = 0; // Track start of longest substring
        
        Map<Character, Integer> charFrequency = new HashMap<>();
        
        for (int right = 0; right < s.length(); right++) {
            char rightChar = s.charAt(right);
            charFrequency.put(rightChar, charFrequency.getOrDefault(rightChar, 0) + 1);
            
            while (charFrequency.size() > k) {
                char leftChar = s.charAt(left);
                charFrequency.put(leftChar, charFrequency.get(leftChar) - 1);
                if (charFrequency.get(leftChar) == 0) {
                    charFrequency.remove(leftChar);
                }
                left++;
            }
            
            // Update max length and starting position
            if (right - left + 1 > maxLength) {
                maxLength = right - left + 1;
                maxStart = left;
            }
        }
        
        return s.substring(maxStart, maxStart + maxLength);
    }
    
    /**
     * ============================================================================
     * TEST CASES AND DEMONSTRATIONS
     * ============================================================================
     */
    public static void main(String[] args) {
        LongestSubstringKDistinct solution = new LongestSubstringKDistinct();
        
        System.out.println("=== TEST CASE 1: Basic Example ===");
        String s1 = "eceba";
        int k1 = 2;
        System.out.println("Input: s = \"" + s1 + "\", k = " + k1);
        System.out.println("HashMap Approach: " + solution.lengthOfLongestSubstringKDistinct(s1, k1));
        System.out.println("Array Approach: " + solution.lengthOfLongestSubstringKDistinct_Array(s1, k1));
        System.out.println("LinkedHashMap: " + solution.lengthOfLongestSubstringKDistinct_LinkedHashMap(s1, k1));
        System.out.println("Brute Force: " + solution.lengthOfLongestSubstringKDistinct_BruteForce(s1, k1));
        System.out.println("Actual Substring: \"" + solution.getLongestSubstringKDistinct(s1, k1) + "\"");
        System.out.println("Expected: 3 (substring \"ece\")\n");
        
        System.out.println("=== TEST CASE 2: All Same Character ===");
        String s2 = "aa";
        int k2 = 1;
        System.out.println("Input: s = \"" + s2 + "\", k = " + k2);
        System.out.println("HashMap Approach: " + solution.lengthOfLongestSubstringKDistinct(s2, k2));
        System.out.println("Actual Substring: \"" + solution.getLongestSubstringKDistinct(s2, k2) + "\"");
        System.out.println("Expected: 2 (substring \"aa\")\n");
        
        System.out.println("=== TEST CASE 3: Complex String ===");
        String s3 = "aaabbccd";
        int k3 = 2;
        System.out.println("Input: s = \"" + s3 + "\", k = " + k3);
        System.out.println("HashMap Approach: " + solution.lengthOfLongestSubstringKDistinct(s3, k3));
        System.out.println("Actual Substring: \"" + solution.getLongestSubstringKDistinct(s3, k3) + "\"");
        System.out.println("Expected: 6 (substring \"aaabbb\" or \"bbccdd\" - both have 2 distinct)\n");
        
        System.out.println("=== TEST CASE 4: K = 0 ===");
        String s4 = "abc";
        int k4 = 0;
        System.out.println("Input: s = \"" + s4 + "\", k = " + k4);
        System.out.println("HashMap Approach: " + solution.lengthOfLongestSubstringKDistinct(s4, k4));
        System.out.println("Expected: 0 (can't have any characters)\n");
        
        System.out.println("=== TEST CASE 5: K >= Distinct Characters ===");
        String s5 = "abcde";
        int k5 = 10;
        System.out.println("Input: s = \"" + s5 + "\", k = " + k5);
        System.out.println("HashMap Approach: " + solution.lengthOfLongestSubstringKDistinct(s5, k5));
        System.out.println("Actual Substring: \"" + solution.getLongestSubstringKDistinct(s5, k5) + "\"");
        System.out.println("Expected: 5 (entire string)\n");
        
        System.out.println("=== TEST CASE 6: Single Character ===");
        String s6 = "a";
        int k6 = 1;
        System.out.println("Input: s = \"" + s6 + "\", k = " + k6);
        System.out.println("HashMap Approach: " + solution.lengthOfLongestSubstringKDistinct(s6, k6));
        System.out.println("Expected: 1\n");
        
        System.out.println("=== TEST CASE 7: Repeated Pattern ===");
        String s7 = "abaccc";
        int k7 = 2;
        System.out.println("Input: s = \"" + s7 + "\", k = " + k7);
        System.out.println("HashMap Approach: " + solution.lengthOfLongestSubstringKDistinct(s7, k7));
        System.out.println("Actual Substring: \"" + solution.getLongestSubstringKDistinct(s7, k7) + "\"");
        System.out.println("Expected: 4 (substring \"accc\")\n");
        
        // Performance comparison
        System.out.println("=== PERFORMANCE COMPARISON ===");
        StringBuilder largeSb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            largeSb.append((char)('a' + (i % 26)));
        }
        String largeString = largeSb.toString();
        int kLarge = 5;
        
        long start, end;
        
        start = System.nanoTime();
        int result1 = solution.lengthOfLongestSubstringKDistinct(largeString, kLarge);
        end = System.nanoTime();
        System.out.println("HashMap: " + result1 + " (Time: " + (end - start) / 1000000.0 + " ms)");
        
        start = System.nanoTime();
        int result2 = solution.lengthOfLongestSubstringKDistinct_Array(largeString, kLarge);
        end = System.nanoTime();
        System.out.println("Array: " + result2 + " (Time: " + (end - start) / 1000000.0 + " ms)");
    }
}

/**
 * ============================================================================
 * COMPREHENSIVE ANALYSIS AND INTERVIEW GUIDE
 * ============================================================================
 * 
 * PROBLEM PATTERN RECOGNITION:
 * 
 * This problem belongs to the "SLIDING WINDOW" pattern. Key indicators:
 * - "Longest/Shortest substring"
 * - "At most/At least K" constraint
 * - Need to find optimal contiguous sequence
 * - Can use two pointers (left and right)
 * 
 * SLIDING WINDOW TEMPLATE (MEMORIZE THIS!):
 * 
 * int left = 0, maxLength = 0;
 * Map<Character, Integer> window = new HashMap<>();
 * 
 * for (int right = 0; right < s.length(); right++) {
 *     // 1. Add right element to window
 *     window.put(s.charAt(right), window.getOrDefault(...) + 1);
 *     
 *     // 2. Shrink window while invalid
 *     while (window_is_invalid) {
 *         // Remove left element
 *         // Move left pointer
 *         left++;
 *     }
 *     
 *     // 3. Update result (window is valid here)
 *     maxLength = Math.max(maxLength, right - left + 1);
 * }
 * 
 * APPROACH COMPARISON:
 * 
 * 1. HashMap (Approach 1) - BEST FOR INTERVIEWS
 *    ✓ Most flexible (works with any characters)
 *    ✓ Clean and readable code
 *    ✓ O(n) time, O(k) space
 *    ✓ Standard solution everyone knows
 * 
 * 2. Array (Approach 2) - OPTIMIZATION FOR KNOWN CHARSET
 *    ✓ Faster constant factors
 *    ✓ Better space for small charsets
 *    ✗ Limited to ASCII/fixed charset
 *    + Mention as optimization if asked
 * 
 * 3. LinkedHashMap (Approach 3) - ELEGANT ALTERNATIVE
 *    ✓ Maintains order automatically
 *    ✓ Clean shrinking logic
 *    + Good to mention for bonus points
 * 
 * 4. Brute Force (Approach 4) - BASELINE UNDERSTANDING
 *    ✗ Too slow (O(n²))
 *    + Good to mention to show thought process
 * 
 * COMMON VARIATIONS AND FOLLOW-UPS:
 * 
 * 1. "Return the actual longest substring, not just length"
 *    → Track maxStart position, return s.substring(maxStart, maxStart + maxLength)
 * 
 * 2. "What if we want EXACTLY k distinct characters?"
 *    → Use same approach but only update max when size == k
 * 
 * 3. "What about AT LEAST k distinct characters?"
 *    → Find longest substring with < k distinct, subtract from total length
 * 
 * 4. "What if string is very large and streaming?"
 *    → Sliding window still works! Process character by character
 * 
 * 5. "What if we want all substrings with at most k distinct?"
 *    → Track all valid windows (modifications needed)
 * 
 * EDGE CASES TO DISCUSS:
 * 
 * ✓ k = 0: Return 0 (handle explicitly)
 * ✓ k >= distinct chars: Return entire string length
 * ✓ Empty string: Return 0
 * ✓ Single character: Return 1 (if k >= 1)
 * ✓ All same character: Return string length (if k >= 1)
 * ✓ Very large k: No shrinking needed
 * 
 * INTERVIEW STRATEGY:
 * 
 * 1. Clarify Requirements (30 sec)
 *    - What characters? (ASCII, Unicode, lowercase only?)
 *    - What's the range of k?
 *    - Return length or actual substring?
 * 
 * 2. Explain Approach (1 min)
 *    - "This is a sliding window problem"
 *    - "Use HashMap to track character frequencies"
 *    - "Expand right, shrink left when invalid"
 * 
 * 3. Walk Through Example (1 min)
 *    - Use s = "eceba", k = 2
 *    - Show each step of window expansion/contraction
 * 
 * 4. Code Solution (3-4 min)
 *    - Start with Approach 1 (HashMap)
 *    - Write clean, well-commented code
 * 
 * 5. Test with Edge Cases (1 min)
 *    - k = 0, k = 1, k = length
 * 
 * 6. Analyze Complexity (30 sec)
 *    - Time: O(n) - each character visited at most twice
 *    - Space: O(k) - HashMap stores at most k chars
 * 
 * 7. Discuss Optimizations (if time)
 *    - Array for fixed charset
 *    - LinkedHashMap for cleaner code
 * 
 * COMMON MISTAKES TO AVOID:
 * 
 * ✗ Using 'if' instead of 'while' for shrinking
 *   → Might need multiple shrinks in one iteration!
 * 
 * ✗ Forgetting to remove character from map when count = 0
 *   → map.size() won't reflect actual distinct count
 * 
 * ✗ Not handling k = 0 edge case
 *   → Should return 0 immediately
 * 
 * ✗ Updating maxLength before shrinking
 *   → Always update after window is valid
 * 
 * ✗ Using Set instead of Map
 *   → Need frequencies, not just presence
 * 
 * RELATED PROBLEMS TO MENTION:
 * 
 * - LeetCode 3: Longest Substring Without Repeating Characters (k = all distinct)
 * - LeetCode 159: Longest Substring with At Most Two Distinct Characters (k = 2)
 * - LeetCode 76: Minimum Window Substring (harder version)
 * - LeetCode 904: Fruit Into Baskets (same problem, different story)
 * 
 * KEY INSIGHTS FOR DIFFERENT K VALUES:
 * 
 * - k = 1: Find longest sequence of same character
 * - k = 2: Two types allowed (like "fruit into baskets")
 * - k = all distinct: Entire string (no constraint)
 * - Small k: LinkedHashMap might be cleaner
 * - Large k: Standard HashMap is fine
 * 
 * OPTIMIZATION NOTES:
 * 
 * 1. For lowercase letters only: Use int[26] instead of HashMap
 * 2. For ASCII: Use int[128] or int[256]
 * 3. For Unicode: Stick with HashMap
 * 4. If k is very small (1-3): LinkedHashMap might be more elegant
 * 5. If returning substring: Track start position, not just length
 * 
 * TIME COMPLEXITY PROOF:
 * 
 * - Right pointer moves forward n times: O(n)
 * - Left pointer moves forward at most n times: O(n)
 * - Each character is added once and removed once: O(n)
 * - HashMap operations (get/put/remove) are O(1) average
 * - Total: O(n) time complexity
 * 
 * SPACE COMPLEXITY PROOF:
 * 
 * - HashMap stores at most k distinct characters: O(k)
 * - In worst case, k = n (all characters distinct): O(n)
 * - Therefore: O(min(n, k)) space complexity
 * 
 * ============================================================================
 * MEMORIZE THIS PATTERN - IT APPLIES TO MANY PROBLEMS!
 * ============================================================================
 */
