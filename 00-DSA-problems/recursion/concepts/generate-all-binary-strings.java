import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class BinaryStringGenerator {
    
    // Approach 1: Backtracking (Recursive) - OPTIMAL for generating strings
    // Time: O(2^n), Space: O(n) recursion depth
    public List<String> generateBinaryStrings(int n) {
        List<String> result = new ArrayList<>();
        if (n <= 0) {
            return result;
        }
        
        backtrack(new StringBuilder(), n, result);
        return result;
    }
    
    private void backtrack(StringBuilder current, int remaining, List<String> result) {
        // Base case: string is complete
        if (remaining == 0) {
            result.add(current.toString());
            return;
        }
        
        // Always can add '0'
        current.append('0');
        backtrack(current, remaining - 1, result);
        current.deleteCharAt(current.length() - 1);  // Backtrack
        
        // Can add '1' only if last character is not '1'
        if (current.length() == 0 || current.charAt(current.length() - 1) != '1') {
            current.append('1');
            backtrack(current, remaining - 1, result);
            current.deleteCharAt(current.length() - 1);  // Backtrack
        }
    }
    
    // Approach 2: Iterative BFS-style generation
    // Time: O(2^n), Space: O(2^n) for queue
    public List<String> generateBinaryStringsIterative(int n) {
        List<String> result = new ArrayList<>();
        if (n <= 0) {
            return result;
        }
        
        Queue<String> queue = new LinkedList<>();
        queue.offer("");
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            // If we've built a complete string
            if (current.length() == n) {
                result.add(current);
                continue;
            }
            
            // Always can add '0'
            queue.offer(current + '0');
            
            // Can add '1' only if last character is not '1'
            if (current.isEmpty() || current.charAt(current.length() - 1) != '1') {
                queue.offer(current + '1');
            }
        }
        
        return result;
    }
    
    // Approach 3: Dynamic Programming (Count only)
    // Counts how many valid strings exist (Fibonacci sequence!)
    // Time: O(n), Space: O(1)
    public int countBinaryStrings(int n) {
        if (n == 0) return 0;
        if (n == 1) return 2;
        
        // dp[i][0] = count of valid strings of length i ending in '0'
        // dp[i][1] = count of valid strings of length i ending in '1'
        
        int endsWith0 = 1;  // For n=1: "0"
        int endsWith1 = 1;  // For n=1: "1"
        
        for (int i = 2; i <= n; i++) {
            int newEndsWith0 = endsWith0 + endsWith1;  // Can append '0' to anything
            int newEndsWith1 = endsWith0;               // Can append '1' only after '0'
            
            endsWith0 = newEndsWith0;
            endsWith1 = newEndsWith1;
        }
        
        return endsWith0 + endsWith1;
    }
    
    // Approach 4: Using bit manipulation (less intuitive but interesting)
    // Generate all possible n-bit numbers and filter valid ones
    // Time: O(2^n), Space: O(2^n)
    public List<String> generateBinaryStringsBitManipulation(int n) {
        List<String> result = new ArrayList<>();
        if (n <= 0) {
            return result;
        }
        
        int totalNumbers = 1 << n;  // 2^n
        
        for (int i = 0; i < totalNumbers; i++) {
            String binary = String.format("%" + n + "s", Integer.toBinaryString(i))
                                  .replace(' ', '0');
            
            if (isValid(binary)) {
                result.add(binary);
            }
        }
        
        return result;
    }
    
    // Check if binary string has no consecutive 1s
    private boolean isValid(String s) {
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '1' && s.charAt(i + 1) == '1') {
                return false;
            }
        }
        return true;
    }
}

// Test and demonstration class
class BinaryStringTester {
    
    public static void main(String[] args) {
        BinaryStringGenerator generator = new BinaryStringGenerator();
        
        System.out.println("=== Binary Strings Without Consecutive 1s ===\n");
        
        // Example 1: n = 1
        System.out.println("Example 1: n = 1");
        List<String> result1 = generator.generateBinaryStrings(1);
        System.out.println("Output: " + result1);
        System.out.println("Count: " + result1.size());
        System.out.println("Explanation: Both \"0\" and \"1\" are valid (no consecutive 1s)\n");
        
        // Example 2: n = 2
        System.out.println("Example 2: n = 2");
        List<String> result2 = generator.generateBinaryStrings(2);
        System.out.println("Output: " + result2);
        System.out.println("Count: " + result2.size());
        System.out.println("Explanation: \"00\", \"01\", \"10\" are valid. \"11\" is invalid.\n");
        
        // Example 3: n = 3
        System.out.println("Example 3: n = 3");
        List<String> result3 = generator.generateBinaryStrings(3);
        System.out.println("Output: " + result3);
        System.out.println("Count: " + result3.size());
        System.out.println("Explanation:");
        System.out.println("  Valid:   000, 001, 010, 100, 101");
        System.out.println("  Invalid: 011 (consecutive 1s), 110 (consecutive 1s), 111 (consecutive 1s)\n");
        
        // Example 4: n = 4
        System.out.println("Example 4: n = 4");
        List<String> result4 = generator.generateBinaryStrings(4);
        System.out.println("Output: " + result4);
        System.out.println("Count: " + result4.size());
        System.out.println();
        
        // Example 5: n = 5
        System.out.println("Example 5: n = 5");
        List<String> result5 = generator.generateBinaryStrings(5);
        System.out.println("First 10: " + result5.subList(0, Math.min(10, result5.size())));
        System.out.println("Count: " + result5.size());
        System.out.println();
        
        System.out.println("=== Testing Different Approaches ===\n");
        
        int testN = 4;
        System.out.println("Testing all approaches with n = " + testN + ":\n");
        
        List<String> res1 = generator.generateBinaryStrings(testN);
        System.out.println("Backtracking (Recursive): " + res1);
        System.out.println("Count: " + res1.size() + "\n");
        
        List<String> res2 = generator.generateBinaryStringsIterative(testN);
        System.out.println("Iterative (BFS): " + res2);
        System.out.println("Count: " + res2.size() + "\n");
        
        List<String> res3 = generator.generateBinaryStringsBitManipulation(testN);
        System.out.println("Bit Manipulation: " + res3);
        System.out.println("Count: " + res3.size() + "\n");
        
        int count = generator.countBinaryStrings(testN);
        System.out.println("DP Count Only: " + count + "\n");
        
        System.out.println("=== Pattern Analysis ===\n");
        
        System.out.println("Count of valid strings follows Fibonacci sequence!");
        System.out.println();
        System.out.println("n | Count | Explanation");
        System.out.println("--|-------|------------");
        for (int i = 1; i <= 10; i++) {
            int cnt = generator.countBinaryStrings(i);
            System.out.printf("%2d | %5d | ", i, cnt);
            
            if (i == 1) System.out.println("Base case: '0', '1'");
            else if (i == 2) System.out.println("Base case: '00', '01', '10'");
            else System.out.println("F(n) = F(n-1) + F(n-2)");
        }
        
        System.out.println("\nThis is the Fibonacci sequence (starting from 2, 3)!");
        
        System.out.println("\n=== Algorithm Explanation ===\n");
        
        System.out.println("Approach 1: Backtracking (Recursive) ⭐");
        System.out.println("Time: O(2^n), Space: O(n) recursion depth");
        System.out.println();
        System.out.println("Key idea:");
        System.out.println("  - At each position, try adding '0' or '1'");
        System.out.println("  - Can always add '0'");
        System.out.println("  - Can only add '1' if previous character is not '1'");
        System.out.println("  - Build string character by character");
        System.out.println();
        System.out.println("Decision tree for n=3:");
        System.out.println();
        System.out.println("                    ''");
        System.out.println("                  /    \\");
        System.out.println("                 0      1");
        System.out.println("               /  \\    /  \\");
        System.out.println("              00  01  10  (11) ✗");
        System.out.println("             / \\ / \\ / \\");
        System.out.println("           000 001 010 (011)✗ 100 101");
        System.out.println();
        System.out.println("Valid strings: 000, 001, 010, 100, 101");
        System.out.println();
        
        System.out.println("---\n");
        
        System.out.println("Why Fibonacci?");
        System.out.println("-------------");
        System.out.println("Let f(n) = number of valid strings of length n");
        System.out.println();
        System.out.println("For a string of length n:");
        System.out.println("  Case 1: Ends with '0' → previous n-1 chars can be any valid string");
        System.out.println("          Count = f(n-1)");
        System.out.println();
        System.out.println("  Case 2: Ends with '1' → must be preceded by '0'");
        System.out.println("          So it's '...01' where '...' is valid string of length n-2");
        System.out.println("          Count = f(n-2)");
        System.out.println();
        System.out.println("Therefore: f(n) = f(n-1) + f(n-2)");
        System.out.println();
        System.out.println("Base cases:");
        System.out.println("  f(1) = 2  ['0', '1']");
        System.out.println("  f(2) = 3  ['00', '01', '10']");
        System.out.println();
        System.out.println("Sequence: 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, ...");
        System.out.println();
        
        System.out.println("---\n");
        
        System.out.println("Lexicographic Order:");
        System.out.println("-------------------");
        System.out.println("The backtracking approach naturally generates strings in");
        System.out.println("lexicographic order because:");
        System.out.println("  1. We always try '0' before '1' at each position");
        System.out.println("  2. We build from left to right");
        System.out.println();
        System.out.println("Example for n=3:");
        System.out.println("  000 < 001 < 010 < 100 < 101");
        System.out.println("  (alphabetically sorted)");
        System.out.println();
        
        System.out.println("---\n");
        
        System.out.println("Complexity Summary:");
        System.out.println("------------------");
        System.out.println("Backtracking:       Time O(2^n), Space O(n) - Best for generation");
        System.out.println("Iterative BFS:      Time O(2^n), Space O(2^n) - Memory intensive");
        System.out.println("Bit Manipulation:   Time O(2^n), Space O(2^n) - Brute force filter");
        System.out.println("DP (count only):    Time O(n), Space O(1) - Only counting");
        System.out.println();
        
        System.out.println("Use Cases:");
        System.out.println("---------");
        System.out.println("✓ Generate all strings: Use Backtracking");
        System.out.println("✓ Count only: Use DP (Fibonacci)");
        System.out.println("✓ Check if specific string valid: Use simple iteration");
        
        System.out.println("\n=== Performance Test ===\n");
        
        // Performance comparison for n=15
        int perfN = 15;
        System.out.println("Testing with n = " + perfN + ":\n");
        
        long start, end;
        
        start = System.nanoTime();
        List<String> perfResult = generator.generateBinaryStrings(perfN);
        end = System.nanoTime();
        System.out.println("Backtracking: " + perfResult.size() + " strings in " + 
                          (end - start) / 1_000_000 + " ms");
        
        start = System.nanoTime();
        int perfCount = generator.countBinaryStrings(perfN);
        end = System.nanoTime();
        System.out.println("DP Count:     " + perfCount + " strings in " + 
                          (end - start) / 1_000 + " microseconds");
        
        System.out.println("\nNote: DP is ~1000x faster when only counting!");
    }
    
}
