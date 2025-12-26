import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
/*
 * Given a string s containing just the characters '(', ')', '{', '}', '[' and
 * ']', determine if the input string is valid.
 * 
 * An input string is valid if:
 * 
 * Open brackets must be closed by the same type of brackets.
 * Open brackets must be closed in the correct order.
 * Every close bracket has a corresponding open bracket of the same type.
 * 
 * 
 * Example 1:
 * Input: s = "()"
 * Output: true
 * 
 * Example 2:
 * Input: s = "()[]{}"
 * Output: true
 * 
 * Example 3:
 * Input: s = "(]"
 * Output: false
 * 
 * Example 4:
 * Input: s = "([])"
 * Output: true
 */

class Solution {

    /**
     * Approach 1: Stack with HashMap (Most Readable)
     * Time Complexity: O(n) - single pass through string
     * Space Complexity: O(n) - worst case all opening brackets
     */
    public boolean isValid(String s) {
        if (s == null || s.length() % 2 != 0) {
            return false; // Odd length can't be balanced
        }

        Stack<Character> stack = new Stack<>();
        Map<Character, Character> mapping = new HashMap<>();
        mapping.put(')', '(');
        mapping.put('}', '{');
        mapping.put(']', '[');

        for (char c : s.toCharArray()) {
            if (mapping.containsKey(c)) {
                // It's a closing bracket
                if (stack.isEmpty() || stack.pop() != mapping.get(c)) {
                    return false;
                }
            } else {
                // It's an opening bracket
                stack.push(c);
            }
        }

        return stack.isEmpty();
    }

    /**
     * Approach 2: Stack with Direct Character Comparison (More Efficient)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public boolean isValid2(String s) {
        if (s == null || s.length() % 2 != 0) {
            return false;
        }

        Stack<Character> stack = new Stack<>();

        for (char c : s.toCharArray()) {
            switch (c) {
                case '(':
                case '{':
                case '[':
                    stack.push(c);
                    break;
                case ')':
                    if (stack.isEmpty() || stack.pop() != '(')
                        return false;
                    break;
                case '}':
                    if (stack.isEmpty() || stack.pop() != '{')
                        return false;
                    break;
                case ']':
                    if (stack.isEmpty() || stack.pop() != '[')
                        return false;
                    break;
                default:
                    return false; // Invalid character
            }
        }

        return stack.isEmpty();
    }

    /**
     * Approach 3: Using Deque (Recommended for better performance)
     * ArrayDeque is faster than Stack for this use case
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public boolean isValid3(String s) {
        if (s == null || s.length() % 2 != 0) {
            return false;
        }

        Deque<Character> stack = new ArrayDeque<>();

        for (char c : s.toCharArray()) {
            if (c == '(' || c == '{' || c == '[') {
                stack.push(c);
            } else {
                if (stack.isEmpty())
                    return false;

                char top = stack.pop();
                if ((c == ')' && top != '(') ||
                        (c == '}' && top != '{') ||
                        (c == ']' && top != '[')) {
                    return false;
                }
            }
        }

        return stack.isEmpty();
    }

    /**
     * Approach 4: Optimized with Early Termination
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public boolean isValid4(String s) {
        int n = s.length();
        if (n % 2 != 0)
            return false;

        Deque<Character> stack = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);

            if (c == '(' || c == '{' || c == '[') {
                stack.push(c);
                // Early termination: if we have more than half opening brackets
                if (stack.size() > n / 2)
                    return false;
            } else {
                if (stack.isEmpty())
                    return false;

                char top = stack.pop();
                if (!isMatchingPair(top, c)) {
                    return false;
                }
            }
        }

        return stack.isEmpty();
    }

    private boolean isMatchingPair(char open, char close) {
        return (open == '(' && close == ')') ||
                (open == '{' && close == '}') ||
                (open == '[' && close == ']');
    }

    /**
     * Approach 5: String Replacement (Not Recommended - Just for Learning)
     * This approach repeatedly removes valid pairs until no more can be removed
     * Time Complexity: O(n²) - potentially multiple passes
     * Space Complexity: O(n) - string operations
     */
    public boolean isValid5(String s) {
        if (s.length() % 2 != 0)
            return false;

        while (s.contains("()") || s.contains("{}") || s.contains("[]")) {
            s = s.replace("()", "").replace("{}", "").replace("[]", "");
        }

        return s.isEmpty();
    }

    // Test cases
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test cases from examples
        System.out.println("Test 1 - '()': " + solution.isValid("()")); // Expected: true
        System.out.println("Test 2 - '()[{}]': " + solution.isValid("()[]{}")); // Expected: true
        System.out.println("Test 3 - '(]': " + solution.isValid("(]")); // Expected: false
        System.out.println("Test 4 - '([])': " + solution.isValid("([])")); // Expected: true

        // Additional test cases
        System.out.println("Test 5 - '': " + solution.isValid("")); // Expected: true (empty string)
        System.out.println("Test 6 - '((': " + solution.isValid("((")); // Expected: false
        System.out.println("Test 7 - '))': " + solution.isValid("))")); // Expected: false
        System.out.println("Test 8 - '({[]})': " + solution.isValid("({[]})")); // Expected: true
        System.out.println("Test 9 - '([)]': " + solution.isValid("([)]")); // Expected: false
        System.out.println("Test 10 - '{[]}': " + solution.isValid("{[]}")); // Expected: true

        // Edge cases
        System.out.println("Test 11 - '(': " + solution.isValid("(")); // Expected: false (odd length)
        System.out.println("Test 12 - ')': " + solution.isValid(")")); // Expected: false (starts with closing)

        // Performance comparison for different approaches
        String longString = "(((((((())))))))(((((((())))))))";
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            solution.isValid3(longString); // Using ArrayDeque
        }
        long end = System.nanoTime();
        System.out.println("ArrayDeque approach time: " + (end - start) / 1000000.0 + " ms");
    }

}

/**
 * EXPLANATION:
 * 
 * The Valid Parentheses problem requires checking if brackets are properly
 * balanced
 * and nested. A stack is the perfect data structure for this because:
 * 
 * 1. LIFO (Last In, First Out) property matches the nesting behavior
 * 2. When we encounter an opening bracket, we push it onto the stack
 * 3. When we encounter a closing bracket, we check if it matches the most
 * recent opening bracket
 * 
 * Algorithm Steps:
 * 1. Initialize an empty stack
 * 2. Iterate through each character in the string:
 * - If it's an opening bracket: push onto stack
 * - If it's a closing bracket:
 * * Check if stack is empty (no matching opening bracket) → return false
 * * Pop from stack and check if it matches → if not, return false
 * 3. After processing all characters, stack should be empty for valid string
 * 
 * Key Insights:
 * - Odd-length strings can never be valid (early termination)
 * - Empty string is considered valid
 * - Stack helps maintain the order of nested brackets
 * - ArrayDeque is generally faster than Stack class in Java
 * 
 * Time Complexity: O(n) - single pass through the string
 * Space Complexity: O(n) - worst case when all characters are opening brackets
 * 
 * Common Pitfalls:
 * - Forgetting to check if stack is empty before popping
 * - Not checking if stack is empty at the end
 * - Not handling edge cases like empty strings or odd-length strings
 */
