/*
 * Given two strings text1 and text2, return the length of their longest common
 * subsequence. If there is no common subsequence, return 0.
 * 
 * A subsequence of a string is a new string generated from the original string
 * with some characters (can be none) deleted without changing the relative
 * order of the remaining characters.
 * 
 * For example, "ace" is a subsequence of "abcde".
 * A common subsequence of two strings is a subsequence that is common to both
 * strings.
 * 
 * Example 1:
 * Input: text1 = "abcde", text2 = "ace"
 * Output: 3
 * Explanation: The longest common subsequence is "ace" and its length is 3.
 * 
 * Example 2:
 * Input: text1 = "abc", text2 = "abc"
 * Output: 3
 * Explanation: The longest common subsequence is "abc" and its length is 3.
 * 
 * Example 3:
 * Input: text1 = "abc", text2 = "def"
 * Output: 0
 * Explanation: There is no such common subsequence, so the result is 0.
 */

class LongestCommonSubsequenceSolution {

    public int longestCommonSubsequence(String text1, String text2) {
        int len1 = text1.length();
        int len2 = text2.length();
        Integer[][] memo = new Integer[len1][len2];
        return helper(text1, text2, memo, len1 - 1, len2 - 1);
    }

    private int helper(String text1, String text2, Integer[][] memo, int index1, int index2) {
        if (index1 < 0 || index2 < 0) {
            return 0;
        }
        if (memo[index1][index2] != null) {
            return memo[index1][index2];
        }
        int longest;
        if (text1.charAt(index1) == text2.charAt(index2)) {
            longest = 1 + helper(text1, text2, memo, index1 - 1, index2 - 1);
        } else {

            longest = Math.max(helper(text1, text2, memo, index1 - 1, index2),
                    helper(text1, text2, memo, index1, index2 - 1));
        }
        return memo[index1][index2] = longest;
    }

}

class LongestCommonSubsequence {

    // Solution 1: Space Optimized DP - Single Array (Most Efficient)
    // Time Complexity: O(m * n)
    // Space Complexity: O(min(m, n))
    public int longestCommonSubsequence(String text1, String text2) {
        // Ensure text2 is the shorter string to minimize space
        if (text1.length() < text2.length()) {
            return longestCommonSubsequence(text2, text1);
        }

        int m = text1.length();
        int n = text2.length();

        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    curr[j] = prev[j - 1] + 1;
                } else {
                    curr[j] = Math.max(prev[j], curr[j - 1]);
                }
            }
            // Swap arrays for next iteration
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[n];
    }

    // Solution 2: 2D Dynamic Programming (Most Intuitive)
    // Time Complexity: O(m * n)
    // Space Complexity: O(m * n)
    public int longestCommonSubsequence2D(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();

        // dp[i][j] = LCS length of text1[0..i-1] and text2[0..j-1]
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    // Characters match - extend previous LCS
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    // Characters don't match - take max of skipping one character
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[m][n];
    }

    // Solution 3: 2D DP with LCS String Reconstruction
    // Time Complexity: O(m * n)
    // Space Complexity: O(m * n)
    public String getLCSString(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();
        int[][] dp = new int[m + 1][n + 1];

        // Build DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Backtrack to reconstruct LCS
        StringBuilder lcs = new StringBuilder();
        int i = m, j = n;

        while (i > 0 && j > 0) {
            if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                lcs.append(text1.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        return lcs.reverse().toString();
    }

    // Solution 4: Recursion with Memoization (Top-Down)
    // Time Complexity: O(m * n)
    // Space Complexity: O(m * n) for memo + O(m + n) for recursion stack
    public int longestCommonSubsequenceMemo(String text1, String text2) {
        Integer[][] memo = new Integer[text1.length()][text2.length()];
        return lcsHelper(text1, text2, 0, 0, memo);
    }

    private int lcsHelper(String s1, String s2, int i, int j, Integer[][] memo) {
        // Base case: reached end of either string
        if (i >= s1.length() || j >= s2.length()) {
            return 0;
        }

        if (memo[i][j] != null) {
            return memo[i][j];
        }

        if (s1.charAt(i) == s2.charAt(j)) {
            // Characters match - include in LCS
            memo[i][j] = 1 + lcsHelper(s1, s2, i + 1, j + 1, memo);
        } else {
            // Characters don't match - try skipping from either string
            int skipS1 = lcsHelper(s1, s2, i + 1, j, memo);
            int skipS2 = lcsHelper(s1, s2, i, j + 1, memo);
            memo[i][j] = Math.max(skipS1, skipS2);
        }

        return memo[i][j];
    }

    // Solution 5: Pure Recursion (Brute Force - for understanding)
    // Time Complexity: O(2^(m+n)) - exponential
    // Space Complexity: O(m + n) for recursion stack
    public int longestCommonSubsequenceBruteForce(String text1, String text2) {
        return lcsBruteForce(text1, text2, 0, 0);
    }

    private int lcsBruteForce(String s1, String s2, int i, int j) {
        if (i >= s1.length() || j >= s2.length()) {
            return 0;
        }

        if (s1.charAt(i) == s2.charAt(j)) {
            return 1 + lcsBruteForce(s1, s2, i + 1, j + 1);
        } else {
            int skipS1 = lcsBruteForce(s1, s2, i + 1, j);
            int skipS2 = lcsBruteForce(s1, s2, i, j + 1);
            return Math.max(skipS1, skipS2);
        }
    }

    // Solution 6: Ultra Space Optimized - Single Array
    // Time Complexity: O(m * n)
    // Space Complexity: O(n)
    public int longestCommonSubsequenceUltra(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();

        int[] dp = new int[n + 1];

        for (int i = 1; i <= m; i++) {
            int prev = 0; // Represents dp[i-1][j-1]
            for (int j = 1; j <= n; j++) {
                int temp = dp[j]; // Save current value before overwriting
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[j] = prev + 1;
                } else {
                    dp[j] = Math.max(dp[j], dp[j - 1]);
                }
                prev = temp; // Update prev for next iteration
            }
        }

        return dp[n];
    }

    // Helper method to visualize DP table
    public void printDPTable(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        System.out.println("\n=== DP Table Visualization ===");
        System.out.println("text1: " + text1 + ", text2: " + text2);
        System.out.print("    ");
        for (char c : text2.toCharArray()) {
            System.out.print(c + " ");
        }
        System.out.println();

        for (int i = 0; i <= m; i++) {
            if (i > 0)
                System.out.print(text1.charAt(i - 1) + " ");
            else
                System.out.print("  ");

            for (int j = 0; j <= n; j++) {
                System.out.print(dp[i][j] + " ");
            }
            System.out.println();
        }
    }

    // Test cases
    public static void main(String[] args) {
        LongestCommonSubsequence solution = new LongestCommonSubsequence();

        // Test Case 1
        String text1_1 = "abcde";
        String text2_1 = "ace";
        System.out.println("Example 1:");
        System.out.println("Input: text1 = \"" + text1_1 + "\", text2 = \"" + text2_1 + "\"");
        System.out.println("Output (Optimized): " + solution.longestCommonSubsequence(text1_1, text2_1));
        System.out.println("Output (2D DP): " + solution.longestCommonSubsequence2D(text1_1, text2_1));
        System.out.println("LCS String: \"" + solution.getLCSString(text1_1, text2_1) + "\"");
        System.out.println("Expected: 3");
        solution.printDPTable(text1_1, text2_1);

        // Test Case 2
        String text1_2 = "abc";
        String text2_2 = "abc";
        System.out.println("\nExample 2:");
        System.out.println("Input: text1 = \"" + text1_2 + "\", text2 = \"" + text2_2 + "\"");
        System.out.println("Output: " + solution.longestCommonSubsequence(text1_2, text2_2));
        System.out.println("LCS String: \"" + solution.getLCSString(text1_2, text2_2) + "\"");
        System.out.println("Expected: 3");

        // Test Case 3
        String text1_3 = "abc";
        String text2_3 = "def";
        System.out.println("\nExample 3:");
        System.out.println("Input: text1 = \"" + text1_3 + "\", text2 = \"" + text2_3 + "\"");
        System.out.println("Output: " + solution.longestCommonSubsequence(text1_3, text2_3));
        System.out.println("LCS String: \"" + solution.getLCSString(text1_3, text2_3) + "\"");
        System.out.println("Expected: 0");

        // Additional Test Cases
        String text1_4 = "bsbininm";
        String text2_4 = "jmjkbkjkv";
        System.out.println("\nAdditional Test:");
        System.out.println("Input: text1 = \"" + text1_4 + "\", text2 = \"" + text2_4 + "\"");
        System.out.println("Output: " + solution.longestCommonSubsequence(text1_4, text2_4));
        System.out.println("LCS String: \"" + solution.getLCSString(text1_4, text2_4) + "\"");

        String text1_5 = "AGGTAB";
        String text2_5 = "GXTXAYB";
        System.out.println("\nClassic Example:");
        System.out.println("Input: text1 = \"" + text1_5 + "\", text2 = \"" + text2_5 + "\"");
        System.out.println("Output: " + solution.longestCommonSubsequence(text1_5, text2_5));
        System.out.println("LCS String: \"" + solution.getLCSString(text1_5, text2_5) + "\"");
        System.out.println("Expected: 4 (GTAB)");
        solution.printDPTable(text1_5, text2_5);

        // Performance comparison
        System.out.println("\n=== Performance Comparison ===");
        String large1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(10);
        String large2 = "ACEGIKMOQSUWY".repeat(20);

        long start = System.nanoTime();
        int result1 = solution.longestCommonSubsequence(large1, large2);
        long end = System.nanoTime();
        System.out.println("Space Optimized (2 arrays): " + result1 +
                " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result2 = solution.longestCommonSubsequenceUltra(large1, large2);
        end = System.nanoTime();
        System.out.println("Ultra Optimized (1 array): " + result2 +
                " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result3 = solution.longestCommonSubsequence2D(large1, large2);
        end = System.nanoTime();
        System.out.println("2D DP: " + result3 +
                " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result4 = solution.longestCommonSubsequenceMemo(large1, large2);
        end = System.nanoTime();
        System.out.println("Memoization: " + result4 +
                " (Time: " + (end - start) / 1000 + " μs)");
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: Space Optimized DP (Two Arrays)
 * - Time Complexity: O(m * n)
 * m = length of text1, n = length of text2
 * We fill the DP table with m*n cells
 * - Space Complexity: O(min(m, n))
 * We only keep two rows at a time
 * Swap to use shorter string as columns
 * 
 * Solution 2: 2D Dynamic Programming (MOST INTUITIVE)
 * - Time Complexity: O(m * n)
 * - Space Complexity: O(m * n)
 * Full 2D table of size (m+1) × (n+1)
 * Easy to understand and debug
 * 
 * Solution 3: 2D DP with String Reconstruction
 * - Time Complexity: O(m * n)
 * - Space Complexity: O(m * n)
 * Same as Solution 2, plus O(min(m,n)) for result string
 * 
 * Solution 4: Recursion with Memoization
 * - Time Complexity: O(m * n)
 * Each unique state computed once
 * - Space Complexity: O(m * n) + O(m + n)
 * Memo table + recursion stack depth
 * 
 * Solution 5: Pure Recursion (Brute Force)
 * - Time Complexity: O(2^(m+n))
 * Exponential - explores all combinations
 * - Space Complexity: O(m + n)
 * Maximum recursion depth
 * 
 * Solution 6: Ultra Space Optimized (Single Array)
 * - Time Complexity: O(m * n)
 * - Space Complexity: O(n)
 * Single array, uses prev variable cleverly
 * 
 * KEY INSIGHTS:
 * 
 * 1. DP State Definition:
 * dp[i][j] = length of LCS of text1[0..i-1] and text2[0..j-1]
 * 
 * 2. State Transition:
 * if (text1[i-1] == text2[j-1]):
 * dp[i][j] = dp[i-1][j-1] + 1 // Match found, extend LCS
 * else:
 * dp[i][j] = max(dp[i-1][j], dp[i][j-1]) // Skip char from either string
 * 
 * 3. Why This Works:
 * - If characters match: both are part of LCS, add 1 to previous LCS
 * - If don't match: LCS is the better of:
 * Skipping current char from text1
 * Skipping current char from text2
 * 
 * 4. Subsequence vs Substring:
 * - Subsequence: characters don't need to be consecutive
 * - Substring: characters must be consecutive
 * - LCS allows skipping characters, maintaining order
 * 
 * 5. Base Cases:
 * - dp[0][j] = 0 (empty text1)
 * - dp[i][0] = 0 (empty text2)
 * 
 * 6. LCS Reconstruction:
 * - Backtrack from dp[m][n]
 * - If chars match: include in LCS, move diagonally
 * - Else: move in direction of larger value
 * 
 * 7. Space Optimization:
 * - Only need previous row to compute current row
 * - Can reduce from O(m*n) to O(n) space
 * - Trade-off: can't reconstruct string easily
 * 
 * 8. Common Applications:
 * - Diff tools (git diff)
 * - DNA sequence alignment
 * - File comparison
 * - Spell checkers
 */
