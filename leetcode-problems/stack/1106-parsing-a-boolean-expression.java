
import java.util.HashSet;
import java.util.Stack;

/**
 * BOOLEAN EXPRESSION PARSING - COMPLETE ANALYSIS
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * We need to parse and evaluate boolean expressions with specific syntax:
 * - 't' = true
 * - 'f' = false
 * - '!(expr)' = NOT
 * - '&(expr1,expr2,...)' = AND
 * - '|(expr1,expr2,...)' = OR
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ===============================================
 * 
 * Step 1: IDENTIFY THE PATTERN
 * - This is a RECURSIVE PARSING problem
 * - Each operator can contain nested sub-expressions
 * - Think: "How would I evaluate this by hand?"
 *   Example: "&(|(t,f),!(f))"
 *   - Start with '&', need to evaluate its children
 *   - First child: '|(t,f)' - recursively evaluate
 *   - Second child: '!(f)' - recursively evaluate
 *   - Combine results with AND
 * 
 * Step 2: RECOGNIZE THE DATA STRUCTURE
 * - This is essentially a TREE structure
 * - Each operator is a node with children
 * - Base cases ('t', 'f') are leaf nodes
 * - Natural fit for RECURSION or STACK-based parsing
 * 
 * Step 3: CHOOSE YOUR WEAPON
 * Three main approaches:
 * 
 * A) RECURSIVE DESCENT PARSING (Most Intuitive)
 *    - Use index pointer, recursively parse
 *    - Clean, matches how we think about the problem
 *    - Time: O(n), Space: O(n) for call stack
 * 
 * B) STACK-BASED PARSING (Iterative)
 *    - Simulate recursion with explicit stack
 *    - Better for very deep nesting (avoid stack overflow)
 *    - Time: O(n), Space: O(n) for stack
 * 
 * C) BUILD EXPRESSION TREE THEN EVALUATE
 *    - Two-pass: parse into tree, then evaluate
 *    - Overkill for this problem but good for extensions
 *    - Time: O(n), Space: O(n)
 * 
 * Step 4: HANDLE EDGE CASES IN INTERVIEW
 * - Single character: "t" or "f"
 * - Nested expressions: "!(!(t))"
 * - Multiple operands: "&(t,f,t,f,t)"
 * - Complex nesting: "&(|(t,f),&(f,t))"
 * 
 * INTERVIEW COMMUNICATION TIPS:
 * ============================
 * 1. "I notice this is a recursive structure, so I'll use recursive descent parsing"
 * 2. "Let me trace through an example: &(t,f)"
 * 3. "I'll need to track my position in the string as I parse"
 * 4. "For AND/OR, I'll parse comma-separated sub-expressions"
 * 5. "Let me verify with a complex example before coding"
 * 
 * TIME COMPLEXITY: O(n) where n is length of expression
 * - We visit each character exactly once
 * 
 * SPACE COMPLEXITY: O(n) for recursion stack
 * - In worst case (deep nesting), call stack depth = n
 * - Example: "!(!(!(!(t))))" has depth 4
 */

class BooleanExpressionParser {
    
    // Global index to track our position in the string
    // Using array to make it mutable across recursive calls
    private int[] index;
    
    /**
     * MAIN SOLUTION - RECURSIVE DESCENT PARSER
     * 
     * This is the cleanest approach for interviews.
     * The key insight: each recursive call handles ONE complete sub-expression
     */
    public boolean parseBoolExpr(String expression) {
        index = new int[]{0}; // Start at position 0
        return parse(expression);
    }
    
    /**
     * CORE RECURSIVE FUNCTION
     * 
     * Parses one complete expression starting at index[0]
     * Updates index[0] to position after the parsed expression
     * 
     * LOGIC FLOW:
     * 1. Look at current character to determine expression type
     * 2. For 't'/'f': simple base case
     * 3. For operators: parse operator, skip '(', parse operands, skip ')'
     */
    private boolean parse(String expr) {
        char current = expr.charAt(index[0]);
        index[0]++; // Move past the current character
        
        // BASE CASES: simple boolean values
        if (current == 't') return true;
        if (current == 'f') return false;
        
        // RECURSIVE CASES: operators with sub-expressions
        
        // We've seen the operator ('!', '&', or '|')
        // Next character must be '(' so skip it
        index[0]++; // Skip '('
        
        if (current == '!') {
            // NOT operator: exactly one operand
            boolean result = !parse(expr);
            index[0]++; // Skip ')'
            return result;
        }
        
        if (current == '&') {
            // AND operator: all operands must be true
            // Parse comma-separated list of operands
            boolean result = true;
            
            while (expr.charAt(index[0]) != ')') {
                // Parse one operand
                result &= parse(expr);
                
                // If we see a comma, skip it for next operand
                if (expr.charAt(index[0]) == ',') {
                    index[0]++;
                }
            }
            
            index[0]++; // Skip ')'
            return result;
        }
        
        // Must be '|' - OR operator
        // At least one operand must be true
        boolean result = false;
        
        while (expr.charAt(index[0]) != ')') {
            // Parse one operand
            result |= parse(expr);
            
            // If we see a comma, skip it for next operand
            if (expr.charAt(index[0]) == ',') {
                index[0]++;
            }
        }
        
        index[0]++; // Skip ')'
        return result;
    }
    
    /**
     * ALTERNATIVE SOLUTION 1: STACK-BASED APPROACH
     * 
     * Better for avoiding deep recursion in production code.
     * Simulates the recursive approach using explicit stack.
     * 
     * KEY IDEA:
     * - Traverse string left to right
     * - Push characters onto stack
     * - When we see ')', we've completed an expression - evaluate it
     * - Replace the completed expression with its result on stack
     */
    public boolean parseBoolExprStack(String expression) {
        java.util.Stack<Character> stack = new java.util.Stack<>();
        
        for (char c : expression.toCharArray()) {
            if (c == ',') {
                // Commas just separate operands, we can skip them
                continue;
            }
            
            if (c == ')') {
                // Time to evaluate - pop until we find the operator
                java.util.List<Character> operands = new java.util.ArrayList<>();
                
                // Collect all operands (t's and f's)
                while (stack.peek() != '(') {
                    operands.add(stack.pop());
                }
                
                stack.pop(); // Remove '('
                char operator = stack.pop(); // Get the operator
                
                // Evaluate based on operator
                boolean result = evaluateExpression(operator, operands);
                
                // Push result back onto stack as 't' or 'f'
                stack.push(result ? 't' : 'f');
            } else {
                // Push everything else: operators, '(', 't', 'f'
                stack.push(c);
            }
        }
        
        // Final result is the only thing left on stack
        return stack.peek() == 't';
    }
    
    /**
     * Helper to evaluate an expression given operator and operands
     */
    private boolean evaluateExpression(char operator, java.util.List<Character> operands) {
        if (operator == '!') {
            // NOT: flip the single operand
            return operands.get(0) == 'f';
        } else if (operator == '&') {
            // AND: all must be true
            for (char op : operands) {
                if (op == 'f') return false;
            }
            return true;
        } else { // operator == '|'
            // OR: at least one must be true
            for (char op : operands) {
                if (op == 't') return true;
            }
            return false;
        }
    }
    
    /**
     * ALTERNATIVE SOLUTION 2: CLEANER RECURSIVE WITH STRING BUILDER
     * 
     * Uses substring instead of index tracking.
     * Less efficient (O(n²) due to substring) but easier to understand.
     */
    public boolean parseBoolExprSubstring(String expression) {
        // Base cases
        if (expression.equals("t")) return true;
        if (expression.equals("f")) return false;
        
        char operator = expression.charAt(0);
        // Extract content between parentheses
        String content = expression.substring(2, expression.length() - 1);
        
        if (operator == '!') {
            return !parseBoolExprSubstring(content);
        }
        
        // Parse comma-separated operands for & and |
        java.util.List<Boolean> operands = parseOperands(content);
        
        if (operator == '&') {
            // AND: all true
            for (boolean op : operands) {
                if (!op) return false;
            }
            return true;
        } else { // operator == '|'
            // OR: any true
            for (boolean op : operands) {
                if (op) return true;
            }
            return false;
        }
    }
    
    /**
     * Helper to parse comma-separated operands
     * Tricky part: commas inside nested expressions don't count!
     * Example: "|(t,f),&(t,t)" has 2 operands, not 4
     */
    private java.util.List<Boolean> parseOperands(String content) {
        java.util.List<Boolean> result = new java.util.ArrayList<>();
        int depth = 0; // Track nesting level
        int start = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                // Found a top-level comma - end of operand
                result.add(parseBoolExprSubstring(content.substring(start, i)));
                start = i + 1;
            }
        }
        
        // Don't forget the last operand
        result.add(parseBoolExprSubstring(content.substring(start)));
        return result;
    }
    
    /**
     * TESTING & VERIFICATION
     */
    public static void main(String[] args) {
        BooleanExpressionParser parser = new BooleanExpressionParser();
        
        // Test Case 1: Simple base cases
        assert parser.parseBoolExpr("t") == true : "Test 1 failed";
        assert parser.parseBoolExpr("f") == false : "Test 2 failed";
        
        // Test Case 2: NOT operator
        assert parser.parseBoolExpr("!(t)") == false : "Test 3 failed";
        assert parser.parseBoolExpr("!(f)") == true : "Test 4 failed";
        
        // Test Case 3: AND operator
        assert parser.parseBoolExpr("&(t,t)") == true : "Test 5 failed";
        assert parser.parseBoolExpr("&(t,f)") == false : "Test 6 failed";
        assert parser.parseBoolExpr("&(t,t,t)") == true : "Test 7 failed";
        
        // Test Case 4: OR operator
        assert parser.parseBoolExpr("|(f,f)") == false : "Test 8 failed";
        assert parser.parseBoolExpr("|(t,f)") == true : "Test 9 failed";
        assert parser.parseBoolExpr("|(f,f,t)") == true : "Test 10 failed";
        
        // Test Case 5: Nested expressions
        assert parser.parseBoolExpr("!(|(f,f))") == true : "Test 11 failed";
        assert parser.parseBoolExpr("&(|(t,f),!(f))") == true : "Test 12 failed";
        assert parser.parseBoolExpr("|(f,&(t,f))") == false : "Test 13 failed";
        
        // Test Case 6: Complex nested
        assert parser.parseBoolExpr("!(&(f,t))") == true : "Test 14 failed";
        assert parser.parseBoolExpr("&(|(f,t),!(!(t)))") == true : "Test 15 failed";
        
        // Test all three implementations give same results
        String testExpr = "&(|(t,f),!(f))";
        boolean r1 = parser.parseBoolExpr(testExpr);
        boolean r2 = parser.parseBoolExprStack(testExpr);
        boolean r3 = parser.parseBoolExprSubstring(testExpr);
        assert r1 == r2 && r2 == r3 : "Implementations don't match!";
        
        System.out.println("All tests passed! ✓");
        
        // Performance comparison (optional for interview)
        performanceTest();
    }
    
    private static void performanceTest() {
        BooleanExpressionParser parser = new BooleanExpressionParser();
        String complexExpr = "&(|(t,f,t),&(t,!(f),t),|(!(t),t,t))";
        
        long start = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            parser.parseBoolExpr(complexExpr);
        }
        long recursive = System.nanoTime() - start;
        
        start = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            parser.parseBoolExprStack(complexExpr);
        }
        long stack = System.nanoTime() - start;
        
        System.out.println("\nPerformance (100k iterations):");
        System.out.println("Recursive: " + recursive / 1_000_000 + "ms");
        System.out.println("Stack: " + stack / 1_000_000 + "ms");
    }
}

/**
 * INTERVIEW TALKING POINTS SUMMARY:
 * =================================
 * 
 * 1. "This is a classic recursive parsing problem, similar to evaluating 
 *     arithmetic expressions or building parse trees."
 * 
 * 2. "I'll use recursive descent parsing - each recursive call handles 
 *     one complete sub-expression and updates our position in the string."
 * 
 * 3. "Time complexity is O(n) since we visit each character once.
 *     Space complexity is O(n) for the recursion stack in worst case."
 * 
 * 4. "The key insight is handling comma-separated operands correctly -
 *     we parse each operand recursively and combine with the operator."
 * 
 * 5. "For production code, I might use the stack-based approach to avoid
 *     deep recursion, but recursive is cleaner for an interview setting."
 * 
 * COMMON MISTAKES TO AVOID:
 * ========================
 * - Forgetting to skip '(' and ')' characters
 * - Not handling commas correctly between operands
 * - Off-by-one errors in index tracking
 * - Not considering nested expressions properly
 * - Modifying local variable instead of shared index
 * 
 * FOLLOW-UP QUESTIONS YOU MIGHT GET:
 * ==================================
 * Q: "What if we need to handle syntax errors?"
 * A: Add validation - check for valid characters, matching parentheses,
 *    correct operand counts for each operator.
 * 
 * Q: "How would you handle more operators like XOR?"
 * A: Add another case in the switch/if-else chain. The structure stays the same.
 * 
 * Q: "Could we optimize space complexity?"
 * A: Not really - we need O(n) space either for call stack or explicit stack.
 *    The string itself is already O(n).
 */


/**
 * Evaluates a boolean expression consisting of:
 *  - 't'  -> true
 *  - 'f'  -> false
 *  - '!(subExpr)'
 *  - '&(subExpr1, subExpr2, ..., subExprN)'
 *  - '|(subExpr1, subExpr2, ..., subExprN)'
 *
 * Interview mindset:
 * ------------------
 * 1. Observe grammar: fully parenthesized expressions.
 * 2. Each operator applies only when ')' is encountered.
 * 3. Stack is ideal for parsing nested expressions.
 *
 * Time Complexity:  O(N)
 * Space Complexity: O(N)
 */
class BooleanExpressionParser2 {

    public static boolean parseBoolExpr(String expression) {
        // Stack to hold characters during parsing.
        Stack<Character> stack = new Stack<>();

        // Traverse the input expression character by character.
        for (char ch : expression.toCharArray()) {

            // Ignore commas as they don't affect evaluation.
            if (ch == ',') {
                continue;
            }

            // If not a closing parenthesis, push onto stack.
            if (ch != ')') {
                stack.push(ch);
            } else {
                // When we see ')', evaluate the sub-expression.
                // Pop elements until '(' is encountered.
                Set<Character> seen = new HashSet<>();

                while (stack.peek() != '(') {
                    seen.add(stack.pop());
                }

                // Pop '('
                stack.pop();

                // The operator is just before '('
                char operator = stack.pop();

                // Evaluate based on operator
                char result;

                if (operator == '!') {
                    // NOT: exactly one operand.
                    // If operand is 't', result = 'f'
                    // If operand is 'f', result = 't'
                    result = (seen.contains('t')) ? 'f' : 't';
                } else if (operator == '&') {
                    // AND: if any 'f' exists, result is 'f'
                    result = (seen.contains('f')) ? 'f' : 't';
                } else { // operator == '|'
                    // OR: if any 't' exists, result is 't'
                    result = (seen.contains('t')) ? 't' : 'f';
                }

                // Push the result back to stack.
                stack.push(result);
            }
        }

        // Final result is the only element in the stack.
        return stack.pop() == 't';
    }

    // Simple test harness
    public static void main(String[] args) {
        String[] tests = {
            "t",
            "f",
            "!(t)",
            "!(f)",
            "&(t,f)",
            "&(t,t,f)",
            "|(f,t)",
            "|(f,f,f)",
            "|(&(t,f,t),!(t))"
        };

        for (String test : tests) {
            System.out.println(test + " -> " + parseBoolExpr(test));
        }
    }
}
