 import java.util.*;
/*
 * Given a string s representing a valid expression, implement a basic
 * calculator to evaluate it, and return the result of the evaluation.
 * 
 * Note: You are not allowed to use any built-in function which evaluates
 * strings as mathematical expressions, such as eval().
 * 
 * Example 1:
 * Input: s = "1 + 1"
 * Output: 2
 * 
 * Example 2:
 * Input: s = " 2-1 + 2 "
 * Output: 3
 * 
 * Example 3:
 * Input: s = "(1+(4+5+2)-3)+(6+8)"
 * Output: 23
 */

class Solution {
    
    /**
     * Approach 1: Stack-based Solution (Most Common)
     * Time Complexity: O(n) where n is the length of string
     * Space Complexity: O(n) for the stack
     */
    public int calculate(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        
        Stack<Integer> stack = new Stack<>();
        int num = 0;
        int sign = 1; // 1 for positive, -1 for negative
        int result = 0;
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            
            if (Character.isDigit(c)) {
                // Build the number (handle multi-digit numbers)
                num = num * 10 + (c - '0');
            } else if (c == '+') {
                // Add current number to result with its sign
                result += sign * num;
                num = 0;
                sign = 1;
            } else if (c == '-') {
                // Add current number to result with its sign
                result += sign * num;
                num = 0;
                sign = -1;
            } else if (c == '(') {
                // Push current result and sign onto stack
                stack.push(result);
                stack.push(sign);
                // Reset for the new sub-expression
                result = 0;
                sign = 1;
            } else if (c == ')') {
                // Complete current number
                result += sign * num;
                num = 0;
                // Pop sign and previous result from stack
                result *= stack.pop(); // This is the sign before '('
                result += stack.pop(); // This is the result before '('
            }
            // Ignore spaces
        }
        
        // Add the last number
        result += sign * num;
        return result;
    }
    
    /**
     * Approach 2: Recursive Descent Parser
     * Time Complexity: O(n)
     * Space Complexity: O(n) for recursion stack
     */
    private int index = 0;
    
    public int calculate2(String s) {
        index = 0;
        return parseExpression(s);
    }
    
    private int parseExpression(String s) {
        int result = 0;
        int sign = 1;
        int num = 0;
        
        while (index < s.length()) {
            char c = s.charAt(index);
            
            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            } else if (c == '+') {
                result += sign * num;
                num = 0;
                sign = 1;
            } else if (c == '-') {
                result += sign * num;
                num = 0;
                sign = -1;
            } else if (c == '(') {
                index++; // Skip '('
                int subResult = parseExpression(s);
                num = subResult;
            } else if (c == ')') {
                result += sign * num;
                return result;
            }
            // Skip spaces
            index++;
        }
        
        result += sign * num;
        return result;
    }
    
    /**
     * Approach 3: Two-Stack Solution (Operands and Operators)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public int calculate3(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        
        Stack<Integer> operands = new Stack<>();
        Stack<Character> operators = new Stack<>();
        
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == ' ') {
                i++;
                continue;
            }
            
            if (Character.isDigit(s.charAt(i))) {
                int num = 0;
                while (i < s.length() && Character.isDigit(s.charAt(i))) {
                    num = num * 10 + (s.charAt(i) - '0');
                    i++;
                }
                operands.push(num);
            } else if (s.charAt(i) == '(') {
                operators.push(s.charAt(i));
                i++;
            } else if (s.charAt(i) == ')') {
                // Process until we find '('
                while (!operators.isEmpty() && operators.peek() != '(') {
                    processOperation(operands, operators);
                }
                operators.pop(); // Remove '('
                i++;
            } else if (s.charAt(i) == '+' || s.charAt(i) == '-') {
                // Process operations with equal or higher precedence
                while (!operators.isEmpty() && operators.peek() != '(' && 
                       precedence(operators.peek()) >= precedence(s.charAt(i))) {
                    processOperation(operands, operators);
                }
                operators.push(s.charAt(i));
                i++;
            }
        }
        
        // Process remaining operations
        while (!operators.isEmpty()) {
            processOperation(operands, operators);
        }
        
        return operands.pop();
    }
    
    private void processOperation(Stack<Integer> operands, Stack<Character> operators) {
        int b = operands.pop();
        int a = operands.pop();
        char op = operators.pop();
        
        switch (op) {
            case '+':
                operands.push(a + b);
                break;
            case '-':
                operands.push(a - b);
                break;
        }
    }
    
    private int precedence(char op) {
        if (op == '+' || op == '-') return 1;
        return 0;
    }
    
    /**
     * Approach 4: Single Pass with State Machine
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public int calculate4(String s) {
        return evaluate(s, new int[]{0});
    }
    
    private int evaluate(String s, int[] index) {
        int result = 0;
        int sign = 1;
        int num = 0;
        
        while (index[0] < s.length()) {
            char c = s.charAt(index[0]);
            
            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            } else if (c == '+') {
                result += sign * num;
                num = 0;
                sign = 1;
            } else if (c == '-') {
                result += sign * num;
                num = 0;
                sign = -1;
            } else if (c == '(') {
                index[0]++; // Skip '('
                num = evaluate(s, index); // Recursive call
            } else if (c == ')') {
                break; // End of current sub-expression
            }
            index[0]++;
        }
        
        return result + sign * num;
    }
    
    /**
     * Approach 5: Clean Stack Solution with Helper Class
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    static class Calculator {
        private Stack<Integer> stack;
        
        public Calculator() {
            stack = new Stack<>();
        }
        
        public int calculate(String s) {
            int num = 0;
            char sign = '+';
            
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                
                if (Character.isDigit(c)) {
                    num = num * 10 + (c - '0');
                }
                
                if (c == '(') {
                    // Find matching closing parenthesis
                    int count = 0;
                    int start = i;
                    while (i < s.length()) {
                        if (s.charAt(i) == '(') count++;
                        if (s.charAt(i) == ')') count--;
                        if (count == 0) break;
                        i++;
                    }
                    // Recursively calculate the sub-expression
                    num = calculate(s.substring(start + 1, i));
                }
                
                if (c == '+' || c == '-' || i == s.length() - 1) {
                    if (sign == '+') {
                        stack.push(num);
                    } else if (sign == '-') {
                        stack.push(-num);
                    }
                    sign = c;
                    num = 0;
                }
            }
            
            int result = 0;
            while (!stack.isEmpty()) {
                result += stack.pop();
            }
            return result;
        }
    }
    
    public int calculate5(String s) {
        return new Calculator().calculate(s);
    }
    
    // Test cases
    public static void main(String[] args) {
        Solution solution = new Solution();
        
        // Test case 1
        System.out.println("Test 1: " + solution.calculate("1 + 1")); 
        // Expected: 2
        
        // Test case 2
        System.out.println("Test 2: " + solution.calculate(" 2-1 + 2 ")); 
        // Expected: 3
        
        // Test case 3
        System.out.println("Test 3: " + solution.calculate("(1+(4+5+2)-3)+(6+8)")); 
        // Expected: 23
        
        // Additional test cases
        System.out.println("Test 4: " + solution.calculate("1")); 
        // Expected: 1
        
        System.out.println("Test 5: " + solution.calculate("2147483647")); 
        // Expected: 2147483647
        
        System.out.println("Test 6: " + solution.calculate("1-(     -2)")); 
        // Expected: 3
        
        System.out.println("Test 7: " + solution.calculate("- (3 + (4 + 5))")); 
        // Expected: -12
        
        System.out.println("Test 8: " + solution.calculate("((1+2))")); 
        // Expected: 3
        
        System.out.println("Test 9: " + solution.calculate("1-11")); 
        // Expected: -10
        
        System.out.println("Test 10: " + solution.calculate("(7)-(0)+(4)")); 
        // Expected: 11
        
        // Test different approaches
        System.out.println("\nTesting different approaches:");
        String testExpr = "(1+(4+5+2)-3)+(6+8)";
        System.out.println("Approach 1: " + solution.calculate(testExpr));
        System.out.println("Approach 2: " + solution.calculate2(testExpr));
        System.out.println("Approach 3: " + solution.calculate3(testExpr));
        System.out.println("Approach 4: " + solution.calculate4(testExpr));
        System.out.println("Approach 5: " + solution.calculate5(testExpr));
    }

}

/**
 * DETAILED EXPLANATION:
 * 
 * This problem requires evaluating mathematical expressions with:
 * 1. Addition and subtraction operations
 * 2. Parentheses for grouping
 * 3. Multi-digit numbers
 * 4. Spaces (to be ignored)
 * 
 * APPROACH 1 - Stack-based (Recommended):
 * Key insight: Use stack to store intermediate results when encountering parentheses
 * 
 * Algorithm:
 * 1. Maintain current result, current number, and current sign
 * 2. For digits: build the current number
 * 3. For '+'/'-': add current number to result, update sign
 * 4. For '(': push current result and sign to stack, reset for sub-expression
 * 5. For ')': complete sub-expression, multiply by sign from stack, add to previous result
 * 
 * Example trace for "(1+2)":
 * - '(': stack=[0,1], result=0, sign=1
 * - '1': num=1
 * - '+': result=0+1*1=1, sign=1, num=0
 * - '2': num=2  
 * - ')': result=1+1*2=3, sign=stack.pop()=1, result=3*1+stack.pop()=3+0=3
 * 
 * APPROACH 2 - Recursive Descent:
 * Treats each parentheses group as a recursive sub-problem
 * Natural for nested expressions but uses recursion stack
 * 
 * APPROACH 3 - Two Stacks:
 * Classical approach with separate stacks for operands and operators
 * Good for understanding operator precedence concepts
 * 
 * APPROACH 4 - State Machine:
 * Uses array to pass index by reference, enabling recursive calls
 * Clean separation of parsing logic
 * 
 * APPROACH 5 - Helper Class:
 * Encapsulates calculator logic, handles parentheses by substring recursion
 * Good for object-oriented design
 * 
 * Key Challenges:
 * 1. **Multi-digit numbers**: Build numbers digit by digit
 * 2. **Nested parentheses**: Use stack or recursion to handle nesting
 * 3. **Sign handling**: Track signs carefully, especially with parentheses
 * 4. **Space handling**: Skip whitespace characters
 * 5. **Edge cases**: Single numbers, nested parentheses, negative results
 * 
 * Time Complexity: O(n) - single pass through string
 * Space Complexity: O(n) - stack depth proportional to nesting level
 */
