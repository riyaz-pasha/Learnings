import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Decode Ways
 * Given a string of digits, return the number of possible ways to decode it.
 * Mapping: "1" -> 'A', "2" -> 'B', ..., "26" -> 'Z'.
 * Note: "06" cannot be mapped to 'F'. A sequence must not have leading zeros 
 * to be considered a valid 2-digit number.
 * 
 * Constraints:
 * 1 <= decodeStr.length <= 100
 * decodeStr contains only digits and may contain leading zero(s).
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, acknowledging the edge cases immediately is critical:
 * 
 * Q: "Can the string start with a '0'?"
 * A: Yes, the constraints say it may contain leading zeros. If it starts with '0', 
 *    it is physically impossible to decode because '0' doesn't map to anything, 
 *    and "0X" is invalid. We should immediately return 0.
 * 
 * Q: "What if there are consecutive zeros like '100'?"
 * A: The first zero makes '10', which is 'J'. The second zero stands alone, 
 *    which is invalid, or forms '00', which is invalid. Thus, "100" has 0 ways 
 *    to decode. Our logic must naturally handle these dead ends.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given character at index 'i', I have two potential choices to decode:
 * 
 * 1. SINGLE DIGIT: I can try to decode the character by itself.
 *    - Condition: The character MUST NOT be '0'.
 * 
 * 2. DOUBLE DIGIT: I can try to group this character with the next one.
 *    - Condition: The two-character string must form a valid number between 
 *      10 and 26.
 * 
 * Since I'm exploring combinations and the number of ways to decode the rest 
 * of the string remains exactly the same regardless of how I reached index 'i', 
 * we have overlapping subproblems. This requires Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: s = "226"
 * Let's trace from the start (Index 0):
 * 
 * '2': Valid single digit. Recurse on "26".
 *   -> "26":
 *      -> '2': Valid single digit. Recurse on "6".
 *         -> '6': Valid single digit. Recurse on "" (End of string -> +1 way).
 *      -> "26": Valid double digit. Recurse on "" (End of string -> +1 way).
 *      (Total for "26" is 2 ways: "B,F" and "Z")
 * 
 * "22": Valid double digit. Recurse on "6".
 *   -> "6":
 *      -> '6': Valid single digit. Recurse on "" (End of string -> +1 way).
 *      (Total for "6" is 1 way: "F")
 * 
 * Total ways = 2 + 1 = 3 ways. ("B,B,F", "B,Z", "V,F")
 */
public class DecodeWays {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Traverse the string. At each step, branch into a 1-digit decode 
     * and a 2-digit decode if they are valid.
     * 
     * Time Complexity: O(2^n) - Exponential branching at every character.
     * Space Complexity: O(n) - Maximum depth of the recursion tree.
     */
    public int numDecodingsRecursive(String s) {
        if (s == null || s.length() == 0) return 0;
        return solveRecursive(s, 0);
    }

    private int solveRecursive(String s, int index) {
        // BASE CASE REASONING:
        // If we have successfully parsed every character and advanced our index 
        // to the very end of the string, it means the sequence of choices we made 
        // resulted in a perfectly valid decoding. Return 1 to count this path.
        if (index == s.length()) {
            return 1;
        }

        // BASE CASE REASONING (The '0' Trap):
        // If the current character is '0', it CANNOT be decoded as a single digit, 
        // nor can it be the start of a 2-digit number (e.g., "01" is invalid).
        // This entire path is a failure. Return 0.
        if (s.charAt(index) == '0') {
            return 0;
        }

        // Choice 1: Take 1 character
        // We know it's valid because it's not '0' (handled by the if statement above).
        int ways = solveRecursive(s, index + 1);

        // Choice 2: Take 2 characters
        // We must check if there is actually a second character available.
        if (index + 1 < s.length()) {
            // Extract the two characters and parse them as an integer.
            int twoDigitVal = Integer.parseInt(s.substring(index, index + 2));
            
            // It is only valid if it falls between 10 and 26 inclusive.
            if (twoDigitVal >= 10 && twoDigitVal <= 26) {
                ways += solveRecursive(s, index + 2);
            }
        }

        return ways;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the number of ways to decode the substring starting at `index`.
     * 
     * Time Complexity: O(n) - We evaluate each index exactly once.
     * Space Complexity: O(n) - For the memo array + call stack.
     */
    public int numDecodingsMemo(String s) {
        if (s == null || s.length() == 0) return 0;
        
        Integer[] memo = new Integer[s.length()];
        return solveMemo(s, 0, memo);
    }

    private int solveMemo(String s, int index, Integer[] memo) {
        // BASE CASES (Same physical logic as brute force)
        if (index == s.length()) return 1;
        if (s.charAt(index) == '0') return 0;

        if (memo[index] != null) {
            return memo[index];
        }

        int ways = solveMemo(s, index + 1, memo);

        if (index + 1 < s.length()) {
            int twoDigitVal = Integer.parseInt(s.substring(index, index + 2));
            if (twoDigitVal >= 10 && twoDigitVal <= 26) {
                ways += solveMemo(s, index + 2, memo);
            }
        }

        memo[index] = ways;
        return memo[index];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 1D)
     * ========================================================================
     * Idea: Build an array where dp[i] represents the total number of valid 
     * ways to decode a string of length 'i' (the prefix of the string).
     * 
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public int numDecodingsTabulation(String s) {
        if (s == null || s.length() == 0) return 0;
        
        // Immediate short-circuit: A string starting with '0' is completely invalid.
        if (s.charAt(0) == '0') return 0;

        int n = s.length();
        
        // dp[i] signifies: "How many ways can I decode the first 'i' characters of the string?"
        int[] dp = new int[n + 1];

        // BASE CASE REASONING:
        // dp[0]: An empty string has 1 valid way to be decoded (by doing nothing). 
        // This is mathematically necessary to anchor the 2-digit lookbacks.
        dp[0] = 1;
        
        // dp[1]: A string of length 1 has exactly 1 valid decoding (since we already 
        // proved it isn't '0' in our short-circuit above).
        dp[1] = 1;

        // We iterate starting from length 2 up to length 'n'.
        for (int i = 2; i <= n; i++) {
            
            // --- DETAILED TABULATION EXPLANATION ---
            
            // Step 1: Look at the SINGLE character just added (at index i-1).
            // Can this single character stand on its own?
            // Yes, as long as it's not a '0'.
            int oneDigit = s.charAt(i - 1) - '0';
            if (oneDigit >= 1 && oneDigit <= 9) {
                // If it is valid, it simply rides on the coattails of whatever 
                // combinations we made up to length (i - 1). 
                // We add those exact same ways to our current total.
                dp[i] += dp[i - 1];
            }
            
            // Step 2: Look at the DOUBLE character formed by this character and the one before it.
            // Can these two characters combine to form a valid alphabet mapping?
            int twoDigits = Integer.parseInt(s.substring(i - 2, i));
            if (twoDigits >= 10 && twoDigits <= 26) {
                // If they form a valid pair (like "15" -> O), they consume TWO characters.
                // This means they ride on the combinations we made up to length (i - 2).
                // We ADD those historical combinations to our total.
                dp[i] += dp[i - 2];
            }
            
            // Note: If both conditions fail (e.g., "00" or "30"), dp[i] naturally 
            // remains 0, which correctly kills that dead-end path!
        }

        return dp[n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, to calculate dp[i], we ONLY ever look at dp[i-1] 
     * and dp[i-2]. The rest of the array history is completely unused.
     * We can collapse the O(n) array into two O(1) integer variables!
     * 
     * Time Complexity: O(n) - Single pass through the string.
     * Space Complexity: O(1) - Massively reduced memory footprint.
     */
    public int numDecodingsSpaceOptimized(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) == '0') {
            return 0;
        }

        // BASE CASE REASONING:
        // 'twoStepsBack' represents dp[i-2]. Initially, this is dp[0] (empty string).
        int twoStepsBack = 1;
        
        // 'oneStepBack' represents dp[i-1]. Initially, this is dp[1] (first char).
        int oneStepBack = 1;

        for (int i = 2; i <= s.length(); i++) {
            
            int currentWays = 0;
            
            // Check 1-digit
            if (s.charAt(i - 1) != '0') {
                currentWays += oneStepBack;
            }
            
            // Check 2-digit
            int twoDigits = Integer.parseInt(s.substring(i - 2, i));
            if (twoDigits >= 10 && twoDigits <= 26) {
                currentWays += twoStepsBack;
            }
            
            // Advance our sliding window variables forward for the next iteration.
            twoStepsBack = oneStepBack;
            oneStepBack = currentWays;
        }

        // The answer sits in 'oneStepBack' at the end of the loop.
        return oneStepBack;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new DecodeWays();
        
        record TestCase(String s, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("12", 2),          // "AB" or "L"
            new TestCase("226", 3),         // "BBF", "BZ", "VF"
            new TestCase("06", 0),          // Leading zero is invalid
            new TestCase("231012", 4),      // From problem statement
            new TestCase("2101", 1),        // "2", "10", "1" -> B J A. ("21", "01" invalid)
            new TestCase("111111111111111111111111111111111111111111111", 1836311903) // Stress test
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("String  : " + tc.s);
            System.out.println("Expected: " + tc.expected);
            
            // Throttle brute force recursion to avoid locking up on long strings
            if (tc.s.length() <= 20) {
                System.out.println("Recursive (Brute) : " + solver.numDecodingsRecursive(tc.s));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Too long for O(2^N))");
            }
            
            System.out.println("Memoization       : " + solver.numDecodingsMemo(tc.s));
            System.out.println("Tabulation        : " + solver.numDecodingsTabulation(tc.s));
            System.out.println("Space Optimized   : " + solver.numDecodingsSpaceOptimized(tc.s));
            System.out.println();
        }
    }
}
