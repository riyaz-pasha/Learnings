/**
 * ============================================================================
 * MINIMUM REMOVE TO MAKE VALID PARENTHESES - INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given a string `s` of '(' , ')' and lowercase English characters. 
 * Your task is to remove the minimum number of parentheses ( '(' or ')', in any 
 * positions ) so that the resulting parentheses string is valid and return it.
 * 
 * Simple Explanation for Interviewer:
 * "We are scanning a string containing letters and parentheses. Some of the 
 * parentheses don't have a matching partner and break the valid structure. 
 * Our goal is to identify and drop exactly those unmatched parentheses while 
 * keeping everything else intact."
 * 
 * Core Idea & Intuition:
 * There are two types of invalid parentheses:
 * 1. A closing ')' that appears BEFORE any available opening '('. 
 *    (e.g., "a)b" -> We know instantly that ')' is invalid).
 * 2. An opening '(' that is never closed by the time we reach the end of the string.
 *    (e.g., "a(b" -> We only know it's invalid AFTER scanning the whole string).
 * 
 * Because we process left-to-right, we can easily catch the first type immediately 
 * using a counter or a stack. The second type requires us to look back at the 
 * remaining unmatched '(' and remove them.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "If there are multiple valid answers, can I return any of them?"
 *    Why: e.g., for "a(b(c)d", removing the first '(' gives "ab(c)d", while 
 *    removing the second gives "a(bc)d". Both involve exactly 1 removal.
 *    Impact: Usually, any valid result is accepted. It's crucial to confirm.
 * 
 * 2. Q: "Are there other types of brackets like '{' or '['?"
 *    Why: If multiple bracket types exist, a simple counter fails, and we MUST 
 *    use a Stack.
 *    Impact: Problem says only '(' and ')'.
 * 
 * 3. Q: "Can the string be completely empty after removals? e.g., ')))((('"
 *    Why: Determines if an empty string is considered a valid output. (Yes, it is).
 * 
 * 4. Q: "Should the relative order of letters change?"
 *    Why: Ensures we are just deleting characters and not sorting or shifting.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1: 
 * Input: "lee(t(c)o)de)"
 * Output: "lee(t(c)o)de"
 * 
 * Example 2 (Unmatched opens and closes):
 * Input: "))(("
 * Output: "" 
 * Reason: First two ')' have no prior '('. Last two '(' have no subsequent ')'.
 * 
 * Visualizing the Stack approach for "a)b(c":
 * 
 * Index | Char | Action                  | Stack (holds indices of '(')
 * ---------------------------------------------------------------------
 * 0     | 'a'  | Skip (it's a letter)    | []
 * 1     | ')'  | Stack empty! Invalid!   | []   -> Mark index 1 for deletion
 * 2     | 'b'  | Skip                    | []
 * 3     | '('  | Push index              | [3]
 * 4     | 'c'  | Skip                    | [3]
 * ---------------------------------------------------------------------
 * End of string. Stack has [3]. Index 3 is an unmatched '('. Mark for deletion.
 * Result string: "abc".
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Valid parentheses rules. ( needs to come before ).
 * 2. Clarify: Confirm it's only 1 type of bracket and returning any valid match is OK.
 * 3. Restate: "I need to remove excess ')' on the fly, and excess '(' at the end."
 * 4. Approach 1 (Stack): "Since we need to match pairs and remember the positions 
 *    of unmatched '(', a Stack storing indices is a perfect fit."
 * 5. Approach 2 (Two Pass): "To optimize space overhead of the stack, we can use 
 *    a two-pass counter. First pass removes bad ')'. Second pass goes right-to-left 
 *    and removes bad '('."
 * 6. Code: Write the Two-Pass or Stack method. (Stack is easier to reason about, 
 *    Two-Pass shows optimization skills).
 * 7. Dry-run: Trace with "a)b(c".
 * 8. Complexity: State O(N) time and O(N) space (since Strings are immutable in Java).
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - All closing brackets: "))))"
 * - All opening brackets: "(((("
 * - No brackets at all: "abc"
 * - Deeply nested but valid: "(((a)))"
 * 
 * Common Mistakes:
 * - Removing the WRONG opening bracket during a two-pass approach. If you iterate 
 *   left-to-right to remove excess '(', you might break valid inner pairs. 
 *   (e.g., in "(a(b)", if you remove the second '(', you get "(ab)" which is valid, 
 *   but if you just cap the open counter, it gets messy. Safest is right-to-left!).
 * - Using `String += char` inside a loop (O(N^2) time). Always use `StringBuilder`.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class MinimumRemoveValidParentheses {

    public static void main(String[] args) {
        String test1 = "lee(t(c)o)de)";
        String test2 = "a)b(c)d";
        String test3 = "))((";

        System.out.println("--- Stack Approach ---");
        System.out.println(test1 + " -> " + minRemoveToMakeValidStack(test1));
        System.out.println(test2 + " -> " + minRemoveToMakeValidStack(test2));
        System.out.println(test3 + " -> " + minRemoveToMakeValidStack(test3));

        System.out.println("\n--- Optimal Two-Pass Approach ---");
        System.out.println(test1 + " -> " + minRemoveToMakeValidTwoPass(test1));
        System.out.println(test2 + " -> " + minRemoveToMakeValidTwoPass(test2));
        System.out.println(test3 + " -> " + minRemoveToMakeValidTwoPass(test3));
    }

    /**
     * SOLUTION 1: STACK APPROACH
     * ------------------------------------------------------------------------
     * Idea: We convert the string to a char array. We use a Stack to store the 
     * INDICES of the '(' characters. When we encounter a ')', we try to pop from 
     * the stack. If the stack is empty, it's an invalid ')' and we mark it with '*'.
     * After checking the whole string, any indices left in the stack are invalid 
     * '(' characters. We mark them with '*' too. Finally, we build a string 
     * ignoring the '*' characters.
     * 
     * Why it works: The stack perfectly tracks unresolved dependencies. By storing 
     * indices, we can retroactively invalidate opening brackets.
     * 
     * Time Complexity: O(N). One pass to find invalid chars, one pass to build string.
     * Space Complexity: O(N) for the character array and the stack.
     */
    public static String minRemoveToMakeValidStack(String s) {
        if (s == null || s.isEmpty()) return "";

        char[] chars = s.toCharArray();
        // Stack to keep track of indices of unmatched '('
        Deque<Integer> stack = new ArrayDeque<>();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '(') {
                stack.push(i);
            } else if (chars[i] == ')') {
                if (stack.isEmpty()) {
                    // Invalid ')', mark for deletion
                    chars[i] = '*';
                } else {
                    // Valid ')', resolve the most recent '('
                    stack.pop();
                }
            }
        }

        // Any remaining '(' in the stack are unmatched and invalid
        while (!stack.isEmpty()) {
            chars[stack.pop()] = '*';
        }

        // Reconstruct the string without the marked characters
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            if (c != '*') {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * SOLUTION 2: OPTIMAL TWO-PASS (Counter)
     * ------------------------------------------------------------------------
     * Idea: 
     * Pass 1 (Left to Right): Keep a `balance` counter. Increment for '(', 
     * decrement for ')'. If `balance` is 0 and we see ')', we skip adding it to 
     * our intermediate StringBuilder because it's invalid.
     * Pass 2 (Right to Left): The intermediate string now has NO invalid ')', but 
     * might have extra '('. We iterate backwards. If we see a '(', and `balance` 
     * (which represents the number of excess '(' ) is > 0, we skip it and decrement 
     * the balance.
     * 
     * Why it works: The right-most '(' without matching ')' are the ones that 
     * break the string. By going right-to-left, we safely strip exactly those.
     * 
     * Time Complexity: O(N). Two linear passes.
     * Space Complexity: O(N) for the StringBuilders. (We save the space of the Stack, 
     * making the constant factor slightly better in practice).
     */
    public static String minRemoveToMakeValidTwoPass(String s) {
        if (s == null || s.isEmpty()) return "";

        // Pass 1: Remove invalid ')'
        StringBuilder sb = new StringBuilder();
        int balance = 0;

        for (char c : s.toCharArray()) {
            if (c == '(') {
                balance++;
                sb.append(c);
            } else if (c == ')') {
                if (balance == 0) {
                    continue; // Skip invalid ')'
                }
                balance--;
                sb.append(c);
            } else {
                sb.append(c); // Always append letters
            }
        }

        // Pass 2: Remove invalid '('
        // At this point, 'balance' equals the exact number of excess '(' 
        // that we must remove from the right side.
        StringBuilder result = new StringBuilder();
        
        for (int i = sb.length() - 1; i >= 0; i--) {
            char c = sb.charAt(i);
            if (c == '(' && balance > 0) {
                balance--;
                continue; // Skip invalid '('
            }
            result.append(c);
        }

        // Because we iterated right-to-left, the result is backwards. Reverse it.
        return result.reverse().toString();
    }

    /**
     * ========================================================================
     * 8. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time   | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Stack Approach | O(N)   | O(N)  | Easiest to write & explain | ⭐ PRESENT THIS. 
     * Two-Pass (Opt) | O(N)   | O(N)  | No stack, less overhead    | Great follow-up.
     * 
     * Recommendation: Start with the Stack approach. It perfectly maps to the 
     * mental model of "parenthesis matching." If the interviewer asks for 
     * O(1) auxiliary space (excluding the output string builder), offer Two-Pass.
     * 
     * ========================================================================
     * 9. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if there were multiple types of brackets? (e.g., '[]', '{}', '()')"
     * A1: The Two-Pass counter approach immediately fails because counters cannot 
     *     track the ORDER of different brackets (e.g., "([)]" would have balanced 
     *     counters but is invalid). We MUST use a Stack.
     * 
     * Q2: "Can this be done in exactly ONE pass without a Stack?"
     * A2: Generally, no. A closing parenthesis immediately invalidates itself if 
     *     there is no opening parenthesis, which can be checked in one pass. But an 
     *     opening parenthesis is only known to be invalid at the END of the string.
     * 
     * ========================================================================
     * 10. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: For parenthesis problems, ')' fails immediately, '(' fails 
     *   eventually. 
     * - Trick: To mutate strings efficiently based on positions, convert to `char[]`, 
     *   mark characters with a placeholder (like '*'), and rebuild ignoring '*'.
     * - In Two-Pass: Always remove excess '(' starting from the RIGHT side. Removing 
     *   from the left might break valid pairs!
     */
}
