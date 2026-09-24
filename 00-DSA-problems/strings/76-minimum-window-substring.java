import java.util.HashMap;
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

class MinimumWindowSubstringSolution {

    public String minWindow(String s, String t) {
        if (s.length() < t.length()) {
            return "";
        }

        Map<Character, Integer> tCount = new HashMap<>();
        for (Character ch : t.toCharArray()) {
            // tCount.merge(ch, 1, Integer::sum);
            // tCount.compute(ch, (key, val) -> val == null ? 1 : val + 1);
            tCount.put(ch, tCount.getOrDefault(ch, 0) + 1);
        }

        int windowStart = 0;
        int minStart = 0;
        int minLen = Integer.MAX_VALUE;
        int matched = 0;
        Map<Character, Integer> window = new HashMap<>();

        for (int windowEnd = 0; windowEnd < s.length(); windowEnd++) {
            char endChar = s.charAt(windowEnd);

            // Add the new character to the window
            if (tCount.containsKey(endChar)) {
                window.put(endChar, window.getOrDefault(endChar, 0) + 1);
                if (window.get(endChar).equals(tCount.get(endChar))) {
                    matched++;
                }
            }

            // Shrink the window if all characters in t are matched
            while (matched == tCount.size()) {
                if ((windowEnd - windowStart + 1) < minLen) {
                    minLen = windowEnd - windowStart + 1;
                    minStart = windowStart;
                }

                char startChar = s.charAt(windowStart);
                windowStart++;

                if (tCount.containsKey(startChar)) {
                    if (window.get(startChar).equals(tCount.get(startChar))) {
                        matched--;
                    }
                    window.put(startChar, window.get(startChar) - 1);
                }
            }
        }

        return minLen == Integer.MAX_VALUE ? "" : s.substring(minStart, minStart + minLen);
    }

}

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
        int formed = 0; // Number of unique chars in current window with desired frequency
        int required = tCount.size(); // Number of unique chars in t

        // Dictionary to keep count of characters in current window
        Map<Character, Integer> windowCount = new HashMap<>();

        // Answer tuple (window length, left, right)
        int[] ans = { -1, 0, 0 }; // length, left, right

        while (right < s.length()) {
            // Add one character from right to the window
            char c = s.charAt(right);
            windowCount.put(c, windowCount.getOrDefault(c, 0) + 1);

            // If frequency of current character added equals desired count in t, increment
            // formed
            if (tCount.containsKey(c) && windowCount.get(c).intValue() == tCount.get(c).intValue()) {
                formed++;
            }

            // Try to contract the window till it ceases to be 'desirable'
            while (left <= right && formed == required) {
                c = s.charAt(left);

                // Save the smallest window
                if (ans[0] == -1 || right - left + 1 < ans[0]) {
                    ans[0] = right - left + 1;
                    ans[1] = left;
                    ans[2] = right;
                }

                // Remove from left of our window
                windowCount.put(c, windowCount.get(c) - 1);
                if (tCount.containsKey(c) && windowCount.get(c) < tCount.get(c)) {
                    formed--;
                }

                left++;
            }

            right++;
        }

        return ans[0] == -1 ? "" : s.substring(ans[1], ans[2] + 1);
    }

    /**
     * Alternative solution using array for character counting (faster for ASCII)
     * Time Complexity: O(|s| + |t|)
     * Space Complexity: O(1) - fixed size arrays
     */
    public String minWindowArray(String s, String t) {
        if (s == null || t == null || s.length() < t.length()) {
            return "";
        }

        // Count characters in t
        int[] tCount = new int[128]; // ASCII characters
        int uniqueChars = 0;

        for (char c : t.toCharArray()) {
            if (tCount[c] == 0)
                uniqueChars++;
            tCount[c]++;
        }

        int left = 0, right = 0;
        int formed = 0;
        int[] windowCount = new int[128];

        int minLen = Integer.MAX_VALUE;
        int minLeft = 0;

        while (right < s.length()) {
            char c = s.charAt(right);
            windowCount[c]++;

            if (tCount[c] > 0 && windowCount[c] == tCount[c]) {
                formed++;
            }

            while (left <= right && formed == uniqueChars) {
                if (right - left + 1 < minLen) {
                    minLen = right - left + 1;
                    minLeft = left;
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

        return minLen == Integer.MAX_VALUE ? "" : s.substring(minLeft, minLeft + minLen);
    }

    // Test method
    public static void main(String[] args) {
        MinimumWindowSubstring solution = new MinimumWindowSubstring();

        // Test cases
        System.out.println("Test 1: " + solution.minWindow("ADOBECODEBANC", "ABC")); // Expected: "BANC"
        System.out.println("Test 2: " + solution.minWindow("a", "a")); // Expected: "a"
        System.out.println("Test 3: " + solution.minWindow("a", "aa")); // Expected: ""
        System.out.println("Test 4: " + solution.minWindow("ab", "b")); // Expected: "b"
        System.out.println("Test 5: " + solution.minWindow("ADOBECODEBANC", "AABC")); // Expected: "ADOBEC"

        // Testing array-based solution
        System.out.println("\nArray-based solution:");
        System.out.println("Test 1: " + solution.minWindowArray("ADOBECODEBANC", "ABC")); // Expected: "BANC"
        System.out.println("Test 2: " + solution.minWindowArray("a", "a")); // Expected: "a"
        System.out.println("Test 3: " + solution.minWindowArray("a", "aa")); // Expected: ""
    }
}

/**
 * Algorithm Explanation:
 * 
 * 1. Use sliding window technique with two pointers (left and right)
 * 2. Expand the window by moving right pointer until we have a valid window
 * 3. Once valid, try to shrink from left to find minimum window
 * 4. Keep track of the minimum valid window found
 * 
 * Key Variables:
 * - tCount: frequency of characters in string t
 * - windowCount: frequency of characters in current window
 * - formed: number of unique characters in current window that match required
 * frequency
 * - required: number of unique characters in t
 * 
 * Time Complexity: O(|s| + |t|) where |s| and |t| are lengths of strings
 * Space Complexity: O(|s| + |t|) for HashMap approach, O(1) for array approach
 */