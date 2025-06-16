import java.util.*;
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
