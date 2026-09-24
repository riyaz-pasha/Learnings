/**
 * ============================================================================
 * REMOVE ALL ADJACENT DUPLICATES IN STRING - INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given a string of lowercase English letters, repeatedly remove 
 * adjacent duplicate letters (pairs) until no more can be removed. Return the 
 * final string.
 * 
 * Simple Explanation for Interviewer:
 * "We are scanning a string. Whenever we see two identical characters next to 
 * each other, we delete them. But deleting them might cause the characters 
 * before and after the deleted pair to touch. If those two characters are also 
 * identical, they form a new pair that must be deleted. We repeat this until 
 * no pairs are left."
 * 
 * Core Idea & Intuition:
 * This problem has a cascading resolution property. Resolving a pair in the 
 * present might uncover a new pair from the past. This means we need a way to 
 * "remember" the most recently processed, unmatched characters. 
 * The Last-In-First-Out (LIFO) pattern perfectly describes this need: the most 
 * recently seen character must be the first one matched. Therefore, a Stack 
 * (or a stack-like structure) is the ideal tool.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Does the string only contain lowercase English letters?"
 *    Why: Ensures we don't need to handle case-insensitivity (e.g., 'A' vs 'a') 
 *    or invalid characters. (Constraint confirms yes).
 * 
 * 2. Q: "If there are three identical characters in a row, e.g., 'abbba', 
 *        do I remove just a pair, leaving 'aba', or all three?"
 *    Why: Clarifies the behavior of odd-numbered adjacent duplicates.
 *    Impact: Problem says "pairs", so 'bbb' -> remove 'bb', leaving 'b'.
 * 
 * 3. Q: "Can the string be empty?"
 *    Why: Standard edge case check. 
 *    Impact: Constraint says length >= 1, but we should always add a null/empty check.
 * 
 * 4. Q: "Is modifying the input string in place required/allowed?"
 *    Why: In C/C++, we can modify `char*` in O(1) space. In Java, Strings are 
 *    immutable, so we must use O(N) space. Calling this out shows language mastery.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Basic Example: 
 * Input: "abbaca"
 * Output: "ca"
 * 
 * Step-by-step collapse:
 * "a b b a c a" -> (remove 'b' and 'b') -> "a a c a" -> (remove 'a' and 'a') -> "ca"
 * 
 * Visualizing the Stack approach for "abbaca":
 * 
 * Char | Action         | Stack State (Bottom -> Top)
 * ---------------------------------------------------
 * 'a'  | Push 'a'       | [a]
 * 'b'  | Push 'b'       | [a, b]
 * 'b'  | Pop 'b' (pair) | [a]
 * 'a'  | Pop 'a' (pair) | []
 * 'c'  | Push 'c'       | [c]
 * 'a'  | Push 'a'       | [c, a]
 * 
 * Result: Read stack from bottom to top -> "ca".
 * 
 * ============================================================================
 * 4. INTERVIEW APPROACH (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand the problem: Acknowledge the "collapsing" nature of the pairs.
 * 2. Clarify assumptions: Confirm behavior on triple characters ("bbb").
 * 3. Start simplest: Mention we could use `String.replace()` repeatedly, but 
 *    note its O(N^2) time complexity because strings are immutable.
 * 4. Identify the pattern: Point out that resolving current characters affects 
 *    previous ones, which screams Stack (LIFO).
 * 5. Choose data structure: Start by proposing `Deque<Character>`, but quickly 
 *    pivot to `StringBuilder` or a `char[]` as a more optimal stack to avoid 
 *    autoboxing overhead and reversing strings.
 * 6. Code: Write the `StringBuilder` or `char[]` solution.
 * 7. Dry-run: Trace "abbaca".
 * 8. Discuss Complexity: O(N) time and O(N) space.
 * 
 * ============================================================================
 * 5. EDGE CASES
 * ----------------------------------------------------------------------------
 * - No duplicates: "abc" -> "abc". (Stack fills up, nothing pops).
 * - Fully resolved string: "abba" -> "". (Returns empty string).
 * - Odd length duplicates: "aaa" -> "a". (First two pop, third gets pushed).
 * - Single character: "a" -> "a".
 * - Long cascading resolution: "abccba" -> "". (Multiple nested pops).
 * 
 * ============================================================================
 * 6. COMMON MISTAKES
 * ----------------------------------------------------------------------------
 * 1. Using java.util.Stack: It's a legacy class and synchronized. Always use Deque.
 * 2. Iterating a Deque backwards: If using Deque, popping elements out yields the 
 *    reversed string ("ac" becomes "ca"). You must use `pollLast()` or an iterator 
 *    to reconstruct the string in the correct order.
 * 3. O(N^2) String concatenations: Doing `s = s.substring(0, i) + s.substring(i+2)` 
 *    inside a loop creates a new String every time, causing massive performance drops.
 * 
 * ============================================================================
 * 7. DETAILED TRACING (Optimal StringBuilder Solution)
 * ----------------------------------------------------------------------------
 * Input: "abbaca"
 * Init: StringBuilder sb = new StringBuilder()
 * 
 * i = 0, char = 'a'
 * - sb is empty.
 * - sb.append('a') -> sb = "a"
 * 
 * i = 1, char = 'b'
 * - sb last char is 'a'. 'a' != 'b'.
 * - sb.append('b') -> sb = "ab"
 * 
 * i = 2, char = 'b'
 * - sb last char is 'b'. 'b' == 'b'.
 * - sb.deleteCharAt(1) -> sb = "a"
 * 
 * i = 3, char = 'a'
 * - sb last char is 'a'. 'a' == 'a'.
 * - sb.deleteCharAt(0) -> sb = ""
 * 
 * i = 4, char = 'c'
 * - sb is empty.
 * - sb.append('c') -> sb = "c"
 * 
 * i = 5, char = 'a'
 * - sb last char is 'c'. 'c' != 'a'.
 * - sb.append('a') -> sb = "ca"
 * 
 * Loop ends. Return sb.toString() -> "ca"
 * 
 * ============================================================================
 * 8. POSSIBLE INTERVIEWER FOLLOW-UP QUESTIONS
 * ----------------------------------------------------------------------------
 * Q1: "What if we need to remove K adjacent duplicates instead of 2?"
 * A1: A simple character stack isn't enough because we need to know how many 
 *     times we've seen the current character consecutively. We would use a Stack 
 *     of objects/records: `class Pair { char c; int count; }`. If the top character 
 *     matches, we increment its count. If the count reaches K, we pop it.
 * 
 * Q2: "Can you optimize the space complexity further?"
 * A2: In Java, Strings are immutable, so we MUST return a new String and use O(N) 
 *     space. However, we can optimize the *constant factors* by using a `char[]` 
 *     as our stack instead of a `StringBuilder` or `Deque`. This avoids object 
 *     wrappers and array resizing overhead.
 * 
 * Q3: "What if the string was passed as a mutable char array in C++?"
 * A3: We could achieve true O(1) auxiliary space using a two-pointer approach 
 *     (a read pointer and a write pointer). The write pointer acts as the top of 
 *     the stack, overwriting characters in place. (This logic is simulated in 
 *     the `removeOptimalArray` method below).
 * 
 * ============================================================================
 * 9. SOLUTION COMPARISON
 * ----------------------------------------------------------------------------
 * Approach           Time       Space      Difficulty      When to Use
 * ----------------------------------------------------------------------------
 * Brute Force (Sub)  O(N^2)     O(N)       Easy            Never in production.
 * Deque<Character>   O(N)       O(N)       Medium          If exact stack semantics needed.
 * StringBuilder      O(N)       O(N)       Medium          Best overall for readability.
 * Char Array (2 Ptr) O(N)       O(N)       Harder          When maximum performance is required.
 * 
 * ============================================================================
 * 10. FINAL TAKEAWAYS
 * ----------------------------------------------------------------------------
 * - Pattern: "Cascading removals" or "Adjacent cancellations" = Stack.
 * - In Java, a StringBuilder is an excellent, highly optimized Stack for characters.
 * - Using an array with a `top` pointer is the fastest way to simulate a stack, 
 *   as it avoids `deleteCharAt` and array expansion overhead entirely.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class RemoveAdjacentDuplicates {

    public static void main(String[] args) {
        String input1 = "abbaca";
        String input2 = "azxxzy";
        String input3 = "aaaaaa";

        System.out.println("--- Better Stack Approach ---");
        System.out.println("Input: " + input1 + " | Output: " + removeBetterStack(input1)); // ca
        
        System.out.println("\n--- Optimal StringBuilder Approach ---");
        System.out.println("Input: " + input2 + " | Output: " + removeOptimalStringBuilder(input2)); // ay
        
        System.out.println("\n--- Ultra-Optimal Char Array Approach ---");
        System.out.println("Input: " + input3 + " | Output: " + removeOptimalCharArray(input3)); // ""
    }

    /**
     * SOLUTION 1: BETTER APPROACH (Using explicit Stack / Deque)
     * ------------------------------------------------------------------------
     * Idea: Standard stack application. Push character if it's different from 
     * the top of the stack. If it's the same, pop the stack.
     * 
     * Time: O(N). We visit each character once.
     * Space: O(N) for the Deque.
     * 
     * Disadvantages: Autoboxing `char` to `Character` objects takes extra memory 
     * and CPU cycles. We also have to do a second pass to build the string.
     */
    public static String removeBetterStack(String s) {
        if (s == null || s.isEmpty()) return s;

        // Use ArrayDeque instead of Stack (legacy)
        Deque<Character> stack = new ArrayDeque<>();

        for (char c : s.toCharArray()) {
            if (!stack.isEmpty() && stack.peekLast() == c) {
                // Duplicate found, destroy the pair
                stack.pollLast(); 
            } else {
                // No match, push to stack
                stack.addLast(c);
            }
        }

        // Reconstruct the string
        StringBuilder sb = new StringBuilder(stack.size());
        for (char c : stack) {
            sb.append(c);
        }
        
        return sb.toString();
    }

    /**
     * SOLUTION 2: OPTIMAL APPROACH (Using StringBuilder as a Stack)
     * ------------------------------------------------------------------------
     * Idea: We can use a StringBuilder to act as our stack. 
     * - The "top" of the stack is `sb.charAt(sb.length() - 1)`.
     * - "Push" is `sb.append(c)`.
     * - "Pop" is `sb.deleteCharAt(sb.length() - 1)`.
     * 
     * Time: O(N)
     * Space: O(N)
     * 
     * Advantages: Very idiomatic Java. Avoids the `Character` object overhead.
     * The result is already in a StringBuilder, so no second pass is needed.
     */
    public static String removeOptimalStringBuilder(String s) {
        if (s == null || s.isEmpty()) return s;

        StringBuilder sb = new StringBuilder();

        for (char c : s.toCharArray()) {
            int len = sb.length();
            
            // Check if our pseudo-stack has elements and the top matches the current char
            if (len > 0 && sb.charAt(len - 1) == c) {
                // Pop from stack
                sb.deleteCharAt(len - 1);
            } else {
                // Push to stack
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * SOLUTION 3: ULTRA-OPTIMAL APPROACH (Char Array / Two Pointers)
     * ------------------------------------------------------------------------
     * Idea: We allocate a char array of the same length as the input string.
     * We use an integer pointer `top` to represent the top of our simulated stack.
     * 
     * Time: O(N) - Single pass.
     * Space: O(N) - Minimal constant overhead. No dynamic resizing.
     * 
     * Why is this faster? It avoids all method call overheads (no .length(), 
     * no .charAt(), no .deleteCharAt()). It does pure array indexing which is 
     * heavily optimized by the JVM.
     * 
     * This simulates the C++ O(1) in-place algorithm perfectly, just using a 
     * duplicate array since Java strings are immutable.
     */
    public static String removeOptimalCharArray(String s) {
        if (s == null || s.length() <= 1) return s;

        char[] stack = new char[s.length()];
        int top = -1; // Points to the index of the top element in our 'stack'

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            
            if (top >= 0 && stack[top] == c) {
                // Pair found! "Pop" the stack by simply moving the pointer down.
                top--;
            } else {
                // "Push" onto the stack by moving pointer up and writing.
                top++;
                stack[top] = c;
            }
        }

        // The valid characters remain from index 0 to 'top' in the array.
        // String(char[], offset, count) is an O(N) operation to build the final string.
        return new String(stack, 0, top + 1);
    }
}
