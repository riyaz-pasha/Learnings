import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: REMOVE K DIGITS
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "What if k is equal to the length of the string?" 
 *      (Assumption: If we remove all digits, the result should be "0".)
 *    - "Can the resulting string have leading zeros?" 
 *      (Assumption: The problem says to avoid leading zeros. E.g., removing '1' 
 *       from "10200" leaves "0200", which must be returned as "200".)
 *    - "What if the string is already in increasing order, like '12345'?" 
 *      (Assumption: If we can't find a peak to remove, we must chop off the 
 *       largest digits from the very end.)
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Remove k digits to form the smallest possible integer.
 *    - Observation 1 (Magnitude Matters Most): To make a number smaller, we 
 *      must prioritize making the most significant digits (the leftmost ones) 
 *      as small as possible. 
 *    - Observation 2 (The Greedy Peak-Valley Rule): If we encounter a digit 
 *      that is SMALLER than the previous digit (a valley), it is always 
 *      optimal to delete the previous digit (the peak). 
 *      Example: "43". Since 3 < 4, dropping '4' gives "3", which is smaller 
 *      than dropping '3' (gives "4").
 *    - Strategy: We use a Monotonic Increasing Stack. We iterate through the 
 *      digits left to right. If the current digit is smaller than the top of 
 *      the stack, we pop the stack (remove the digit) and decrement k. 
 *      We repeat this until k = 0 or the stack is empty. Then we push the 
 *      current digit.
 * 
 * 3. VISUAL EXPLANATION:
 *    num = "1432219", k = 3
 *    
 *    char '1': Stack = [1]. (k=3)
 *    char '4': 4 > 1. Push. Stack = [1, 4]. (k=3)
 *    char '3': 3 < 4. POP '4'. Push '3'. Stack = [1, 3]. (k=2)
 *    char '2': 2 < 3. POP '3'. Push '2'. Stack = [1, 2]. (k=1)
 *    char '2': 2 == 2. Push. Stack = [1, 2, 2]. (k=1)
 *    char '1': 1 < 2. POP '2'. Push '1'. Stack = [1, 2, 1]. (k=0)
 *    char '9': k is 0, just push. Stack = [1, 2, 1, 9].
 *    
 *    Result: "1219".
 * 
 * ============================================================================
 */
public class RemoveKDigits {

    /**
     * APPROACH 1: Monotonic Stack using Deque (Standard, Readable)
     * 
     * Time Complexity: O(N) where N is the length of the string. Each digit is 
     * pushed and popped at most once.
     * Space Complexity: O(N) for the stack/StringBuilder.
     */
    public String removeKdigitsStandard(String num, int k) {
        // Edge Case: If we must remove all digits, return "0"
        if (k == num.length()) {
            return "0";
        }
        
        Deque<Character> stack = new ArrayDeque<>();
        
        // 1. Build the Monotonic Increasing Stack
        for (char digit : num.toCharArray()) {
            // While we still have digits to remove, and the stack isn't empty, 
            // and the current digit is SMALLER than the top of the stack:
            while (k > 0 && !stack.isEmpty() && stack.peekLast() > digit) {
                stack.removeLast(); // Remove the larger digit (the peak)
                k--;
            }
            stack.addLast(digit);
        }
        
        // 2. Handle Edge Case: "12345" where digits only increase. 
        // We still need to remove k digits, so remove them from the end.
        while (k > 0) {
            stack.removeLast();
            k--;
        }
        
        // 3. Build the final string and strip leading zeros
        StringBuilder sb = new StringBuilder();
        boolean leadingZero = true;
        
        for (char digit : stack) {
            if (leadingZero && digit == '0') {
                continue; // Skip leading zeros
            }
            leadingZero = false;
            sb.append(digit);
        }
        
        // If string is empty after removing leading zeros (e.g., all 0s were removed), return "0"
        return sb.length() == 0 ? "0" : sb.toString();
    }

    /**
     * APPROACH 2: Ultra-Optimized Array Stack (Fastest)
     * 
     * In an interview, mentioning this after writing the standard approach shows 
     * deep performance knowledge. Using a primitive char[] array directly avoids 
     * Collection overhead and resizing costs.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(N)
     */
    public String removeKdigitsOptimized(String num, int k) {
        int n = num.length();
        if (k == n) return "0";
        
        char[] stack = new char[n];
        int top = 0; // Points to the next empty spot in the stack
        
        for (int i = 0; i < n; i++) {
            char digit = num.charAt(i);
            
            while (k > 0 && top > 0 && stack[top - 1] > digit) {
                top--; // "Pop" the element by decrementing the top pointer
                k--;
            }
            stack[top++] = digit; // Push the current element
        }
        
        // If k is still > 0, we just chop off from the end by reducing 'top'
        top -= k;
        
        // Find the first non-zero character to strip leading zeros
        int start = 0;
        while (start < top && stack[start] == '0') {
            start++;
        }
        
        // If start == top, it means the stack was entirely zeros
        return start == top ? "0" : new String(stack, start, top - start);
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(String num, int k, String expected) {}

    public static void main(String[] args) {
        RemoveKDigits solver = new RemoveKDigits();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase("1432219", 3, "1219"),
            new TestCase("10200", 1, "200"),   // Strips the leading zero correctly
            new TestCase("10", 2, "0"),        // Removes all digits
            new TestCase("12345", 2, "123"),   // Array is entirely increasing
            new TestCase("9", 1, "0")          // Edge case: single digit
        );
        
        System.out.println("--- Running Approach 1 (Standard Deque Stack) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            String result = solver.removeKdigitsStandard(tc.num(), tc.k());
            System.out.printf("Test %d: Expected = \"%s\", Got = \"%s\" -> %s%n", 
                i + 1, tc.expected(), result, (result.equals(tc.expected()) ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Optimized char[] Stack) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            String result = solver.removeKdigitsOptimized(tc.num(), tc.k());
            System.out.printf("Test %d: Expected = \"%s\", Got = \"%s\" -> %s%n", 
                i + 1, tc.expected(), result, (result.equals(tc.expected()) ? "PASS" : "FAIL"));
        }
    }
}
