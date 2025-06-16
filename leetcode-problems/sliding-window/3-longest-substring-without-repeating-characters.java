import java.util.*;
/*
 * Given a string s, find the length of the longest substring without duplicate
 * characters.
 * 
 * Example 1:
 * Input: s = "abcabcbb"
 * Output: 3
 * Explanation: The answer is "abc", with the length of 3.
 * 
 * Example 2:
 * Input: s = "bbbbb"
 * Output: 1
 * Explanation: The answer is "b", with the length of 1.
 * 
 * Example 3:
 * Input: s = "pwwkew"
 * Output: 3
 * Explanation: The answer is "wke", with the length of 3.
 * Notice that the answer must be a substring, "pwke" is a subsequence and not a
 * substring.
 */

class LongestSubstringWithoutRepeating {

    // Solution 1: Brute Force Approach
    // Time Complexity: O(n³), Space Complexity: O(min(m,n)) where m is charset size
    public int lengthOfLongestSubstringBruteForce(String s) {
        int n = s.length();
        int maxLen = 0;

        // Check all possible substrings
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (hasUniqueCharacters(s, i, j)) {
                    maxLen = Math.max(maxLen, j - i + 1);
                }
            }
        }

        return maxLen;
    }

    // Helper method for brute force
    private boolean hasUniqueCharacters(String s, int start, int end) {
        Set<Character> chars = new HashSet<>();
        for (int i = start; i <= end; i++) {
            char c = s.charAt(i);
            if (chars.contains(c)) {
                return false;
            }
            chars.add(c);
        }
        return true;
    }

    // Solution 2: Sliding Window with HashSet
    // Time Complexity: O(2n) = O(n), Space Complexity: O(min(m,n))
    public int lengthOfLongestSubstringHashSet(String s) {
        int n = s.length();
        Set<Character> chars = new HashSet<>();
        int left = 0, right = 0;
        int maxLen = 0;

        while (right < n) {
            char c = s.charAt(right);

            // If character is already in set, remove characters from left
            while (chars.contains(c)) {
                chars.remove(s.charAt(left));
                left++;
            }

            chars.add(c);
            maxLen = Math.max(maxLen, right - left + 1);
            right++;
        }

        return maxLen;
    }

    // Solution 3: Sliding Window with HashMap (Optimal)
    // Time Complexity: O(n), Space Complexity: O(min(m,n))
    public int lengthOfLongestSubstring(String s) {
        int n = s.length();
        Map<Character, Integer> charIndex = new HashMap<>();
        int left = 0;
        int maxLen = 0;

        for (int right = 0; right < n; right++) {
            char c = s.charAt(right);

            // If character is seen and is within current window
            if (charIndex.containsKey(c) && charIndex.get(c) >= left) {
                left = charIndex.get(c) + 1;
            }

            charIndex.put(c, right);
            maxLen = Math.max(maxLen, right - left + 1);
        }

        return maxLen;
    }

    // Solution 4: Optimized with Array (for ASCII characters)
    // Time Complexity: O(n), Space Complexity: O(1) - fixed size array
    public int lengthOfLongestSubstringArray(String s) {
        int n = s.length();
        int[] charIndex = new int[128]; // ASCII characters
        Arrays.fill(charIndex, -1);

        int left = 0;
        int maxLen = 0;

        for (int right = 0; right < n; right++) {
            char c = s.charAt(right);

            // If character is seen and is within current window
            if (charIndex[c] >= left) {
                left = charIndex[c] + 1;
            }

            charIndex[c] = right;
            maxLen = Math.max(maxLen, right - left + 1);
        }

        return maxLen;
    }

    // Solution 5: Sliding Window with Detailed Tracking
    // Time Complexity: O(n), Space Complexity: O(min(m,n))
    public int lengthOfLongestSubstringDetailed(String s) {
        int n = s.length();
        if (n == 0)
            return 0;

        Map<Character, Integer> charIndex = new HashMap<>();
        int left = 0;
        int maxLen = 0;
        String longestSubstring = "";

        for (int right = 0; right < n; right++) {
            char c = s.charAt(right);

            // If character is already seen in current window
            if (charIndex.containsKey(c) && charIndex.get(c) >= left) {
                left = charIndex.get(c) + 1;
            }

            charIndex.put(c, right);
            int currentLen = right - left + 1;

            if (currentLen > maxLen) {
                maxLen = currentLen;
                longestSubstring = s.substring(left, right + 1);
            }
        }

        System.out.println("Longest substring: \"" + longestSubstring + "\"");
        return maxLen;
    }

    // Visualization method to show sliding window process
    public int lengthOfLongestSubstringWithVisualization(String s) {
        int n = s.length();
        Map<Character, Integer> charIndex = new HashMap<>();
        int left = 0;
        int maxLen = 0;

        System.out.println("Sliding Window Visualization for: \"" + s + "\"");
        System.out.println("Format: [window] - length");
        System.out.println();

        for (int right = 0; right < n; right++) {
            char c = s.charAt(right);

            if (charIndex.containsKey(c) && charIndex.get(c) >= left) {
                System.out.printf("Duplicate '%c' found at index %d (previous at %d)\n",
                        c, right, charIndex.get(c));
                left = charIndex.get(c) + 1;
                System.out.printf("Move left pointer to %d\n", left);
            }

            charIndex.put(c, right);
            int currentLen = right - left + 1;
            maxLen = Math.max(maxLen, currentLen);

            String window = s.substring(left, right + 1);
            System.out.printf("Window [%d,%d]: \"%s\" - length %d%s\n",
                    left, right, window, currentLen,
                    currentLen == maxLen ? " (new max)" : "");
        }

        System.out.println("\nFinal result: " + maxLen);
        return maxLen;
    }

    // Test method
    public static void main(String[] args) {
        LongestSubstringWithoutRepeating solution = new LongestSubstringWithoutRepeating();

        // Test case 1
        String s1 = "abcabcbb";
        System.out.println("=== Test Case 1: \"" + s1 + "\" ===");
        System.out.println("Brute Force: " + solution.lengthOfLongestSubstringBruteForce(s1));
        System.out.println("HashSet: " + solution.lengthOfLongestSubstringHashSet(s1));
        System.out.println("HashMap: " + solution.lengthOfLongestSubstring(s1));
        System.out.println("Array: " + solution.lengthOfLongestSubstringArray(s1));
        solution.lengthOfLongestSubstringDetailed(s1);
        System.out.println();

        // Test case 2
        String s2 = "bbbbb";
        System.out.println("=== Test Case 2: \"" + s2 + "\" ===");
        System.out.println("HashMap: " + solution.lengthOfLongestSubstring(s2));
        System.out.println();

        // Test case 3
        String s3 = "pwwkew";
        System.out.println("=== Test Case 3: \"" + s3 + "\" ===");
        System.out.println("HashMap: " + solution.lengthOfLongestSubstring(s3));
        System.out.println();

        // Visualization
        System.out.println("=== Detailed Visualization ===");
        solution.lengthOfLongestSubstringWithVisualization("abcabcbb");
        System.out.println();
        solution.lengthOfLongestSubstringWithVisualization("pwwkew");

        // Edge cases
        System.out.println("\n=== Edge Cases ===");
        System.out.println("Empty string: " + solution.lengthOfLongestSubstring(""));
        System.out.println("Single char: " + solution.lengthOfLongestSubstring("a"));
        System.out.println("All unique: " + solution.lengthOfLongestSubstring("abcdef"));
        System.out.println("All same: " + solution.lengthOfLongestSubstring("aaaa"));
    }

}

/*
 * ALGORITHM EXPLANATION:
 * 
 * The optimal solution uses the Sliding Window technique with HashMap
 * optimization:
 * 
 * SLIDING WINDOW WITH HASHMAP APPROACH:
 * 1. Use two pointers (left, right) to maintain a window
 * 2. Use HashMap to store character -> last seen index mapping
 * 3. When duplicate found, jump left pointer to position after the duplicate
 * 4. Track maximum window size seen so far
 * 
 * WHY HASHMAP OPTIMIZATION WORKS:
 * - Instead of incrementally moving left pointer (like HashSet approach),
 * we can jump directly to the optimal position
 * - This reduces time complexity from O(2n) to O(n)
 * - HashMap stores the last seen index of each character
 * 
 * DETAILED WALKTHROUGH FOR "abcabcbb":
 * Index: 0 1 2 3 4 5 6 7
 * Chars: a b c a b c b b
 * 
 * Step by step:
 * 1. right=0, char='a': window="a", length=1, map={a:0}
 * 2. right=1, char='b': window="ab", length=2, map={a:0,b:1}
 * 3. right=2, char='c': window="abc", length=3, map={a:0,b:1,c:2}
 * 4. right=3, char='a': duplicate! jump left to index 1, window="bca", length=3
 * 5. right=4, char='b': duplicate! jump left to index 2, window="cab", length=3
 * 6. right=5, char='c': duplicate! jump left to index 3, window="abc", length=3
 * 7. right=6, char='b': duplicate! jump left to index 5, window="cb", length=2
 * 8. right=7, char='b': duplicate! jump left to index 7, window="b", length=1
 * 
 * Maximum length found: 3
 * 
 * COMPARISON OF APPROACHES:
 * 
 * 1. BRUTE FORCE O(n³):
 * - Check all substrings
 * - For each substring, verify uniqueness
 * - Simple but very slow
 * 
 * 2. SLIDING WINDOW + HASHSET O(2n):
 * - Expand window with right pointer
 * - When duplicate found, contract from left one by one
 * - Better but still has redundant left pointer movements
 * 
 * 3. SLIDING WINDOW + HASHMAP O(n):
 * - Expand window with right pointer
 * - When duplicate found, jump left pointer directly
 * - Optimal solution - each character visited at most once
 * 
 * 4. ARRAY OPTIMIZATION O(n):
 * - Same as HashMap but uses array for ASCII characters
 * - Slightly faster due to array access vs HashMap operations
 * - Space is O(1) since array size is fixed (128 for ASCII)
 * 
 * SPACE COMPLEXITY:
 * - HashMap: O(min(m,n)) where m is charset size, n is string length
 * - Array: O(1) for fixed charset (like ASCII)
 * 
 * KEY INSIGHTS:
 * - The HashMap stores the MOST RECENT index of each character
 * - When we find a duplicate, we jump left pointer to (last_seen_index + 1)
 * - We only jump if the duplicate is within the current window (>= left)
 * - This ensures we never revisit characters unnecessarily
 * 
 * BEST SOLUTION: HashMap approach strikes the perfect balance of efficiency and
 * readability.
 */
