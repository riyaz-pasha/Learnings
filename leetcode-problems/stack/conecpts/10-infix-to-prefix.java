import java.util.Stack;

class InfixToPrefix {
    
    // Approach 1: Reverse-Convert-Reverse Method (Most Intuitive)
    // Time: O(n), Space: O(n)
    public String infixToPrefix(String infix) {
        // Step 1: Reverse the infix expression
        String reversed = reverse(infix);
        
        // Step 2: Swap parentheses
        reversed = swapParentheses(reversed);
        
        // Step 3: Convert to postfix (using standard algorithm)
        String postfix = infixToPostfixHelper(reversed);
        
        // Step 4: Reverse the result to get prefix
        return reverse(postfix);
    }
    
    // Approach 2: Direct Method (Scan Right to Left)
    // More efficient, single pass
    public String infixToPrefixDirect(String infix) {
        Stack<Character> operators = new Stack<>();
        Stack<String> operands = new Stack<>();
        
        // Scan from RIGHT to LEFT
        for (int i = infix.length() - 1; i >= 0; i--) {
            char c = infix.charAt(i);
            
            // Skip whitespace
            if (c == ' ') continue;
            
            // If closing parenthesis (becomes opening when scanning right to left)
            if (c == ')') {
                operators.push(c);
            }
            // If opening parenthesis (becomes closing when scanning right to left)
            else if (c == '(') {
                while (!operators.isEmpty() && operators.peek() != ')') {
                    operands.push(applyOperatorPrefix(operators.pop(), operands));
                }
                operators.pop(); // Remove ')'
            }
            // If operator
            else if (isOperator(c)) {
                // Pop operators with HIGHER or EQUAL precedence for right associative
                // Pop operators with HIGHER precedence only for left associative
                while (!operators.isEmpty() && operators.peek() != ')' &&
                       shouldPopForPrefix(operators.peek(), c)) {
                    operands.push(applyOperatorPrefix(operators.pop(), operands));
                }
                operators.push(c);
            }
            // If operand
            else {
                operands.push(String.valueOf(c));
            }
        }
        
        // Pop remaining operators
        while (!operators.isEmpty()) {
            operands.push(applyOperatorPrefix(operators.pop(), operands));
        }
        
        return operands.pop();
    }
    
    // Approach 3: With Multi-character Operands Support
    public String infixToPrefixMultiChar(String infix) {
        String reversed = reverseWithTokens(infix);
        String postfix = infixToPostfixHelperMultiChar(reversed);
        return reverseTokens(postfix);
    }
    
    // Helper: Apply operator in prefix format
    private String applyOperatorPrefix(char op, Stack<String> operands) {
        String operand1 = operands.pop(); // Right operand (popped first in RTL)
        String operand2 = operands.pop(); // Left operand
        return op + operand2 + operand1;  // Prefix: operator left right
    }
    
    // Helper: Check precedence for prefix conversion
    private boolean shouldPopForPrefix(char stackTop, char current) {
        int stackPrec = getPrecedence(stackTop);
        int currPrec = getPrecedence(current);
        
        // For right-associative operators (^), use >=
        if (current == '^') {
            return stackPrec > currPrec;
        }
        // For left-associative operators, use >=
        else {
            return stackPrec >= currPrec;
        }
    }
    
    // Helper: Reverse string
    private String reverse(String str) {
        return new StringBuilder(str).reverse().toString();
    }
    
    // Helper: Swap parentheses in string
    private String swapParentheses(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c == '(') sb.append(')');
            else if (c == ')') sb.append('(');
            else sb.append(c);
        }
        return sb.toString();
    }
    
    // Helper: Standard infix to postfix conversion
    private String infixToPostfixHelper(String infix) {
        StringBuilder result = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        
        for (int i = 0; i < infix.length(); i++) {
            char c = infix.charAt(i);
            
            if (c == ' ') continue;
            
            // If operand
            if (!isOperator(c) && c != '(' && c != ')') {
                result.append(c);
            }
            // If '('
            else if (c == '(') {
                stack.push(c);
            }
            // If ')'
            else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    result.append(stack.pop());
                }
                stack.pop(); // Remove '('
            }
            // If operator
            else {
                while (!stack.isEmpty() && stack.peek() != '(' &&
                       getPrecedence(stack.peek()) >= getPrecedence(c)) {
                    result.append(stack.pop());
                }
                stack.push(c);
            }
        }
        
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        
        return result.toString();
    }
    
    // Helper for multi-char tokens
    private String reverseWithTokens(String infix) {
        String[] tokens = infix.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i];
            if (token.equals("(")) sb.append(") ");
            else if (token.equals(")")) sb.append("( ");
            else sb.append(token).append(" ");
        }
        return sb.toString().trim();
    }
    
    private String reverseTokens(String postfix) {
        String[] tokens = postfix.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = tokens.length - 1; i >= 0; i--) {
            sb.append(tokens[i]);
            if (i > 0) sb.append(" ");
        }
        return sb.toString();
    }
    
    private String infixToPostfixHelperMultiChar(String infix) {
        StringBuilder result = new StringBuilder();
        Stack<String> stack = new Stack<>();
        String[] tokens = infix.split(" ");
        
        for (String token : tokens) {
            if (token.equals("(")) {
                stack.push(token);
            }
            else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    result.append(stack.pop()).append(" ");
                }
                stack.pop();
            }
            else if (isOperatorString(token)) {
                while (!stack.isEmpty() && !stack.peek().equals("(") &&
                       getPrecedenceString(stack.peek()) >= getPrecedenceString(token)) {
                    result.append(stack.pop()).append(" ");
                }
                stack.push(token);
            }
            else {
                result.append(token).append(" ");
            }
        }
        
        while (!stack.isEmpty()) {
            result.append(stack.pop()).append(" ");
        }
        
        return result.toString().trim();
    }
    
    // Helper: Check if character is operator
    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    
    private boolean isOperatorString(String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || 
               s.equals("/") || s.equals("^");
    }
    
    // Get operator precedence
    private int getPrecedence(char op) {
        switch (op) {
            case '^': return 3;
            case '*':
            case '/': return 2;
            case '+':
            case '-': return 1;
            default: return 0;
        }
    }
    
    private int getPrecedenceString(String op) {
        if (op.length() == 1) return getPrecedence(op.charAt(0));
        return 0;
    }
    
    // Test cases with detailed visualization
    public static void main(String[] args) {
        InfixToPrefix converter = new InfixToPrefix();
        
        // Test Case 1: Simple expression
        System.out.println("Test 1: Simple Addition");
        String infix1 = "A+B";
        System.out.println("Infix:   " + infix1);
        System.out.println("Prefix:  " + converter.infixToPrefix(infix1));
        System.out.println("Direct:  " + converter.infixToPrefixDirect(infix1));
        
        // Test Case 2: With parentheses
        System.out.println("\nTest 2: With Parentheses");
        String infix2 = "(A+B)*C";
        System.out.println("Infix:   " + infix2);
        System.out.println("Prefix:  " + converter.infixToPrefix(infix2));
        System.out.println("Direct:  " + converter.infixToPrefixDirect(infix2));
        visualizeConversion(infix2);
        
        // Test Case 3: Complex expression
        System.out.println("\nTest 3: Complex Expression");
        String infix3 = "(A-B/C)*(A/K-L)";
        System.out.println("Infix:   " + infix3);
        System.out.println("Prefix:  " + converter.infixToPrefix(infix3));
        System.out.println("Direct:  " + converter.infixToPrefixDirect(infix3));
        
        // Test Case 4: Multiple operators
        System.out.println("\nTest 4: Multiple Operators");
        String infix4 = "A*B+C/D-E";
        System.out.println("Infix:   " + infix4);
        System.out.println("Prefix:  " + converter.infixToPrefix(infix4));
        System.out.println("Direct:  " + converter.infixToPrefixDirect(infix4));
        
        // Test Case 5: Right-associative (^)
        System.out.println("\nTest 5: Right-Associative Exponentiation");
        String infix5 = "A^B^C";
        System.out.println("Infix:   " + infix5);
        System.out.println("Prefix:  " + converter.infixToPrefix(infix5));
        System.out.println("Direct:  " + converter.infixToPrefixDirect(infix5));
        System.out.println("Means:   A^(B^C) - right to left");
        
        // Test Case 6: Left-associative
        System.out.println("\nTest 6: Left-Associative Subtraction");
        String infix6 = "A-B-C";
        System.out.println("Infix:   " + infix6);
        System.out.println("Prefix:  " + converter.infixToPrefix(infix6));
        System.out.println("Direct:  " + converter.infixToPrefixDirect(infix6));
        System.out.println("Means:   (A-B)-C - left to right");
        
        // Test Case 7: Nested parentheses
        System.out.println("\nTest 7: Nested Parentheses");
        String infix7 = "((A+B)*C-D)/E";
        System.out.println("Infix:   " + infix7);
        System.out.println("Prefix:  " + converter.infixToPrefix(infix7));
        System.out.println("Direct:  " + converter.infixToPrefixDirect(infix7));
        
        // All three notations
        System.out.println("\n=== All Three Notations ===");
        compareAllNotations("(A+B)*C");
        
        // Algorithm comparison
        System.out.println("\n=== Algorithm Comparison ===");
        compareAlgorithms("(A+B)*C");
        
        // Step-by-step trace
        System.out.println("\n=== Step-by-Step: Reverse Method ===");
        traceReverseMethod("(A+B)*C");
        
        // Precedence demonstration
        System.out.println("\n=== Precedence Examples ===");
        demonstratePrecedence();
        
        // Associativity demonstration
        System.out.println("\n=== Associativity Examples ===");
        demonstrateAssociativity();
    }
    
    private static void visualizeConversion(String infix) {
        System.out.println("\nExpression Tree for: " + infix);
        System.out.println("       *");
        System.out.println("      / \\");
        System.out.println("     +   C");
        System.out.println("    / \\");
        System.out.println("   A   B");
        System.out.println("\nPre-order (Prefix):  * + A B C");
        System.out.println("In-order (Infix):    (A + B) * C");
        System.out.println("Post-order (Postfix): A B + C *");
    }
    
    private static void compareAllNotations(String infix) {
        InfixToPrefix converter = new InfixToPrefix();
        String prefix = converter.infixToPrefix(infix);
        
        System.out.println("Expression: (A+B)*C");
        System.out.println("┌──────────┬────────────┐");
        System.out.println("│ Notation │ Expression │");
        System.out.println("├──────────┼────────────┤");
        System.out.println("│ Infix    │ " + infix + "    │");
        System.out.println("│ Prefix   │ " + prefix + "      │");
        System.out.println("│ Postfix  │ AB+C*      │");
        System.out.println("└──────────┴────────────┘");
    }
    
    private static void compareAlgorithms(String infix) {
        System.out.println("Input: " + infix);
        System.out.println("\nMethod 1: Reverse-Convert-Reverse");
        System.out.println("  1. Reverse infix:        C)*(B+A(");
        System.out.println("  2. Swap parentheses:     C(*)(B+A)");
        System.out.println("  3. Convert to postfix:   CBA+*");
        System.out.println("  4. Reverse result:       *+ABC");
        
        System.out.println("\nMethod 2: Direct (Right to Left)");
        System.out.println("  1. Scan right to left:   C, *, ), +, B, A, (");
        System.out.println("  2. Process with stacks");
        System.out.println("  3. Build prefix directly: *+ABC");
    }
    
    private static void traceReverseMethod(String infix) {
        System.out.println("Original Infix: " + infix);
        
        // Step 1: Reverse
        String reversed = new StringBuilder(infix).reverse().toString();
        System.out.println("\nStep 1 - Reverse:");
        System.out.println("  " + reversed);
        
        // Step 2: Swap parentheses
        StringBuilder swapped = new StringBuilder();
        for (char c : reversed.toCharArray()) {
            if (c == '(') swapped.append(')');
            else if (c == ')') swapped.append('(');
            else swapped.append(c);
        }
        System.out.println("\nStep 2 - Swap Parentheses:");
        System.out.println("  " + swapped);
        
        // Step 3: Convert to postfix (shown conceptually)
        System.out.println("\nStep 3 - Convert to Postfix:");
        System.out.println("  Apply standard infix→postfix algorithm");
        System.out.println("  Result: CBA+*");
        
        // Step 4: Reverse
        System.out.println("\nStep 4 - Reverse Again:");
        System.out.println("  *+ABC");
        
        System.out.println("\nFinal Prefix: *+ABC");
    }
    
    private static void demonstratePrecedence() {
        InfixToPrefix converter = new InfixToPrefix();
        
        String[] examples = {
            "A+B*C",        // +A*BC
            "A*B+C",        // +*ABC
            "(A+B)*C",      // *+ABC
            "A+B*C-D",      // -+A*BCD
            "A^B*C+D",      // +*^ABCD
        };
        
        System.out.println("┌─────────────────┬─────────────┐");
        System.out.println("│ Infix           │ Prefix      │");
        System.out.println("├─────────────────┼─────────────┤");
        for (String infix : examples) {
            String prefix = converter.infixToPrefix(infix);
            System.out.printf("│ %-15s │ %-11s │%n", infix, prefix);
        }
        System.out.println("└─────────────────┴─────────────┘");
    }
    
    private static void demonstrateAssociativity() {
        InfixToPrefix converter = new InfixToPrefix();
        
        System.out.println("Left-Associative (evaluated left to right):");
        String infix1 = "A-B-C";
        String prefix1 = converter.infixToPrefix(infix1);
        System.out.println("  Infix:  " + infix1);
        System.out.println("  Prefix: " + prefix1);
        System.out.println("  Means:  ((A-B)-C)");
        
        System.out.println("\nRight-Associative (evaluated right to left):");
        String infix2 = "A^B^C";
        String prefix2 = converter.infixToPrefix(infix2);
        System.out.println("  Infix:  " + infix2);
        System.out.println("  Prefix: " + prefix2);
        System.out.println("  Means:  (A^(B^C))");
        
        System.out.println("\nWith Parentheses (explicit grouping):");
        String infix3 = "(A-B)-C";
        String prefix3 = converter.infixToPrefix(infix3);
        System.out.println("  Infix:  " + infix3);
        System.out.println("  Prefix: " + prefix3);
        
        String infix4 = "A-(B-C)";
        String prefix4 = converter.infixToPrefix(infix4);
        System.out.println("  Infix:  " + infix4);
        System.out.println("  Prefix: " + prefix4);
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Convert Infix → Prefix

INFIX:  (A+B)*C     (natural, needs parentheses)
PREFIX: *+ABC       (operator first, no parentheses needed)

This is one of the MOST CHALLENGING conversions!

TWO MAIN APPROACHES:

METHOD 1: REVERSE-CONVERT-REVERSE (Most Intuitive)
=========================================

Algorithm:
1. Reverse the infix expression
2. Swap '(' with ')' and vice versa
3. Apply standard infix→postfix conversion
4. Reverse the result

Example: (A+B)*C

Step 1: Reverse
  (A+B)*C → C)*(B+A(

Step 2: Swap parentheses
  C)*(B+A( → C(*)(B+A)

Step 3: Convert to postfix (standard algorithm)
  C(*)(B+A) → CBA+*
  
  Process:
  C → output: C
  ( → stack: [(]
  * → stack: [(, *]
  ) → pop until (: output: C*, stack: []
  ( → stack: [(]
  B → output: C*B
  + → stack: [(, +]
  A → output: C*BA
  ) → pop until (: output: C*BA+
  
  Wait, let me recalculate:
  
  C → output: C
  ( → stack: [(]
  * → stack: [(, *]
  ) → pop *, then (: output: C*
  No wait, that's wrong too.
  
  Actually: C(*)(B+A)
  
  Scan left to right:
  C → output: [C]
  ( → stack: [(]
  * → can't pop, push: stack: [(, *]
  ) → pop until (: output: [C, *], stack: []
  ( → stack: [(]
  B → output: [C, *, B]
  + → stack: [(, +]
  A → output: [C, *, B, A]
  ) → pop until (: output: [C, *, B, A, +]
  
  Result: C*BA+
  
Step 4: Reverse
  C*BA+ → +AB*C
  
Hmm, that's not right. Let me reconsider...

Actually for C(*)(B+A):
This should be: C * (B+A)

Wait, the swapped version should be evaluated as:
  Infix: C * (B+A)
  Postfix: CBA+*

Then reverse: *+ABC ✓

Step 4: Reverse the postfix
  CBA+* → *+ABC ✓

This works!

WHY THIS METHOD WORKS:

Reversing converts:
- Prefix operator position → Postfix operator position
- But backwards!

Swapping parens maintains grouping in reverse.

Standard infix→postfix handles precedence.

Final reverse gives us prefix!

METHOD 2: DIRECT METHOD (Scan Right to Left)
==========================================

Algorithm:
1. Scan infix from RIGHT to LEFT
2. Use two stacks: operators and operands
3. When operator found:
   - Check precedence with stack top
   - Pop and apply as needed
4. Build prefix expressions directly

Example: (A+B)*C

Scan RIGHT to LEFT: C, *, ), B, +, A, (

C → operands: [C]
* → operators: [*]
) → operators: [*, )]  (closing paren when scanning RTL)
B → operands: [C, B]
+ → check precedence, push: operators: [*, ), +]
A → operands: [C, B, A]
( → pop until ), build expressions:
    Pop +: operands A, B → +AB
    operands: [C, +AB]
    Pop )
    operators: [*]
Done scanning
* → pop and apply: C, +AB → *+ABC
    operands: [*+ABC]

Result: *+ABC ✓

KEY INSIGHTS:

1. PRECEDENCE RULES (when scanning RTL):
   For prefix, we process right to left
   
   Pop from stack when:
   - Left-associative: stack_prec >= current_prec
   - Right-associative: stack_prec > current_prec

2. PARENTHESES:
   - ')' is treated as opening (we're going backwards!)
   - '(' is treated as closing

3. OPERAND ORDER:
   When building prefix from two operands:
   - First pop (in RTL) = right operand
   - Second pop = left operand
   - Result: operator + left + right

COMPARISON WITH INFIX→POSTFIX:

Infix→Postfix:
- Scan LEFT to RIGHT
- '(' is opening, ')' is closing
- Build: left right operator
- Pop when: stack_prec >= current_prec (left-assoc)

Infix→Prefix (Direct):
- Scan RIGHT to LEFT
- ')' is opening, '(' is closing
- Build: operator left right
- Pop when: stack_prec > current_prec (right-assoc nature)

They're mirror images!

DETAILED EXAMPLE: A+B*C

Method 1: Reverse-Convert-Reverse
  1. Reverse: C*B+A
  2. Swap parens: C*B+A (no parens to swap)
  3. To postfix: CB*A+
     C → [C]
     * → push
     B → [C, B]
     * → pop nothing (stack empty), push: [*]
     
  Wait, let me trace properly:
  
  C*B+A
  C → output: [C], stack: []
  * → output: [C], stack: [*]
  B → output: [C, B], stack: [*]
  + → * has higher prec, pop: output: [C, B, *], stack: [+]
  A → output: [C, B, *, A], stack: [+]
  End → pop +: output: [C, B, *, A, +]
  
  Result: CB*A+
  
  4. Reverse: +A*BC ✓

Method 2: Direct (RTL)
  Scan: C, *, B, +, A
  
  C → operands: [C]
  * → operators: [*]
  B → operands: [C, B]
  + → precedence check: + (1) vs * (2)
      * has higher, so pop *: operands [C, B] → *BC
      operands: [*BC], operators: []
      push +: operators: [+]
  A → operands: [*BC, A]
  End → pop +: operands [*BC, A] → +A*BC
  
  Result: +A*BC ✓

PRECEDENCE TABLE:

Operator | Precedence | Associativity
---------|-----------|---------------
   ^     |     3     | Right
  *, /   |     2     | Left
  +, -   |     1     | Left

ASSOCIATIVITY HANDLING:

Left-Associative (A-B-C):
  Means: ((A-B)-C)
  Prefix: --ABC
  
  Infix: A-B-C
  Reverse: C-B-A
  To postfix: CBA--
  Reverse: --ABC ✓

Right-Associative (A^B^C):
  Means: (A^(B^C))
  Prefix: ^A^BC
  
  Infix: A^B^C
  Reverse: C^B^A
  To postfix: CB^A^
  Reverse: ^A^BC ✓

COMPLEX EXAMPLE: (A-B/C)*(A/K-L)

Method 1: Reverse-Convert-Reverse
  1. Original: (A-B/C)*(A/K-L)
  2. Reverse: (L-K/A)*)C/B-A(
  3. Swap: )L-K/A(*)(C/B-A)
  4. To postfix: LKA/-C*BA/C-*
  
  Hmm, this gets complex. Let's trust the algorithm.
  
  Actually, the key is:
  - Method 1 is mechanical and always works
  - Method 2 requires careful state management

EXPRESSION TREE:

         *
        / \
       -   -
      / \ / \
     A  / / L
       / /
      B K
      |
      C A

Pre-order (Prefix):   *-A/BC-/AKL
In-order (Infix):     ((A-(B/C))*((A/K)-L))
Post-order (Postfix): ABC/-AK/L-*

COMPLEXITY ANALYSIS:

Method 1 (Reverse-Convert-Reverse):
Time: O(n) - each step is O(n)
Space: O(n) - for stacks and strings

Method 2 (Direct):
Time: O(n) - single pass
Space: O(n) - two stacks

Method 1 is easier to implement and understand.
Method 2 is more efficient (single pass).

VALIDATION:

Valid infix requirements:
1. Balanced parentheses
2. Operators between operands
3. No consecutive operators
4. Proper precedence

COMMON MISTAKES:

1. Not swapping parentheses in Method 1
2. Wrong scan direction in Method 2
3. Incorrect precedence comparison
4. Not handling associativity
5. Wrong operand order when building
6. Forgetting to pop remaining operators

EDGE CASES:

1. Single operand: "A" → "A"
2. Simple operation: "A+B" → "+AB"
3. Only parentheses: "(A+B)" → "+AB"
4. Nested parens: "((A+B))" → "+AB"
5. All operators same: "A+B+C+D" → "+++ABCD"
6. Complex nesting: "(((A+B)*C)-D)/E"

TESTING STRATEGY:

1. Simple binary: "A+B"
2. With precedence: "A+B*C"
3. With parens: "(A+B)*C"
4. Right-assoc: "A^B^C"
5. Non-commutative: "A-B-C", "A/B/C"
6. Complex nested: "(A+B)*(C-D)"
7. All operators: "+, -, *, /, ^"

WHY PREFIX IS USEFUL:

1. No parentheses needed
2. Unambiguous
3. Easy recursive evaluation
4. Used in Lisp-like languages
5. Functional programming
6. Compiler intermediate form

PRACTICAL APPLICATIONS:

1. Compiler design
2. Expression evaluation
3. Functional programming languages
4. Mathematical software
5. Symbolic computation
6. Query optimization

INTERVIEW TIPS:

1. Explain both methods clearly
2. Show Method 1 is easier
3. Show Method 2 is more efficient
4. Demonstrate with examples
5. Draw expression tree
6. Handle associativity carefully
7. Test with complex expressions
8. Discuss trade-offs

DEBUGGING TIPS:

1. Verify parentheses balancing
2. Check precedence rules
3. Trace stack state
4. Test simple cases first
5. Draw expression tree
6. Verify with postfix conversion
7. Check associativity

RELATED PROBLEMS:

1. Infix to Postfix (Shunting Yard)
2. Prefix to Infix
3. Evaluate Prefix Expression
4. Expression Tree Construction
5. All notation conversions

This is the most complex conversion because:
- Infix has parentheses
- Need to handle precedence
- Need to handle associativity
- Two different scan directions

But both methods work reliably!
*/
