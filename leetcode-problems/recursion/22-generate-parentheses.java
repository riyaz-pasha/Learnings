import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class ParenthesesGenerator {
    
    // Approach 1: Backtracking - OPTIMAL
    // Time: O(4^n / sqrt(n)) [Catalan number], Space: O(n) recursion depth
    public List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();
        backtrack(result, new StringBuilder(), 0, 0, n);
        return result;
    }
    
    private void backtrack(List<String> result, StringBuilder current, 
                          int open, int close, int max) {
        // Base case: if string is complete
        if (current.length() == max * 2) {
            result.add(current.toString());
            return;
        }
        
        // Add '(' if we haven't used all opening parentheses
        if (open < max) {
            current.append('(');
            backtrack(result, current, open + 1, close, max);
            current.deleteCharAt(current.length() - 1);  // Backtrack
        }
        
        // Add ')' only if it doesn't exceed number of '('
        if (close < open) {
            current.append(')');
            backtrack(result, current, open, close + 1, max);
            current.deleteCharAt(current.length() - 1);  // Backtrack
        }
    }
    
    // Approach 2: Backtracking with String (simpler but less efficient)
    // Time: O(4^n / sqrt(n)), Space: O(n)
    public List<String> generateParenthesisWithString(int n) {
        List<String> result = new ArrayList<>();
        backtrackString(result, "", 0, 0, n);
        return result;
    }
    
    private void backtrackString(List<String> result, String current, 
                                int open, int close, int max) {
        if (current.length() == max * 2) {
            result.add(current);
            return;
        }
        
        if (open < max) {
            backtrackString(result, current + "(", open + 1, close, max);
        }
        
        if (close < open) {
            backtrackString(result, current + ")", open, close + 1, max);
        }
    }
    
    // Approach 3: Dynamic Programming
    // Time: O(4^n / sqrt(n)), Space: O(4^n / sqrt(n))
    public List<String> generateParenthesisDP(int n) {
        List<List<String>> dp = new ArrayList<>();
        
        // Base case: 0 pairs
        List<String> base = new ArrayList<>();
        base.add("");
        dp.add(base);
        
        // Build up from 1 to n pairs
        for (int i = 1; i <= n; i++) {
            List<String> current = new ArrayList<>();
            
            // For each way to split i pairs into two groups
            for (int j = 0; j < i; j++) {
                // Left side has j pairs, right side has i-1-j pairs
                List<String> left = dp.get(j);
                List<String> right = dp.get(i - 1 - j);
                
                for (String l : left) {
                    for (String r : right) {
                        // Wrap left in parentheses and append right
                        current.add("(" + l + ")" + r);
                    }
                }
            }
            
            dp.add(current);
        }
        
        return dp.get(n);
    }
    
    // Approach 4: Iterative BFS
    // Time: O(4^n / sqrt(n)), Space: O(4^n / sqrt(n))
    public List<String> generateParenthesisIterative(int n) {
        List<String> result = new ArrayList<>();
        if (n == 0) return result;
        
        Queue<ParenthesesState> queue = new LinkedList<>();
        queue.offer(new ParenthesesState("", 0, 0));
        
        while (!queue.isEmpty()) {
            ParenthesesState state = queue.poll();
            
            // If we've built a complete string
            if (state.current.length() == n * 2) {
                result.add(state.current);
                continue;
            }
            
            // Try adding '('
            if (state.open < n) {
                queue.offer(new ParenthesesState(
                    state.current + "(", state.open + 1, state.close));
            }
            
            // Try adding ')'
            if (state.close < state.open) {
                queue.offer(new ParenthesesState(
                    state.current + ")", state.open, state.close + 1));
            }
        }
        
        return result;
    }
    
    // Helper class for iterative approach
    static class ParenthesesState {
        String current;
        int open;
        int close;
        
        ParenthesesState(String current, int open, int close) {
            this.current = current;
            this.open = open;
            this.close = close;
        }
    }
    
    // Helper method to count Catalan number
    // The number of valid parentheses combinations is the nth Catalan number
    public long catalanNumber(int n) {
        if (n <= 1) return 1;
        
        long[] catalan = new long[n + 1];
        catalan[0] = catalan[1] = 1;
        
        for (int i = 2; i <= n; i++) {
            for (int j = 0; j < i; j++) {
                catalan[i] += catalan[j] * catalan[i - 1 - j];
            }
        }
        
        return catalan[n];
    }
}

// Test and demonstration class
class ParenthesesTester {
    
    public static void main(String[] args) {
        ParenthesesGenerator generator = new ParenthesesGenerator();
        
        System.out.println("=== Generate Well-Formed Parentheses ===\n");
        
        // Example 1: n = 3
        System.out.println("Example 1: n = 3");
        List<String> result1 = generator.generateParenthesis(3);
        System.out.println("Output: " + result1);
        System.out.println("Count: " + result1.size());
        System.out.println("Expected: 5 combinations (Catalan number C(3))\n");
        
        // Example 2: n = 1
        System.out.println("Example 2: n = 1");
        List<String> result2 = generator.generateParenthesis(1);
        System.out.println("Output: " + result2);
        System.out.println("Count: " + result2.size());
        System.out.println();
        
        // Example 3: n = 2
        System.out.println("Example 3: n = 2");
        List<String> result3 = generator.generateParenthesis(2);
        System.out.println("Output: " + result3);
        System.out.println("Count: " + result3.size());
        System.out.println("Explanation: \"(())\", \"()()\" are the only valid combinations\n");
        
        // Example 4: n = 4
        System.out.println("Example 4: n = 4");
        List<String> result4 = generator.generateParenthesis(4);
        System.out.println("Output: " + result4);
        System.out.println("Count: " + result4.size());
        System.out.println();
        
        System.out.println("=== Catalan Numbers ===\n");
        
        System.out.println("The count of valid parentheses follows Catalan numbers!");
        System.out.println();
        System.out.println("n | Catalan(n) | Formula");
        System.out.println("--|------------|--------");
        for (int i = 0; i <= 8; i++) {
            long catalan = generator.catalanNumber(i);
            List<String> combos = generator.generateParenthesis(i);
            System.out.printf("%d | %10d | C(%d) = %d\n", i, catalan, i, combos.size());
        }
        
        System.out.println("\nSequence: 1, 1, 2, 5, 14, 42, 132, 429, 1430, ...");
        System.out.println("Formula: C(n) = (2n)! / ((n+1)! * n!)");
        System.out.println("Recursive: C(n) = Σ C(i) * C(n-1-i) for i = 0 to n-1");
        
        System.out.println("\n=== Testing Different Approaches ===\n");
        
        int testN = 3;
        System.out.println("Testing all approaches with n = " + testN + ":\n");
        
        List<String> res1 = generator.generateParenthesis(testN);
        System.out.println("Backtracking (StringBuilder): " + res1);
        
        List<String> res2 = generator.generateParenthesisWithString(testN);
        System.out.println("Backtracking (String):         " + res2);
        
        List<String> res3 = generator.generateParenthesisDP(testN);
        System.out.println("Dynamic Programming:           " + res3);
        
        List<String> res4 = generator.generateParenthesisIterative(testN);
        System.out.println("Iterative BFS:                 " + res4);
        
        System.out.println("\n=== Algorithm Explanation ===\n");
        
        System.out.println("Approach 1: Backtracking ⭐ OPTIMAL");
        System.out.println("Time: O(4^n / sqrt(n)), Space: O(n) recursion depth");
        System.out.println();
        System.out.println("Key constraints:");
        System.out.println("  1. Can add '(' if open < n");
        System.out.println("  2. Can add ')' if close < open");
        System.out.println();
        System.out.println("Why these rules work:");
        System.out.println("  • Rule 1 ensures we don't use too many '('");
        System.out.println("  • Rule 2 ensures ')' never exceeds '(' (keeps valid)");
        System.out.println();
        System.out.println("Decision tree for n=2:");
        System.out.println();
        System.out.println("                    ''");
        System.out.println("                    |");
        System.out.println("                    (          [open=1, close=0]");
        System.out.println("                  /   \\");
        System.out.println("                ((     ()       [open=2/1, close=0/1]");
        System.out.println("                |      |");
        System.out.println("               (()    ()(       [open=2, close=1/2]");
        System.out.println("                |      |");
        System.out.println("              (())   ()()       [open=2, close=2] ✓");
        System.out.println();
        System.out.println("Valid: \"(())\", \"()()\"");
        System.out.println();
        
        System.out.println("Example trace for n=2:");
        System.out.println("  Step 1: \"\" → add '(' → \"(\"     [open=1, close=0]");
        System.out.println("  Step 2: \"(\" → add '(' → \"((\"    [open=2, close=0]");
        System.out.println("  Step 3: \"((\" → add ')' → \"(()\"   [open=2, close=1]");
        System.out.println("  Step 4: \"(()\" → add ')' → \"(())\"  [open=2, close=2] ✓ VALID");
        System.out.println("  Backtrack to step 2...");
        System.out.println("  Step 5: \"(\" → add ')' → \"()\"    [open=1, close=1]");
        System.out.println("  Step 6: \"()\" → add '(' → \"()(\"   [open=2, close=1]");
        System.out.println("  Step 7: \"()(\" → add ')' → \"()()\"  [open=2, close=2] ✓ VALID");
        System.out.println();
        
        System.out.println("---\n");
        
        System.out.println("Approach 2: Dynamic Programming");
        System.out.println("Time: O(4^n / sqrt(n)), Space: O(4^n / sqrt(n))");
        System.out.println();
        System.out.println("Idea: Build combinations for n pairs using smaller solutions");
        System.out.println();
        System.out.println("For n pairs, partition into:");
        System.out.println("  \"(\" + [j pairs inside] + \")\" + [remaining pairs outside]");
        System.out.println();
        System.out.println("Example for n=3:");
        System.out.println("  j=0: \"(\" + \"\" + \")\" + [2 pairs] = \"()((...))\", \"()(())\", \"()()()\"");
        System.out.println("  j=1: \"(\" + [1 pair] + \")\" + [1 pair] = \"(())((...))\", \"(())()\"");
        System.out.println("  j=2: \"(\" + [2 pairs] + \")\" + \"\" = \"((()))\", \"(()())\", \"(())()\"");
        System.out.println();
        System.out.println("This gives us the Catalan recurrence!");
        System.out.println();
        
        System.out.println("---\n");
        
        System.out.println("Catalan Numbers in Computer Science:");
        System.out.println("------------------------------------");
        System.out.println("The nth Catalan number appears in many problems:");
        System.out.println();
        System.out.println("  • Valid parentheses combinations");
        System.out.println("  • Number of binary search trees with n nodes");
        System.out.println("  • Number of ways to triangulate a polygon");
        System.out.println("  • Number of paths in a grid (not crossing diagonal)");
        System.out.println("  • Number of ways to associate n applications of binary operator");
        System.out.println();
        
        System.out.println("---\n");
        
        System.out.println("Complexity Analysis:");
        System.out.println("-------------------");
        System.out.println("Time Complexity: O(4^n / sqrt(n))");
        System.out.println("  - This is the nth Catalan number");
        System.out.println("  - We generate each valid string exactly once");
        System.out.println("  - Each string takes O(n) time to build");
        System.out.println();
        System.out.println("Space Complexity:");
        System.out.println("  - Backtracking: O(n) for recursion stack");
        System.out.println("  - DP/Iterative: O(4^n / sqrt(n)) to store all results");
        System.out.println();
        
        System.out.println("Why Backtracking is Best:");
        System.out.println("-------------------------");
        System.out.println("✓ Minimal space usage O(n)");
        System.out.println("✓ Generates only valid combinations (no filtering)");
        System.out.println("✓ Easy to understand and implement");
        System.out.println("✓ Naturally prunes invalid branches");
        
        System.out.println("\n=== Performance Test ===\n");
        
        for (int n = 1; n <= 8; n++) {
            long start = System.nanoTime();
            List<String> result = generator.generateParenthesis(n);
            long end = System.nanoTime();
            
            System.out.printf("n=%d: %4d combinations in %6.2f ms\n", 
                            n, result.size(), (end - start) / 1_000_000.0);
        }
        
        System.out.println("\n=== Visual Examples ===\n");
        
        System.out.println("n=1: ()");
        System.out.println();
        System.out.println("n=2: (())  ()()");
        System.out.println();
        System.out.println("n=3: ((()))  (()())  (())()  ()(())  ()()()");
        System.out.println();
        System.out.println("Pattern: Each level adds one more pair of parentheses");
        System.out.println("Count follows: 1, 2, 5, 14, 42, 132, 429, 1430...");
    }
}
