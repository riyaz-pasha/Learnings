/**
 * ============================================================================
 * LONGEST VALID PARENTHESES - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given a string containing only '(' and ')', find the length of 
 * the longest contiguous substring that is a valid parentheses sequence.
 * 
 * Simple Explanation for Interviewer:
 * "We are looking for the longest unbroken chain of properly opened and closed 
 * parentheses. If the chain breaks (e.g., we get a closing bracket without an 
 * opening one), we must start looking for a new valid segment. We need to 
 * return the maximum length found."
 * 
 * Core Idea & Intuition:
 * Identifying valid parentheses usually involves a Stack. However, finding the 
 * *longest contiguous* valid segment introduces a new challenge: how do we connect 
 * adjacent valid segments? (e.g., "()()"). 
 * 
 * Key Observations:
 * 1. A valid substring is always interrupted by an extra ')'. 
 * 2. An extra '(' doesn't immediately invalidate the substring, but it might 
 *    remain unresolved at the end of the string.
 * 3. By tracking the *indices* of unresolved characters, the distance between 
 *    the current index and the last unresolved index gives the exact length of 
 *    the valid substring!
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Is the goal to find a substring (contiguous) or subsequence (scattered)?"
 *    Why: Subsequences are usually DP problems, whereas substrings can often 
 *    be solved with sliding windows or stacks. (Prompt confirms: contiguous substring).
 * 
 * 2. Q: "Can the string be empty?"
 *    Why: Basic edge case. 
 *    Impact: If yes, we need a quick `if (s.isEmpty()) return 0;`.
 * 
 * 3. Q: "Will the string contain other characters like letters or numbers?"
 *    Why: If it does, they might interrupt a valid sequence or be ignored.
 *    Impact: Prompt guarantees only '(' and ')'.
 * 
 * 4. Q: "Are we constrained by memory? Is O(N) space acceptable?"
 *    Why: There is a brilliant O(1) space solution. Asking this sets you up 
 *    to present it as a major optimization later.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: "(()"
 * Output: 2 (The valid part is "()")
 * 
 * Example 2:
 * Input: ")()())"
 * Output: 4 (The valid part is "()()")
 * 
 * Visualizing the Stack approach for ")()())":
 * The stack stores *indices*. We initialize the stack with -1 to represent the 
 * "base" or "floor" of our current sequence.
 * 
 * Char | Index | Action                      | Stack State | Max Length
 * ---------------------------------------------------------------------
 * Init |       | Push -1 (Base)              | [-1]        | 0
 *  )   |  0    | Pop, stack empty! Push 0    | [0]         | 0 (Base moves to 0)
 *  (   |  1    | Push 1                      | [0, 1]      | 0
 *  )   |  2    | Pop (1). len = 2 - peek(0)  | [0]         | max(0, 2) = 2
 *  (   |  3    | Push 3                      | [0, 3]      | 2
 *  )   |  4    | Pop (3). len = 4 - peek(0)  | [0]         | max(2, 4) = 4
 *  )   |  5    | Pop (0), stack empty! Push 5| [5]         | 4 (Base moves to 5)
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Confirm we are finding contiguous valid segments.
 * 2. Clarify: Ask about empty strings and memory constraints.
 * 3. Restate: "I need to find the longest unbroken sequence of matching brackets."
 * 4. Brute Force: Mention we could check every even-length substring for validity (O(N^3)).
 * 5. Better (Stack): "Since we need to match pairs, we can use a stack. But instead 
 *    of storing characters, we store indices to calculate lengths."
 * 6. Code: Write the Stack approach. It shows strong fundamental understanding.
 * 7. Optimize (Two-Pass): If prompted for O(1) space, explain how scanning 
 *    left-to-right, then right-to-left with counters achieves this.
 * 8. Dry-run: Trace the counter approach with ")()())".
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Completely invalid: "))))" -> Returns 0.
 * - All opening brackets: "((((" -> Returns 0.
 * - Valid sequence at the very end: "(()())" -> Returns 6.
 * - Nested valid sequences: "((()))" -> Returns 6.
 * 
 * Common Mistakes:
 * - Forgetting to initialize the Stack with -1. Without this "anchor", you 
 *   cannot calculate the length of a valid sequence that starts at index 0.
 * - In the Two-Pass approach, forgetting the Right-to-Left pass. Scanning only 
 *   left-to-right fails on strings like "(()" because the extra '(' never gets 
 *   resolved, so `left == right` is never true.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class LongestValidParentheses {

    public static void main(String[] args) {
        String test1 = "(()";
        String test2 = ")()())";
        String test3 = "()(()";

        System.out.println("--- Stack Approach (O(N) Space) ---");
        System.out.println("(()     -> " + longestValidParenthesesStack(test1)); // 2
        System.out.println(")()())  -> " + longestValidParenthesesStack(test2)); // 4
        System.out.println("()(()   -> " + longestValidParenthesesStack(test3)); // 2

        System.out.println("\n--- Optimal Two-Pass Approach (O(1) Space) ---");
        System.out.println("(()     -> " + longestValidParenthesesOptimal(test1)); // 2
        System.out.println(")()())  -> " + longestValidParenthesesOptimal(test2)); // 4
        System.out.println("()(()   -> " + longestValidParenthesesOptimal(test3)); // 2
    }

    /**
     * SOLUTION 1: STACK APPROACH
     * ------------------------------------------------------------------------
     * Idea: We use a Stack to store the INDICES of the characters, not the characters 
     * themselves. We seed the stack with -1, which represents the boundary right 
     * before the beginning of a potential valid substring.
     * When we see '(', we push its index.
     * When we see ')', we pop. If the stack becomes empty, it means this ')' is 
     * unmatched. It now becomes our new "boundary", so we push its index.
     * If the stack is not empty after popping, we just formed a valid pair! The 
     * length of the contiguous valid string so far is `current_index - stack.top()`.
     * 
     * Time Complexity: O(N) - We visit every character exactly once.
     * Space Complexity: O(N) - The stack can grow up to size N (e.g., "(((((").
     */
    public static int longestValidParenthesesStack(String s) {
        if (s == null || s.length() < 2) return 0;

        int maxLength = 0;
        Deque<Integer> stack = new ArrayDeque<>();
        
        // Seed the stack with -1 to act as a base anchor for length calculations.
        stack.push(-1);

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                // Unresolved opening bracket, save its index
                stack.push(i);
            } else {
                // Encountered a closing bracket, resolve the most recent opening bracket
                stack.pop();
                
                if (stack.isEmpty()) {
                    // There was no opening bracket to match!
                    // This closing bracket is invalid. It becomes our new anchor/base.
                    stack.push(i);
                } else {
                    // A valid match was made. The length is the distance from the 
                    // current index to the new top of the stack (which is the index 
                    // right BEFORE our contiguous valid sequence started).
                    maxLength = Math.max(maxLength, i - stack.peek());
                }
            }
        }
        
        return maxLength;
    }

    /**
     * SOLUTION 2: OPTIMAL TWO-PASS (Counters)
     * ------------------------------------------------------------------------
     * Idea: We can avoid O(N) space by just counting brackets.
     * Pass 1 (Left to Right): Track `left` and `right` counts.
     * - If left == right, we have a valid sequence! Length is 2 * right.
     * - If right > left, the sequence is broken by an extra ')'. Reset counters.
     * 
     * Why two passes? 
     * If we only scan left-to-right, a string like "(()" will result in 
     * left=2, right=1. Since left never equals right, we would return 0.
     * To fix this, we do Pass 2 (Right to Left).
     * Pass 2 (Right to Left): 
     * - If left == right, we have a valid sequence.
     * - If left > right, the sequence is broken by an extra '('. Reset counters.
     * 
     * Detailed Dry Run for "()(()":
     * L->R:
     * - '(': L=1, R=0
     * - ')': L=1, R=1. Equal! max = 2.
     * - '(': L=2, R=1
     * - '(': L=3, R=1
     * - ')': L=3, R=2. Never equal again. Max is 2.
     * 
     * R->L:
     * - ')': L=0, R=1
     * - '(': L=1, R=1. Equal! max = max(2, 2) = 2.
     * - '(': L=2, R=1. L > R! Invalid. Reset L=0, R=0.
     * - ')': L=0, R=1
     * - '(': L=1, R=1. Equal! max = 2.
     * Result: 2.
     * 
     * Time Complexity: O(N) - Two complete passes through the string.
     * Space Complexity: O(1) - Only using integer variables.
     */
    public static int longestValidParenthesesOptimal(String s) {
        if (s == null || s.length() < 2) return 0;

        int maxLength = 0;
        int left = 0, right = 0;

        // Pass 1: Left to Right
        // Handles cases where ')' breaks validity
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                left++;
            } else {
                right++;
            }

            if (left == right) {
                maxLength = Math.max(maxLength, 2 * right);
            } else if (right > left) {
                // Invalid state: more closing than opening brackets.
                // Reset the counters to start fresh from the next character.
                left = 0;
                right = 0;
            }
        }

        // Reset counters for the second pass
        left = 0;
        right = 0;

        // Pass 2: Right to Left
        // Handles cases where '(' is extra and unresolved (e.g., "(()" )
        for (int i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) == '(') {
                left++;
            } else {
                right++;
            }

            if (left == right) {
                maxLength = Math.max(maxLength, 2 * left);
            } else if (left > right) {
                // Invalid state from this direction: more opening than closing.
                left = 0;
                right = 0;
            }
        }

        return maxLength;
    }

    /**
     * ========================================================================
     * 6. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time   | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Stack Approach | O(N)   | O(N)  | Intuitive, extends nicely  | Good starting point.
     * Two-Pass       | O(N)   | O(1)  | Clever, minimal memory     | ⭐ OPTIMAL. Present this for full marks.
     * 
     * ========================================================================
     * 7. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if there are multiple types of brackets, like '[]' and '{}'?"
     * A1: The Two-Pass counter approach entirely fails because it cannot guarantee 
     *     that the brackets are properly interleaved (e.g., "([)]" would have 
     *     balanced counts but is invalid). We MUST use a Stack. The stack logic 
     *     would become more complex because an invalid mismatch (like a '}' trying 
     *     to close a '(') immediately breaks the chain and forces a new anchor.
     * 
     * Q2: "Can this be solved using Dynamic Programming?"
     * A2: Yes! Let `dp[i]` be the length of the longest valid substring ending at `i`.
     *     - If s[i] == ')':
     *       - If s[i-1] == '(': `dp[i] = (i >= 2 ? dp[i-2] : 0) + 2`
     *       - If s[i-1] == ')' and s[i - dp[i-1] - 1] == '(': 
     *         `dp[i] = dp[i-1] + (i - dp[i-1] >= 2 ? dp[i - dp[i-1] - 2] : 0) + 2`
     *     This is an O(N) time and O(N) space DP solution. It's beautiful but 
     *     often harder to reason about under interview pressure than the Stack approach.
     * 
     * ========================================================================
     * 8. FINAL TAKEAWAYS
     * ========================================================================
     * - Storing INDICES instead of values in a stack is a superpower for determining lengths/distances.
     * - When a left-to-right pass fails on asymmetric edge cases (like trailing characters), 
     *   a right-to-left mirror pass is a classic trick to achieve O(1) space.
     */
}
