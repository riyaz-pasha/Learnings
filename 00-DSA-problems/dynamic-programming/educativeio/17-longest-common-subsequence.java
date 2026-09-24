import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Longest Common Subsequence (LCS)
 * Given two strings, return the length of their longest common subsequence.
 * A subsequence is a string generated from the original string by deleting some 
 * characters without changing the relative order of the remaining characters.
 * 
 * Constraints:
 * 1 <= str1.length, str2.length <= 500
 * str1 and str2 consist of only lowercase English characters.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * This is arguably the most famous 2D Dynamic Programming problem. In an L4/L5 
 * interview, you should establish a strong foundation before jumping to code:
 * 
 * Q: "Do the characters have to be contiguous?"
 * A: No, a subsequence allows skipping characters (unlike a substring).
 * 
 * Q: "Does case matter?"
 * A: Constraints specify only lowercase English characters, so we don't need 
 *    to worry about case-insensitive matching.
 * 
 * Q: "What if one string is much shorter than the other?"
 * A: This is a great observation. In our space-optimized DP, we can guarantee 
 *    we use the shorter string as the columns to minimize memory footprint. 
 *    (Mentioning this trick earns major senior-level bonus points).
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We are comparing two strings by walking backward from their ends. 
 * Let's say we are looking at character `i` of String1 and character `j` of String2.
 * I have two possible scenarios:
 * 
 * SCENARIO A (Match!): str1[i] == str2[j]
 * Awesome, we found a common character! This character belongs to our LCS. 
 * We add 1 to our total length and move BOTH pointers back by 1 to check the 
 * rest of the strings.
 * 
 * SCENARIO B (Mismatch!): str1[i] != str2[j]
 * They don't match. This means they cannot BOTH be part of the LCS at the 
 * exact same time. We must explore two branching parallel universes:
 *   1. We ignore the character from String1 and keep String2's character.
 *   2. We ignore the character from String2 and keep String1's character.
 * We want the maximum result from these two parallel universes.
 * 
 * Because we will repeatedly evaluate the same combinations of string suffixes 
 * (overlapping subproblems), this requires 2D Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: str1 = "abcde", str2 = "ace"
 * 
 * We compare from the end:
 * 'e' == 'e' -> Match! Length = 1 + LCS("abcd", "ac").
 * 
 * Now comparing "abcd" and "ac":
 * 'd' != 'c' -> Mismatch! We explore two paths:
 *   Path 1: LCS("abc", "ac")  <- Dropped 'd'
 *   Path 2: LCS("abcd", "a")  <- Dropped 'c'
 * 
 * In Path 1, comparing "abc" and "ac":
 * 'c' == 'c' -> Match! Length = 1 + LCS("ab", "a").
 * 
 * The sequence of matches forms "ace" with a total length of 3.
 */
public class LongestCommonSubsequence {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Evaluate from the end of both strings backwards. Branch into 
     * two paths whenever characters do not match.
     * 
     * Time Complexity: O(2^(m+n)) - Exponential branching at every mismatch.
     * Space Complexity: O(m+n) - Maximum depth of the recursion tree.
     */
    public int longestCommonSubsequenceRecursive(String str1, String str2) {
        if (str1 == null || str2 == null) return 0;
        return solveRecursive(str1, str2, str1.length() - 1, str2.length() - 1);
    }

    private int solveRecursive(String str1, String str2, int index1, int index2) {
        // BASE CASE REASONING:
        // If either pointer falls below 0, it means we have completely exhausted 
        // one of the strings. If one string is empty, it is physically impossible 
        // for them to share any more common characters. 
        // The length of any common subsequence with an empty string is 0.
        if (index1 < 0 || index2 < 0) {
            return 0;
        }

        // If characters match, we successfully found 1 common character.
        // We add 1 to our count, and shrink BOTH strings by 1 character.
        if (str1.charAt(index1) == str2.charAt(index2)) {
            return 1 + solveRecursive(str1, str2, index1 - 1, index2 - 1);
        } 
        
        // If they do NOT match, we branch into two possibilities.
        // Option 1: Drop the character from str1 (move index1 back).
        // Option 2: Drop the character from str2 (move index2 back).
        // We greedily take the maximum of these two choices.
        else {
            int dropFromStr1 = solveRecursive(str1, str2, index1 - 1, index2);
            int dropFromStr2 = solveRecursive(str1, str2, index1, index2 - 1);
            return Math.max(dropFromStr1, dropFromStr2);
        }
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the results of [index1][index2] to avoid recalculating 
     * the LCS for the same pair of string suffixes.
     * 
     * Time Complexity: O(m * n) - We evaluate every pair of indices exactly once.
     * Space Complexity: O(m * n) - For the 2D memo array + call stack.
     */
    public int longestCommonSubsequenceMemo(String str1, String str2) {
        if (str1 == null || str2 == null) return 0;
        
        int m = str1.length();
        int n = str2.length();
        int[][] memo = new int[m][n];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(str1, str2, m - 1, n - 1, memo);
    }

    private int solveMemo(String str1, String str2, int index1, int index2, int[][] memo) {
        // BASE CASES (Same physical logic as brute force)
        if (index1 < 0 || index2 < 0) return 0;

        // Return cached result if we've seen these two indices before
        if (memo[index1][index2] != -1) {
            return memo[index1][index2];
        }

        if (str1.charAt(index1) == str2.charAt(index2)) {
            memo[index1][index2] = 1 + solveMemo(str1, str2, index1 - 1, index2 - 1, memo);
        } else {
            int dropFromStr1 = solveMemo(str1, str2, index1 - 1, index2, memo);
            int dropFromStr2 = solveMemo(str1, str2, index1, index2 - 1, memo);
            memo[index1][index2] = Math.max(dropFromStr1, dropFromStr2);
        }

        return memo[index1][index2];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build a 2D spreadsheet. dp[i][j] signifies the LCS of the first 'i' 
     * characters of str1 and the first 'j' characters of str2.
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n)
     */
    public int longestCommonSubsequenceTabulation(String str1, String str2) {
        int m = str1.length();
        int n = str2.length();
        
        // dp[i][j] represents: "The length of the Longest Common Subsequence 
        // using ONLY the first 'i' characters of str1 and the first 'j' characters of str2."
        int[][] dp = new int[m + 1][n + 1];

        // BASE CASE REASONING (Implicit in Java):
        // Row 0 (dp[0][j]): What is the LCS if we use 0 characters from str1? 
        // It's physically 0. (Comparing an empty string against str2).
        // Col 0 (dp[i][0]): What is the LCS if we use 0 characters from str2?
        // It's physically 0. (Comparing str1 against an empty string).
        // Java initializes integer arrays to 0 by default, so we don't need to write this loop!

        // Outer loop (i): We slowly reveal one character of str1 at a time.
        for (int i = 1; i <= m; i++) {
            
            char char1 = str1.charAt(i - 1);
            
            // Inner loop (j): For the current character of str1, we test it against 
            // every gradually revealed character of str2.
            for (int j = 1; j <= n; j++) {
                
                char char2 = str2.charAt(j - 1);

                // PHYSICAL CHECK: Do the two characters we just revealed match?
                if (char1 == char2) {
                    
                    // THEY MATCH! This is the best case scenario.
                    // Because they match, they form a valid pair in our common sequence.
                    // We consume BOTH characters, and look DIAGONALLY UP-LEFT in our spreadsheet.
                    // "What was the best LCS before we revealed these two characters?"
                    // We take that previous best and add 1 to it.
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                    
                } else {
                    
                    // THEY DON'T MATCH. We cannot use both of them in the sequence.
                    // We must decide which character to theoretically discard.
                    
                    // UNIVERSE 1: Discard char1. 
                    // We look DIRECTLY UP in the spreadsheet.
                    // "What was the LCS if we didn't have char1, but we kept char2?"
                    int discardChar1 = dp[i - 1][j];
                    
                    // UNIVERSE 2: Discard char2.
                    // We look DIRECTLY LEFT in the spreadsheet.
                    // "What was the LCS if we kept char1, but didn't have char2?"
                    int discardChar2 = dp[i][j - 1];
                    
                    // Because we are trying to find the LONGEST sequence, we greedily 
                    // take whichever universe gave us a larger number.
                    dp[i][j] = Math.max(discardChar1, discardChar2);
                }
            }
        }

        return dp[m][n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, notice that to calculate row 'i', we ONLY ever look 
     * at the current row 'i' (left) and the previous row 'i-1' (up and diagonal).
     * Any row before 'i-1' is dead memory. We can compress our O(m*n) matrix 
     * into just TWO arrays: one for the 'previous' row and one for the 'current' row.
     * 
     * PRO-TIP: We can swap the strings so the shorter string becomes the columns, 
     * strictly bounding our space complexity to O(min(m, n)).
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(min(m, n)) - Massively reduced memory footprint.
     */
    public int longestCommonSubsequenceSpaceOptimized(String str1, String str2) {
        // Optimization: Ensure str2 is always the shorter string for minimum array size
        if (str1.length() < str2.length()) {
            return longestCommonSubsequenceSpaceOptimized(str2, str1);
        }

        int m = str1.length();
        int n = str2.length();
        
        // We only need two rows of size n + 1
        int[] prevRow = new int[n + 1];
        int[] currRow = new int[n + 1];

        // BASE CASE REASONING:
        // prevRow starts entirely as 0s, representing comparing against an empty str1.
        
        for (int i = 1; i <= m; i++) {
            char char1 = str1.charAt(i - 1);
            
            for (int j = 1; j <= n; j++) {
                char char2 = str2.charAt(j - 1);

                if (char1 == char2) {
                    // Match: Look diagonal up-left (prevRow[j-1])
                    currRow[j] = 1 + prevRow[j - 1];
                } else {
                    // Mismatch: Look directly UP (prevRow[j]) or directly LEFT (currRow[j-1])
                    currRow[j] = Math.max(prevRow[j], currRow[j - 1]);
                }
            }
            
            // Advance the window: The current row now becomes the previous row 
            // for the next iteration. We must deep copy or swap references.
            // Swapping references is faster than System.arraycopy.
            int[] temp = prevRow;
            prevRow = currRow;
            currRow = temp;
            // Note: After swap, currRow holds old data, but it will be safely 
            // overwritten from left to right in the next j-loop.
        }

        // Because we swapped at the end of the very last iteration, our final 
        // answer actually lives in 'prevRow'.
        return prevRow[n];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new LongestCommonSubsequence();
        
        record TestCase(String str1, String str2, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("abcde", "ace", 3),     // "ace"
            new TestCase("abc", "abc", 3),       // "abc"
            new TestCase("abc", "def", 0),       // No common characters
            new TestCase("bsbininm", "jmjkbkjkv", 1), // "b" or "j" etc.
            new TestCase("ezupkr", "ubmrapg", 2) // "ur"
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("String 1: " + tc.str1);
            System.out.println("String 2: " + tc.str2);
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.longestCommonSubsequenceRecursive(tc.str1, tc.str2));
            System.out.println("Memoization       : " + solver.longestCommonSubsequenceMemo(tc.str1, tc.str2));
            System.out.println("Tabulation 2D     : " + solver.longestCommonSubsequenceTabulation(tc.str1, tc.str2));
            System.out.println("Space Optimized   : " + solver.longestCommonSubsequenceSpaceOptimized(tc.str1, tc.str2));
            System.out.println();
        }
    }
}
