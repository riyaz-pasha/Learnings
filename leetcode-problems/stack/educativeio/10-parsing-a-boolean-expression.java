/**
 * ============================================================================
 * PARSING A BOOLEAN EXPRESSION - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: You are given a string representing a boolean expression containing:
 * - Values: 't' (true), 'f' (false)
 * - Operators: '!' (NOT, 1 argument), '&' (AND, 1+ arguments), '|' (OR, 1+ args)
 * - Grouping & Separators: '(', ')', and ','
 * You need to evaluate it and return the boolean result.
 * 
 * Simple Explanation for Interviewer:
 * "The problem asks us to evaluate a string formatted like a nested function 
 * call in programming. Because expressions can be nested inside other expressions 
 * (like `!( &(t, f) )`), we have to evaluate the innermost parts first and 
 * pass their results outward. This 'inside-out' evaluation strongly points to 
 * a Stack data structure or Recursion."
 * 
 * Core Idea & Intuition:
 * This is a classic parsing problem. 
 * - When we read left-to-right, we don't know the result of an operator until 
 *   we evaluate all its sub-expressions. 
 * - Therefore, we delay evaluation. We push everything onto a stack until we hit 
 *   a closing parenthesis ')'. 
 * - A ')' signals that a sub-expression is fully built. We can pop its arguments, 
 *   evaluate them against their operator, and push the single boolean result 
 *   back onto the stack for the outer expression to use.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can the expression just be a single 't' or 'f' without operators?"
 *    Why: Sets the base case for parsing. (Answer: Yes, constraints allow length 1).
 * 2. Q: "Are we guaranteed that the syntax is always valid?"
 *    Why: If invalid syntax is possible, we need error handling (e.g., mismatched 
 *    parentheses or missing operators). (Answer: Guaranteed valid).
 * 3. Q: "Can AND (&) or OR (|) operators take only one argument?"
 *    Why: Ensures our evaluation logic handles single arguments correctly. 
 *    (Answer: Yes, the prompt specifies 'one or more' subexpressions).
 * 4. Q: "Are there any spaces in the input string?"
 *    Why: Spaces would need to be trimmed or skipped. (Answer: No spaces).
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: "!(f)"
 * Output: true
 * 
 * Example 2:
 * Input: "|(&(t,f,t),!(t))"
 * Output: false
 * 
 * Visualizing the Stack Approach for: "|(&(t,f),!(t))"
 * (Note: We completely ignore commas to save stack space)
 * 
 * Char | Action             | Stack State (Bottom -> Top)
 * -------------------------------------------------------------------
 * '|'  | Push               | [|]
 * '('  | Push               | [|, (]
 * '&'  | Push               | [|, (, &]
 * '('  | Push               | [|, (, &, (]
 * 't'  | Push               | [|, (, &, (, t]
 * ','  | Ignore             | [|, (, &, (, t]
 * 'f'  | Push               | [|, (, &, (, t, f]
 * ')'  | POP until '('      | Args = [f, t], Op = '&' -> Result = 'f'
 *      | PUSH result        | [|, (, f]
 * ','  | Ignore             | [|, (, f]
 * '!'  | Push               | [|, (, f, !]
 * '('  | Push               | [|, (, f, !, (]
 * 't'  | Push               | [|, (, f, !, (, t]
 * ')'  | POP until '('      | Args = [t], Op = '!' -> Result = 'f'
 *      | PUSH result        | [|, (, f, f]
 * ')'  | POP until '('      | Args = [f, f], Op = '|' -> Result = 'f'
 *      | PUSH result        | [f]
 * 
 * Final Stack has 1 element: 'f'. Return false.
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Acknowledge the nested, inside-out nature of the problem.
 * 2. Clarify: Ask about spaces, single operators, and valid syntax.
 * 3. Approach 1 (Iterative Stack): Explain pushing chars and skipping commas. 
 *    When hitting ')', pop to evaluate and push the result.
 * 4. Approach 2 (Recursive): Explain recursively evaluating sub-expressions whenever 
 *    we encounter an operator.
 * 5. Code: Write the Stack approach as it is very explicit and tests data structure 
 *    knowledge well. (Optionally provide Recursive if asked for alternative).
 * 6. Dry-run: Trace the stack with a small example like "&(t,f)".
 * 7. Discuss Complexity: O(N) Time and Space.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Deeply nested negations: "!(!(t))"
 * - Single char input: "t" or "f"
 * - Single argument for & or |: "&(f)" or "|(t)"
 * 
 * Common Mistakes:
 * - Pushing ',' into the stack. It complicates popping. Just ignore them.
 * - Collecting popped arguments into an ArrayList. This is unnecessary overhead. 
 *   We only need to know if we saw at least one 't' or 'f' among the arguments 
 *   to evaluate '&' and '|'.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class ParseBooleanExpression {

    public static void main(String[] args) {
        String test1 = "!(f)";                 // true
        String test2 = "|(f,t)";               // true
        String test3 = "&(t,f)";               // false
        String test4 = "|(&(t,f,t),!(t))";     // false

        System.out.println("--- Stack Approach ---");
        System.out.println(test1 + " -> " + parseBoolExprStack(test1));
        System.out.println(test4 + " -> " + parseBoolExprStack(test4));

        System.out.println("\n--- Recursive Approach ---");
        System.out.println(test2 + " -> " + parseBoolExprRecursive(test2));
        System.out.println(test3 + " -> " + parseBoolExprRecursive(test3));
    }

    /**
     * SOLUTION 1: ITERATIVE STACK APPROACH (Optimal Iterative)
     * ------------------------------------------------------------------------
     * Idea: Traverse the string character by character. Push operators, '(', 
     * and boolean values ('t', 'f') onto a stack. Ignore commas completely.
     * When encountering ')', pop elements off the stack until you hit '('.
     * Evaluate those popped boolean values using the operator that immediately 
     * precedes the '('. Push the resulting 't' or 'f' back onto the stack.
     * 
     * Trick: Instead of storing popped values in a list, we just track whether 
     * we saw a 't' (hasTrue) or an 'f' (hasFalse). This saves time and memory.
     * 
     * Time Complexity: O(N). Each character is pushed and popped exactly once.
     * Space Complexity: O(N) in the worst case (e.g., deeply nested expressions).
     */
    public static boolean parseBoolExprStack(String expression) {
        if (expression == null || expression.isEmpty()) return false;

        Deque<Character> stack = new ArrayDeque<>();

        for (char c : expression.toCharArray()) {
            // Ignore commas, they don't add semantic value to our stack
            if (c == ',') continue;

            if (c != ')') {
                // Push everything else ('t', 'f', '!', '&', '|', '(')
                stack.push(c);
            } else {
                // ')' encountered, evaluate the current inner expression
                boolean hasTrue = false;
                boolean hasFalse = false;

                // Pop all boolean arguments until we hit the opening '('
                while (stack.peek() != '(') {
                    char val = stack.pop();
                    if (val == 't') hasTrue = true;
                    if (val == 'f') hasFalse = true;
                }

                // Pop the '('
                stack.pop();
                
                // Pop the operator that governs this expression
                char op = stack.pop();

                // Evaluate based on the operator
                if (op == '!') {
                    // NOT flips the single argument. 
                    // If we saw a 't' (hasTrue), result is 'f', else 't'.
                    stack.push(hasTrue ? 'f' : 't');
                } else if (op == '&') {
                    // AND is only true if there are NO false values
                    stack.push(hasFalse ? 'f' : 't');
                } else if (op == '|') {
                    // OR is true if there is AT LEAST ONE true value
                    stack.push(hasTrue ? 't' : 'f');
                }
            }
        }

        // At the end, exactly one boolean value remains on the stack
        return stack.pop() == 't';
    }

    /**
     * SOLUTION 2: RECURSIVE DESCENT PARSER
     * ------------------------------------------------------------------------
     * Idea: Mathematical expressions naturally map to recursive function calls.
     * We keep a global pointer (index) to traverse the string.
     * - If we see 't' or 'f', we return the boolean.
     * - If we see an operator, we skip the '(' and recursively evaluate arguments 
     *   until we hit the closing ')', then apply the operator logic.
     * 
     * Time Complexity: O(N). We advance the pointer past every character once.
     * Space Complexity: O(N) due to the recursive call stack depth.
     */
    
    // We use an object variable to track the index across recursive calls
    private static int index = 0;

    public static boolean parseBoolExprRecursive(String expression) {
        // Reset index for safe repeated static usage
        index = 0; 
        return evaluate(expression);
    }

    private static boolean evaluate(String s) {
        char c = s.charAt(index++);

        // Base cases
        if (c == 't') return true;
        if (c == 'f') return false;

        // Recursive cases
        if (c == '!') {
            index++; // Skip '('
            boolean res = !evaluate(s);
            index++; // Skip ')'
            return res;
        }

        if (c == '&') {
            index++; // Skip '('
            boolean res = true;
            while (s.charAt(index) != ')') {
                if (s.charAt(index) == ',') {
                    index++; // Skip comma
                } else {
                    // Important: use &= to accumulate the AND logic
                    res &= evaluate(s);
                }
            }
            index++; // Skip ')'
            return res;
        }

        if (c == '|') {
            index++; // Skip '('
            boolean res = false;
            while (s.charAt(index) != ')') {
                if (s.charAt(index) == ',') {
                    index++; // Skip comma
                } else {
                    // Important: use |= to accumulate the OR logic
                    res |= evaluate(s);
                }
            }
            index++; // Skip ')'
            return res;
        }

        return false;
    }

    /**
     * ========================================================================
     * 8. SOLUTION COMPARISON
     * ========================================================================
     * Approach         | Time | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Iterative Stack  | O(N) | O(N)  | Easy to explain, no stack  | ⭐ STRONGLY REC.
     *                  |      |       | overflow risk.             | 
     * Recursive DFS    | O(N) | O(N)  | Very elegant code, but     | Great alternative 
     *                  |      |       | mutates global/class state.| if you prefer recursion.
     * 
     * ========================================================================
     * 9. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if the expression had variables like 'x' and 'y' instead of just 
     *      't' or 'f', and we were given a Map of variable assignments?"
     * A1: We would simply modify our parsing logic. When we pop a character (or 
     *     recursively evaluate), if it's not an operator, we look it up in the 
     *     Map to get its boolean value before applying the logic.
     * 
     * Q2: "In the recursive approach, does `res &= evaluate(s)` short-circuit?"
     * A2: In Java, bitwise `&=` does NOT short circuit, meaning it evaluates the 
     *     right side even if `res` is already false. To optimize and allow 
     *     short-circuiting, we could write: `boolean next = evaluate(s); res = res && next;` 
     *     However, because we MUST advance the `index` pointer past the entire 
     *     expression anyway to maintain parsing alignment, short-circuiting isn't 
     *     actually helpful here! We *have* to evaluate (parse) everything.
     * 
     * ========================================================================
     * 10. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: Nested structures evaluated from inside-out are the textbook 
     *   use case for a Stack.
     * - Optimization Trick: You do not need to save popped stack elements into a 
     *   temporary list to evaluate them. Using flags (like `hasTrue`, `hasFalse`) 
     *   is much more memory and time efficient.
     * - Always ignore separator characters (like commas) in stack parsing problems 
     *   unless they dictate operator precedence.
     */
}

import java.util.*;

/**
 * ============================================================
 * 🌳 Boolean Expression → AST + Evaluation (DEEP EXPLANATION)
 * ============================================================
 *
 * 🧠 CORE IDEA:
 * Instead of evaluating directly,
 * we first CONVERT the expression into a TREE (AST),
 * then evaluate that tree using DFS.
 *
 * WHY?
 * - Separation of concerns (Parsing vs Evaluation)
 * - Clean design (like compilers/interpreters)
 * - Easy to extend (add XOR, NAND etc)
 *
 * ============================================================
 * 📌 OVERALL FLOW:
 *
 * Input:  "|(&(t,f,t),!(t))"
 *
 * Step 1: Parse → Build Tree
 * Step 2: Evaluate → DFS
 *
 * ============================================================
 */
public class BooleanExpressionAST {

    public static void main(String[] args) {

        // Example expression
        String expression = "|(&(t,f,t),!(t))";

        /**
         * STEP 1: BUILD AST
         *
         * This converts string → tree structure
         */
        Node root = buildAST(expression);

        /**
         * STEP 2: EVALUATE TREE
         *
         * Traverse the tree and compute result
         */
        boolean result = evaluate(root);

        System.out.println(result); // false
    }

    /**
     * ============================================================
     * 🌳 NODE STRUCTURE (Using Java Record)
     * ============================================================
     *
     * Each node represents:
     *
     * operator:
     *   't', 'f' → leaf nodes (no children)
     *   '!', '&', '|' → internal nodes
     *
     * children:
     *   list of sub-expressions
     *
     * Example:
     *   &(t,f,t)
     *
     * becomes:
     *   Node('&', [Node('t'), Node('f'), Node('t')])
     */
    record Node(char operator, List<Node> children) {}

    /**
     * GLOBAL POINTER
     *
     * 🔥 VERY IMPORTANT:
     * Instead of passing index everywhere,
     * we maintain a shared pointer.
     *
     * It always points to:
     * 👉 "current character being parsed"
     */
    static int index = 0;

    /**
     * ============================================================
     * 🔨 BUILD AST (ENTRY METHOD)
     * ============================================================
     */
    public static Node buildAST(String exp) {

        // Reset index before parsing
        index = 0;

        // Start recursive parsing
        return parse(exp);
    }

    /**
     * ============================================================
     * 🔍 PARSER FUNCTION (CORE LOGIC)
     * ============================================================
     *
     * Think like grammar:
     *
     * expr :=
     *   't'
     * | 'f'
     * | '!(expr)'
     * | '&(expr,expr,...)'
     * | '|(expr,expr,...)'
     *
     * ============================================================
     */
    private static Node parse(String exp) {

        /**
         * STEP 1: Look at current character
         */
        char ch = exp.charAt(index);

        /**
         * ========================================================
         * ✅ CASE 1: LEAF NODE ('t' or 'f')
         * ========================================================
         *
         * If we see:
         *   't' → TRUE node
         *   'f' → FALSE node
         *
         * No children → return immediately
         */
        if (ch == 't' || ch == 'f') {

            index++; // Move forward (IMPORTANT)

            return new Node(ch, List.of()); // empty children
        }

        /**
         * ========================================================
         * ✅ CASE 2: OPERATOR NODE (!, &, |)
         * ========================================================
         *
         * Example:
         *   &(t,f,t)
         *
         * At index → '&'
         */
        char operator = ch;

        /**
         * WHY index += 2 ?
         *
         * Because after operator, we always have '('
         *
         * Example:
         *   &(t,f,t)
         *    ^
         *   index at '&'
         *
         * Move to first operand:
         *   skip '&' and '(' → index += 2
         */
        index += 2;

        /**
         * This will store all sub-expressions
         */
        List<Node> children = new ArrayList<>();

        /**
         * ========================================================
         * 🔁 PARSE ALL CHILDREN
         * ========================================================
         *
         * Example:
         *   &(t,f,t)
         *
         * We recursively parse:
         *   t, f, t
         */
        while (true) {

            /**
             * Recursive call:
             * This parses ONE full expression (could be nested)
             *
             * 🔥 KEY INSIGHT:
             * parse() always consumes a FULL expression
             */
            children.add(parse(exp));

            /**
             * After parsing child, check what's next:
             *
             * Case 1: ')'
             *   → end of this expression
             *
             * Case 2: ','
             *   → more children exist
             */
            if (exp.charAt(index) == ')') {

                index++; // consume ')'
                break;   // stop parsing children
            }

            /**
             * Otherwise it must be comma ','
             * Skip it and continue
             */
            index++;
        }

        /**
         * Finally create node with collected children
         */
        return new Node(operator, children);
    }

    /**
     * ============================================================
     * ⚡ EVALUATE AST (DFS)
     * ============================================================
     *
     * Traverse tree and compute result
     */
    public static boolean evaluate(Node node) {

        /**
         * ========================================================
         * ✅ BASE CASE: LEAF NODE
         * ========================================================
         */
        if (node.operator == 't') return true;
        if (node.operator == 'f') return false;

        /**
         * ========================================================
         * ✅ NOT OPERATOR
         * ========================================================
         *
         * Only ONE child
         */
        if (node.operator == '!') {

            // Evaluate child and negate
            return !evaluate(node.children.get(0));
        }

        /**
         * ========================================================
         * ✅ AND OPERATOR
         * ========================================================
         *
         * All children must be TRUE
         *
         * Short-circuit optimization:
         * If any child is FALSE → return FALSE immediately
         */
        if (node.operator == '&') {

            for (Node child : node.children) {

                if (!evaluate(child)) {
                    return false; // early exit
                }
            }

            return true;
        }

        /**
         * ========================================================
         * ✅ OR OPERATOR
         * ========================================================
         *
         * At least one child must be TRUE
         */
        if (node.operator == '|') {

            for (Node child : node.children) {

                if (evaluate(child)) {
                    return true; // early exit
                }
            }

            return false;
        }

        /**
         * Should never reach here (input guaranteed valid)
         */
        throw new IllegalStateException("Invalid operator");
    }
}
