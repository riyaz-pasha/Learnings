import java.util.Stack;

class PostfixToPrefix {
    
    // Approach 1: Using Stack (Optimal)
    // Time: O(n), Space: O(n)
    public String postfixToPrefix(String postfix) {
        Stack<String> stack = new Stack<>();
        
        // Read postfix expression from LEFT to RIGHT
        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);
            
            // If character is operator
            if (isOperator(c)) {
                // Pop two operands (note: order matters!)
                String operand2 = stack.pop();  // Right operand
                String operand1 = stack.pop();  // Left operand
                
                // Create prefix: operator operand1 operand2
                String prefix = c + operand1 + operand2;
                
                // Push back to stack
                stack.push(prefix);
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
    
    // Approach 2: With Space Separation for Clarity
    public String postfixToPrefixWithSpaces(String postfix) {
        Stack<String> stack = new Stack<>();
        
        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);
            
            if (isOperator(c)) {
                String operand2 = stack.pop();
                String operand1 = stack.pop();
                
                // Create with spaces for readability
                String prefix = c + " " + operand1 + " " + operand2;
                stack.push(prefix);
            }
            else {
                stack.push(String.valueOf(c));
            }
        }
        
        return stack.pop();
    }
    
    // Approach 3: With Multi-character Operands Support
    public String postfixToPrefixMultiChar(String postfix) {
        Stack<String> stack = new Stack<>();
        String[] tokens = postfix.split(" ");
        
        // Process from left to right
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            
            if (isOperatorString(token)) {
                String op2 = stack.pop();
                String op1 = stack.pop();
                String prefix = token + " " + op1 + " " + op2;
                stack.push(prefix);
            }
            else {
                stack.push(token);
            }
        }
        
        return stack.pop();
    }
    
    // Helper: Check if character is operator
    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    
    private boolean isOperatorString(String s) {
        return s.length() == 1 && isOperator(s.charAt(0));
    }
    
    // Test cases with detailed visualization
    public static void main(String[] args) {
        PostfixToPrefix converter = new PostfixToPrefix();
        
        // Test Case 1: Simple expression
        String postfix1 = "AB+";
        String prefix1 = converter.postfixToPrefix(postfix1);
        System.out.println("Test 1:");
        System.out.println("Postfix: " + postfix1);
        System.out.println("Prefix:  " + prefix1);
        visualizeConversion(postfix1, prefix1);
        
        // Test Case 2: With precedence
        String postfix2 = "AB+C*";
        String prefix2 = converter.postfixToPrefix(postfix2);
        System.out.println("\nTest 2:");
        System.out.println("Postfix: " + postfix2);
        System.out.println("Prefix:  " + prefix2);
        visualizeConversion(postfix2, prefix2);
        
        // Test Case 3: More complex
        String postfix3 = "ABC/-AK/L-*";
        String prefix3 = converter.postfixToPrefix(postfix3);
        System.out.println("\nTest 3:");
        System.out.println("Postfix: " + postfix3);
        System.out.println("Prefix:  " + prefix3);
        
        // Test Case 4: Different operators
        String postfix4 = "AB*C+DE/-";
        String prefix4 = converter.postfixToPrefix(postfix4);
        System.out.println("\nTest 4:");
        System.out.println("Postfix: " + postfix4);
        System.out.println("Prefix:  " + prefix4);
        
        // Test Case 5: With exponentiation
        String postfix5 = "ABC*-D^";
        String prefix5 = converter.postfixToPrefix(postfix5);
        System.out.println("\nTest 5:");
        System.out.println("Postfix: " + postfix5);
        System.out.println("Prefix:  " + prefix5);
        
        // Compare with spaces
        System.out.println("\n=== With Space Separation ===");
        System.out.println("Postfix: " + postfix2);
        System.out.println("Prefix:  " + converter.postfixToPrefixWithSpaces(postfix2));
        
        // All three notations
        System.out.println("\n=== All Three Notations ===");
        compareAllNotations("AB+C*");
        
        // Step-by-step trace
        System.out.println("\n=== Step-by-Step Trace for 'AB+C*' ===");
        traceConversion("AB+C*");
    }
    
    private static void visualizeConversion(String postfix, String prefix) {
        System.out.println("\nVisualization:");
        System.out.println("Postfix: " + postfix + " (operator after operands)");
        System.out.println("Prefix:  " + prefix + "  (operator before operands)");
        
        if (postfix.equals("AB+C*")) {
            System.out.println("\nExpression Tree:");
            System.out.println("       *");
            System.out.println("      / \\");
            System.out.println("     +   C");
            System.out.println("    / \\");
            System.out.println("   A   B");
            System.out.println("\nPostfix reads bottom-up: A B + C *");
            System.out.println("Prefix reads top-down:   * + A B C");
        }
    }
    
    private static void compareAllNotations(String postfix) {
        PostfixToPrefix converter = new PostfixToPrefix();
        String prefix = converter.postfixToPrefix(postfix);
        
        System.out.println("Expression: (A+B)*C");
        System.out.println("┌──────────┬────────────┐");
        System.out.println("│ Notation │ Expression │");
        System.out.println("├──────────┼────────────┤");
        System.out.println("│ Infix    │ (A+B)*C    │");
        System.out.println("│ Prefix   │ " + prefix + "      │");
        System.out.println("│ Postfix  │ " + postfix + "     │");
        System.out.println("└──────────┴────────────┘");
    }
    
    private static void traceConversion(String postfix) {
        Stack<String> stack = new Stack<>();
        
        System.out.println("Reading from LEFT to RIGHT:");
        System.out.println("┌────────┬─────────────┬────────────────────────┐");
        System.out.println("│ Char   │ Action      │ Stack                  │");
        System.out.println("├────────┼─────────────┼────────────────────────┤");
        
        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);
            String action = "";
            
            if (isOperatorChar(c)) {
                String op2 = stack.pop();
                String op1 = stack.pop();
                String result = c + op1 + op2;
                stack.push(result);
                action = "Pop 2, build";
            }
            else {
                stack.push(String.valueOf(c));
                action = "Push operand";
            }
            
            System.out.printf("│   %c    │ %-11s │ %-22s │%n", 
                            c, action, stackToString(stack));
        }
        
        System.out.println("└────────┴─────────────┴────────────────────────┘");
        System.out.println("Final Result: " + stack.peek());
        
        // Show the transformation
        System.out.println("\nTransformation:");
        System.out.println("Postfix: A B + C *  (op after operands)");
        System.out.println("         ↓ ↓ ↓ ↓ ↓");
        System.out.println("Prefix:  * + A B C  (op before operands)");
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
        return sb.length() > 22 ? sb.substring(0, 19) + "..." : sb.toString();
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Convert Postfix → Prefix

POSTFIX: Operator after operands:  A B +
PREFIX:  Operator before operands: + A B

KEY INSIGHT: SCAN LEFT TO RIGHT + BUILD PREFIX

ALGORITHM:

1. Initialize empty stack
2. Scan postfix from LEFT to RIGHT
3. For each character:
   - If OPERAND → push to stack
   - If OPERATOR →
     * Pop 2 operands: op2 (right), op1 (left)
     * Create prefix: operator + op1 + op2
     * Push result back
4. Final stack top is the prefix

STEP-BY-STEP EXAMPLE: "AB+C*"

Reading LEFT to RIGHT: A, B, +, C, *

Step 1: Read 'A' (operand)
  Stack: [A]

Step 2: Read 'B' (operand)
  Stack: [A, B]

Step 3: Read '+' (operator)
  Pop: B (op2), A (op1)
  Create prefix: +AB
  Stack: [+AB]

Step 4: Read 'C' (operand)
  Stack: [+AB, C]

Step 5: Read '*' (operator)
  Pop: C (op2), +AB (op1)
  Create prefix: *+ABC
  Stack: [*+ABC]

Result: *+ABC

CRITICAL: OPERAND ORDER

When popping for operator:
  First pop = RIGHT operand (op2)
  Second pop = LEFT operand (op1)

For postfix: AB-
  Pop: B (op2), A (op1)
  Prefix: -AB
  Meaning: A - B ✓

If we reversed:
  Pop: B (op1), A (op2)
  Prefix: -BA
  Meaning: B - A ✗

ANOTHER EXAMPLE: "ABC*+DE/-"

Reading LEFT to RIGHT:

A → [A]
B → [A, B]
C → [A, B, C]
* → Pop C,B → *BC → [A, *BC]
+ → Pop *BC,A → +A*BC → [+A*BC]
D → [+A*BC, D]
E → [+A*BC, D, E]
/ → Pop E,D → /DE → [+A*BC, /DE]
- → Pop /DE,+A*BC → -+A*BC/DE → [-+A*BC/DE]

Result: -+A*BC/DE

COMPARISON TABLE:

┌─────────────────┬──────────────┬─────────────┐
│ Conversion      │ Scan         │ Build       │
├─────────────────┼──────────────┼─────────────┤
│ Prefix→Postfix  │ Right to Left│ op1 op2 op  │
│ Postfix→Prefix  │ Left to Right│ op op1 op2  │
│ Prefix→Infix    │ Right to Left│ (op1 op op2)│
│ Postfix→Infix   │ Left to Right│ (op1 op op2)│
└─────────────────┴──────────────┴─────────────┘

EXPRESSION TREE RELATIONSHIP:

         *
        / \
       +   /
      / \ / \
     A  * D E
       / \
      B   C

Post-order (Postfix): ABC*+DE/-
Pre-order (Prefix):   -+A*BC/DE

Tree traversals give notations!

WHY LEFT TO RIGHT?

Postfix has operator LAST:
  A B + C *
        ↑
  When we reach *, we need A+B ready

Reading forwards ensures operands
are processed before operators need them!

COMPLEXITY ANALYSIS:

Time: O(n)
- Single pass through postfix
- Each character processed once
- Stack operations O(1)

Space: O(n)
- Stack stores intermediate results
- Each result can grow to O(n)
- Total space for string building

VALIDATION:

Valid postfix properties:
1. #operators = #operands - 1
2. At any point reading left to right:
   #operands > #operators
3. Final: exactly 1 result

COMMON MISTAKES:

1. Scanning right to left (WRONG!)
2. Wrong operand order: using op1, op2 instead of op2, op1
3. Building postfix instead of prefix
4. Stack underflow (invalid postfix)
5. Not handling associativity

EDGE CASES:

1. Single operand: "A" → "A"
2. Simple operation: "AB+" → "+AB"
3. All same operator: "AB+C+D+" → "+++ABCD"
4. Long chain: "ABC+D+E+F+G+"
5. Empty string: "" → ""

ASSOCIATIVITY PRESERVATION:

Postfix: AB+C+
Stack: A, B, +, C, +
Build: +AB, then ++ABC
Prefix: ++ABC
Means: ((A+B)+C) - left associative ✓

Postfix: ABC^^
Prefix: ^^ABC
Means: A^(B^C) - right associative ✓

The conversion preserves semantics!

PRACTICAL APPLICATIONS:

1. Compiler intermediate representations
2. Expression parsing
3. Calculator implementations
4. Query optimization
5. Mathematical software

DEBUGGING TIPS:

1. Print stack after each operation
2. Verify operator count
3. Check operand order carefully
4. Test with non-commutative operators (-, /)
5. Draw expression tree

INTERVIEW TIPS:

1. Emphasize LEFT to RIGHT scan
2. Highlight operand order (op2, op1)
3. Show stack state clearly
4. Compare with prefix→postfix
5. Draw expression tree
6. Test with subtraction/division
7. Discuss complexity

RELATED PROBLEMS:

1. Prefix to Postfix (reverse)
2. Infix to Prefix
3. Evaluate Postfix Expression
4. Evaluate Prefix Expression
5. Expression Tree from Postfix

OPTIMIZATION:

1. Use StringBuilder for concatenation
2. Pre-allocate stack capacity
3. Validate input first
4. Use character arrays for speed
5. Avoid repeated string creation

This algorithm demonstrates:
- Stack-based parsing
- Notation conversion
- Tree traversal equivalence
- Forward scanning technique
*/
