/**
 * Remove K Digits - Complete Solutions
 * 
 * Problem: Given a string num representing a non-negative integer and an integer k,
 * return the smallest possible integer after removing k digits from num.
 * 
 * Key Insights:
 * 1. To get smallest number, remove larger digits that appear before smaller digits
 * 2. Use monotonic increasing stack to build the result
 * 3. Greedy approach: remove first digit where digit[i] > digit[i+1]
 * 4. Handle edge cases: leading zeros, removing all digits
 * 
 * Core Strategy:
 * - Build result digit by digit
 * - For each digit, remove previous larger digits (up to k removals)
 * - This creates a monotonic increasing sequence (smallest possible)
 */

import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

class RemoveKDigits {
    
    // ========================================================================
    // SOLUTION 1: MONOTONIC STACK (OPTIMAL)
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Monotonic Stack Approach
     * 
     * Strategy:
     * 1. Use stack to build result, maintaining increasing order
     * 2. For each digit:
     *    - While current digit < stack top AND k > 0: pop stack, decrement k
     *    - Push current digit
     * 3. If k > 0 after processing all digits, remove from end
     * 4. Remove leading zeros
     * 5. Return "0" if result is empty
     * 
     * Why this works:
     * - To minimize number, we want smaller digits earlier
     * - If digit[i] > digit[i+1], we should remove digit[i]
     * - Stack naturally maintains this by popping larger previous digits
     * 
     * Example: "1432219", k=3
     * Process: 1 -> 14 -> 13 (pop 4) -> 132 -> 122 (pop 3) -> 121 (pop 2) -> 1219
     * Removed: 4, 3, 2 (k=3)
     */
    public static String removeKdigits(String num, int k) {
        int n = num.length();
        
        // Edge case: remove all digits
        if (k >= n) {
            return "0";
        }
        
        Stack<Character> stack = new Stack<>();
        
        for (int i = 0; i < n; i++) {
            char digit = num.charAt(i);
            
            // Remove larger digits from stack while we can
            while (!stack.isEmpty() && k > 0 && stack.peek() > digit) {
                stack.pop();
                k--;
            }
            
            stack.push(digit);
        }
        
        // If k > 0, remove from the end (largest remaining digits)
        while (k > 0) {
            stack.pop();
            k--;
        }
        
        // Build result string from stack
        StringBuilder result = new StringBuilder();
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        result.reverse();
        
        // Remove leading zeros
        while (result.length() > 1 && result.charAt(0) == '0') {
            result.deleteCharAt(0);
        }
        
        // Handle empty result
        return result.length() == 0 ? "0" : result.toString();
    }
    
    // ========================================================================
    // SOLUTION 2: DEQUE APPROACH (MORE EFFICIENT)
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Deque-Based Solution
     * 
     * Similar to stack but uses Deque for better performance and cleaner code.
     * Deque allows us to build result without reversing.
     */
    public static String removeKdigitsDeque(String num, int k) {
        int n = num.length();
        
        if (k >= n) {
            return "0";
        }
        
        Deque<Character> deque = new LinkedList<>();
        
        for (char digit : num.toCharArray()) {
            // Remove larger digits from end
            while (!deque.isEmpty() && k > 0 && deque.peekLast() > digit) {
                deque.removeLast();
                k--;
            }
            deque.addLast(digit);
        }
        
        // Remove remaining k digits from end
        while (k > 0) {
            deque.removeLast();
            k--;
        }
        
        // Build result, skipping leading zeros
        StringBuilder result = new StringBuilder();
        boolean leadingZero = true;
        
        for (char digit : deque) {
            if (digit == '0' && leadingZero) {
                continue;
            }
            leadingZero = false;
            result.append(digit);
        }
        
        return result.length() == 0 ? "0" : result.toString();
    }
    
    // ========================================================================
    // SOLUTION 3: STRINGBUILDER APPROACH (MEMORY OPTIMIZED)
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * StringBuilder Solution
     * 
     * Uses StringBuilder as a stack-like structure.
     * More memory efficient as it's the final result container.
     */
    public static String removeKdigitsStringBuilder(String num, int k) {
        int n = num.length();
        
        if (k >= n) {
            return "0";
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (char digit : num.toCharArray()) {
            // Remove larger digits from end
            while (sb.length() > 0 && k > 0 && sb.charAt(sb.length() - 1) > digit) {
                sb.deleteCharAt(sb.length() - 1);
                k--;
            }
            sb.append(digit);
        }
        
        // Remove remaining k digits from end
        while (k > 0) {
            sb.deleteCharAt(sb.length() - 1);
            k--;
        }
        
        // Remove leading zeros
        while (sb.length() > 1 && sb.charAt(0) == '0') {
            sb.deleteCharAt(0);
        }
        
        return sb.length() == 0 ? "0" : sb.toString();
    }
    
    // ========================================================================
    // SOLUTION 4: VERBOSE SOLUTION WITH STEP-BY-STEP TRACKING
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Detailed Solution with Debug Information
     * 
     * Shows each step of the algorithm for educational purposes.
     */
    public static String removeKdigitsVerbose(String num, int k, boolean debug) {
        int n = num.length();
        
        if (debug) {
            System.out.println("\n=== Processing: num = \"" + num + "\", k = " + k + " ===");
        }
        
        if (k >= n) {
            if (debug) System.out.println("k >= n, removing all digits, result = \"0\"");
            return "0";
        }
        
        Stack<Character> stack = new Stack<>();
        int removed = 0;
        
        for (int i = 0; i < n; i++) {
            char digit = num.charAt(i);
            
            if (debug) {
                System.out.println("\nProcessing digit: " + digit);
                System.out.println("  Current stack: " + stack);
                System.out.println("  Removed so far: " + removed + "/" + k);
            }
            
            while (!stack.isEmpty() && removed < k && stack.peek() > digit) {
                char popped = stack.pop();
                removed++;
                if (debug) {
                    System.out.println("  Removing: " + popped + " (larger than " + digit + ")");
                }
            }
            
            stack.push(digit);
            if (debug) {
                System.out.println("  After push: " + stack);
            }
        }
        
        // Remove remaining from end
        while (removed < k) {
            char popped = stack.pop();
            removed++;
            if (debug) {
                System.out.println("\nRemoving from end: " + popped);
            }
        }
        
        if (debug) {
            System.out.println("\nFinal stack: " + stack);
        }
        
        StringBuilder result = new StringBuilder();
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        result.reverse();
        
        if (debug) {
            System.out.println("Before removing leading zeros: \"" + result + "\"");
        }
        
        while (result.length() > 1 && result.charAt(0) == '0') {
            result.deleteCharAt(0);
        }
        
        String finalResult = result.length() == 0 ? "0" : result.toString();
        
        if (debug) {
            System.out.println("After removing leading zeros: \"" + finalResult + "\"");
        }
        
        return finalResult;
    }
    
    // ========================================================================
    // SOLUTION 5: GREEDY APPROACH (ALTERNATIVE IMPLEMENTATION)
    // Time Complexity: O(n*k) worst case
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Greedy Approach
     * 
     * Repeatedly find and remove the first digit where digit[i] > digit[i+1].
     * Not optimal but demonstrates the greedy strategy clearly.
     */
    public static String removeKdigitsGreedy(String num, int k) {
        if (k >= num.length()) {
            return "0";
        }
        
        StringBuilder sb = new StringBuilder(num);
        
        for (int i = 0; i < k; i++) {
            int idx = 0;
            
            // Find first position where digit > next digit
            while (idx < sb.length() - 1 && sb.charAt(idx) <= sb.charAt(idx + 1)) {
                idx++;
            }
            
            // Remove the digit at idx
            sb.deleteCharAt(idx);
        }
        
        // Remove leading zeros
        while (sb.length() > 1 && sb.charAt(0) == '0') {
            sb.deleteCharAt(0);
        }
        
        return sb.length() == 0 ? "0" : sb.toString();
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Visualize the digit removal process
     */
    public static void visualizeRemoval(String num, int k) {
        System.out.println("\nVisualization for: num = \"" + num + "\", k = " + k);
        System.out.println("Original: " + num);
        System.out.println("Need to remove: " + k + " digits");
        System.out.println("Result will have: " + Math.max(0, num.length() - k) + " digits");
    }
    
    /**
     * Test a single case with all solutions
     */
    public static void testCase(String name, String num, int k) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE: " + name);
        System.out.println("=".repeat(70));
        visualizeRemoval(num, k);
        
        String result1 = removeKdigits(num, k);
        String result2 = removeKdigitsDeque(num, k);
        String result3 = removeKdigitsStringBuilder(num, k);
        String result4 = removeKdigitsGreedy(num, k);
        
        System.out.println("\nResults:");
        System.out.println("Stack Solution:        \"" + result1 + "\"");
        System.out.println("Deque Solution:        \"" + result2 + "\"");
        System.out.println("StringBuilder:         \"" + result3 + "\"");
        System.out.println("Greedy:                \"" + result4 + "\"");
        
        boolean match = result1.equals(result2) && result2.equals(result3) && result3.equals(result4);
        System.out.println("All solutions match:   " + match);
        
        // Show detailed trace
        System.out.println("\n--- Detailed Trace ---");
        removeKdigitsVerbose(num, k, true);
    }
    
    /**
     * Analyze why a particular digit is removed
     */
    public static void explainRemoval(String num, int k) {
        System.out.println("\nExplanation for: \"" + num + "\", k=" + k);
        System.out.println("\nGreedy Strategy:");
        System.out.println("• To minimize result, remove larger digits that appear before smaller ones");
        System.out.println("• Look for first position where digit[i] > digit[i+1]");
        System.out.println("• This ensures we keep smaller digits in earlier positions");
        
        System.out.println("\nDigit Analysis:");
        for (int i = 0; i < num.length() - 1; i++) {
            if (num.charAt(i) > num.charAt(i + 1)) {
                System.out.println("  Position " + i + ": " + num.charAt(i) + 
                                 " > " + num.charAt(i + 1) + 
                                 " → Good candidate for removal");
            } else {
                System.out.println("  Position " + i + ": " + num.charAt(i) + 
                                 " <= " + num.charAt(i + 1) + 
                                 " → Keep for now");
            }
        }
    }
    
    // ========================================================================
    // MAIN METHOD WITH TEST CASES
    // ========================================================================
    
    public static void main(String[] args) {
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.println("║" + " ".repeat(22) + "REMOVE K DIGITS" + " ".repeat(31) + "║");
        System.out.println("╚" + "═".repeat(68) + "╝");
        
        // Example 1
        testCase("Example 1", "1432219", 3);
        explainRemoval("1432219", 3);
        
        // Example 2
        testCase("Example 2", "10200", 1);
        
        // Example 3
        testCase("Example 3", "10", 2);
        
        // Additional test cases
        testCase("All same digits", "1111", 2);
        testCase("Increasing sequence", "123456", 3);
        testCase("Decreasing sequence", "654321", 3);
        testCase("Leading zeros", "10001", 1);
        testCase("Large number", "9876543210", 5);
        testCase("Remove one from middle", "54321", 1);
        testCase("Complex case", "112", 1);
        testCase("All zeros", "0000", 1);
        testCase("Single digit", "9", 1);
        testCase("Keep smallest", "100", 1);
        
        // Edge cases
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EDGE CASES");
        System.out.println("=".repeat(70));
        
        testCase("Remove all digits", "12345", 5);
        testCase("k = 0", "12345", 0);
        testCase("Very large k", "12345", 10);
        
        // Performance test
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PERFORMANCE TEST");
        System.out.println("=".repeat(70));
        
        StringBuilder largeNum = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeNum.append((int)(Math.random() * 10));
        }
        String testNum = largeNum.toString();
        
        long start = System.nanoTime();
        String result = removeKdigits(testNum, 5000);
        long end = System.nanoTime();
        
        System.out.println("\nInput length: 10,000 digits");
        System.out.println("k = 5,000");
        System.out.println("Result length: " + result.length());
        System.out.println("Time taken: " + (end - start) / 1_000_000.0 + " ms");
        
        // Complexity analysis
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(70));
        
        System.out.println("\n1. Monotonic Stack (RECOMMENDED):");
        System.out.println("   Time:  O(n) - each digit pushed/popped at most once");
        System.out.println("   Space: O(n) - stack space");
        System.out.println("   Best for: Production code, optimal solution");
        
        System.out.println("\n2. Deque Approach:");
        System.out.println("   Time:  O(n)");
        System.out.println("   Space: O(n)");
        System.out.println("   Best for: Clean code, no reversing needed");
        
        System.out.println("\n3. StringBuilder:");
        System.out.println("   Time:  O(n)");
        System.out.println("   Space: O(n)");
        System.out.println("   Best for: Memory efficiency");
        
        System.out.println("\n4. Greedy (Multiple Passes):");
        System.out.println("   Time:  O(n*k) worst case");
        System.out.println("   Space: O(n)");
        System.out.println("   Best for: Understanding the strategy");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("KEY INSIGHTS");
        System.out.println("=".repeat(70));
        System.out.println("• Goal: Minimize the resulting number");
        System.out.println("• Strategy: Keep smaller digits in earlier positions");
        System.out.println("• Remove digits where current > next (peaks)");
        System.out.println("• Monotonic stack maintains increasing sequence naturally");
        System.out.println("• Handle edge cases: leading zeros, empty result");
        System.out.println("• If k remains after processing, remove from end (largest digits)");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMMON PATTERNS");
        System.out.println("=".repeat(70));
        System.out.println("\n1. Decreasing sequence → Remove from left");
        System.out.println("   \"54321\", k=2 → \"321\"");
        System.out.println("\n2. Increasing sequence → Remove from right");
        System.out.println("   \"12345\", k=2 → \"123\"");
        System.out.println("\n3. Mixed → Remove peaks (local maxima)");
        System.out.println("   \"1432219\", k=3 → \"1219\"");
        System.out.println("\n4. Leading zeros → Must be removed");
        System.out.println("   \"10200\", k=1 → \"200\" (not \"0200\")");
    }
}

/**
 * ============================================================================
 * DETAILED ALGORITHM WALKTHROUGH
 * ============================================================================
 * 
 * Example: num = "1432219", k = 3
 * 
 * Goal: Find smallest number after removing 3 digits
 * 
 * Step-by-step Process:
 * 
 * Initial: num = "1432219", k = 3, stack = []
 * 
 * 1. Process '1':
 *    - Stack empty, just push
 *    - Stack: [1]
 * 
 * 2. Process '4':
 *    - 4 > 1, but we want increasing, so push
 *    - Stack: [1, 4]
 * 
 * 3. Process '3':
 *    - 3 < 4 (top), remove 4 (k=2)
 *    - Stack: [1]
 *    - Push 3
 *    - Stack: [1, 3]
 * 
 * 4. Process '2':
 *    - 2 < 3 (top), remove 3 (k=1)
 *    - Stack: [1]
 *    - Push 2
 *    - Stack: [1, 2]
 * 
 * 5. Process '2':
 *    - 2 <= 2 (top), no removal needed
 *    - But k=0, can't remove anymore
 *    - Push 2
 *    - Stack: [1, 2, 2]
 * 
 * 6. Process '1':
 *    - 1 < 2 (top), but k=0, can't remove
 *    - Push 1
 *    - Stack: [1, 2, 2, 1]
 * 
 * 7. Process '9':
 *    - Push 9
 *    - Stack: [1, 2, 2, 1, 9]
 * 
 * Wait, this gives "12219" but we want "1219"!
 * 
 * Let me recalculate:
 * 
 * 1. Process '1': Stack: [1]
 * 2. Process '4': Stack: [1, 4]
 * 3. Process '3': 3 < 4, remove 4 (k=2), Stack: [1, 3]
 * 4. Process '2': 2 < 3, remove 3 (k=1), Stack: [1, 2]
 * 5. Process '2': 2 <= 2, push, Stack: [1, 2, 2]
 * 6. Process '1': 1 < 2, but k=0, push, Stack: [1, 2, 2, 1]
 * 7. Process '9': push, Stack: [1, 2, 2, 1, 9]
 * 
 * Result would be "12219", but we removed only 2 digits (4, 3).
 * We need to remove one more!
 * 
 * After loop: k=1 remaining, remove from end: [1, 2, 2, 1]
 * 
 * Hmm, still not right. Let me trace again more carefully:
 * 
 * num = "1432219", k = 3
 * 
 * i=0, digit='1': stack=[], push '1', stack=['1']
 * i=1, digit='4': stack=['1'], '4' vs '1', push, stack=['1','4']
 * i=2, digit='3': stack=['1','4'], '3' < '4', pop '4' (k=2), stack=['1'], push '3', stack=['1','3']
 * i=3, digit='2': stack=['1','3'], '2' < '3', pop '3' (k=1), stack=['1'], push '2', stack=['1','2']
 * i=4, digit='2': stack=['1','2'], '2' <= '2', push, stack=['1','2','2']
 * i=5, digit='1': stack=['1','2','2'], '1' < '2', pop '2' (k=0), stack=['1','2'], push '1', stack=['1','2','1']
 * i=6, digit='9': stack=['1','2','1'], push '9', stack=['1','2','1','9']
 * 
 * Result: "1219" ✓
 * 
 * Removed: '4', '3', '2' (the first '2')
 * 
 * ============================================================================
 */
