/**
 * ============================================================================
 * PROBLEM STATEMENT: Generate Parentheses
 * ============================================================================
 * For a given number, n, generate all combinations of balanced parentheses.
 * 
 * Constraints:
 * - 1 <= n <= 10
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS (To ask in an interview):
 * ============================================================================
 * 1. Can n be 0 or negative? 
 *    (Constraint states n >= 1. If n=0, we'd typically return an empty string 
 *    or [""]).
 * 2. Are there other types of brackets involved like [] or {}?
 *    (No, the problem exclusively asks for round parentheses).
 * 3. Does the order of the output strings matter?
 *    (Usually no, but standard backtracking natively produces them in 
 *    lexicographical order).
 * 
 * ============================================================================
 * INTERVIEW APPROACH:
 * ============================================================================
 * 1. Acknowledge constraints: n <= 10. The output size for n pairs is the 
 *    n-th Catalan number. For n=10, this is 16,796, which is well within 
 *    acceptable bounds for an O(Catalan(n)) approach.
 * 2. Discuss the Brute Force approach first to establish a baseline: Generate 
 *    all 2^(2n) possible sequences of '(' and ')' and filter out the invalid 
 *    ones. Acknowledge this is highly inefficient.
 * 3. Propose the Optimal Backtracking approach: We only add '(' or ')' when 
 *    we know it will lead to a valid sequence. We do this by keeping track 
 *    of the count of 'open' and 'close' brackets used so far.
 * 4. (Bonus) Mention the Dynamic Programming (Closure Number) approach. This 
 *    shows deep mathematical maturity, recognizing that any valid sequence 
 *    can be built by decomposing it into smaller valid sub-sequences.
 * 
 * ============================================================================
 * TIME & SPACE COMPLEXITY (For Backtracking and DP):
 * ============================================================================
 * - Time Complexity: O(4^n / sqrt(n)). This is bounded by the n-th Catalan 
 *   number. We generate exactly the valid permutations and nothing else.
 * - Space Complexity: O(4^n / sqrt(n)) to store the results. The auxiliary 
 *   space for the recursion stack in DFS is O(2n) = O(n).
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

class GenerateParenthesesSolver {

    public static void main(String[] args) {
        int n = 3;
        
        System.out.println("Input n: 3 (Pairs of parentheses)\n");

        System.out.println("1. Optimal Backtracking Approach:");
        System.out.println(generateParenthesesBacktracking(n));
        
        System.out.println("\n2. Dynamic Programming (Closure Number) Approach:");
        System.out.println(generateParenthesesDP(n));
        
        System.out.println("\n3. Brute Force (Generate and Validate) Approach:");
        System.out.println(generateParenthesesBruteForce(n));
    }

    /**
     * ========================================================================
     * SOLUTION 1: BACKTRACKING (OPTIMAL & STANDARD)
     * ========================================================================
     * Idea & Intuition:
     * Instead of adding '(' and ')' blindly, we constrain our choices:
     * 1. We can only add an open parenthesis '(' if we haven't used all 'n' of them.
     * 2. We can only add a close parenthesis ')' if we have more open parentheses 
     *    currently in our string than close parentheses (this ensures it stays balanced).
     * 
     * Visual Decision Tree for n=2:
     *                            "" (0, 0)
     *                                |
     *                           "(" (1, 0)
     *                           /          \
     *                 "((" (2, 0)          "()" (1, 1)
     *                     |                      |
     *                "(()" (2, 1)          "()(" (2, 1)
     *                     |                      |
     *               "(())" (2, 2)          "()()" (2, 2)
     * 
     * Key Observation: Since we know the final string will be exactly 2*n characters 
     * long, using a char[] array in-place is the absolute fastest way to build 
     * the strings, avoiding the overhead of StringBuilder resizing/modifying.
     */
    public static List<String> generateParenthesesBacktracking(int n) {
        var result = new ArrayList<String>();
        // Using char array for maximum performance, length will always be 2 * n
        char[] currentString = new char[2 * n];
        backtrack(result, currentString, 0, 0, 0, n);
        return result;
    }

    private static void backtrack(List<String> result, char[] currentString, 
                                  int index, int openCount, int closeCount, int max) {
        // Base case: the sequence is complete when we reach 2 * n characters
        if (index == currentString.length) {
            result.add(new String(currentString));
            return;
        }

        // Choice 1: Add an open parenthesis if we have remaining ones
        if (openCount < max) {
            currentString[index] = '(';
            backtrack(result, currentString, index + 1, openCount + 1, closeCount, max);
        }

        // Choice 2: Add a close parenthesis if it keeps the sequence balanced
        if (closeCount < openCount) {
            currentString[index] = ')';
            backtrack(result, currentString, index + 1, openCount, closeCount + 1, max);
        }
    }

    /**
     * ========================================================================
     * SOLUTION 2: DYNAMIC PROGRAMMING (CLOSURE NUMBER)
     * ========================================================================
     * Idea & Intuition:
     * Every balanced sequence of length 2n can be thought of as:
     * "(" + {valid sequence of length 2*i} + ")" + {valid sequence of length 2*(n-1-i)}
     * where 'i' is the number of pairs inside the first outer parentheses.
     * 
     * Example for n=3:
     * To build for n=3, we look at combinations of i from 0 to 2:
     * - i=0: "(" + dp[0] + ")" + dp[2] -> "(" + "" + ")" + ["()()", "(())"]
     * - i=1: "(" + dp[1] + ")" + dp[1] -> "(" + "()" + ")" + ["()"]
     * - i=2: "(" + dp[2] + ")" + dp[0] -> "(" + ["()()", "(())"] + ")" + ""
     * 
     * Key Observation: This is a bottom-up approach that iteratively builds 
     * larger solutions from smaller, already computed subproblems.
     */
    public static List<String> generateParenthesesDP(int n) {
        if (n == 0) return new ArrayList<>(Collections.singletonList(""));
        
        // dp.get(i) will store all valid combinations for i pairs of parentheses
        List<List<String>> dp = new ArrayList<>();
        dp.add(Collections.singletonList("")); // Base case: 0 pairs

        for (int i = 1; i <= n; i++) {
            var currentList = new ArrayList<String>();
            // j represents the number of pairs INSIDE the first pair of parentheses
            for (int j = 0; j < i; j++) {
                List<String> insideList = dp.get(j);
                List<String> outsideList = dp.get(i - 1 - j);
                
                for (String inside : insideList) {
                    for (String outside : outsideList) {
                        currentList.add("(" + inside + ")" + outside);
                    }
                }
            }
            dp.add(currentList);
        }
        
        return dp.get(n);
    }

    /**
     * ========================================================================
     * SOLUTION 3: BRUTE FORCE (GENERATE ALL AND VALIDATE)
     * ========================================================================
     * Idea & Intuition:
     * Generate every possible sequence of length 2*n containing only '(' and ')'.
     * Once a sequence of length 2*n is generated, check if it is mathematically 
     * balanced.
     * 
     * Why we discuss this: It's important to demonstrate to an interviewer 
     * that you know how to identify the absolute worst-case scenario and why 
     * it needs optimization.
     * 
     * Time Complexity: O(2^(2n) * n). 2^(2n) combinations, taking O(n) to validate each.
     */
    public static List<String> generateParenthesesBruteForce(int n) {
        var result = new ArrayList<String>();
        char[] currentString = new char[2 * n];
        generateAll(currentString, 0, result);
        return result;
    }

    private static void generateAll(char[] currentString, int index, List<String> result) {
        if (index == currentString.length) {
            if (isValid(currentString)) {
                result.add(new String(currentString));
            }
            return;
        }

        currentString[index] = '(';
        generateAll(currentString, index + 1, result);
        
        currentString[index] = ')';
        generateAll(currentString, index + 1, result);
    }

    private static boolean isValid(char[] currentString) {
        int balance = 0;
        for (char c : currentString) {
            if (c == '(') {
                balance++;
            } else {
                balance--;
            }
            // If balance ever goes negative, we have more closing than opening brackets
            if (balance < 0) {
                return false;
            }
        }
        // At the end, balance must be exactly 0 (equal number of open and close)
        return balance == 0;
    }
}

/**
 * ============================================================
 * 🔥 Generate Balanced Parentheses — Backtracking
 * ============================================================
 *
 * Key Idea:
 * Build string using decision tree while maintaining validity.
 *
 * Rules:
 * 1. Add '(' if open < n
 * 2. Add ')' if close < open
 *
 * ============================================================
 */
class GenerateParentheses {

    public static void main(String[] args) {
        int n = 3;
        System.out.println(generateParenthesis(n));
    }

    public static List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();

        // Start recursion
        backtrack(result, new StringBuilder(), 0, 0, n);

        return result;
    }

    /**
     * @param result -> stores valid combinations
     * @param current -> current string being built
     * @param open -> number of '(' used
     * @param close -> number of ')' used
     * @param n -> total pairs
     */
    private static void backtrack(List<String> result,
                                  StringBuilder current,
                                  int open,
                                  int close,
                                  int n) {

        // 🎯 Base Case: valid combination formed
        if (open == n && close == n) {
            result.add(current.toString());
            return;
        }

        // ✅ Choice 1: Add '('
        if (open < n) {
            current.append('(');
            backtrack(result, current, open + 1, close, n);

            // 🔙 Undo (Backtrack)
            current.deleteCharAt(current.length() - 1);
        }

        // ✅ Choice 2: Add ')'
        if (close < open) {
            current.append(')');
            backtrack(result, current, open, close + 1, n);

            // 🔙 Undo
            current.deleteCharAt(current.length() - 1);
        }
    }
}
