/**
 * ============================================================================
 * BASIC CALCULATOR II (EVALUATE MATHEMATICAL EXPRESSION) - INTERVIEW GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given a string `s` representing a valid mathematical expression 
 * containing non-negative integers, '+', '-', '*', and '/', evaluate it and 
 * return the integer result. (Integer division truncates toward zero).
 * 
 * Simple Explanation for Interviewer:
 * "We need to parse a string as a math equation and calculate the result. 
 * Since there are no parentheses, we only have to worry about standard order 
 * of operations (BODMAS/PEMDAS): multiplication and division happen *before* 
 * addition and subtraction."
 * 
 * Core Idea & Intuition:
 * When reading the string left-to-right, we can't immediately add or subtract 
 * numbers because a multiplication or division might come right after them, 
 * taking precedence. 
 * E.g., for "3 + 2 * 2", we can't evaluate "3 + 2" yet. 
 * 
 * Solution Pattern: "Delayed Evaluation". 
 * We keep track of the *previous* operator we saw. When we finish parsing a 
 * number, we look at the operator that came *before* it:
 * - If it was '+' or '-', we temporarily store the number (e.g., in a stack).
 * - If it was '*' or '/', we immediately pop the last stored number, perform 
 *   the operation with the current number, and store the result back.
 * Finally, we just sum up everything we've stored.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFICATION QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Are there any parentheses in the expression?"
 *    Why: Parentheses completely change the problem, requiring recursion or an 
 *         explicit operator stack. (Prompt says no, just non-negative integers 
 *         and 4 operators).
 * 2. Q: "Can there be negative numbers in the input string, like '-3 + 2'?"
 *    Why: Determines if we need to handle unary minuses. Prompt says "non-negative 
 *         integers", so the first number is always positive or zero.
 * 3. Q: "What should happen if there's a division by zero?"
 *    Why: Edge case handling. Usually, valid expressions won't divide by zero, 
 *         but it's a great defensive programming check.
 * 4. Q: "Are there whitespace characters, and can numbers be multiple digits?"
 *    Why: Spaces must be ignored. Multi-digit numbers must be parsed properly 
 *         using `num = num * 10 + digit`.
 * 5. Q: "Will intermediate calculations fit in a 32-bit signed integer?"
 *    Why: If intermediate sums exceed 2^31-1, we would need to use `long`. 
 *         Prompt guarantees they fit.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: "3+2*2"
 * Output: 7
 * 
 * Example 2:
 * Input: " 3/2 "
 * Output: 1
 * 
 * Visualizing the Stack Approach for "3 - 5 * 2 + 8":
 * Init: prev_operator = '+', stack = [], num = 0
 * 
 * Char | parsed num | action at operator or end | Stack State (Bottom->Top)
 * -----------------------------------------------------------------------
 * '3'  | 3          | -                         | []
 * ' '  | 3          | (ignore space)            | []
 * '-'  | 3          | Op was '+'. Push 3.       | [3]
 * '5'  | 5          | -                         | [3]
 * '*'  | 5          | Op was '-'. Push -5.      | [3, -5]
 * '2'  | 2          | -                         | [3, -5]
 * '+'  | 2          | Op was '*'. Pop(-5) * 2.  | [3, -10]
 * '8'  | 8          | -                         | [3, -10]
 * End  | 8          | Op was '+'. Push 8.       | [3, -10, 8]
 * 
 * Sum the stack: 3 + (-10) + 8 = 1. Result: 1.
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Confirm BODMAS rules (no parentheses).
 * 2. Clarify: Ask about spaces, multi-digit numbers, and division by zero.
 * 3. Brute Force / Initial Thought: Mention evaluating in two passes (one for 
 *    * and /, another for + and -). Point out this requires modifying a list.
 * 4. Better Approach (Stack): Explain "Delayed Evaluation". Keep a stack of 
 *    terms to be added. If we see *, pop the top term, multiply, and push back.
 * 5. Optimal Approach (O(1) Space): Notice we only ever need the *last* number 
 *    to process * or /, and an accumulated total for + and -. We can drop the stack.
 * 6. Code: Write the O(1) Space optimal solution directly (or Stack if easier).
 * 7. Dry-run: Trace "3+2*2".
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Single number: "42" -> returns 42.
 * - Lots of spaces: "   14   /   3   " -> spaces must not reset `num`.
 * - Multi-digit processing: "234" -> handled via `num = num*10 + (c - '0')`.
 * - Last character: We must evaluate the *final* number when `i == s.length() - 1`.
 * 
 * Common Mistakes:
 * - Forgetting to process the final number. (Operators trigger processing, but 
 *   there is no operator after the last number).
 * - Doing `char - '0'` on non-digit characters.
 * - Resetting `currentNumber = 0` every time we see a space. (Spaces should be 
 *   completely ignored, only operators should trigger a reset).
 */

import java.util.*;

public class BasicCalculatorII {

    public static void main(String[] args) {
        String test1 = "3+2*2";          // 7
        String test2 = " 3/2 ";          // 1
        String test3 = " 3+5 / 2 ";      // 5
        String test4 = "14-3*2";         // 8

        System.out.println("--- Better Stack Approach ---");
        System.out.println(test1 + " -> " + calculateStack(test1));
        System.out.println(test4 + " -> " + calculateStack(test4));

        System.out.println("\n--- Optimal Space Approach ---");
        System.out.println(test1 + " -> " + calculateOptimal(test1));
        System.out.println(test2 + " -> " + calculateOptimal(test2));
        System.out.println(test3 + " -> " + calculateOptimal(test3));
    }

    /**
     * SOLUTION 1: BETTER (Stack Approach)
     * ------------------------------------------------------------------------
     * Idea: Traverse the string. Build the current number digit by digit.
     * When we hit an operator (or the end of the string), we evaluate the number 
     * we just built based on the *previous* operator we saw.
     * - If prev operator was '+', push number to stack.
     * - If prev operator was '-', push -number to stack.
     * - If prev operator was '*' or '/', pop top of stack, perform operation, and push result.
     * At the end, everything left in the stack just needs to be summed up.
     * 
     * Time Complexity: O(N) where N is the length of the string.
     * Space Complexity: O(N) worst case (e.g., "1+2+3+4" stores everything in stack).
     */
    public static int calculateStack(String s) {
        if (s == null || s.isEmpty()) return 0;

        Deque<Integer> stack = new ArrayDeque<>();
        int currentNumber = 0;
        char operation = '+'; // Default to + for the very first number

        for (int i = 0; i < s.length(); i++) {
            char currentChar = s.charAt(i);

            // 1. Build the number if it's a digit
            if (Character.isDigit(currentChar)) {
                // Avoid using Integer.parseInt() by converting char to int dynamically
                currentNumber = (currentNumber * 10) + (currentChar - '0');
            }

            // 2. Process the number if we hit an operator OR the end of the string.
            // Note: We deliberately ignore spaces unless it's the last character.
            if (!Character.isDigit(currentChar) && !Character.isWhitespace(currentChar) || i == s.length() - 1) {
                if (operation == '+') {
                    stack.push(currentNumber);
                } else if (operation == '-') {
                    stack.push(-currentNumber);
                } else if (operation == '*') {
                    stack.push(stack.pop() * currentNumber);
                } else if (operation == '/') {
                    stack.push(stack.pop() / currentNumber);
                }
                
                // Update the operation for the NEXT number and reset currentNumber
                operation = currentChar;
                currentNumber = 0;
            }
        }

        // 3. Sum all the processed chunks in the stack
        int result = 0;
        for (int num : stack) {
            result += num;
        }

        return result;
    }

    /**
     * SOLUTION 2: OPTIMAL (Space-Optimized Approach)
     * ------------------------------------------------------------------------
     * Idea: If we look closely at the Stack approach, we realize we only ever 
     * interact with the *top* element of the stack. 
     * Instead of storing every term to sum at the end, we can keep a running 
     * `result` for all completed additions/subtractions, and a `lastNumber` 
     * variable that acts as the "top of the stack" for pending * and / operations.
     * 
     * Detailed Dry Run for "3+2*2":
     * - Init: res=0, lastNum=0, curNum=0, op='+'
     * - '3': curNum=3.
     * - '+': (End of num). op is '+'. 
     *        res += lastNum (0+0=0). lastNum = curNum (3). 
     *        op = '+', curNum = 0.
     * - '2': curNum=2.
     * - '*': (End of num). op is '+'. 
     *        res += lastNum (0+3=3). lastNum = curNum (2). 
     *        op = '*', curNum = 0.
     * - '2': curNum=2. (Also end of string!).
     *        op is '*'. lastNum = lastNum * curNum (2*2 = 4).
     * - Loop ends. Return res + lastNum (3 + 4 = 7).
     * 
     * Time Complexity: O(N) - One pass.
     * Space Complexity: O(1) - Only primitive variables used.
     */
    public static int calculateOptimal(String s) {
        if (s == null || s.isEmpty()) return 0;

        int currentNumber = 0;
        int lastNumber = 0; // Acts as the 'top' of our conceptual stack
        int result = 0;     // Accumulator for finished operations
        char operation = '+'; 

        for (int i = 0; i < s.length(); i++) {
            char currentChar = s.charAt(i);

            if (Character.isDigit(currentChar)) {
                currentNumber = (currentNumber * 10) + (currentChar - '0');
            }

            if (!Character.isDigit(currentChar) && !Character.isWhitespace(currentChar) || i == s.length() - 1) {
                if (operation == '+') {
                    // Previous operation is finished, add it to total result
                    result += lastNumber;
                    // The current number is now the new pending "lastNumber"
                    lastNumber = currentNumber;
                } else if (operation == '-') {
                    result += lastNumber;
                    lastNumber = -currentNumber; // Negate for subtraction
                } else if (operation == '*') {
                    // Multiplication binds tighter, modify the pending lastNumber directly
                    lastNumber = lastNumber * currentNumber;
                } else if (operation == '/') {
                    lastNumber = lastNumber / currentNumber;
                }

                // Prepare for the next number
                operation = currentChar;
                currentNumber = 0;
            }
        }

        // Add the very last pending number to the final result
        result += lastNumber;
        return result;
    }

    /**
     * ========================================================================
     * 8. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Two-Pass Eval  | O(N) | O(N)  | Requires complex lists     | Avoid.
     * Stack Approach | O(N) | O(N)  | Easy to explain & extend   | Great starting point.
     * Space Optimal  | O(N) | O(1)  | Small logic jump from Stack| ⭐ OPTIMAL. Present this!
     * 
     * ========================================================================
     * 9. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if the string contains parentheses like '3 + (2 * 2)'?"
     * A1: We would need a recursive approach OR an explicit operator stack (Shunting 
     *     Yard algorithm). When we see '(', we recursively call our evaluate function 
     *     on the substring until ')', and treat the returned value as `currentNumber`.
     * 
     * Q2: "What if there is an exponentiation operator '^'?"
     * A2: Exponentiation binds tighter than * and /, and is usually right-associative.
     *     We would need to adjust our logic. A general purpose solution like the 
     *     Shunting Yard algorithm with precedence levels would be safer to implement 
     *     if operators keep expanding.
     * 
     * Q3: "What if numbers can be negative at the start, e.g., '-3 + 2'?"
     * A3: Our current logic actually handles this perfectly! The default `operation` 
     *     is '+'. If the string starts with '-', `currentNumber` will be 0 when we 
     *     hit the '-'. We do `+ 0`, and set `operation` to '-'. Then we read '3', 
     *     and the '-' operation pushes `-3`. It works seamlessly.
     * 
     * ========================================================================
     * 10. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: "Delayed Evaluation". When evaluating expressions with precedence, 
     *   evaluate the *previous* operator using the *current* number.
     * - Optimization Trick: A Stack summing elements at the end can often be optimized 
     *   to O(1) space by maintaining a `runningSum` and a `lastProcessedTerm`.
     * - Clean Parsing: `num = num * 10 + (c - '0')` is the standard, fastest way 
     *   to parse integers char-by-char without relying on regex, split(), or substrings.
     */
}

/**
 * ================================================================
 * 🔥 BASIC CALCULATOR II — COMPLETE INTERVIEW VERSION
 * ================================================================
 *
 * Problem:
 * Evaluate expression with +, -, *, /
 * No parentheses
 * Division truncates toward zero
 *
 * ================================================================
 * 🧠 KEY IDEA:
 * ------------------------------------------------
 * We process the string in ONE PASS
 *
 * Maintain:
 * 1. num → current number being built
 * 2. op  → previous operator
 * 3. stack → to handle precedence
 *
 * ================================================================
 */
public class BasicCalculatorII {

    public static void main(String[] args) {
        String s = "3+2*2";
        System.out.println(calculate(s)); // Expected: 7
    }

    public static int calculate(String s) {

        // Stack to simulate evaluation
        Deque<Integer> stack = new ArrayDeque<>();

        int num = 0;       // Current number being formed
        char op = '+';     // Previous operator (default '+' for first number)

        /**
         * ============================================================
         * 🔄 Iterate over string
         * ============================================================
         */
        for (int i = 0; i < s.length(); i++) {

            char ch = s.charAt(i);

            /**
             * --------------------------------------------------------
             * STEP 1: BUILD NUMBER (multi-digit support)
             * --------------------------------------------------------
             *
             * Example:
             * "123" → num = ((1*10)+2)*10 + 3 = 123
             */
            if (Character.isDigit(ch)) {
                num = num * 10 + (ch - '0');
            }

            /**
             * --------------------------------------------------------
             * STEP 2: WHEN WE HIT AN OPERATOR OR END
             * --------------------------------------------------------
             *
             * Condition:
             * - current char is operator OR
             * - we reached end of string
             *
             * Why process at END?
             * → Last number won't be processed otherwise
             */
            if ((!Character.isDigit(ch) && ch != ' ') || i == s.length() - 1) {

                /**
                 * ----------------------------------------------------
                 * APPLY PREVIOUS OPERATOR (op)
                 * ----------------------------------------------------
                 *
                 * VERY IMPORTANT:
                 * We apply the PREVIOUS operator on current number
                 */
                switch (op) {

                    /**
                     * '+' → push number as is
                     */
                    case '+' -> stack.push(num);

                    /**
                     * '-' → push negative number
                     * (convert subtraction into addition)
                     */
                    case '-' -> stack.push(-num);

                    /**
                     * '*' → resolve immediately
                     * pop last number and multiply
                     */
                    case '*' -> {
                        int prev = stack.pop();
                        int result = prev * num;
                        stack.push(result);
                    }

                    /**
                     * '/' → resolve immediately
                     * pop last number and divide
                     *
                     * Java handles truncation toward zero automatically
                     */
                    case '/' -> {
                        int prev = stack.pop();
                        int result = prev / num;
                        stack.push(result);
                    }
                }

                /**
                 * ----------------------------------------------------
                 * UPDATE STATE
                 * ----------------------------------------------------
                 */
                op = ch;   // update operator
                num = 0;   // reset number
            }
        }

        /**
         * ============================================================
         * FINAL STEP: SUM ALL VALUES
         * ============================================================
         *
         * Why works?
         * Because:
         * - '+' pushed positive numbers
         * - '-' pushed negative numbers
         */
        int result = 0;

        while (!stack.isEmpty()) {
            result += stack.pop();
        }

        return result;
    }
}

/**
 * ================================================================
 * 🔥 SPACE OPTIMIZED VERSION (NO STACK)
 * ================================================================
 *
 * Key Trick:
 * Instead of stack → maintain:
 * 1. result → final sum
 * 2. lastNumber → last processed number
 *
 * Why?
 * - '*' and '/' affect ONLY the last number
 */
class BasicCalculatorII_Optimized {

    public static int calculate(String s) {

        int num = 0;
        int result = 0;
        int lastNumber = 0;
        char op = '+';

        for (int i = 0; i < s.length(); i++) {

            char ch = s.charAt(i);

            // Build number
            if (Character.isDigit(ch)) {
                num = num * 10 + (ch - '0');
            }

            if ((!Character.isDigit(ch) && ch != ' ') || i == s.length() - 1) {

                switch (op) {

                    case '+' -> {
                        result += lastNumber;
                        lastNumber = num;
                    }

                    case '-' -> {
                        result += lastNumber;
                        lastNumber = -num;
                    }

                    case '*' -> lastNumber = lastNumber * num;

                    case '/' -> lastNumber = lastNumber / num;
                }

                op = ch;
                num = 0;
            }
        }

        return result + lastNumber;
    }
}
