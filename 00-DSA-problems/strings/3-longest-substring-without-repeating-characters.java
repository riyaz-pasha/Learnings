import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class LongestSubstringWithoutRepeatingCharacters {

    public int lengthOfLongestSubstring(String s) {
        int longestLength = 0;
        int len = s.length();
        int windowStart = 0;
        int windowEnd = 0;
        Set<Character> uniqueChars = new HashSet<>();

        while (windowEnd < len) {
            char cha = s.charAt(windowEnd);
            while (uniqueChars.contains(cha)) {
                uniqueChars.remove(s.charAt(windowStart++));
            }
            uniqueChars.add(cha);
            windowEnd++;
            longestLength = Math.max(longestLength, uniqueChars.size());
        }

        return longestLength;
    }

    public int lengthOfLongestSubstringSol2(String s) {
        int longestLength = 0;
        int len = s.length();
        int windowStart = 0;
        int windowEnd = 0;
        Map<Character, Integer> uniqueChars = new HashMap<>();

        while (windowEnd < len) {
            char cha = s.charAt(windowEnd);
            if (uniqueChars.containsKey(cha) && uniqueChars.get(cha) >= windowStart) {
                windowStart = uniqueChars.get(cha) + 1;
            }
            uniqueChars.put(cha, windowEnd);
            windowEnd++;
            longestLength = Math.max(longestLength, windowEnd - windowStart);
        }

        return longestLength;
    }

}

class LongestSubstringWithoutRepeating {

    // Solution 1: Brute Force Approach
    // Time Complexity: O(n³), Space Complexity: O(min(m,n))
    public int lengthOfLongestSubstring1(String s) {
        int n = s.length();
        int maxLength = 0;

        // Check all possible substrings
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (allUnique(s, i, j)) {
                    maxLength = Math.max(maxLength, j - i + 1);
                }
            }
        }

        return maxLength;
    }

    // Helper method to check if substring has all unique characters
    private boolean allUnique(String s, int start, int end) {
        Set<Character> set = new HashSet<>();
        for (int i = start; i <= end; i++) {
            char c = s.charAt(i);
            if (set.contains(c)) {
                return false;
            }
            set.add(c);
        }
        return true;
    }

    // Solution 2: Sliding Window with HashSet
    // Time Complexity: O(2n) = O(n), Space Complexity: O(min(m,n))
    public int lengthOfLongestSubstring2(String s) {
        int n = s.length();
        Set<Character> set = new HashSet<>();
        int maxLength = 0, i = 0, j = 0;

        while (i < n && j < n) {
            if (!set.contains(s.charAt(j))) {
                set.add(s.charAt(j++));
                maxLength = Math.max(maxLength, j - i);
            } else {
                set.remove(s.charAt(i++));
            }
        }

        return maxLength;
    }

    // Solution 3: Optimized Sliding Window with HashMap
    // Time Complexity: O(n), Space Complexity: O(min(m,n))
    public int lengthOfLongestSubstring3(String s) {
        int n = s.length();
        Map<Character, Integer> map = new HashMap<>();
        int maxLength = 0;

        for (int j = 0, i = 0; j < n; j++) {
            char c = s.charAt(j);
            if (map.containsKey(c)) {
                i = Math.max(map.get(c) + 1, i);
            }
            maxLength = Math.max(maxLength, j - i + 1);
            map.put(c, j);
        }

        return maxLength;
    }

    // Solution 4: Optimized with Array (for ASCII characters)
    // Time Complexity: O(n), Space Complexity: O(m) where m is charset size
    public int lengthOfLongestSubstring4(String s) {
        int n = s.length();
        int[] index = new int[128]; // ASCII characters
        Arrays.fill(index, -1);

        int maxLength = 0;
        int start = 0;

        for (int end = 0; end < n; end++) {
            char c = s.charAt(end);
            if (index[c] >= start) {
                start = index[c] + 1;
            }
            index[c] = end;
            maxLength = Math.max(maxLength, end - start + 1);
        }

        return maxLength;
    }

    // Test method with examples
    public static void main(String[] args) {
        LongestSubstringWithoutRepeating solution = new LongestSubstringWithoutRepeating();

        // Test cases
        String[] testCases = { "abcabcbb", "bbbbb", "pwwkew", "", "dvdf" };
        int[] expected = { 3, 1, 3, 0, 3 };

        System.out.println("Testing all solutions:");
        System.out.println("======================");

        for (int i = 0; i < testCases.length; i++) {
            String s = testCases[i];
            int exp = expected[i];

            System.out.printf("Input: \"%s\"\n", s);
            System.out.printf("Expected: %d\n", exp);

            int result1 = solution.lengthOfLongestSubstring1(s);
            int result2 = solution.lengthOfLongestSubstring2(s);
            int result3 = solution.lengthOfLongestSubstring3(s);
            int result4 = solution.lengthOfLongestSubstring4(s);

            System.out.printf("Solution 1 (Brute Force): %d %s\n",
                    result1, result1 == exp ? "✓" : "✗");
            System.out.printf("Solution 2 (Sliding Window + Set): %d %s\n",
                    result2, result2 == exp ? "✓" : "✗");
            System.out.printf("Solution 3 (Optimized HashMap): %d %s\n",
                    result3, result3 == exp ? "✓" : "✗");
            System.out.printf("Solution 4 (Array Index): %d %s\n",
                    result4, result4 == exp ? "✓" : "✗");
            System.out.println("-".repeat(30));
        }

        // Additional method to find the actual substring (bonus)
        System.out.println("\nActual longest substrings:");
        for (String s : testCases) {
            String longest = solution.findLongestSubstring(s);
            System.out.printf("Input: \"%s\" -> Longest: \"%s\" (length: %d)\n",
                    s, longest, longest.length());
        }
    }

    // Bonus: Method to return the actual longest substring
    public String findLongestSubstring(String s) {
        int n = s.length();
        Map<Character, Integer> map = new HashMap<>();
        int maxLength = 0;
        int maxStart = 0;

        for (int j = 0, i = 0; j < n; j++) {
            char c = s.charAt(j);
            if (map.containsKey(c)) {
                i = Math.max(map.get(c) + 1, i);
            }
            if (j - i + 1 > maxLength) {
                maxLength = j - i + 1;
                maxStart = i;
            }
            map.put(c, j);
        }

        return s.substring(maxStart, maxStart + maxLength);
    }
}

/*
 * ALGORITHM EXPLANATIONS:
 * 
 * 1. BRUTE FORCE (Solution 1):
 * - Check every possible substring
 * - For each substring, verify if all characters are unique
 * - Time: O(n³), Space: O(min(m,n))
 * 
 * 2. SLIDING WINDOW WITH SET (Solution 2):
 * - Use two pointers (i, j) and a HashSet
 * - Expand j when no duplicates, shrink i when duplicates found
 * - Time: O(2n), Space: O(min(m,n))
 * 
 * 3. OPTIMIZED SLIDING WINDOW (Solution 3):
 * - Use HashMap to store character positions
 * - Jump directly to position after duplicate character
 * - Time: O(n), Space: O(min(m,n))
 * 
 * 4. ARRAY INDEX OPTIMIZATION (Solution 4):
 * - Use array instead of HashMap for ASCII characters
 * - Slightly faster due to array access vs hash operations
 * - Time: O(n), Space: O(m) where m is charset size
 * 
 * KEY INSIGHTS:
 * - The sliding window technique is optimal for this problem
 * - HashMap/Array stores the most recent index of each character
 * - When duplicate found, move start pointer to avoid re-checking
 * - Always update the character's index after processing
 * 
 * EDGE CASES HANDLED:
 * - Empty string: returns 0
 * - Single character: returns 1
 * - All same characters: returns 1
 * - All unique characters: returns string length
 */
