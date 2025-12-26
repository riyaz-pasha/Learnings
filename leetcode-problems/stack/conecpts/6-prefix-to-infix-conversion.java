import java.util.Stack;

class PrefixToInfix {
    
    // Approach 1: Using Stack (Optimal)
    // Time: O(n), Space: O(n)
    public String prefixToInfix(String prefix) {
        Stack<String> stack = new Stack<>();
        
        // Read prefix expression from RIGHT to LEFT
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);
            
            // If character is operator
            if (isOperator(c)) {
                // Pop two operands
                String operand1 = stack.pop();
                String operand2 = stack.pop();
                
                // Create infix expression: (operand1 operator operand2)
                String infix = "(" + operand1 + c + operand2 + ")";
                
                // Push back to stack
                stack.push(infix);
            }
            // If character is operand
            else {
                // Push to stack as string
                stack.push(String.valueOf(c));
            }
        }
        
        // Final result is at top of stack
        return stack.pop();
    }
    
    // Approach 2: Without Extra Parentheses (Minimal Parentheses)
    // Uses precedence to avoid unnecessary parentheses
    public String prefixToInfixMinimal(String prefix) {
        Stack<Node> stack = new Stack<>();
        
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);
            
            if (isOperator(c)) {
                Node operand1 = stack.pop();
                Node operand2 = stack.pop();
                
                // Build expression considering precedence
                String left = needsParentheses(operand1, c, true) ? 
                             "(" + operand1.expr + ")" : operand1.expr;
                String right = needsParentheses(operand2, c, false) ? 
                              "(" + operand2.expr + ")" : operand2.expr;
                
                String infix = left + c + right;
                stack.push(new Node(infix, c, getPrecedence(c)));
            }
            else {
                stack.push(new Node(String.valueOf(c), c, Integer.MAX_VALUE));
            }
        }
        
        return stack.pop().expr;
    }
    
    // Helper class for tracking expression with operator info
    static class Node {
        String expr;
        char operator;
        int precedence;
        
        Node(String expr, char operator, int precedence) {
            this.expr = expr;
            this.operator = operator;
            this.precedence = precedence;
        }
    }
    
    // Check if parentheses needed based on precedence
    private boolean needsParentheses(Node node, char parentOp, boolean isLeft) {
        if (node.precedence == Integer.MAX_VALUE) return false; // Operand
        
        int parentPrec = getPrecedence(parentOp);
        
        if (node.precedence < parentPrec) return true;
        if (node.precedence > parentPrec) return false;
        
        // Same precedence: check associativity
        if (isLeft) {
            return false; // Left operand doesn't need parens
        } else {
            return parentOp == '-' || parentOp == '/'; // Right operand needs parens for - and /
        }
    }
    
    // Approach 3: With Multi-character Operands
    public String prefixToInfixMultiChar(String prefix) {
        Stack<String> stack = new Stack<>();
        StringBuilder operand = new StringBuilder();
        
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);
            
            if (c == ' ') {
                if (operand.length() > 0) {
                    stack.push(operand.reverse().toString());
                    operand = new StringBuilder();
                }
            }
            else if (isOperator(c)) {
                if (operand.length() > 0) {
                    stack.push(operand.reverse().toString());
                    operand = new StringBuilder();
                }
                
                String op1 = stack.pop();
                String op2 = stack.pop();
                String infix = "(" + op1 + c + op2 + ")";
                stack.push(infix);
            }
            else {
                operand.append(c);
            }
        }
        
        if (operand.length() > 0) {
            stack.push(operand.reverse().toString());
        }
        
        return stack.pop();
    }
    
    // Helper: Check if character is operator
    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    
    // Helper: Get operator precedence
    private int getPrecedence(char op) {
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
        return 0;
    }
    
    // Test cases with detailed visualization
    public static void main(String[] args) {
        PrefixToInfix converter = new PrefixToInfix();
        
        // Test Case 1: Simple expression
        String prefix1 = "+AB";
        String infix1 = converter.prefixToInfix(prefix1);
        System.out.println("Test 1:");
        System.out.println("Prefix: " + prefix1);
        System.out.println("Infix:  " + infix1);
        visualizeConversion(prefix1, infix1);
        
        // Test Case 2: With precedence
        String prefix2 = "*+ABC";
        String infix2 = converter.prefixToInfix(prefix2);
        System.out.println("\nTest 2:");
        System.out.println("Prefix: " + prefix2);
        System.out.println("Infix:  " + infix2);
        visualizeConversion(prefix2, infix2);
        
        // Test Case 3: Complex expression
        String prefix3 = "*-A/BC-/AKL";
        String infix3 = converter.prefixToInfix(prefix3);
        System.out.println("\nTest 3:");
        System.out.println("Prefix: " + prefix3);
        System.out.println("Infix:  " + infix3);
        
        // Test Case 4: More complex
        String prefix4 = "+*ABC";
        String infix4 = converter.prefixToInfix(prefix4);
        System.out.println("\nTest 4:");
        System.out.println("Prefix: " + prefix4);
        System.out.println("Infix:  " + infix4);
        
        // Test Case 5: With exponentiation
        String prefix5 = "^+AB*CD";
        String infix5 = converter.prefixToInfix(prefix5);
        System.out.println("\nTest 5:");
        System.out.println("Prefix: " + prefix5);
        System.out.println("Infix:  " + infix5);
        
        // Compare fully parenthesized vs minimal
        System.out.println("\n=== Comparison: Full vs Minimal Parentheses ===");
        String test = "*+ABC";
        System.out.println("Prefix: " + test);
        System.out.println("Full:   " + converter.prefixToInfix(test));
        System.out.println("Minimal:" + converter.prefixToInfixMinimal(test));
        
        // Step-by-step trace
        System.out.println("\n=== Step-by-Step Trace for '*+ABC' ===");
        traceConversion("*+ABC");
    }
    
    private static void visualizeConversion(String prefix, String infix) {
        System.out.println("\nVisualization:");
        System.out.println("Prefix: " + prefix + "  (operator before operands)");
        System.out.println("Infix:  " + infix + "  (operator between operands)");
        
        if (prefix.equals("*+ABC")) {
            System.out.println("\nStructure:");
            System.out.println("     *");
            System.out.println("    / \\");
            System.out.println("   +   C");
            System.out.println("  / \\");
            System.out.println(" A   B");
            System.out.println("\nReads as: (A+B)*C");
        }
    }
    
    private static void traceConversion(String prefix) {
        Stack<String> stack = new Stack<>();
        
        System.out.println("Reading from RIGHT to LEFT:");
        System.out.println("┌────────┬─────────────┬──────────────────────┐");
        System.out.println("│ Char   │ Action      │ Stack                │");
        System.out.println("├────────┼─────────────┼──────────────────────┤");
        
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);
            String action = "";
            
            if (isOperatorChar(c)) {
                String op1 = stack.pop();
                String op2 = stack.pop();
                String result = "(" + op1 + c + op2 + ")";
                stack.push(result);
                action = "Pop 2, combine";
            }
            else {
                stack.push(String.valueOf(c));
                action = "Push operand";
            }
            
            System.out.printf("│   %c    │ %-11s │ %-20s │%n", 
                            c, action, stackToString(stack));
        }
        
        System.out.println("└────────┴─────────────┴──────────────────────┘");
        System.out.println("Final Result: " + stack.peek());
    }
    
    private static boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    
    private static String stackToString(Stack<String> stack) {
        if (stack.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < stack.size(); i++) {
            sb.append(stack.get(i));
            if (i < stack.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.length() > 20 ? sb.substring(0, 17) + "..." : sb.toString();
    }
}

/*
DETAILED EXPLANATION:

WHAT IS PREFIX NOTATION?
Operator comes BEFORE operands: + A B
- Also called Polish Notation
- No parentheses needed
- Evaluation: right to left

EXAMPLES:
Prefix:  + A B          Infix: (A + B)
Prefix:  * + A B C      Infix: ((A + B) * C)
Prefix:  + * A B C      Infix: ((A * B) + C)
Prefix:  - / A B C      Infix: ((A / B) - C)

KEY INSIGHT: SCAN RIGHT TO LEFT

Unlike postfix (left to right), prefix is scanned RIGHT to LEFT!

Why? Because operator comes first:
  Prefix: + A B
  Read right to left: B, A, +
  Combine: (A + B)

ALGORITHM:

1. Initialize empty stack
2. Scan prefix from RIGHT to LEFT
3. For each character:
   - If OPERAND → push to stack
   - If OPERATOR → 
     * Pop 2 operands (op1, op2)
     * Create: (op1 operator op2)
     * Push result back
4. Final stack top is the answer

STEP-BY-STEP EXAMPLE: "*+ABC"

Reading RIGHT to LEFT: C, B, A, +, *

Step 1: Read 'C' (operand)
  Stack: [C]

Step 2: Read 'B' (operand)
  Stack: [C, B]

Step 3: Read 'A' (operand)
  Stack: [C, B, A]

Step 4: Read '+' (operator)
  Pop B, A
  Create: (A+B)
  Stack: [C, (A+B)]

Step 5: Read '*' (operator)
  Pop (A+B), C
  Create: ((A+B)*C)
  Stack: [((A+B)*C)]

Result: ((A+B)*C)

VISUAL REPRESENTATION:

Prefix: * + A B C

Expression Tree:
       *
      / \
     +   C
    / \
   A   B

Reading right to left builds tree bottom-up!

ANOTHER EXAMPLE: "+*ABC"

Reading RIGHT to LEFT: C, B, A, *, +

Step 1: C → Stack: [C]
Step 2: B → Stack: [C, B]
Step 3: A → Stack: [C, B, A]
Step 4: * → Pop A,B → (A*B) → Stack: [C, (A*B)]
Step 5: + → Pop (A*B),C → ((A*B)+C) → Stack: [((A*B)+C)]

Result: ((A*B)+C)

COMPARISON WITH POSTFIX:

Postfix (AB+C*):
- Read LEFT to RIGHT
- Operator after operands
- Build: A, B, +, C, *

Prefix (*+ABC):
- Read RIGHT to LEFT
- Operator before operands
- Build: C, B, A, +, *

Both use stacks but in opposite directions!

WHY FULLY PARENTHESIZED?

Fully parenthesized: ((A+B)*C)
Minimal parentheses: (A+B)*C

Fully parenthesized is:
1. Unambiguous
2. Easier to implement
3. Safe for all cases
4. No precedence concerns

OPERATOR ORDER MATTERS:

Prefix: - A B means A - B, NOT B - A
Prefix: / A B means A / B, NOT B / A

When popping: first pop = left operand, second pop = right operand

Wait, actually in our algorithm:
  op1 = stack.pop()  (first popped)
  op2 = stack.pop()  (second popped)
  result = (op1 operator op2)

For RIGHT to LEFT scan:
  Prefix: - A B
  Read: B, A, -
  Stack: [B], then [B, A]
  Pop: op1=A, op2=B
  Result: (A-B) ✓ Correct!

COMPLEX EXAMPLE: "*-A/BC-/AKL"

Reading RIGHT to LEFT:
L, K, A, /, -, C, B, /, A, -, *

Step by step:
L → [L]
K → [L, K]
A → [L, K, A]
/ → pop A,K → (A/K) → [L, (A/K)]
- → pop (A/K),L → ((A/K)-L) → [((A/K)-L)]
C → [((A/K)-L), C]
B → [((A/K)-L), C, B]
/ → pop B,C → (B/C) → [((A/K)-L), (B/C)]
A → [((A/K)-L), (B/C), A]
- → pop A,(B/C) → (A-(B/C)) → [((A/K)-L), (A-(B/C))]
* → pop (A-(B/C)),((A/K)-L) → ((A-(B/C))*((A/K)-L))

COMPLEXITY ANALYSIS:

Time: O(n)
- Single pass (right to left)
- Each character processed once
- Stack operations O(1)

Space: O(n)
- Stack stores intermediate results
- Each result can be O(n) length
- Total space for building strings

EDGE CASES:

1. Single operand: "A" → "A"
2. Simple operation: "+AB" → "(A+B)"
3. All same operator: "+++ABCD" → "(((A+B)+C)+D)"
4. Nested operations: complex expressions
5. All operands at end: stack grows then shrinks

COMMON MISTAKES:

1. Scanning left to right (wrong!)
2. Wrong operand order: (B+A) instead of (A+B)
3. Not handling precedence correctly
4. Forgetting parentheses
5. Stack underflow (invalid prefix)

VALIDATION:

Valid prefix must:
1. Have balanced operators and operands
2. Never cause stack underflow
3. Result in single expression

Invalid examples:
- "++AB" (too few operands)
- "AB+" (not prefix, this is postfix!)
- "+A" (missing operand)

OPERATOR PROPERTIES:

Commutative: +, *
  + A B = + B A
  (A+B) = (B+A)

Non-commutative: -, /, ^
  - A B ≠ - B A
  (A-B) ≠ (B-A)

Order matters for non-commutative operators!

PRACTICAL APPLICATIONS:

1. Compiler design (expression parsing)
2. Calculator implementations
3. Expression evaluation
4. Language processing (LISP uses prefix)
5. Mathematical software

NOTATION COMPARISON:

Infix:   (A + B) * C
Prefix:  * + A B C
Postfix: A B + C *

All represent same expression!

INTERVIEW TIPS:

1. Emphasize RIGHT to LEFT scan
2. Show stack state at each step
3. Explain operand order
4. Handle parenthesization
5. Discuss time/space complexity
6. Test with examples
7. Consider edge cases

RELATED PROBLEMS:

1. Prefix to Postfix
2. Infix to Prefix
3. Postfix to Infix
4. Evaluate Prefix Expression
5. Expression Tree Construction

OPTIMIZATION:

1. Pre-allocate StringBuilder
2. Avoid string concatenation
3. Use character arrays
4. Validate input first
5. Handle multi-char operands

ASSOCIATIVITY:

Right-associative operators (^):
  Prefix: ^ ^ A B C
  Means: A ^ (B ^ C)
  Not: (A ^ B) ^ C

Left-associative operators (+,-,*,/):
  Prefix: + + A B C
  Means: (A + B) + C

LISP CONNECTION:

LISP uses prefix notation:
  (+ 1 2) = 1 + 2
  (* (+ 1 2) 3) = (1 + 2) * 3
  
Prefix is natural for LISP!

This algorithm beautifully demonstrates:
- Stack-based parsing
- Expression tree construction
- Notation conversion
- Reverse scanning techniques
*/
