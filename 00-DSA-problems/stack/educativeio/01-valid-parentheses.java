/**
 * ============================================================================
 * VALID PARENTHESES - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 * 
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given a string containing just the characters '(', ')', '{', '}', 
 * '[' and ']', determine if the input string is valid.
 * 
 * A string is valid if:
 * - Open brackets are closed by the same type of brackets.
 * - Open brackets are closed in the correct order.
 * 
 * Simple Explanation for Interviewer:
 * "We need to parse a string of brackets and ensure that every opening bracket 
 * has a matching closing bracket, and that they are properly nested. If we open
 * a bracket, it must be the very next thing we close before closing any 
 * brackets that were opened before it."
 * 
 * Core Idea & Intuition:
 * The problem follows a Last-In-First-Out (LIFO) pattern. The most recently 
 * opened bracket MUST be the first one to be closed. Whenever a problem 
 * dictates that the most recent unresolved item must be resolved first, 
 * a Stack is the ideal data structure.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can the string be empty?"
 *    Why: An empty string is technically perfectly balanced, but constraints 
 *    often state length >= 1. 
 *    Impact: If it can be empty, we need an immediate `if (s.isEmpty()) return true;`
 * 
 * 2. Q: "Will the string contain non-bracket characters like letters or numbers?"
 *    Why: To know if we need to filter or ignore characters.
 *    Impact: The prompt constraints say only brackets, but if not, we'd need 
 *    to skip non-bracket characters in our loop.
 * 
 * 3. Q: "What is the maximum length of the string?"
 *    Why: Helps determine acceptable time complexity. Length 10^3 means O(N^2) 
 *    would pass, but O(N) is still expected. If it was 10^7, O(N) is strictly required.
 * 
 * 4. Q: "Are there any space complexity constraints?"
 *    Why: To see if an in-place modification (O(1) space) is strictly required, 
 *    though it's impossible in Java as strings are immutable. O(N) space via 
 *    a Stack is standard.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Basic Valid Example: s = "()[]{}" -> Valid
 * 
 * Edge Case Valid (Deep nesting): s = "([{}])" -> Valid
 * 
 * Common Invalid Example: s = "([)]" -> Invalid
 * (The '[' is closed by ')' which is a type mismatch, and they interleave).
 * 
 * Visualizing LIFO (Stack) for "([{}])":
 * 
 * Step 1: Read '(' -> Push ')'    | Stack: [')']
 * Step 2: Read '[' -> Push ']'    | Stack: [')', ']']
 * Step 3: Read '{' -> Push '}'    | Stack: [')', ']', '}']
 * Step 4: Read '}' -> Pop matches | Stack: [')', ']']
 * Step 5: Read ']' -> Pop matches | Stack: [')']
 * Step 6: Read ')' -> Pop matches | Stack: []
 * Result: Stack empty -> VALID!
 * 
 * ============================================================================
 * 4. INTERVIEW APPROACH
 * ----------------------------------------------------------------------------
 * 1. Understand & Clarify: Confirm it's only brackets and ask about string length.
 * 2. Initial Thought (Brute Force): Mention we could repeatedly replace "()", 
 *    "[]", and "{}" with an empty string until the string stops changing.
 *    (This shows you can at least solve it fundamentally).
 * 3. Optimize: Point out string replacement takes O(N^2) time because string 
 *    manipulation requires shifting elements. Suggest a Stack to track open brackets.
 * 4. Code: Write the Stack solution. Mention the trick of pushing the *closing* 
 *    bracket onto the stack when you see an opening one, to make matching trivial.
 * 5. Edge Cases: Explicitly mention checking for odd lengths (which can never 
 *    be valid) to get a free early exit.
 * 6. Dry Run: Walk through an example like "([)]" with your code.
 * 
 * ============================================================================
 * 5. EDGE CASES
 * ----------------------------------------------------------------------------
 * - Odd length string: e.g., "()(". Can't possibly be valid. (Fast fail)
 * - Single element: e.g., "]". Stack is empty when we try to pop.
 * - All opening brackets: e.g., "(((". Loop finishes but stack isn't empty.
 * - All closing brackets: e.g., ")))". Try to pop from empty stack immediately.
 * 
 * ============================================================================
 * 6. COMMON MISTAKES
 * ----------------------------------------------------------------------------
 * 1. Forgetting to check if the stack is empty before popping. (Throws EmptyStackException).
 * 2. Returning `true` immediately after the loop without checking if the stack 
 *    is empty (fails on "(((").
 * 3. Using `java.util.Stack`. In modern Java, `Stack` is a legacy class that 
 *    is synchronized (slow). Use `Deque<Character> stack = new ArrayDeque<>();` instead.
 * 
 * ============================================================================
 * 7. SOLUTION COMPARISON
 * ----------------------------------------------------------------------------
 * Approach        Time       Space      Difficulty      When to Use
 * ----------------------------------------------------------------------------
 * Brute Force     O(N^2)     O(N)       Easy            Never in production. Good to mention.
 * Stack (Optimal) O(N)       O(N)       Medium          ALWAYS use this in interviews.
 * 
 * ============================================================================
 * 8. FINAL TAKEAWAYS
 * ----------------------------------------------------------------------------
 * - Pattern: LIFO (Last-In-First-Out) == Stack.
 * - Trick: When encountering an opening bracket, push its CLOSING counterpart 
 *   to the stack. This makes the validation step a simple equality check.
 * - Legacy Java: Always use Deque/ArrayDeque instead of Stack.
 * - Quick Check: Odd-length strings are mathematically impossible to validate.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class ValidParentheses {

    public static void main(String[] args) {
        System.out.println("--- Brute Force Approach ---");
        System.out.println("()[]{} : " + isValidBruteForce("()[]{}")); // true
        System.out.println("([)]   : " + isValidBruteForce("([)]"));   // false

        System.out.println("\n--- Optimal Stack Approach ---");
        System.out.println("()[]{} : " + isValidOptimal("()[]{}")); // true
        System.out.println("([{}]) : " + isValidOptimal("([{}])")); // true
        System.out.println("([)]   : " + isValidOptimal("([)]"));   // false
        System.out.println("(((    : " + isValidOptimal("((("));    // false
        System.out.println("]      : " + isValidOptimal("]"));      // false
    }

    /**
     * SOLUTION 1: BRUTE FORCE (String Replacement)
     * ------------------------------------------------------------------------
     * Idea: A valid set of parentheses must contain at least one adjacent matching pair.
     * If we repeatedly find and remove "()", "{}", and "[]", a valid string 
     * will eventually become empty.
     * 
     * Time Complexity: O(N^2) in the worst case (e.g., "((((....))))") because
     * string replacement creates a new string and shifts characters every time.
     * Space Complexity: O(N) to store the modified string at each step.
     */
    public static boolean isValidBruteForce(String s) {
        // Edge case: Odd lengths cannot be valid
        if (s.length() % 2 != 0) return false;

        // Keep replacing inner valid brackets until string length stops changing
        int lengthBefore;
        do {
            lengthBefore = s.length();
            s = s.replace("()", "")
                 .replace("[]", "")
                 .replace("{}", "");
        } while (s.length() < lengthBefore); // Continue if we made progress

        // If string is empty, everything matched perfectly
        return s.isEmpty();
    }

    /**
     * SOLUTION 2: OPTIMAL APPROACH (Stack)
     * ------------------------------------------------------------------------
     * Idea: Iterate through the string. When we see an opening bracket, we push
     * what we EXPECT the closing bracket to be onto a stack. When we see a closing 
     * bracket, we check if it matches the top of the stack.
     * 
     * Why it works: The stack naturally enforces the "last opened, first closed" rule.
     * 
     * Time Complexity: O(N) where N is the length of the string. We visit each 
     * character exactly once, and stack operations (push/pop) are O(1).
     * Space Complexity: O(N) in the worst case (e.g., "(((((") where all 
     * characters are opening brackets and pushed to the stack.
     * 
     * Detailed Dry Run for "([)]":
     * - Init: Stack = []
     * - Char '(': Push ')'. Stack = [')']
     * - Char '[': Push ']'. Stack = [')', ']']
     * - Char ')': Is it an open bracket? No. 
     *             Is stack empty? No. 
     *             Does pop() ']' equal ')'? NO. 
     *             Return false.
     */
    public static boolean isValidOptimal(String s) {
        // Quick optimization: If the string has an odd number of characters,
        // it is impossible for every opening bracket to have a closing pair.
        if (s.length() % 2 != 0) {
            return false;
        }

        // We use ArrayDeque instead of java.util.Stack. 
        // java.util.Stack is a legacy class that introduces overhead because 
        // all its methods are synchronized. ArrayDeque is faster and idiomatic.
        Deque<Character> stack = new ArrayDeque<>();

        // Iterate through each character in the string
        for (char c : s.toCharArray()) {
            
            // If it's an opening bracket, push its exact matching closing bracket.
            // Pushing the expected closing bracket makes the evaluation logic 
            // for closing brackets much cleaner later.
            if (c == '(') {
                stack.push(')');
            } else if (c == '{') {
                stack.push('}');
            } else if (c == '[') {
                stack.push(']');
            } 
            // If it's not an opening bracket, it MUST be a closing bracket.
            else {
                // 1. If stack is empty, it means we have a closing bracket 
                //    without a prior opening bracket (e.g., "]").
                // 2. If stack.pop() doesn't match 'c', we have a mismatch (e.g., "[)").
                // Note: stack.pop() ALSO removes the element, which is exactly what we want.
                if (stack.isEmpty() || stack.pop() != c) {
                    return false;
                }
            }
        }

        // Final check: If the stack isn't empty, it means we had opening brackets
        // left over that were never closed (e.g., "(((").
        return stack.isEmpty();
    }
}
