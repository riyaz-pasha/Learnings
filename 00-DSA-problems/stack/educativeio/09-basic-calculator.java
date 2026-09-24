/**
 * ============================================================================
 * BASIC CALCULATOR (EVALUATE MATHEMATICAL EXPRESSION WITH PARENTHESES)
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given a string containing numbers, '+', '-', '(', and ')', 
 * evaluate the mathematical expression and return its result. Unary minus 
 * (e.g., "-1") is allowed.
 * 
 * Simple Explanation for Interviewer:
 * "We need to evaluate a math equation from left to right. Because there are 
 * no multiplication or division operators, we can just add or subtract numbers 
 * as we see them. The only thing that interrupts this left-to-right flow is 
 * parentheses. When we see a '(', we must 'pause' our current calculation, 
 * compute everything inside the parentheses, and then combine that result with 
 * what we had before."
 * 
 * Core Idea & Intuition:
 * This is a classic state-suspension problem. When you are processing and hit 
 * a sub-problem (parentheses), you must save your current state (the running 
 * total and the sign before the parenthesis), solve the sub-problem, and then 
 * resume. This Last-In-First-Out (LIFO) "pause and resume" behavior is exactly 
 * what a Stack is designed for.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can the string contain spaces?"
 *    Why: Ensures we don't accidentally try to parse spaces as operators or digits.
 *    Impact: We will simply ignore whitespace during traversal.
 * 2. Q: "How should I handle unary minus, like '- (2 + 3)' or '-1'?"
 *    Why: Evaluators can crash if they expect a number before every minus sign.
 *    Impact: We can initialize a default `result = 0` and `sign = 1`. Seeing a 
 *    '-' at the start just sets `sign = -1`, which perfectly handles unary minus.
 * 3. Q: "Are there any nested parentheses?"
 *    Why: Confirms that a simple flag won't work and a Stack/Recursion is required.
 * 4. Q: "Will intermediate calculations fit in a 32-bit signed integer?"
 *    Why: Prompt says yes, meaning we don't need `long` for intermediate totals.
 * 5. Q: "Can there be invalid characters or malformed brackets?"
 *    Why: Determines if we need strict validation. Prompt says it's guaranteed valid.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1: 
 * Input: "1 + 1" -> Output: 2
 * 
 * Example 2:
 * Input: " 2-1 + 2 " -> Output: 3
 * 
 * Example 3 (Unary and Parentheses):
 * Input: "1-(2+3)" -> Output: -4
 * 
 * Visualizing the Stack approach for "1-(2+3)":
 * 
 * Init: result = 0, sign = 1, num = 0, stack = []
 * 
 * Char | Action                                      | State Variables         | Stack
 * --------------------------------------------------------------------------------------
 * '1'  | num = 1                                     | res=0, sign=1, num=1    | []
 * '-'  | res += 1*1 = 1, sign = -1, num = 0          | res=1, sign=-1, num=0   | []
 * '('  | PUSH res (1). PUSH sign (-1).               | res=0, sign=1, num=0    | [-1, 1]
 *      | Reset res = 0, sign = 1 for inner scope.    |                         | 
 * '2'  | num = 2                                     | res=0, sign=1, num=2    | [-1, 1]
 * '+'  | res += 1*2 = 2, sign = 1, num = 0           | res=2, sign=1, num=0    | [-1, 1]
 * '3'  | num = 3                                     | res=2, sign=1, num=3    | [-1, 1]
 * ')'  | res += 1*3 = 5, num = 0.                    | res=5, sign=1, num=0    | [-1, 1]
 *      | End of scope! POP sign (-1): res = 5 * -1   | res=-5                  | [1]
 *      | POP old res (1): res = -5 + 1 = -4.         | res=-4                  | []
 * 
 * Final Result: -4.
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Acknowledge the left-to-right evaluation and the bracket disruption.
 * 2. Clarify: Ask about spaces, nested brackets, and unary operators.
 * 3. Initial Approach: Mention a Recursive approach (evaluate inside parentheses via 
 *    recursive calls), but note it can cause StackOverflow on deep nesting.
 * 4. Optimal Approach 1 (Context Stack): Explain that when we hit '(', we save our 
 *    current `result` and `sign` to a Stack and reset them to evaluate the new context.
 * 5. Optimal Approach 2 (Sign Distribution): As a bonus, mention we could mathematically 
 *    distribute the negative signs inside the brackets using a stack of signs.
 * 6. Code: Implement the Context Stack approach. It's the most intuitive to explain.
 * 7. Dry-run: Trace "1-(2+3)".
 * 8. Complexity: State O(N) time and O(N) space for the stack depth.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Start with negative: "-2+1"
 * - Deep nesting: "(((1)))"
 * - Ending on a number: We must remember to process the final `num` at the very 
 *   end of the string, as operators normally trigger the processing.
 * 
 * Common Mistakes:
 * - Forgetting to process the last number: The loop ends, but `num` might still hold 
 *   a value that wasn't added to `result`. Must do `result += sign * num` after loop.
 * - Pushing order vs Popping order: If you push `result` then `sign`, you must pop 
 *   `sign` then `result`. Getting this backward causes massive mathematical errors.
 * - Parsing digits: using `Integer.parseInt` on string chunks is O(N^2) in a loop. 
 *   Always parse char-by-char with `num = num * 10 + (c - '0')`.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class BasicCalculator {

    public static void main(String[] args) {
        String test1 = "1 + 1";
        String test2 = " 2-1 + 2 ";
        String test3 = "1-(2+3)";
        String test4 = "- (3 + (4 - 5))";

        System.out.println("--- Context Stack Approach ---");
        System.out.println(test1 + " -> " + calculateContextStack(test1)); // 2
        System.out.println(test2 + " -> " + calculateContextStack(test2)); // 3
        System.out.println(test3 + " -> " + calculateContextStack(test3)); // -4
        System.out.println(test4 + " -> " + calculateContextStack(test4)); // -2

        System.out.println("\n--- Sign Distribution Approach ---");
        System.out.println(test1 + " -> " + calculateSignDistribution(test1)); // 2
        System.out.println(test3 + " -> " + calculateSignDistribution(test3)); // -4
    }

    /**
     * SOLUTION 1: CONTEXT STACK APPROACH (Optimal & Intuitive)
     * ------------------------------------------------------------------------
     * Idea: Maintain a running `result` and a `sign`. Parse digits to build `num`.
     * When we see '+' or '-', add the accumulated `num * sign` to `result`, and 
     * update the `sign` for the NEXT number.
     * When we see '(', we push the CURRENT `result` and `sign` onto the stack, 
     * and reset them to 0 and 1. This acts as a clean slate for the parentheses.
     * When we see ')', the inner calculation is finished. We pop the `sign` and 
     * multiply it by the inner result, then pop the old `result` and add it.
     * 
     * Time Complexity: O(N) where N is the length of the string. We process each 
     * character exactly once.
     * Space Complexity: O(N) in the worst case if there are deeply nested parentheses 
     * like "((((1))))".
     */
    public static int calculateContextStack(String s) {
        if (s == null || s.isEmpty()) return 0;

        // Use ArrayDeque instead of java.util.Stack for performance
        Deque<Integer> stack = new ArrayDeque<>();
        int result = 0;
        int num = 0;
        int sign = 1; // 1 represents positive, -1 represents negative

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isDigit(c)) {
                // Build multi-digit numbers safely
                num = num * 10 + (c - '0');
            } else if (c == '+') {
                // Evaluate the PREVIOUS number with the PREVIOUS sign
                result += sign * num;
                // Reset for the next number
                sign = 1;
                num = 0;
            } else if (c == '-') {
                result += sign * num;
                sign = -1;
                num = 0;
            } else if (c == '(') {
                // We are entering a sub-expression.
                // Save the current state (push result, then sign)
                stack.push(result);
                stack.push(sign);

                // Reset the state to evaluate the inside of the parentheses
                result = 0;
                sign = 1;
            } else if (c == ')') {
                // 1. Add the final number inside the parentheses to the inner result
                result += sign * num;
                num = 0; // Reset num as we've processed it

                // 2. Multiply the inner result by the sign that appeared BEFORE the '('
                result *= stack.pop();

                // 3. Add the outer result that appeared BEFORE the '('
                result += stack.pop();
            }
            // Note: We completely ignore spaces.
        }

        // Catch the very last number if the string didn't end with a ')'
        if (num != 0) {
            result += sign * num;
        }

        return result;
    }

    /**
     * SOLUTION 2: SIGN DISTRIBUTION STACK (Alternative Optimal)
     * ------------------------------------------------------------------------
     * Idea: Mathematically, `a - (b - c)` is exactly the same as `a - b + c`.
     * Instead of saving the result and suspending the context, we can just distribute 
     * the negative signs! We use a stack to keep track of the *global* sign multiplier.
     * When entering '(', the global sign becomes `current_sign * stack.peek()`.
     * This means if we are inside a negative parenthesis, all nested numbers 
     * automatically flip their signs.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(N) for the stack of signs.
     * 
     * Trade-off: This is slightly faster and uses less space since we only push 
     * 1 integer per parenthesis instead of 2. However, it requires a firmer grasp 
     * of algebraic distribution to explain properly in an interview.
     */
    public static int calculateSignDistribution(String s) {
        if (s == null || s.isEmpty()) return 0;

        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1); // The default global sign is positive

        int result = 0;
        int num = 0;
        int localSign = 1;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            } else if (c == '+') {
                // Apply the local sign AND the global sign from the stack
                result += num * localSign * stack.peek();
                localSign = 1;
                num = 0;
            } else if (c == '-') {
                result += num * localSign * stack.peek();
                localSign = -1;
                num = 0;
            } else if (c == '(') {
                // The new global scope is the product of the current global scope 
                // and the local sign right before this bracket.
                stack.push(stack.peek() * localSign);
                localSign = 1; // Reset local sign for the new inner scope
            } else if (c == ')') {
                result += num * localSign * stack.peek();
                num = 0;
                stack.pop(); // Leave the current parentheses scope
            }
        }
        
        if (num != 0) {
            result += num * localSign * stack.peek();
        }

        return result;
    }

    /**
     * ========================================================================
     * 6. SOLUTION COMPARISON
     * ========================================================================
     * Approach           | Time | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Context Stack      | O(N) | O(N)  | Very intuitive to trace.   | ⭐ STRONGLY REC.
     * Sign Distribution  | O(N) | O(N)  | Clever, slightly leaner.   | Great alternative.
     * 
     * ========================================================================
     * 7. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if the expression contains multiplication (*) and division (/)?"
     * A1: We could no longer evaluate completely left-to-right on the fly, because 
     *     `*` and `/` have higher precedence. We would need to combine this solution 
     *     with the approach from 'Basic Calculator II'—maintaining a `lastNumber` 
     *     state and delaying addition until a lower precedence operator is found.
     * 
     * Q2: "What if we don't know if the expression is valid? (e.g., mismatched brackets)"
     * A2: We would need to add error handling. We can keep track of bracket balance 
     *     (increment for '(', decrement for ')'). If balance goes below 0, or if 
     *     balance != 0 at the end, the string is malformed. We'd throw an Exception.
     * 
     * Q3: "Can we do this in O(1) space?"
     * A3: Not easily for general cases. Because parentheses can be nested infinitely, 
     *     we need a way to track the nested states. Without a stack (or recursion), 
     *     we cannot parse a context-free language like nested matched parentheses.
     * 
     * ========================================================================
     * 8. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: Stacks are the universal solution for processing nested scopes 
     *   or "pausing" a state to handle a sub-problem.
     * - Trick: When pushing two related items (result, sign) to a single stack, 
     *   remember that LIFO dictates you MUST pop them in reverse order.
     * - Always remember to process the trailing number that remains in your 
     *   buffer after the main traversal loop finishes.
     */
}


/**
 * ================================================================
 * 🔥 BASIC CALCULATOR III (WITH PARENTHESES)
 * ================================================================
 *
 * Supports:
 * - +, -, *, /
 * - Nested parentheses
 *
 * ================================================================
 * 🧠 CORE IDEA:
 * ------------------------------------------------
 * Use recursion to evaluate parentheses
 *
 * Each recursion:
 * - Processes expression until ')'
 * - Returns computed result
 *
 * ================================================================
 */
public class BasicCalculatorIII {

    public static void main(String[] args) {
        String s = "2*(3+(4-1)*5)/3";
        System.out.println(calculate(s)); // Expected: 12
    }

    public static int calculate(String s) {
        // Convert string to queue for easy left-to-right processing
        Deque<Character> queue = new ArrayDeque<>();

        for (char c : s.toCharArray()) {
            if (c != ' ') {
                queue.offer(c);
            }
        }

        return evaluate(queue);
    }

    /**
     * ============================================================
     * 🔥 RECURSIVE EVALUATION FUNCTION
     * ============================================================
     *
     * This method evaluates:
     * - Entire expression OR
     * - Sub-expression inside parentheses
     *
     * Stops when:
     * - Queue is empty OR
     * - We hit ')'
     */
    private static int evaluate(Deque<Character> queue) {

        Deque<Integer> stack = new ArrayDeque<>();

        int num = 0;
        char op = '+'; // previous operator

        while (!queue.isEmpty()) {

            char ch = queue.poll();

            /**
             * --------------------------------------------------------
             * CASE 1: DIGIT → BUILD NUMBER
             * --------------------------------------------------------
             */
            if (Character.isDigit(ch)) {
                num = num * 10 + (ch - '0');
            }

            /**
             * --------------------------------------------------------
             * CASE 2: '(' → RECURSION START
             * --------------------------------------------------------
             *
             * Evaluate sub-expression completely
             * Example:
             * 2 * (3 + 5)
             *      ↑ this part handled recursively
             */
            if (ch == '(') {
                num = evaluate(queue); // recursion
            }

            /**
             * --------------------------------------------------------
             * CASE 3: OPERATOR OR ')' OR END
             * --------------------------------------------------------
             */
            if (!Character.isDigit(ch) || queue.isEmpty()) {

                switch (op) {

                    case '+' -> stack.push(num);

                    case '-' -> stack.push(-num);

                    case '*' -> {
                        int prev = stack.pop();
                        stack.push(prev * num);
                    }

                    case '/' -> {
                        int prev = stack.pop();
                        stack.push(prev / num);
                    }
                }

                // Reset for next number
                num = 0;
                op = ch;

                /**
                 * ----------------------------------------------------
                 * BREAK CONDITION: ')'
                 * ----------------------------------------------------
                 *
                 * When closing bracket is found:
                 * → return current computed value
                 */
                if (ch == ')') {
                    break;
                }
            }
        }

        /**
         * ------------------------------------------------------------
         * FINAL SUM FOR THIS LEVEL
         * ------------------------------------------------------------
         */
        int result = 0;
        while (!stack.isEmpty()) {
            result += stack.pop();
        }

        return result;
    }
}
