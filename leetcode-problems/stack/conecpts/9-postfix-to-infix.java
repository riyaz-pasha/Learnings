import java.util.Stack;

class PostfixToInfix {
    
    // Approach 1: Basic Conversion with Parentheses (Safe)
    // Time: O(n), Space: O(n)
    public String postfixToInfix(String postfix) {
        Stack<String> stack = new Stack<>();
        
        // Read postfix expression from LEFT to RIGHT
        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);
            
            // If character is operand
            if (!isOperator(c)) {
                // Push to stack as string
                stack.push(String.valueOf(c));
            }
            // If character is operator
            else {
                // Pop two operands (order matters!)
                String operand2 = stack.pop();  // Right operand
                String operand1 = stack.pop();  // Left operand
                
                // Create infix: (operand1 operator operand2)
                String infix = "(" + operand1 + c + operand2 + ")";
                
                // Push back to stack
                stack.push(infix);
            }
        }
        
        // Final result is at top of stack
        return stack.pop();
    }
    
    // Approach 2: Optimized with Minimal Parentheses
    // Removes unnecessary outer parentheses based on precedence
    public String postfixToInfixOptimized(String postfix) {
        Stack<ExprNode> stack = new Stack<>();
        
        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);
            
            if (!isOperator(c)) {
                // Operands have highest precedence
                stack.push(new ExprNode(String.valueOf(c), Integer.MAX_VALUE));
            }
            else {
                ExprNode right = stack.pop();
                ExprNode left = stack.pop();
                
                int currPrecedence = getPrecedence(c);
                
                // Add parentheses only when needed
                String leftStr = left.expr;
                String rightStr = right.expr;
                
                // Left needs parens if lower precedence
                if (left.precedence < currPrecedence) {
                    leftStr = "(" + leftStr + ")";
                }
                
                // Right needs parens if lower precedence
                // OR equal precedence for left-associative ops
                if (right.precedence < currPrecedence ||
                    (right.precedence == currPrecedence && isLeftAssociative(c))) {
                    rightStr = "(" + rightStr + ")";
                }
                
                String infix = leftStr + c + rightStr;
                stack.push(new ExprNode(infix, currPrecedence));
            }
        }
        
        return stack.pop().expr;
    }
    
    // Approach 3: With Multi-character Operands
    public String postfixToInfixMultiChar(String postfix) {
        Stack<String> stack = new Stack<>();
        String[] tokens = postfix.split(" ");
        
        for (String token : tokens) {
            if (isOperatorString(token)) {
                String op2 = stack.pop();
                String op1 = stack.pop();
                String infix = "(" + op1 + " " + token + " " + op2 + ")";
                stack.push(infix);
            }
            else {
                stack.push(token);
            }
        }
        
        return stack.pop();
    }
    
    // Approach 4: Without Outer Parentheses
    public String postfixToInfixClean(String postfix) {
        String result = postfixToInfix(postfix);
        // Remove outer parentheses
        if (result.startsWith("(") && result.endsWith(")")) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }
    
    // Helper class for optimized approach
    static class ExprNode {
        String expr;
        int precedence;
        
        ExprNode(String expr, int precedence) {
            this.expr = expr;
            this.precedence = precedence;
        }
    }
    
    // Helper: Check if character is operator
    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    
    private boolean isOperatorString(String s) {
        return s.length() == 1 && isOperator(s.charAt(0));
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
    
    private boolean isLeftAssociative(char op) {
        return op != '^';  // All except ^ are left-associative
    }
    
    // Test cases with detailed visualization
    public static void main(String[] args) {
        PostfixToInfix converter = new PostfixToInfix();
        
        // Test Case 1: Simple expression
        String postfix1 = "AB+";
        System.out.println("Test 1: Simple Addition");
        System.out.println("Postfix: " + postfix1);
        System.out.println("Infix:   " + converter.postfixToInfix(postfix1));
        System.out.println("Clean:   " + converter.postfixToInfixClean(postfix1));
        
        // Test Case 2: With precedence
        String postfix2 = "AB+C*";
        System.out.println("\nTest 2: Precedence Example");
        System.out.println("Postfix: " + postfix2);
        System.out.println("Infix:   " + converter.postfixToInfix(postfix2));
        System.out.println("Clean:   " + converter.postfixToInfixClean(postfix2));
        System.out.println("Optimal: " + converter.postfixToInfixOptimized(postfix2));
        
        // Test Case 3: Complex expression
        String postfix3 = "ABC/-AK/L-*";
        System.out.println("\nTest 3: Complex Expression");
        System.out.println("Postfix: " + postfix3);
        System.out.println("Infix:   " + converter.postfixToInfix(postfix3));
        System.out.println("Clean:   " + converter.postfixToInfixClean(postfix3));
        
        // Test Case 4: Different operators
        String postfix4 = "AB*C+DE/-";
        System.out.println("\nTest 4: Mixed Operators");
        System.out.println("Postfix: " + postfix4);
        System.out.println("Infix:   " + converter.postfixToInfix(postfix4));
        System.out.println("Optimal: " + converter.postfixToInfixOptimized(postfix4));
        
        // Test Case 5: Exponentiation (right-associative)
        String postfix5 = "ABC*-D^";
        System.out.println("\nTest 5: Right-Associative (^)");
        System.out.println("Postfix: " + postfix5);
        System.out.println("Infix:   " + converter.postfixToInfix(postfix5));
        System.out.println("Optimal: " + converter.postfixToInfixOptimized(postfix5));
        
        // Test Case 6: Subtraction (non-commutative)
        String postfix6 = "AB-CD-/";
        System.out.println("\nTest 6: Non-Commutative Operators");
        System.out.println("Postfix: " + postfix6);
        System.out.println("Infix:   " + converter.postfixToInfix(postfix6));
        System.out.println("Optimal: " + converter.postfixToInfixOptimized(postfix6));
        
        // All three notations
        System.out.println("\n=== All Three Notations ===");
        compareAllNotations("AB+C*");
        
        // Step-by-step trace
        System.out.println("\n=== Step-by-Step Trace for 'AB+C*' ===");
        traceConversion("AB+C*");
        
        // Precedence demonstration
        System.out.println("\n=== Precedence Handling ===");
        demonstratePrecedence();
        
        // Associativity demonstration
        System.out.println("\n=== Associativity Handling ===");
        demonstrateAssociativity();
    }
    
    private static void compareAllNotations(String postfix) {
        PostfixToInfix converter = new PostfixToInfix();
        String infix = converter.postfixToInfixClean(postfix);
        
        System.out.println("Expression: (A+B)*C");
        System.out.println("┌──────────┬────────────┐");
        System.out.println("│ Notation │ Expression │");
        System.out.println("├──────────┼────────────┤");
        System.out.println("│ Postfix  │ " + postfix + "     │");
        System.out.println("│ Infix    │ " + infix + "    │");
        System.out.println("│ Prefix   │ *+ABC      │");
        System.out.println("└──────────┴────────────┘");
    }
    
    private static void traceConversion(String postfix) {
        Stack<String> stack = new Stack<>();
        
        System.out.println("Reading from LEFT to RIGHT:");
        System.out.println("┌────────┬─────────────┬──────────────────────────┐");
        System.out.println("│ Char   │ Action      │ Stack                    │");
        System.out.println("├────────┼─────────────┼──────────────────────────┤");
        
        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);
            String action = "";
            
            if (isOperatorChar(c)) {
                String op2 = stack.pop();
                String op1 = stack.pop();
                String result = "(" + op1 + c + op2 + ")";
                stack.push(result);
                action = "Pop 2, build";
            }
            else {
                stack.push(String.valueOf(c));
                action = "Push operand";
            }
            
            System.out.printf("│   %c    │ %-11s │ %-24s │%n", 
                            c, action, stackToString(stack));
        }
        
        System.out.println("└────────┴─────────────┴──────────────────────────┘");
        System.out.println("Final Result: " + stack.peek());
        
        // Show the transformation
        System.out.println("\nTransformation:");
        System.out.println("Postfix: A B + C *  (no parentheses needed)");
        System.out.println("         ↓ ↓ ↓ ↓ ↓");
        System.out.println("Infix:   ((A+B)*C)  (parentheses for clarity)");
    }
    
    private static void demonstratePrecedence() {
        PostfixToInfix converter = new PostfixToInfix();
        
        String[] examples = {
            "AB+CD+*",  // (A+B)*(C+D)
            "ABC*+",    // A+(B*C)
            "AB*CD*+",  // (A*B)+(C*D)
            "ABCD^*+",  // A+(B*(C^D))
        };
        
        System.out.println("┌───────────────┬─────────────────┐");
        System.out.println("│ Postfix       │ Infix           │");
        System.out.println("├───────────────┼─────────────────┤");
        for (String postfix : examples) {
            String infix = converter.postfixToInfixOptimized(postfix);
            System.out.printf("│ %-13s │ %-15s │%n", postfix, infix);
        }
        System.out.println("└───────────────┴─────────────────┘");
    }
    
    private static void demonstrateAssociativity() {
        PostfixToInfix converter = new PostfixToInfix();
        
        System.out.println("Left-Associative (+, -, *, /):");
        String postfix1 = "AB+C+";
        System.out.println("  Postfix: " + postfix1);
        System.out.println("  Infix:   " + converter.postfixToInfixOptimized(postfix1));
        System.out.println("  Means:   ((A+B)+C) - evaluated left to right");
        
        System.out.println("\nRight-Associative (^):");
        String postfix2 = "ABC^^";
        System.out.println("  Postfix: " + postfix2);
        System.out.println("  Infix:   " + converter.postfixToInfixOptimized(postfix2));
        System.out.println("  Means:   (A^(B^C)) - evaluated right to left");
        
        System.out.println("\nNon-Commutative (-):");
        String postfix3 = "ABC--";
        System.out.println("  Postfix: " + postfix3);
        System.out.println("  Infix:   " + converter.postfixToInfixOptimized(postfix3));
        System.out.println("  Means:   ((A-B)-C) - order matters!");
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
        return sb.length() > 24 ? sb.substring(0, 21) + "..." : sb.toString();
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Convert Postfix → Infix

POSTFIX: A B + C *     (no parentheses needed)
INFIX:   ((A+B)*C)     (parentheses required!)

KEY CHALLENGE: Adding parentheses to preserve meaning

ALGORITHM:

1. Initialize empty stack
2. Scan postfix from LEFT to RIGHT
3. For each character:
   - If OPERAND → push to stack
   - If OPERATOR →
     * Pop 2 operands: op2 (right), op1 (left)
     * Create infix: (op1 operator op2)
     * Push result back
4. Final stack top is the infix

STEP-BY-STEP EXAMPLE: "AB+C*"

Reading LEFT to RIGHT: A, B, +, C, *

Step 1: Read 'A' (operand)
  Stack: [A]

Step 2: Read 'B' (operand)
  Stack: [A, B]

Step 3: Read '+' (operator)
  Pop: B (op2), A (op1)
  Create infix: (A+B)
  Stack: [(A+B)]

Step 4: Read 'C' (operand)
  Stack: [(A+B), C]

Step 5: Read '*' (operator)
  Pop: C (op2), (A+B) (op1)
  Create infix: ((A+B)*C)
  Stack: [((A+B)*C)]

Result: ((A+B)*C)

WHY PARENTHESES?

Postfix is unambiguous without parentheses.
Infix NEEDS parentheses to show evaluation order!

Example:
Postfix: ABC*+
Without parens: A+B*C (wrong! means A+(B*C))
With parens: (A+(B*C)) or A+B*C with precedence

OPERAND ORDER (CRITICAL!)

When popping for operator:
  First pop = RIGHT operand (op2)
  Second pop = LEFT operand (op1)

For postfix: AB-
  Pop: B (op2), A (op1)
  Infix: (A-B) ✓

If reversed:
  Pop: B (op1), A (op2)
  Infix: (B-A) ✗ WRONG!

ANOTHER EXAMPLE: "ABC/-AK/L-*"

Reading LEFT to RIGHT:

A → [A]
B → [A, B]
C → [A, B, C]
/ → Pop C,B → (B/C) → [A, (B/C)]
- → Pop (B/C),A → (A-(B/C)) → [(A-(B/C))]
A → [(A-(B/C)), A]
K → [(A-(B/C)), A, K]
/ → Pop K,A → (A/K) → [(A-(B/C)), (A/K)]
L → [(A-(B/C)), (A/K), L]
- → Pop L,(A/K) → ((A/K)-L) → [(A-(B/C)), ((A/K)-L)]
* → Pop ((A/K)-L),(A-(B/C)) → ((A-(B/C))*((A/K)-L))
  Stack: [((A-(B/C))*((A/K)-L))]

Result: ((A-(B/C))*((A/K)-L))

VISUALIZATION WITH TREE:

         *
        / \
       -   -
      / \ / \
     A  / / L
       / /
      B K
      |
      C A

Postfix (Post-order):  ABC/-AK/L-*
Infix (In-order):      ((A-(B/C))*((A/K)-L))

In-order traversal gives infix (with parentheses)!

PARENTHESES OPTIMIZATION:

Basic approach adds parentheses everywhere.
Can optimize based on precedence!

Example: ABC*+

Basic:    ((A+(B*C)))
Optimized: A+B*C  (no parens needed, * has higher precedence)

Rules for minimal parentheses:
1. Higher precedence ops don't need parens
2. Same precedence: consider associativity
3. Operands never need parens

PRECEDENCE LEVELS:

^   (exponentiation)    - 3 - Right-associative
*, /                    - 2 - Left-associative
+, -                    - 1 - Left-associative

ASSOCIATIVITY HANDLING:

Left-associative (+, -, *, /):
  AB+C+ → ((A+B)+C)
  Parentheses on left when same precedence

Right-associative (^):
  ABC^^ → (A^(B^C))
  Parentheses on right when same precedence

Example with subtraction:
  Postfix: ABC--
  Step 1: AB- → (A-B)
  Step 2: (A-B)C- → ((A-B)-C) ✓
  NOT: (A-(B-C)) ✗

COMPARISON TABLE:

┌─────────────────┬──────────────┬─────────────────┐
│ Conversion      │ Scan         │ Build           │
├─────────────────┼──────────────┼─────────────────┤
│ Postfix→Infix   │ Left to Right│ (op1 op op2)    │
│ Postfix→Prefix  │ Left to Right│ op op1 op2      │
│ Prefix→Infix    │ Right to Left│ (op1 op op2)    │
│ Prefix→Postfix  │ Right to Left│ op1 op2 op      │
└─────────────────┴──────────────┴─────────────────┘

COMPLEXITY ANALYSIS:

Time: O(n)
- Single pass through postfix
- Each character processed once
- Stack operations O(1)

Space: O(n)
- Stack stores intermediate expressions
- Each expression can grow to O(n)
- String concatenation overhead

VALIDATION:

Valid postfix requirements:
1. #operators = #operands - 1
2. At any point reading left to right:
   #operands > #operators
3. Stack has exactly 1 element at end

COMMON MISTAKES:

1. Wrong operand order (op2, op1 not op1, op2)
2. Forgetting parentheses
3. Scanning right to left (WRONG!)
4. Stack underflow (invalid postfix)
5. Not handling associativity

EDGE CASES:

1. Single operand: "A" → "A"
2. Simple operation: "AB+" → "(A+B)"
3. Chain of ops: "AB+C+D+" → "(((A+B)+C)+D)"
4. Mixed precedence: "ABC*+D/" → "((A+(B*C))/D)"
5. Exponentiation: "ABC^^" → "(A^(B^C))"

PRACTICAL APPLICATIONS:

1. Expression evaluation
2. Compiler design
3. Calculator implementations
4. Mathematical software
5. Query optimization
6. Teaching tool for notation conversion

WHY POSTFIX IS BETTER FOR COMPUTATION:

Infix:
+ Readable for humans
- Needs parentheses
- Requires precedence rules
- Complex to evaluate

Postfix:
+ No parentheses needed
+ Easy stack-based evaluation
+ Unambiguous
- Less readable for humans

EVALUATION COMPARISON:

Infix: ((A+B)*C)
  1. Scan for operators
  2. Check precedence
  3. Handle parentheses
  4. Evaluate recursively
  → Complex!

Postfix: AB+C*
  1. Scan left to right
  2. Push operands
  3. Pop and compute on operators
  → Simple!

DETAILED TRACE WITH PRECEDENCE:

Postfix: "AB*C+DE/-"

A → [A]
B → [A, B]
* → [(A*B)]              precedence: 2
C → [(A*B), C]
+ → [((A*B)+C)]          precedence: 1
D → [((A*B)+C), D]
E → [((A*B)+C), D, E]
/ → [((A*B)+C), (D/E)]   precedence: 2
- → [(((A*B)+C)-(D/E))]  precedence: 1

Result: (((A*B)+C)-(D/E))

With optimization: ((A*B)+C)-(D/E)
  - Outer parens not needed
  - Inner parens preserve precedence

INTERVIEW TIPS:

1. Emphasize LEFT to RIGHT scan
2. Show operand order: op2, op1
3. Explain parentheses necessity
4. Demonstrate with tree
5. Discuss precedence optimization
6. Test with -, / (non-commutative)
7. Show associativity handling
8. Draw stack state at each step

TESTING STRATEGY:

1. Simple binary ops: "AB+"
2. Multiple ops same precedence: "AB+C+"
3. Mixed precedence: "ABC*+"
4. Non-commutative: "AB-", "AB/"
5. Right-associative: "ABC^^"
6. Complex nested: "ABC/-AK/L-*"
7. All operators: "+, -, *, /, ^"

OPTIMIZATION TECHNIQUES:

1. Use StringBuilder for concatenation
2. Pre-allocate stack capacity
3. Validate postfix before processing
4. Remove unnecessary outer parens
5. Implement precedence-aware version
6. Cache operator precedence

RELATED PROBLEMS:

1. Infix to Postfix (Shunting Yard)
2. Prefix to Infix
3. Evaluate Postfix Expression
4. Expression Tree Construction
5. Infix Evaluation

This algorithm demonstrates:
- Stack-based parsing
- Notation conversion
- Precedence handling
- Parentheses generation
- Tree traversal equivalence
*/
