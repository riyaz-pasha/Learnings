class LongestPalindromeSubstring {

    public String longestPalindrome(String s) {
        if (s.length() <= 1)
            return s;

        String maxStr = s.substring(0, 1);
        for (int i = 0; i < s.length(); i++) {
            String odd = expandFromCenter(s, i, i);
            String even = expandFromCenter(s, i, i + 1);
            if (odd.length() > maxStr.length())
                maxStr = odd;
            if (even.length() > maxStr.length())
                maxStr = even;
        }

        return maxStr;
    }

    private String expandFromCenter(String s, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        return s.substring(left + 1, right);
    }

}

class LongestPalindromeSubstring11 {

    // 🧮 Complexity:
    // Time: O(n²)
    // Space: O(1)
    public String longestPalindrome(String s) {
        if (s == null || s.length() < 1)
            return "";

        int start = 0, end = 0;

        for (int i = 0; i < s.length(); i++) {
            int len1 = expandFromCenter(s, i, i); // odd length
            int len2 = expandFromCenter(s, i, i + 1); // even length
            int len = Math.max(len1, len2);

            if (len > end - start) {
                start = i - (len - 1) / 2;
                end = i + len / 2;
            }
        }

        return s.substring(start, end + 1);
    }

    private int expandFromCenter(String s, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        return right - left - 1;
    }

}

class LongestPalindromeSubstring2 {

    public String longestPalindrome(String s) {
        int n = s.length();
        if (n <= 1)
            return s;

        int start = 0, maxLen = 1;

        boolean[][] dp = new boolean[n][n];
        for (int diag = 0; diag < n; diag++) {
            dp[diag][diag] = true;
        }

        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + (len - 1);
                if (s.charAt(i) == s.charAt(j)) {
                    if (len == 2 || dp[i + 1][j - 1]) {
                        dp[i][j] = true;
                        if (len > maxLen) {
                            maxLen = len;
                            start = i;
                        }
                    }
                }
            }
        }

        return s.substring(start, start + maxLen);
    }

}

/*
 * Longest Palindromic Substring — Dynamic Programming Approach
 * -------------------------------------------------------------
 *
 * 🎯 Goal:
 *   - Find the longest substring in a given string `s` that reads the same forward and backward.
 *
 * 💡 Core Idea:
 *   - Use a 2D DP table: dp[i][j] is true if the substring s[i..j] is a palindrome.
 *
 * 🧠 Reasoning:
 *   - A string is a palindrome if:
 *       1. The first and last characters match, i.e., s[i] == s[j]
 *       2. The inner substring s[i+1..j-1] is also a palindrome
 *   - We build from short substrings to longer ones so inner results are already computed.
 *
 * ✅ Base Cases:
 *   - All substrings of length 1 are palindromes → dp[i][i] = true
 *   - Substrings of length 2 are palindromes if both characters match → dp[i][i+1] = (s[i] == s[i+1])
 *
 * 🔁 Recurrence:
 *   - For substring s[i..j] (length ≥ 3):
 *       dp[i][j] = true if s[i] == s[j] and dp[i+1][j-1] == true
 *
 * 📦 Tracking:
 *   - Track the start index and max length whenever a longer palindrome is found.
 *
 * 🔍 Example Trace (s = "babad"):
 *     i\j  0 1 2 3 4
 *         b a b a d
 *      0  T F T F F
 *      1    T F T F
 *      2      T F F
 *      3        T F
 *      4          T
 *   - Longest palindromic substring = "bab" or "aba"
 *
 * 🧮 Complexity:
 *   - Time: O(n²) → We fill half of an n x n table
 *   - Space: O(n²) → To store the DP table
 *
 * 📌 Easy to Remember:
 *   1. Palindromes grow from inside → out
 *   2. Expand when ends match and middle is a palindrome
 *   3. Fill table diagonally by increasing substring length
 *
 * 🛠 Tip:
 *   - Always check shorter substrings first so inner results are available for longer ones
 */

class LongestPalindromicSubstring {

    // Time Complexity: O(N^2)
    // Space Complexity: O(N^2)
    public String longestPalindrome(String s) {
        if (s == null || s.length() < 1) {
            return "";
        }

        int n = s.length();
        boolean[][] dp = new boolean[n][n];

        int maxLength = 0;
        int startIndex = 0;

        // Base cases: Substrings of length 1
        for (int i = 0; i < n; i++) {
            dp[i][i] = true;
            if (1 > maxLength) {
                maxLength = 1;
                startIndex = i;
            }
        }

        // Base cases: Substrings of length 2
        for (int i = 0; i < n - 1; i++) {
            if (s.charAt(i) == s.charAt(i + 1)) {
                dp[i][i + 1] = true;
                if (2 > maxLength) {
                    maxLength = 2;
                    startIndex = i;
                }
            }
        }

        // Fill the DP table for lengths from 3 to n
        // 'length' represents the current length of the substring
        for (int length = 3; length <= n; length++) {
            // 'i' represents the starting index of the substring
            for (int i = 0; i <= n - length; i++) {
                // 'j' represents the ending index of the substring
                int j = i + length - 1;

                // Check if the outer characters match AND the inner substring is a palindrome
                if (s.charAt(i) == s.charAt(j) && dp[i + 1][j - 1]) {
                    dp[i][j] = true;
                    // If a longer palindrome is found, update maxLength and startIndex
                    if (length > maxLength) {
                        maxLength = length;
                        startIndex = i;
                    }
                }
            }
        }

        // Return the longest palindromic substring using startIndex and maxLength
        return s.substring(startIndex, startIndex + maxLength);
    }

    public static void main(String[] args) {
        LongestPalindromicSubstring solver = new LongestPalindromicSubstring();

        // Test cases
        System.out.println("String: babad -> Longest Palindrome: "
                + solver.longestPalindrome("babad"));
        // Expected:
        // "bab" or
        // "aba"
        System.out.println("String: cbbd -> Longest Palindrome: "
                + solver.longestPalindrome("cbbd"));
        // Expected: "bb"
        System.out.println("String: a -> Longest Palindrome: "
                + solver.longestPalindrome("a"));
        // Expected: "a"
        System.out.println("String: ac -> Longest Palindrome: "
                + solver.longestPalindrome("ac"));
        // Expected: "a" or
        // "c"
        System.out.println("String: racecar -> Longest Palindrome: "
                + solver.longestPalindrome("racecar"));
        // Expected:
        // "racecar"
        System.out.println(
                "String: forgeeksskeegfor -> Longest Palindrome: "
                        + solver.longestPalindrome("forgeeksskeegfor"));
        // Expected:
        // "geeksskeeg"
    }
}

/*
 * Longest Palindromic Substring — Manacher’s Algorithm (O(n) Time)
 * -----------------------------------------------------------------
 *
 * 🎯 Goal:
 *   - Find the longest palindromic substring in linear time, O(n).
 *
 * 💡 Key Insight:
 *   - Every palindrome has symmetry. By keeping track of the center and right edge of 
 *     the currently known longest palindrome, we can reduce unnecessary comparisons.
 *
 * 🔄 Preprocessing:
 *   - Transform the string `s` by inserting a special character (e.g. '#') between each character
 *     and at the beginning and end. This handles both odd and even length palindromes uniformly.
 *     Example: "abba" → "^#a#b#b#a#$"
 *
 * 🧠 Core Idea:
 *   - Use an array `P[i]` to store the radius of the palindrome centered at position `i` 
 *     in the transformed string.
 *   - Maintain two variables:
 *       - `center`: Center of the current right-most palindrome
 *       - `right`: Right edge (farthest reach) of that palindrome
 *   - For each position `i`:
 *       1. Mirror `i_mirror = 2 * center - i`
 *       2. If `i` is within the current right boundary:
 *           Set `P[i] = min(right - i, P[i_mirror])`
 *       3. Expand around `i` to find the maximum radius of palindrome centered at `i`
 *       4. Update `center` and `right` if the palindrome at `i` goes beyond `right`
 *
 * 🧮 Example Trace:
 *   For s = "abba", transformed = "^#a#b#b#a#$"
 *   Index:        0 1 2 3 4 5 6 7 8 9 10 11 12
 *   Char:         ^ # a # b # b # a # $ 
 *   Palindrome:     0 1 0 3 0 1 0 3 0 1
 *   Max center = 7 → longest palindrome radius = 3
 *   Start index in original string = (centerIndex - maxLen) / 2
 *
 * 🚀 Final Step:
 *   - Extract the original substring from `s` using the center and length of the max palindrome
 *
 * 🧠 Complexity:
 *   - Time: O(n)
 *   - Space: O(n) → for transformed string and radius array
 *
 * 📌 Easy to Remember:
 *   1. Insert `#` to normalize even/odd cases
 *   2. Use symmetry to avoid redundant expansions
 *   3. Expand only when necessary, track farthest right reach
 *
 * 🧠 Summary in One Line:
 *   Expand around centers with a twist: reuse previously known palindromes via symmetry.
 */

class LongestPalindromicSubstring5 {

    public String longestPalindrome(String s) {
        if (s == null || s.length() == 0) return "";

        // Step 1: Preprocess the string to handle even/odd length uniformly
        StringBuilder t = new StringBuilder("^");  // Start sentinel
        for (char c : s.toCharArray()) {
            t.append('#').append(c);
        }
        t.append("#$");  // End sentinel
        String str = t.toString();
        int n = str.length();

        // Step 2: Array to store the radius of palindrome at each center
        int[] p = new int[n];
        int center = 0, right = 0;

        // Step 3: Core logic
        for (int i = 1; i < n - 1; i++) {
            int mirror = 2 * center - i;

            // If i is within the right boundary, use mirror's radius as starting point
            if (i < right) {
                p[i] = Math.min(right - i, p[mirror]);
            }

            // Try to expand around i
            while (str.charAt(i + (p[i] + 1)) == str.charAt(i - (p[i] + 1))) {
                p[i]++;
            }

            // Update center and right if expanded palindrome goes beyond right
            if (i + p[i] > right) {
                center = i;
                right = i + p[i];
            }
        }

        // Step 4: Find the maximum palindrome length and its center
        int maxLen = 0, centerIndex = 0;
        for (int i = 1; i < n - 1; i++) {
            if (p[i] > maxLen) {
                maxLen = p[i];
                centerIndex = i;
            }
        }

        // Step 5: Convert back to original string indices
        int start = (centerIndex - maxLen) / 2;
        return s.substring(start, start + maxLen);
    }

}
