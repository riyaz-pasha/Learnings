import java.util.*;
/*
 * You are given an array of strings tokens that represents an arithmetic
 * expression in a Reverse Polish Notation.
 * 
 * Evaluate the expression. Return an integer that represents the value of the
 * expression.
 * 
 * Note that:
 * 
 * The valid operators are '+', '-', '*', and '/'.
 * Each operand may be an integer or another expression.
 * The division between two integers always truncates toward zero.
 * There will not be any division by zero.
 * The input represents a valid arithmetic expression in a reverse polish
 * notation.
 * The answer and all the intermediate calculations can be represented in a
 * 32-bit integer.
 * 
 * Example 1:
 * Input: tokens = ["2","1","+","3","*"]
 * Output: 9
 * Explanation: ((2 + 1) * 3) = 9
 * 
 * Example 2:
 * Input: tokens = ["4","13","5","/","+"]
 * Output: 6
 * Explanation: (4 + (13 / 5)) = 6
 * 
 * Example 3:
 * Input: tokens = ["10","6","9","3","+","-11","*","/","*","17","+","5","+"]
 * Output: 22
 * Explanation: ((10 * (6 / ((9 + 3) * -11))) + 17) + 5
 * = ((10 * (6 / (12 * -11))) + 17) + 5
 * = ((10 * (6 / -132)) + 17) + 5
 * = ((10 * 0) + 17) + 5
 * = (0 + 17) + 5
 * = 17 + 5
 * = 22
 */

class Solution {

    /**
     * Approach 1: Stack-based Solution (Most Common)
     * Time Complexity: O(n) where n is the number of tokens
     * Space Complexity: O(n) for the stack
     */
    public int evalRPN(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return 0;
        }

        Stack<Integer> stack = new Stack<>();

        for (String token : tokens) {
            if (isOperator(token)) {
                // Pop two operands (order matters for - and /)
                int operand2 = stack.pop();
                int operand1 = stack.pop();

                int result = performOperation(operand1, operand2, token);
                stack.push(result);
            } else {
                // It's a number, push to stack
                stack.push(Integer.parseInt(token));
            }
        }

        return stack.pop();
    }

    private boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") ||
                token.equals("*") || token.equals("/");
    }

    private int performOperation(int operand1, int operand2, String operator) {
        switch (operator) {
            case "+":
                return operand1 + operand2;
            case "-":
                return operand1 - operand2;
            case "*":
                return operand1 * operand2;
            case "/":
                // Java division truncates toward zero for positive results
                // but we need to handle negative results carefully
                return (int) ((double) operand1 / operand2);
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }

    /**
     * Approach 2: Using Deque (More Efficient)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public int evalRPN2(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return 0;
        }

        Deque<Integer> deque = new ArrayDeque<>();

        for (String token : tokens) {
            switch (token) {
                case "+":
                    deque.push(deque.pop() + deque.pop());
                    break;
                case "-":
                    int subtrahend = deque.pop();
                    int minuend = deque.pop();
                    deque.push(minuend - subtrahend);
                    break;
                case "*":
                    deque.push(deque.pop() * deque.pop());
                    break;
                case "/":
                    int divisor = deque.pop();
                    int dividend = deque.pop();
                    deque.push(dividend / divisor);
                    break;
                default:
                    deque.push(Integer.parseInt(token));
                    break;
            }
        }

        return deque.pop();
    }

    /**
     * Approach 3: Using Set for Operator Check
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public int evalRPN3(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return 0;
        }

        Set<String> operators = new HashSet<>(Arrays.asList("+", "-", "*", "/"));
        Stack<Integer> stack = new Stack<>();

        for (String token : tokens) {
            if (operators.contains(token)) {
                int b = stack.pop();
                int a = stack.pop();

                switch (token) {
                    case "+":
                        stack.push(a + b);
                        break;
                    case "-":
                        stack.push(a - b);
                        break;
                    case "*":
                        stack.push(a * b);
                        break;
                    case "/":
                        stack.push(a / b);
                        break;
                }
            } else {
                stack.push(Integer.parseInt(token));
            }
        }

        return stack.pop();
    }

    /**
     * Approach 4: Functional Programming Style
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public int evalRPN4(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return 0;
        }

        Stack<Integer> stack = new Stack<>();

        Arrays.stream(tokens).forEach(token -> {
            if ("+".equals(token)) {
                stack.push(stack.pop() + stack.pop());
            } else if ("-".equals(token)) {
                int b = stack.pop(), a = stack.pop();
                stack.push(a - b);
            } else if ("*".equals(token)) {
                stack.push(stack.pop() * stack.pop());
            } else if ("/".equals(token)) {
                int b = stack.pop(), a = stack.pop();
                stack.push(a / b);
            } else {
                stack.push(Integer.parseInt(token));
            }
        });

        return stack.pop();
    }

    /**
     * Approach 5: ArrayList as Stack
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public int evalRPN5(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return 0;
        }

        List<Integer> stack = new ArrayList<>();

        for (String token : tokens) {
            if (isOperator(token)) {
                int size = stack.size();
                int operand2 = stack.remove(size - 1);
                int operand1 = stack.remove(size - 2);

                int result = calculate(operand1, operand2, token);
                stack.add(result);
            } else {
                stack.add(Integer.parseInt(token));
            }
        }

        return stack.get(0);
    }

    private int calculate(int a, int b, String op) {
        switch (op) {
            case "+":
                return a + b;
            case "-":
                return a - b;
            case "*":
                return a * b;
            case "/":
                return a / b;
            default:
                throw new IllegalArgumentException("Invalid operator");
        }
    }

    // Test cases
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        String[] tokens1 = { "2", "1", "+", "3", "*" };
        System.out.println("Test 1: " + solution.evalRPN(tokens1));
        // Expected: 9, Explanation: ((2 + 1) * 3) = 9

        // Test case 2
        String[] tokens2 = { "4", "13", "5", "/", "+" };
        System.out.println("Test 2: " + solution.evalRPN(tokens2));
        // Expected: 6, Explanation: (4 + (13 / 5)) = 6

        // Test case 3
        String[] tokens3 = { "10", "6", "9", "3", "+", "-11", "*", "/", "*", "17", "+", "5", "+" };
        System.out.println("Test 3: " + solution.evalRPN(tokens3));
        // Expected: 22

        // Additional test cases
        String[] tokens4 = { "3", "4", "+" };
        System.out.println("Test 4: " + solution.evalRPN(tokens4));
        // Expected: 7

        String[] tokens5 = { "15", "7", "1", "1", "+", "/", "/", "3", "+" };
        System.out.println("Test 5: " + solution.evalRPN(tokens5));
        // Expected: 5, Explanation: ((15 / (7 / (1 + 1))) + 3) = 5

        String[] tokens6 = { "5", "1", "2", "+", "4", "*", "+", "3", "-" };
        System.out.println("Test 6: " + solution.evalRPN(tokens6));
        // Expected: 14, Explanation: (5 + ((1 + 2) * 4)) - 3 = 14

        // Test with negative numbers
        String[] tokens7 = { "-3", "4", "+" };
        System.out.println("Test 7: " + solution.evalRPN(tokens7));
        // Expected: 1

        // Test division truncation
        String[] tokens8 = { "6", "-132", "/" };
        System.out.println("Test 8: " + solution.evalRPN(tokens8));
        // Expected: 0, Explanation: 6 / (-132) = 0 (truncates toward zero)
    }

}

/**
 * DETAILED EXPLANATION:
 * 
 * Reverse Polish Notation (RPN) is a postfix notation where operators come
 * after operands.
 * This makes it perfect for stack-based evaluation because:
 * 1. When we encounter a number, we push it onto the stack
 * 2. When we encounter an operator, we pop the required operands, compute, and
 * push result
 * 
 * Key Points:
 * 1. **Order matters**: For non-commutative operations (- and /), the first
 * popped element
 * is the second operand, and the second popped element is the first operand
 * 2. **Division truncation**: Java's division naturally truncates toward zero
 * 3. **Negative numbers**: Handle negative numbers in input correctly
 * 
 * Algorithm Steps:
 * 1. Initialize an empty stack
 * 2. For each token:
 * - If it's a number: push to stack
 * - If it's an operator: pop two operands, compute, push result
 * 3. Return the final result (only element left in stack)
 * 
 * Example Trace for ["2","1","+","3","*"]:
 * 1. "2" → stack: [2]
 * 2. "1" → stack: [2, 1]
 * 3. "+" → pop 1, pop 2, compute 2+1=3 → stack: [3]
 * 4. "3" → stack: [3, 3]
 * 5. "*" → pop 3, pop 3, compute 3*3=9 → stack: [9]
 * 6. Result: 9
 * 
 * Important Notes:
 * - For subtraction: if we pop b then a, result is a - b
 * - For division: if we pop b then a, result is a / b
 * - Division truncates toward zero (Java's default behavior)
 * - All intermediate results fit in 32-bit integers
 * 
 * Time Complexity: O(n) - single pass through tokens
 * Space Complexity: O(n) - stack can grow up to n/2 elements in worst case
 */
