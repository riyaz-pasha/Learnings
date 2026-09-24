import java.util.Stack;

class InfixToPostfix {
    
    // Approach 1: Using Stack (Optimal - Shunting Yard Algorithm)
    // Time: O(n), Space: O(n)
    public String infixToPostfix(String infix) {
        StringBuilder result = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        
        for (int i = 0; i < infix.length(); i++) {
            char c = infix.charAt(i);
            
            // If character is operand, add to result
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
            }
            // If character is '(', push to stack
            else if (c == '(') {
                stack.push(c);
            }
            // If character is ')', pop until '('
            else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    result.append(stack.pop());
                }
                stack.pop(); // Remove '('
            }
            // If character is operator
            else {
                while (!stack.isEmpty() && precedence(c) <= precedence(stack.peek())) {
                    result.append(stack.pop());
                }
                stack.push(c);
            }
        }
        
        // Pop remaining operators
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        
        return result.toString();
    }
    
    // Helper: Get operator precedence
    private int precedence(char op) {
        return switch (op) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            case '^' -> 3;
            default -> -1;
        };
    }
    
    // Approach 2: With Associativity Handling
    // Handles right-associative operators like '^'
    public String infixToPostfixWithAssociativity(String infix) {
        StringBuilder result = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        
        for (int i = 0; i < infix.length(); i++) {
            char c = infix.charAt(i);
            
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
            }
            else if (c == '(') {
                stack.push(c);
            }
            else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    result.append(stack.pop());
                }
                stack.pop();
            }
            else {
                // Right associative (^): use <
                // Left associative (+,-,*,/): use <=
                if (c == '^') {
                    while (!stack.isEmpty() && precedence(c) < precedence(stack.peek())) {
                        result.append(stack.pop());
                    }
                } else {
                    while (!stack.isEmpty() && precedence(c) <= precedence(stack.peek())) {
                        result.append(stack.pop());
                    }
                }
                stack.push(c);
            }
        }
        
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        
        return result.toString();
    }
    
    // Approach 3: With Multi-digit Number Support
    public String infixToPostfixWithNumbers(String infix) {
        StringBuilder result = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        
        for (int i = 0; i < infix.length(); i++) {
            char c = infix.charAt(i);
            
            // Skip whitespace
            if (c == ' ') continue;
            
            // Handle multi-digit numbers
            if (Character.isDigit(c)) {
                while (i < infix.length() && Character.isDigit(infix.charAt(i))) {
                    result.append(infix.charAt(i++));
                }
                result.append(' '); // Space separator
                i--;
            }
            else if (Character.isLetter(c)) {
                result.append(c);
            }
            else if (c == '(') {
                stack.push(c);
            }
            else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    result.append(stack.pop());
                    result.append(' ');
                }
                stack.pop();
            }
            else {
                while (!stack.isEmpty() && precedence(c) <= precedence(stack.peek())) {
                    result.append(stack.pop());
                    result.append(' ');
                }
                stack.push(c);
            }
        }
        
        while (!stack.isEmpty()) {
            result.append(stack.pop());
            result.append(' ');
        }
        
        return result.toString().trim();
    }
    
    // Helper: Check if character is operator
    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    
    // Test cases with detailed visualization
    public static void main(String[] args) {
        InfixToPostfix converter = new InfixToPostfix();
        
        // Test Case 1: Simple expression
        String infix1 = "A+B";
        String postfix1 = converter.infixToPostfix(infix1);
        System.out.println("Test 1:");
        System.out.println("Infix:    " + infix1);
        System.out.println("Postfix:  " + postfix1);
        visualizeConversion(infix1, postfix1);
        
        // Test Case 2: With precedence
        String infix2 = "A+B*C";
        String postfix2 = converter.infixToPostfix(infix2);
        System.out.println("\nTest 2:");
        System.out.println("Infix:    " + infix2);
        System.out.println("Postfix:  " + postfix2);
        visualizeConversion(infix2, postfix2);
        
        // Test Case 3: With parentheses
        String infix3 = "(A+B)*C";
        String postfix3 = converter.infixToPostfix(infix3);
        System.out.println("\nTest 3:");
        System.out.println("Infix:    " + infix3);
        System.out.println("Postfix:  " + postfix3);
        visualizeConversion(infix3, postfix3);
        
        // Test Case 4: Complex expression
        String infix4 = "A+B*C-D/E";
        String postfix4 = converter.infixToPostfix(infix4);
        System.out.println("\nTest 4:");
        System.out.println("Infix:    " + infix4);
        System.out.println("Postfix:  " + postfix4);
        
        // Test Case 5: Nested parentheses
        String infix5 = "((A+B)*C-(D-E))*F";
        String postfix5 = converter.infixToPostfix(infix5);
        System.out.println("\nTest 5:");
        System.out.println("Infix:    " + infix5);
        System.out.println("Postfix:  " + postfix5);
        
        // Test Case 6: With exponentiation
        String infix6 = "A^B^C";
        String postfix6 = converter.infixToPostfixWithAssociativity(infix6);
        System.out.println("\nTest 6 (Right associative):");
        System.out.println("Infix:    " + infix6);
        System.out.println("Postfix:  " + postfix6);
        System.out.println("Note: A^B^C = A^(B^C), not (A^B)^C");
        
        // Detailed step-by-step trace
        System.out.println("\n=== Step-by-Step Trace for 'A+B*C' ===");
        traceConversion("A+B*C");
    }
    
    private static void visualizeConversion(String infix, String postfix) {
        System.out.println("\nVisualization:");
        System.out.println("Infix:    " + infix + "  (operators between operands)");
        System.out.println("Postfix:  " + postfix + "  (operators after operands)");
        
        // Show evaluation order
        if (infix.equals("A+B*C")) {
            System.out.println("\nEvaluation order:");
            System.out.println("Infix:   A + (B * C)  → First *, then +");
            System.out.println("Postfix: A B C * +    → Read left to right, apply operators");
        }
    }
    
    private static void traceConversion(String infix) {
        Stack<Character> stack = new Stack<>();
        StringBuilder result = new StringBuilder();
        
        System.out.println("┌────────┬─────────────┬──────────────┬────────────────┐");
        System.out.println("│ Char   │ Action      │ Stack        │ Result         │");
        System.out.println("├────────┼─────────────┼──────────────┼────────────────┤");
        
        for (char c : infix.toCharArray()) {
            String action = "";
            
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
                action = "Add to result";
            }
            else if (c == '(') {
                stack.push(c);
                action = "Push to stack";
            }
            else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    result.append(stack.pop());
                }
                stack.pop();
                action = "Pop until '('";
            }
            else {
                while (!stack.isEmpty() && getPrecedence(c) <= getPrecedence(stack.peek())) {
                    result.append(stack.pop());
                }
                stack.push(c);
                action = "Pop & push";
            }
            
            System.out.printf("│   %c    │ %-11s │ %-12s │ %-14s │%n", 
                            c, action, stackToString(stack), result.toString());
        }
        
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        
        System.out.println("├────────┼─────────────┼──────────────┼────────────────┤");
        System.out.printf("│  END   │ Pop all     │ %-12s │ %-14s │%n", 
                        stackToString(stack), result.toString());
        System.out.println("└────────┴─────────────┴──────────────┴────────────────┘");
    }
    
    private static String stackToString(Stack<Character> stack) {
        if (stack.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < stack.size(); i++) {
            sb.append(stack.get(i));
            if (i < stack.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static int getPrecedence(char op) {
        switch (op) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
            case '^':
                return 3;
        }
        return -1;
    }
}

/*
DETAILED EXPLANATION:

WHAT IS INFIX NOTATION?
Operators are between operands: A + B
- Natural for humans to read
- Requires parentheses for precedence: (A + B) * C

WHAT IS POSTFIX NOTATION (RPN)?
Operators come after operands: A B +
- No parentheses needed
- Easy for computers to evaluate
- Also called Reverse Polish Notation (RPN)

EXAMPLES:
Infix:    A + B          Postfix: A B +
Infix:    A + B * C      Postfix: A B C * +
Infix:    (A + B) * C    Postfix: A B + C *
Infix:    A * B + C      Postfix: A B * C +

SHUNTING YARD ALGORITHM (Dijkstra):

Core Idea: Use stack to reorder operators based on precedence

Rules:
1. Operands → directly to output
2. Operators → stack (pop higher/equal precedence first)
3. '(' → push to stack
4. ')' → pop until matching '('
5. End → pop all remaining operators

OPERATOR PRECEDENCE:
^ (Exponentiation)  → 3 (highest)
* / (Multiply/Div)  → 2
+ - (Add/Subtract)  → 1 (lowest)

ASSOCIATIVITY:
Left: +, -, *, /  → A - B - C = (A - B) - C
Right: ^          → A ^ B ^ C = A ^ (B ^ C)

STEP-BY-STEP EXAMPLE: "A+B*C"

Step 1: Read 'A' (operand)
  Stack: []
  Output: A

Step 2: Read '+' (operator)
  Stack: [+]
  Output: A

Step 3: Read 'B' (operand)
  Stack: [+]
  Output: AB

Step 4: Read '*' (operator)
  * has higher precedence than +
  Stack: [+, *]
  Output: AB

Step 5: Read 'C' (operand)
  Stack: [+, *]
  Output: ABC

Step 6: End - pop all
  Pop *: Output: ABC*
  Pop +: Output: ABC*+

Result: ABC*+

Evaluation: BC* = (B*C), then A + (B*C)

EXAMPLE WITH PARENTHESES: "(A+B)*C"

Step 1: Read '('
  Stack: [(]
  Output: 

Step 2: Read 'A'
  Stack: [(]
  Output: A

Step 3: Read '+'
  Stack: [(, +]
  Output: A

Step 4: Read 'B'
  Stack: [(, +]
  Output: AB

Step 5: Read ')'
  Pop until '(': 
  Stack: []
  Output: AB+

Step 6: Read '*'
  Stack: [*]
  Output: AB+

Step 7: Read 'C'
  Stack: [*]
  Output: AB+C

Step 8: End
  Output: AB+C*

Result: AB+C*

WHY USE POSTFIX?

Advantages:
1. No parentheses needed
2. Easy to evaluate with stack
3. Unambiguous evaluation order
4. Efficient for calculators/compilers

Evaluation of Postfix:
1. Scan left to right
2. If operand → push to stack
3. If operator → pop 2 operands, apply, push result
4. Final stack value is answer

Example: AB+C*
  Push A, Push B
  +: Pop B,A → Compute A+B → Push result
  Push C
  *: Pop C, (A+B) → Compute (A+B)*C → Result

COMPLEXITY ANALYSIS:

Time: O(n)
- Single pass through input
- Each character processed once
- Stack operations O(1)

Space: O(n)
- Stack stores operators/parentheses
- Output string stores result
- Worst case: all operators stacked

EDGE CASES:

1. Single operand: "A" → "A"
2. No operators: "ABC" → "ABC"
3. Only operators: Invalid input
4. Nested parentheses: "((A+B))" → "AB+"
5. Consecutive operators: Not allowed in valid infix
6. Empty string: "" → ""

COMMON MISTAKES:

1. Wrong precedence order
2. Forgetting to pop operators at end
3. Not handling parentheses correctly
4. Wrong associativity for ^
5. Not checking for balanced parentheses

ASSOCIATIVITY HANDLING:

Left-associative: A - B - C
  = (A - B) - C
  Postfix: A B - C -

Right-associative: A ^ B ^ C
  = A ^ (B ^ C)
  Postfix: A B C ^ ^

Implementation difference:
- Left: precedence(c) <= precedence(stack.peek())
- Right: precedence(c) < precedence(stack.peek())

PRACTICAL APPLICATIONS:

1. Compilers: Expression parsing
2. Calculators: HP calculators use RPN
3. Programming languages: FORTH, PostScript
4. Computer architecture: Stack-based VMs
5. Expression evaluation engines

INTERVIEW TIPS:

1. Explain the algorithm clearly
2. Draw the stack at each step
3. Mention operator precedence
4. Handle parentheses correctly
5. Discuss time/space complexity
6. Test with examples
7. Consider edge cases

RELATED PROBLEMS:

1. Postfix to Infix
2. Infix to Prefix
3. Evaluate Postfix Expression
4. Expression Tree Construction
5. Valid Parentheses

VARIATIONS:

1. With functions: sin(A+B) → AB+sin
2. With unary operators: -A+B → A-B+
3. Multi-character operands: "AB+CD*" 
4. With comparison operators: A>B
5. Boolean expressions: A&&B||C

OPTIMIZATION:

1. Pre-allocate StringBuilder size
2. Use array instead of Stack
3. Validate input first
4. Handle multi-digit numbers
5. Support operator precedence table

This algorithm is fundamental in:
- Compiler design
- Expression evaluation
- Calculator implementation
- Language parsing
*/
