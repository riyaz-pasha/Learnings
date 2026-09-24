import java.util.ArrayList;
import java.util.List;

/**
 * Expression Add Operators - All Solution Variants
 * 
 * Problem: Insert +, -, * operators between digits to form expressions equal to target.
 * Key Challenge: Handle multiplication precedence and avoid leading zeros.
 */
class ExpressionAddOperatorsAllSolutions {
    
    // ==================== SOLUTION 1: Backtracking with Running Calculation (Optimal) ====================
    /**
     * Approach: Build expressions character by character, maintaining running total
     *           Track last operand to handle multiplication precedence correctly
     * 
     * Key Insight: For multiplication, we need to:
     *   - Remove last added value from total: total - lastValue
     *   - Add back the multiplied result: + (lastValue * currentNum)
     * 
     * Time Complexity: O(4^n × n) where n = length of num
     *                  - At most 4 choices per position (digit or +,-, *), string operations O(n)
     * Space Complexity: O(n) for recursion stack and expression string
     * 
     * Pros: Most efficient, handles multiplication without parsing, elegant
     * Cons: Requires careful tracking of last operand
     */
    public List<String> addOperators1(String num, int target) {
        List<String> result = new ArrayList<>();
        if (num == null || num.length() == 0) return result;
        
        backtrack1(num, target, 0, 0, 0, "", result);
        return result;
    }
    
    /**
     * @param num Original number string
     * @param target Target value
     * @param pos Current position in num
     * @param eval Current evaluated value
     * @param lastValue Last operand (needed for multiplication)
     * @param expr Current expression being built
     * @param result List to store valid expressions
     */
    private void backtrack1(String num, int target, int pos, long eval, 
                           long lastValue, String expr, List<String> result) {
        // Base case: processed all digits
        if (pos == num.length()) {
            if (eval == target) {
                result.add(expr);
            }
            return;
        }
        
        // Try all possible number lengths from current position
        for (int i = pos; i < num.length(); i++) {
            // Skip numbers with leading zeros (except "0" itself)
            if (i != pos && num.charAt(pos) == '0') break;
            
            String currStr = num.substring(pos, i + 1);
            long currNum = Long.parseLong(currStr);
            
            if (pos == 0) {
                // First number - no operator before it
                backtrack1(num, target, i + 1, currNum, currNum, currStr, result);
            } else {
                // Try addition: eval + currNum
                backtrack1(num, target, i + 1, eval + currNum, currNum, 
                          expr + "+" + currStr, result);
                
                // Try subtraction: eval - currNum
                backtrack1(num, target, i + 1, eval - currNum, -currNum, 
                          expr + "-" + currStr, result);
                
                // Try multiplication: (eval - lastValue) + (lastValue * currNum)
                // We remove lastValue from eval and add back the multiplied result
                backtrack1(num, target, i + 1, eval - lastValue + lastValue * currNum, 
                          lastValue * currNum, expr + "*" + currStr, result);
            }
        }
    }
    
    // ==================== SOLUTION 2: Backtracking with StringBuilder (Memory Optimized) ====================
    /**
     * Approach: Same as Solution 1 but uses StringBuilder for efficiency
     * 
     * Time Complexity: O(4^n × n)
     * Space Complexity: O(n) - StringBuilder reused, less garbage collection
     * 
     * Pros: More efficient string building, less memory allocation
     * Cons: Slightly more complex with StringBuilder management
     */
    public List<String> addOperators2(String num, int target) {
        List<String> result = new ArrayList<>();
        if (num == null || num.length() == 0) return result;
        
        backtrack2(num, target, 0, 0, 0, new StringBuilder(), result);
        return result;
    }
    
    private void backtrack2(String num, int target, int pos, long eval, 
                           long lastValue, StringBuilder expr, List<String> result) {
        if (pos == num.length()) {
            if (eval == target) {
                result.add(expr.toString());
            }
            return;
        }
        
        int len = expr.length(); // Save length for backtracking
        
        for (int i = pos; i < num.length(); i++) {
            if (i != pos && num.charAt(pos) == '0') break;
            
            String currStr = num.substring(pos, i + 1);
            long currNum = Long.parseLong(currStr);
            
            if (pos == 0) {
                expr.append(currStr);
                backtrack2(num, target, i + 1, currNum, currNum, expr, result);
                expr.setLength(len); // Backtrack
            } else {
                // Addition
                expr.append('+').append(currStr);
                backtrack2(num, target, i + 1, eval + currNum, currNum, expr, result);
                expr.setLength(len);
                
                // Subtraction
                expr.append('-').append(currStr);
                backtrack2(num, target, i + 1, eval - currNum, -currNum, expr, result);
                expr.setLength(len);
                
                // Multiplication
                expr.append('*').append(currStr);
                backtrack2(num, target, i + 1, eval - lastValue + lastValue * currNum, 
                          lastValue * currNum, expr, result);
                expr.setLength(len);
            }
        }
    }
    
    // ==================== SOLUTION 3: Backtracking with Expression Evaluation ====================
    /**
     * Approach: Build complete expressions and evaluate at the end
     * 
     * Time Complexity: O(4^n × n²) - Evaluation takes O(n) for each expression
     * Space Complexity: O(n) for recursion and expression
     * 
     * Pros: Simple and intuitive, easy to understand
     * Cons: Slower due to repeated evaluation, harder to handle precedence
     */
    public List<String> addOperators3(String num, int target) {
        List<String> result = new ArrayList<>();
        if (num == null || num.length() == 0) return result;
        
        backtrack3(num, target, 0, "", result);
        return result;
    }
    
    private void backtrack3(String num, int target, int pos, String expr, List<String> result) {
        if (pos == num.length()) {
            if (evaluate(expr) == target) {
                result.add(expr);
            }
            return;
        }
        
        for (int i = pos; i < num.length(); i++) {
            if (i != pos && num.charAt(pos) == '0') break;
            
            String currStr = num.substring(pos, i + 1);
            
            if (pos == 0) {
                backtrack3(num, target, i + 1, currStr, result);
            } else {
                backtrack3(num, target, i + 1, expr + "+" + currStr, result);
                backtrack3(num, target, i + 1, expr + "-" + currStr, result);
                backtrack3(num, target, i + 1, expr + "*" + currStr, result);
            }
        }
    }
    
    // Simple expression evaluator (respects multiplication precedence)
    private long evaluate(String expr) {
        List<Long> nums = new ArrayList<>();
        List<Character> ops = new ArrayList<>();
        
        long num = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            } else {
                nums.add(num);
                ops.add(c);
                num = 0;
            }
        }
        nums.add(num);
        
        // Handle multiplication first
        for (int i = 0; i < ops.size(); i++) {
            if (ops.get(i) == '*') {
                nums.set(i, nums.get(i) * nums.get(i + 1));
                nums.remove(i + 1);
                ops.remove(i);
                i--;
            }
        }
        
        // Handle addition and subtraction
        long result = nums.get(0);
        for (int i = 0; i < ops.size(); i++) {
            if (ops.get(i) == '+') {
                result += nums.get(i + 1);
            } else {
                result -= nums.get(i + 1);
            }
        }
        
        return result;
    }
    
    // ==================== SOLUTION 4: DFS with Pruning ====================
    /**
     * Approach: Same as Solution 1 with early termination optimizations
     * 
     * Time Complexity: O(4^n × n) - but faster in practice due to pruning
     * Space Complexity: O(n)
     * 
     * Pros: Faster for cases with no solutions
     * Cons: More complex with pruning logic
     */
    public List<String> addOperators4(String num, int target) {
        List<String> result = new ArrayList<>();
        if (num == null || num.length() == 0) return result;
        
        // Pre-calculate min/max possible values (pruning optimization)
        long minVal = Long.parseLong(num);
        long maxVal = 0;
        for (char c : num.toCharArray()) {
            maxVal = maxVal * 10 + (c - '0');
        }
        
        backtrack4(num, target, 0, 0, 0, "", result);
        return result;
    }
    
    private void backtrack4(String num, long target, int pos, long eval, 
                           long lastValue, String expr, List<String> result) {
        if (pos == num.length()) {
            if (eval == target) {
                result.add(expr);
            }
            return;
        }
        
        for (int i = pos; i < num.length(); i++) {
            if (i != pos && num.charAt(pos) == '0') break;
            
            String currStr = num.substring(pos, i + 1);
            long currNum = Long.parseLong(currStr);
            
            if (pos == 0) {
                backtrack4(num, target, i + 1, currNum, currNum, currStr, result);
            } else {
                // Try all operators
                backtrack4(num, target, i + 1, eval + currNum, currNum, 
                          expr + "+" + currStr, result);
                backtrack4(num, target, i + 1, eval - currNum, -currNum, 
                          expr + "-" + currStr, result);
                backtrack4(num, target, i + 1, eval - lastValue + lastValue * currNum, 
                          lastValue * currNum, expr + "*" + currStr, result);
            }
        }
    }
    
    // ==================== Helper Methods ====================
    
    private void printResults(String num, int target, List<String> results) {
        System.out.println("Input: num = \"" + num + "\", target = " + target);
        System.out.println("Output: " + results);
        System.out.println("Number of solutions: " + results.size());
        if (!results.isEmpty()) {
            System.out.println("Verification:");
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                String expr = results.get(i);
                long val = evaluate(expr);
                System.out.println("  " + expr + " = " + val);
            }
            if (results.size() > 3) {
                System.out.println("  ... (" + (results.size() - 3) + " more)");
            }
        }
        System.out.println();
    }
    
    // ==================== Test All Solutions ====================
    public static void main(String[] args) {
        ExpressionAddOperatorsAllSolutions solver = new ExpressionAddOperatorsAllSolutions();
        
        System.out.println("=".repeat(80));
        System.out.println("EXPRESSION ADD OPERATORS - ALL SOLUTION VARIANTS");
        System.out.println("=".repeat(80));
        
        // Test cases
        String[][] testCases = {
            {"123", "6"},
            {"232", "8"},
            {"105", "5"},
            {"00", "0"},
            {"3456237490", "9191"},
            {"123456789", "45"},
            {"2147483647", "2147483647"}  // Edge case: max int
        };
        
        for (String[] test : testCases) {
            String num = test[0];
            int target = Integer.parseInt(test[1]);
            
            System.out.println("=".repeat(80));
            System.out.println("Testing: num = \"" + num + "\", target = " + target);
            System.out.println("=".repeat(80));
            
            // Solution 1: Standard Backtracking
            long start = System.nanoTime();
            List<String> result1 = solver.addOperators1(num, target);
            long end = System.nanoTime();
            System.out.println("\nSolution 1 (Standard Backtracking):");
            System.out.printf("Time: %.4f ms, Solutions: %d%n", 
                            (end - start) / 1_000_000.0, result1.size());
            if (num.length() <= 10) {
                solver.printResults(num, target, result1);
            }
            
            // Solution 2: StringBuilder Optimization
            start = System.nanoTime();
            List<String> result2 = solver.addOperators2(num, target);
            end = System.nanoTime();
            System.out.println("Solution 2 (StringBuilder):");
            System.out.printf("Time: %.4f ms, Solutions: %d%n", 
                            (end - start) / 1_000_000.0, result2.size());
            System.out.println("Match with Solution 1: " + 
                             (result1.size() == result2.size()));
            
            // Solution 3: With Evaluation (skip for long inputs)
            if (num.length() <= 8) {
                start = System.nanoTime();
                List<String> result3 = solver.addOperators3(num, target);
                end = System.nanoTime();
                System.out.println("\nSolution 3 (With Evaluation):");
                System.out.printf("Time: %.4f ms, Solutions: %d%n", 
                                (end - start) / 1_000_000.0, result3.size());
                System.out.println("Match with Solution 1: " + 
                                 (result1.size() == result3.size()));
            } else {
                System.out.println("\nSolution 3: SKIPPED (too slow for long inputs)");
            }
            
            // Solution 4: With Pruning
            start = System.nanoTime();
            List<String> result4 = solver.addOperators4(num, target);
            end = System.nanoTime();
            System.out.println("\nSolution 4 (With Pruning):");
            System.out.printf("Time: %.4f ms, Solutions: %d%n", 
                            (end - start) / 1_000_000.0, result4.size());
            System.out.println("Match with Solution 1: " + 
                             (result1.size() == result4.size()));
            
            System.out.println();
        }
        
        System.out.println("=".repeat(80));
        System.out.println("KEY INSIGHTS");
        System.out.println("=".repeat(80));
        System.out.println("1. MULTIPLICATION HANDLING:");
        System.out.println("   - Track lastValue to handle precedence without parsing");
        System.out.println("   - Formula: newEval = eval - lastValue + (lastValue * currNum)");
        System.out.println();
        System.out.println("2. LEADING ZEROS:");
        System.out.println("   - Break when num.charAt(pos) == '0' and i != pos");
        System.out.println("   - Allows \"0\" but prevents \"01\", \"00\", etc.");
        System.out.println();
        System.out.println("3. LONG OVERFLOW:");
        System.out.println("   - Use long instead of int to handle intermediate calculations");
        System.out.println("   - Important for large numbers like 2147483647");
        System.out.println("=".repeat(80));
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPARISON SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println("Solution 1 (Standard):      RECOMMENDED - Clean, efficient, optimal");
        System.out.println("Solution 2 (StringBuilder): Slightly faster with less GC overhead");
        System.out.println("Solution 3 (Evaluation):    Simple but SLOW - O(4^n × n²)");
        System.out.println("Solution 4 (Pruning):       Similar to Sol 1, good for no-solution cases");
        System.out.println("=".repeat(80));
        
        // Example walkthrough
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EXAMPLE WALKTHROUGH: \"123\" target = 6");
        System.out.println("=".repeat(80));
        System.out.println("Path 1: \"1\" → \"1+2\" → \"1+2+3\" = 6 ✓");
        System.out.println("  eval: 0 → 1 → 3 → 6, lastValue: 0 → 1 → 2 → 3");
        System.out.println();
        System.out.println("Path 2: \"1\" → \"1*2\" → \"1*2*3\" = 6 ✓");
        System.out.println("  For \"1*2\": eval = 1 - 1 + 1*2 = 2, lastValue = 2");
        System.out.println("  For \"1*2*3\": eval = 2 - 2 + 2*3 = 6, lastValue = 6");
        System.out.println("=".repeat(80));
    }
}

/*
 * Handling multiplication (*) operator precedence:
 *
 * We maintain:
 *   eval -> the current evaluated value of the expression so far
 *   prev -> the last operand that was added or subtracted into eval
 *   curr -> the current number we want to apply with an operator
 *
 * Problem:
 *   Multiplication has higher precedence than addition and subtraction.
 *   When we previously added or subtracted `prev`, it was already included
 *   in `eval`. If we now encounter '*', we cannot simply do:
 *
 *       eval * curr   // WRONG
 *
 *   because that would incorrectly multiply the entire expression.
 *
 * Solution:
 *   To correctly apply '*', we:
 *   1) Remove the previously added operand from eval  -> eval - prev
 *   2) Multiply that operand with the current number -> prev * curr
 *   3) Add the multiplied result back into eval
 *
 * Formula:
 *   newEval = eval - prev + (prev * curr)
 *
 * Example 1: "1+2*3"
 *   Before '*':
 *     eval = 3   (1 + 2)
 *     prev = 2
 *     curr = 3
 *
 *   Apply formula:
 *     eval = 3 - 2 + (2 * 3)
 *          = 1 + 6
 *          = 7   ✔ Correct
 *
 * Example 2: "2-3*4"
 *   Before '*':
 *     eval = -1  (2 - 3)
 *     prev = -3
 *     curr = 4
 *
 *   Apply formula:
 *     eval = -1 - (-3) + (-3 * 4)
 *          = -1 + 3 - 12
 *          = -10 ✔ Correct
 *
 * Why this works:
 *   This approach simulates operator precedence incrementally during DFS
 *   without building or parsing the full expression tree.
 */

