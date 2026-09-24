import java.util.Stack;

class PrefixToPostfix {
    
    // Approach 1: Using Stack (Optimal)
    // Time: O(n), Space: O(n)
    public String prefixToPostfix(String prefix) {
        Stack<String> stack = new Stack<>();
        
        // Read prefix expression from RIGHT to LEFT
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);
            
            // If character is operator
            if (isOperator(c)) {
                // Pop two operands (note: order matters!)
                String operand1 = stack.pop();
                String operand2 = stack.pop();
                
                // Create postfix: operand1 operand2 operator
                String postfix = operand1 + operand2 + c;
                
                // Push back to stack
                stack.push(postfix);
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
    public String prefixToPostfixWithSpaces(String prefix) {
        Stack<String> stack = new Stack<>();
        
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);
            
            if (isOperator(c)) {
                String operand1 = stack.pop();
                String operand2 = stack.pop();
                
                // Create with spaces for readability
                String postfix = operand1 + " " + operand2 + " " + c;
                stack.push(postfix);
            }
            else {
                stack.push(String.valueOf(c));
            }
        }
        
        return stack.pop();
    }
    
    // Approach 3: Recursive Approach
    public String prefixToPostfixRecursive(String prefix) {
        int[] index = {prefix.length() - 1};
        return convertRecursive(prefix, index);
    }
    
    private String convertRecursive(String prefix, int[] index) {
        if (index[0] < 0) return "";
        
        char c = prefix.charAt(index[0]);
        index[0]--;
        
        if (isOperator(c)) {
            String operand1 = convertRecursive(prefix, index);
            String operand2 = convertRecursive(prefix, index);
            return operand1 + operand2 + c;
        }
        else {
            return String.valueOf(c);
        }
    }
    
    // Approach 4: With Multi-character Operands Support
    public String prefixToPostfixMultiChar(String prefix) {
        Stack<String> stack = new Stack<>();
        String[] tokens = prefix.split(" ");
        
        // Process from right to left
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i];
            
            if (isOperatorString(token)) {
                String op1 = stack.pop();
                String op2 = stack.pop();
                String postfix = op1 + " " + op2 + " " + token;
                stack.push(postfix);
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
        PrefixToPostfix converter = new PrefixToPostfix();
        
        // Test Case 1: Simple expression
        String prefix1 = "+AB";
        String postfix1 = converter.prefixToPostfix(prefix1);
        System.out.println("Test 1:");
        System.out.println("Prefix:  " + prefix1);
        System.out.println("Postfix: " + postfix1);
        visualizeConversion(prefix1, postfix1);
        
        // Test Case 2: With precedence
        String prefix2 = "*+ABC";
        String postfix2 = converter.prefixToPostfix(prefix2);
        System.out.println("\nTest 2:");
        System.out.println("Prefix:  " + prefix2);
        System.out.println("Postfix: " + postfix2);
        visualizeConversion(prefix2, postfix2);
        
        // Test Case 3: More complex
        String prefix3 = "*-A/BC-/AKL";
        String postfix3 = converter.prefixToPostfix(prefix3);
        System.out.println("\nTest 3:");
        System.out.println("Prefix:  " + prefix3);
        System.out.println("Postfix: " + postfix3);
        
        // Test Case 4: Different operators
        String prefix4 = "-+*ABC/DE";
        String postfix4 = converter.prefixToPostfix(prefix4);
        System.out.println("\nTest 4:");
        System.out.println("Prefix:  " + prefix4);
        System.out.println("Postfix: " + postfix4);
        
        // Test Case 5: With exponentiation
        String prefix5 = "^-A*BCD";
        String postfix5 = converter.prefixToPostfix(prefix5);
        System.out.println("\nTest 5:");
        System.out.println("Prefix:  " + prefix5);
        System.out.println("Postfix: " + postfix5);
        
        // Compare with spaces
        System.out.println("\n=== With Space Separation ===");
        System.out.println("Prefix:  " + prefix2);
        System.out.println("Postfix: " + converter.prefixToPostfixWithSpaces(prefix2));
        
        // All three notations
        System.out.println("\n=== All Three Notations ===");
        compareAllNotations("*+ABC");
        
        // Step-by-step trace
        System.out.println("\n=== Step-by-Step Trace for '*+ABC' ===");
        traceConversion("*+ABC");
    }
    
    private static void visualizeConversion(String prefix, String postfix) {
        System.out.println("\nVisualization:");
        System.out.println("Prefix:  " + prefix + "  (operator before operands)");
        System.out.println("Postfix: " + postfix + " (operator after operands)");
        
        if (prefix.equals("*+ABC")) {
            System.out.println("\nExpression Tree:");
            System.out.println("       *");
            System.out.println("      / \\");
            System.out.println("     +   C");
            System.out.println("    / \\");
            System.out.println("   A   B");
            System.out.println("\nPrefix reads top-down: * + A B C");
            System.out.println("Postfix reads bottom-up: A B + C *");
        }
    }
    
    private static void compareAllNotations(String prefix) {
        PrefixToPostfix converter = new PrefixToPostfix();
        String postfix = converter.prefixToPostfix(prefix);
        
        System.out.println("Expression: (A+B)*C");
        System.out.println("┌──────────┬────────────┐");
        System.out.println("│ Notation │ Expression │");
        System.out.println("├──────────┼────────────┤");
        System.out.println("│ Infix    │ (A+B)*C    │");
        System.out.println("│ Prefix   │ " + prefix + "      │");
        System.out.println("│ Postfix  │ " + postfix + "     │");
        System.out.println("└──────────┴────────────┘");
    }
    
    private static void traceConversion(String prefix) {
        Stack<String> stack = new Stack<>();
        
        System.out.println("Reading from RIGHT to LEFT:");
        System.out.println("┌────────┬─────────────┬────────────────────────┐");
        System.out.println("│ Char   │ Action      │ Stack                  │");
        System.out.println("├────────┼─────────────┼────────────────────────┤");
        
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);
            String action = "";
            
            if (isOperatorChar(c)) {
                String op1 = stack.pop();
                String op2 = stack.pop();
                String result = op1 + op2 + c;
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
        System.out.println("Prefix:  * + A B C  (op before operands)");
        System.out.println("         ↓ ↓ ↓ ↓ ↓");
        System.out.println("Postfix: A B + C *  (op after operands)");
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

PROBLEM: Convert Prefix → Postfix

PREFIX:  Operator before operands: + A B
POSTFIX: Operator after operands:  A B +

Both avoid parentheses and are unambiguous!

KEY INSIGHT: SCAN RIGHT TO LEFT + BUILD POSTFIX

Similar to Prefix→Infix, but instead of creating (A+B),
we create AB+

ALGORITHM:

1. Initialize empty stack
2. Scan prefix from RIGHT to LEFT
3. For each character:
   - If OPERAND → push to stack
   - If OPERATOR →
     * Pop 2 operands: op1, op2
     * Create postfix: op1 + op2 + operator
     * Push result back
4. Final stack top is the postfix

STEP-BY-STEP EXAMPLE: "*+ABC"

Reading RIGHT to LEFT: C, B, A, +, *

Step 1: Read 'C' (operand)
  Stack: [C]

Step 2: Read 'B' (operand)
  Stack: [C, B]

Step 3: Read 'A' (operand)
  Stack: [C, B, A]

Step 4: Read '+' (operator)
  Pop: A, B (op1=A, op2=B)
  Create postfix: AB+
  Stack: [C, AB+]

Step 5: Read '*' (operator)
  Pop: AB+, C (op1=AB+, op2=C)
  Create postfix: AB+C*
  Stack: [AB+C*]

Result: AB+C*

VISUALIZATION:

Expression Tree:
       *
      / \
     +   C
    / \
   A   B

Prefix (Pre-order):  * + A B C
Postfix (Post-order): A B + C *

Tree traversals give us the notations!

ANOTHER EXAMPLE: "+*ABC"

Reading RIGHT to LEFT: C, B, A, *, +

C → [C]
B → [C, B]
A → [C, B, A]
* → Pop A,B → AB* → [C, AB*]
+ → Pop AB*,C → AB*C+ → [AB*C+]

Result: AB*C+

COMPARISON WITH OTHER CONVERSIONS:

Prefix → Infix:
  Scan right to left
  Build: (op1 operator op2)
  Result: ((A+B)*C)

Prefix → Postfix:
  Scan right to left
  Build: op1 op2 operator
  Result: AB+C*

Same scan direction, different construction!

WHY RIGHT TO LEFT?

Prefix has operator FIRST:
  * + A B C
  ↑
  This operator needs A+B first

Reading backwards ensures we process operands
before we need them!

OPERAND ORDER:

First pop = left operand
Second pop = right operand

For prefix: - A B
Read: B, A, -
Pop: op1=A, op2=B
Postfix: AB-
Meaning: A - B ✓

COMPLEX EXAMPLE: "*-A/BC-/AKL"

Reading RIGHT to LEFT:
L, K, A, /, -, C, B, /, A, -, *

L → [L]
K → [L, K]
A → [L, K, A]
/ → AK/ → [L, AK/]
- → AK/L- → [AK/L-]
C → [AK/L-, C]
B → [AK/L-, C, B]
/ → BC/ → [AK/L-, BC/]
A → [AK/L-, BC/, A]
- → ABC/- → [AK/L-, ABC/-]
* → ABC/-AK/L-* → [ABC/-AK/L-*]

Result: ABC/-AK/L-*

EXPRESSION TREE RELATIONSHIP:

         *
        / \
       -   -
      / \ / \
     A  / / L
       / / 
      B K
      |
      C A

Pre-order (Prefix):  *-A/BC-/AKL
Post-order (Postfix): ABC/-AK/L-*

Tree traversals = notation conversions!

COMPLEXITY ANALYSIS:

Time: O(n)
- Single pass through prefix
- Each character processed once
- Stack operations O(1)

Space: O(n)
- Stack stores intermediate results
- Each result can grow to O(n)
- Total space for string building

EDGE CASES:

1. Single operand: "A" → "A"
2. Simple operation: "+AB" → "AB+"
3. All same operator: "+++ABCD" → "AB+C+D+"
4. Long chain: "++++++ABCDEFG"
5. Empty string: "" → ""

VALIDATION:

Valid prefix properties:
1. #operators = #operands - 1
2. At any point reading right to left:
   #operands ≥ #operators
3. Final: exactly 1 result

COMMON MISTAKES:

1. Scanning left to right (WRONG!)
2. Wrong operand order in postfix
3. Forgetting to concatenate properly
4. Stack underflow (invalid prefix)
5. Not handling all operators

NOTATION COMPARISON:

Expression: (A+B)*C

Infix:   (A + B) * C    (natural, needs parens)
Prefix:  * + A B C      (operator first)
Postfix: A B + C *      (operator last)

All three are equivalent!

PRACTICAL APPLICATIONS:

1. Compiler optimizations
2. Expression evaluation
3. Calculator implementations
4. Programming language parsers
5. Computer algebra systems

ADVANTAGES OF EACH NOTATION:

Infix:
+ Natural for humans
- Needs parentheses
- Ambiguous without rules

Prefix:
+ No parentheses needed
+ Easy prefix evaluation
- Unnatural for humans

Postfix:
+ No parentheses needed
+ Easy stack evaluation
+ Used in RPN calculators

EVALUATION COMPARISON:

Prefix evaluation:
  Scan left to right
  Use recursion or right-to-left scan

Postfix evaluation:
  Scan left to right
  Use stack
  Simpler than prefix!

INTERVIEW TIPS:

1. Emphasize RIGHT to LEFT scan
2. Show stack state clearly
3. Explain postfix construction
4. Compare with prefix→infix
5. Draw expression tree
6. Test with examples
7. Discuss complexity

RELATED PROBLEMS:

1. Postfix to Prefix (reverse process)
2. Infix to Postfix
3. Evaluate Prefix Expression
4. Evaluate Postfix Expression
5. Expression Tree Construction

ASSOCIATIVITY IN CONVERSION:

Prefix: + + A B C
Stack: C, B, A, +, +
Build: AB+, then AB+C+
Postfix: AB+C+
Means: ((A+B)+C) - left associative

Prefix: ^ ^ A B C
Postfix: ABC^^
Means: A^(B^C) - right associative

The conversion preserves associativity!

OPTIMIZATION:

1. Use StringBuilder for concatenation
2. Pre-allocate stack size
3. Validate input first
4. Use character arrays
5. Avoid repeated string creation

MEMORY CONSIDERATION:

Each intermediate result grows:
AB+C* has length 5
Complex expressions can be long
Space complexity dominated by result strings

DEBUGGING TIPS:

1. Print stack at each step
2. Verify operator count
3. Check operand ordering
4. Test simple cases first
5. Draw expression tree

This algorithm beautifully shows:
- Stack-based parsing
- Notation conversion
- Tree traversal equivalence
- Reverse scanning technique
*/
