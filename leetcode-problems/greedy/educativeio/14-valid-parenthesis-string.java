import java.util.*;

/**
 * ============================================================================
 * INTERVIEW GUIDE: VALID PARENTHESIS STRING
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Can the string be empty?" 
 *      (Assumption: The constraints say length >= 1, but an empty string is 
 *      technically valid since it has no unmatched brackets).
 *    - "Can a '*' act as a left bracket '(' and match a previously seen ')'?" 
 *      (Assumption: No. Order strictly matters. A ')' can only be matched by 
 *      a '(' or '*' that appeared BEFORE it).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Determine if brackets can be perfectly matched given '*' is a wildcard.
 *    
 *    - Observation 1 (Why a standard stack fails): In normal bracket problems, 
 *      a stack is perfect. But '*' can be three different things ('(', ')', or ""). 
 *      Branching recursively for every '*' leads to O(3^N) time complexity. We 
 *      need something smarter.
 *      
 *    - Observation 2 (The Two-Stack Intuition): If we use two stacks—one for 
 *      the indices of '(' and one for the indices of '*'—we can resolve ')' 
 *      as they appear. 
 *      When we see ')', we prefer to match it with an actual '(' (pop stack 1). 
 *      If we have no '(', we use a wildcard '*' (pop stack 2). If both are empty, 
 *      it's invalid! After parsing the string, we pair up leftover '(' with leftover 
 *      '*', ensuring the '*' comes AFTER the '('.
 *      
 *    - Observation 3 (The Greedy Range Intuition - Optimal): Instead of storing 
 *      indices, what if we just track the *range* of possible open brackets?
 *      Let `minOpen` be the minimum possible open brackets we MUST close.
 *      Let `maxOpen` be the maximum possible open brackets we COULD close.
 *      - If we see '(', both minOpen and maxOpen increase.
 *      - If we see ')', both decrease.
 *      - If we see '*', it could be '(' (maxOpen++), ')' (minOpen--), or "" (no change).
 *      If maxOpen < 0, we have too many ')'. Invalid.
 *      If minOpen < 0, it just means some '*' we forced to be ')' shouldn't be. 
 *      We reset minOpen to 0. 
 *      At the end, if minOpen == 0, a valid combination exists!
 * 
 * 3. VISUAL EXPLANATION (Greedy Approach):
 *    String: "(*))"
 *    
 *    Variables: minOpen = 0, maxOpen = 0
 *    
 *    Char '(': minOpen = 1, maxOpen = 1
 *    Char '*': minOpen = 0, maxOpen = 2 
 *              (It could act as ')' resolving the '(', or act as another '(')
 *    Char ')': minOpen = -1 -> reset to 0, maxOpen = 1
 *              (We can't have negative open brackets, meaning our '*' must have been "" or '(')
 *    Char ')': minOpen = -1 -> reset to 0, maxOpen = 0
 *    
 *    End of string. maxOpen never dropped below 0. minOpen is 0. 
 *    Result: TRUE!
 * 
 * ============================================================================
 */
public class ValidParenthesisString {

    /**
     * APPROACH 1: Greedy Range Tracking (Optimal)
     * 
     * Time Complexity: O(N) where N is the length of the string.
     * Space Complexity: O(1) as we only use two integer counters.
     */
    public boolean checkValidStringGreedy(String s) {
        int minOpen = 0; // Minimum open '(' we MUST match
        int maxOpen = 0; // Maximum open '(' we COULD match
        
        for (char c : s.toCharArray()) {
            if (c == '(') {
                minOpen++;
                maxOpen++;
            } else if (c == ')') {
                minOpen--;
                maxOpen--;
            } else {
                // c == '*'
                minOpen--; // If we treat '*' as ')'
                maxOpen++; // If we treat '*' as '('
            }
            
            // If the maximum possible open brackets is negative, 
            // it means we have absolutely too many ')' to ever recover.
            if (maxOpen < 0) {
                return false;
            }
            
            // We can't have negative open brackets. If minOpen drops below 0, 
            // it just means some '*' that we counted as ')' should actually 
            // be treated as an empty string "" instead.
            if (minOpen < 0) {
                minOpen = 0;
            }
        }
        
        // If the minimum required open brackets is exactly 0, it's valid!
        return minOpen == 0;
    }

    /**
     * APPROACH 2: Two Stacks for Indices (Highly Intuitive)
     * 
     * In an interview, if the greedy approach is hard to remember, this Two-Stack 
     * approach is incredibly logical and easy to explain.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(N) in the worst case (all '(' or '*').
     */
    public boolean checkValidStringTwoStacks(String s) {
        // Stack to store the INDICES of open brackets
        Deque<Integer> openBrackets = new ArrayDeque<>();
        // Stack to store the INDICES of stars
        Deque<Integer> stars = new ArrayDeque<>();
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            
            if (c == '(') {
                openBrackets.push(i);
            } else if (c == '*') {
                stars.push(i);
            } else {
                // c == ')'
                if (!openBrackets.isEmpty()) {
                    // Prefer matching with a real '('
                    openBrackets.pop();
                } else if (!stars.isEmpty()) {
                    // Fallback to matching with a wildcard '*'
                    stars.pop();
                } else {
                    // No '(' and no '*' available to match this ')'
                    return false;
                }
            }
        }
        
        // Now resolve any remaining '(' with remaining '*'
        while (!openBrackets.isEmpty() && !stars.isEmpty()) {
            // A star can only match an open bracket if it appears AFTER it!
            if (openBrackets.peek() < stars.peek()) {
                openBrackets.pop();
                stars.pop();
            } else {
                // The star is BEFORE the '('. It cannot close it.
                break;
            }
        }
        
        // If there are no unmatched open brackets left, it's valid.
        return openBrackets.isEmpty();
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records provide a concise way to create immutable data carriers.
     */
    record TestCase(String s, boolean expected) {}

    public static void main(String[] args) {
        ValidParenthesisString solver = new ValidParenthesisString();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase("()", true),
            new TestCase("(*)", true),
            new TestCase("(*))", true),
            new TestCase("*(", false),     // Star cannot close a parenthesis after it
            new TestCase("((*)", true),    // Star acts as empty string
            new TestCase("((**)", true),   // Stars act as closing brackets
            new TestCase(")", false)       // Immediate failure
        );
        
        System.out.println("--- Running Approach 1 (Greedy O(1) Space) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            boolean result = solver.checkValidStringGreedy(tc.s());
            System.out.printf("Test %d: Expected = %b, Got = %b -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Two Stacks O(N) Space) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            boolean result = solver.checkValidStringTwoStacks(tc.s());
            System.out.printf("Test %d: Expected = %b, Got = %b -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}

/**
 * ============================================================
 * 🔥 TOP DOWN DP (RECURSION + MEMOIZATION)
 * ============================================================
 *
 * STATE:
 * dp[index][open] → whether valid from index onward
 *
 * index → current position in string
 * open  → number of unmatched '('
 *
 * TIME  : O(n^2)
 * SPACE : O(n^2)
 */
public class ValidParenthesisString_TopDown {

    public static void main(String[] args) {
        System.out.println(checkValidString("(*)"));   // true
        System.out.println(checkValidString("(*))"));  // true
    }

    public static boolean checkValidString(String s) {
        int n = s.length();

        // memo: -1 = unvisited, 0 = false, 1 = true
        int[][] memo = new int[n][n + 1];

        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }

        return dfs(0, 0, s, memo);
    }

    /**
     * DFS function
     *
     * @param i    current index
     * @param open unmatched '(' count
     */
    private static boolean dfs(int i, int open, String s, int[][] memo) {

        // ❌ invalid: too many ')'
        if (open < 0) return false;

        // ✅ reached end → valid only if no open brackets left
        if (i == s.length()) {
            return open == 0;
        }

        // Memo check
        if (memo[i][open] != -1) {
            return memo[i][open] == 1;
        }

        char ch = s.charAt(i);
        boolean result = false;

        if (ch == '(') {
            // must open
            result = dfs(i + 1, open + 1, s, memo);
        } 
        else if (ch == ')') {
            // must close
            result = dfs(i + 1, open - 1, s, memo);
        } 
        else { // '*'
            // 3 possibilities:

            // 1️⃣ Treat as '('
            boolean takeOpen = dfs(i + 1, open + 1, s, memo);

            // 2️⃣ Treat as ')'
            boolean takeClose = dfs(i + 1, open - 1, s, memo);

            // 3️⃣ Treat as empty
            boolean takeEmpty = dfs(i + 1, open, s, memo);

            result = takeOpen || takeClose || takeEmpty;
        }

        // store result
        memo[i][open] = result ? 1 : 0;
        return result;
    }
}

/**
 * ============================================================
 * 🔥 BOTTOM UP DP (TABULATION)
 * ============================================================
 *
 * dp[i][open] = can substring from i be valid with `open` unmatched '('
 *
 * BASE:
 * dp[n][0] = true  (empty string with no open brackets is valid)
 *
 * TIME  : O(n^2)
 * SPACE : O(n^2)
 */
public class ValidParenthesisString_BottomUp {

    public static void main(String[] args) {
        System.out.println(checkValidString("(*)"));   // true
        System.out.println(checkValidString("(*))"));  // true
    }

    public static boolean checkValidString(String s) {

        int n = s.length();

        boolean[][] dp = new boolean[n + 1][n + 1];

        // BASE CASE
        dp[n][0] = true;

        // Fill from bottom (i = n-1 → 0)
        for (int i = n - 1; i >= 0; i--) {

            char ch = s.charAt(i);

            for (int open = 0; open <= n; open++) {

                boolean result = false;

                if (ch == '(') {
                    // must open
                    if (open + 1 <= n) {
                        result = dp[i + 1][open + 1];
                    }
                } 
                else if (ch == ')') {
                    // must close
                    if (open - 1 >= 0) {
                        result = dp[i + 1][open - 1];
                    }
                } 
                else { // '*'

                    // 3 possibilities:

                    // 1️⃣ treat as '('
                    if (open + 1 <= n) {
                        result |= dp[i + 1][open + 1];
                    }

                    // 2️⃣ treat as ')'
                    if (open - 1 >= 0) {
                        result |= dp[i + 1][open - 1];
                    }

                    // 3️⃣ treat as empty
                    result |= dp[i + 1][open];
                }

                dp[i][open] = result;
            }
        }

        // answer: start from index 0 with 0 open
        return dp[0][0];
    }
}
