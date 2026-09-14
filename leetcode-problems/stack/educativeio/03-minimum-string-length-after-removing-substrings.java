/**
 * ============================================================================
 * SHORTEST STRING LENGTH AFTER REMOVING SUBSTRINGS - INTERVIEW PREPARATION
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given a string `s` containing only uppercase English letters, 
 * repeatedly remove occurrences of "AB" or "CD" until no more can be removed. 
 * Return the minimum possible length of the resulting string. Note that removing 
 * a pair might cause the surrounding characters to come together and form a 
 * new removable pair (e.g., "ACDB" -> remove "CD" -> "AB" -> remove "AB" -> "").
 * 
 * Simple Explanation for Interviewer:
 * "We need to scan the string and delete 'AB' and 'CD'. Because deleting these 
 * characters can cause previously separated characters to touch and form new 
 * 'AB' or 'CD' pairs, this is a 'cascading deletion' problem. Our goal is to 
 * find out how many characters are left when we can no longer make any deletions."
 * 
 * Core Idea & Intuition:
 * The problem exhibits a Last-In-First-Out (LIFO) property. The most recently 
 * unprocessed character is the one that will pair with the current character. 
 * If a deletion happens, the character that came *before* the deleted pair is 
 * now adjacent to the character *after* the deleted pair. This screams for a Stack.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can the order in which we remove substrings change the final length?"
 *    Why: To determine if a Greedy approach works or if we need Backtracking.
 *    Impact: For "AB" and "CD", order doesn't matter (Confluent property). 
 *    "AB" and "CD" do not share characters in a way that causes overlap conflicts 
 *    (like "ABA" where removing the first "AB" breaks the second). 
 *    So, a greedy left-to-right pass is safe.
 * 
 * 2. Q: "Are 'BA' and 'DC' also removable?"
 *    Why: Clarifies the exact matching criteria.
 *    Impact: Problem says only "AB" and "CD". So 'B' followed by 'A' does nothing.
 * 
 * 3. Q: "What is the maximum length of the string?"
 *    Why: Helps determine acceptable time complexity. 
 *    Impact: Constraint is 100. This means even an O(N^2) brute force approach 
 *    will easily pass. However, O(N) is expected for an optimal solution.
 * 
 * 4. Q: "Can the string be empty initially?"
 *    Why: Identifies immediate edge cases.
 *    Impact: The constraints say 1 <= s.length, but it's good practice to add a 
 *    guard clause `if (s == null || s.isEmpty()) return 0;` anyway.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Basic Example: 
 * Input: s = "ABFCACDB"
 * 
 * Step-by-step collapse:
 * 1. "AB F C A C D B" -> remove "AB" at the start
 * 2. "F C A C D B"    -> remove "CD" in the middle
 * 3. "F C A B"        -> removing "CD" made 'A' and 'B' touch! Remove "AB".
 * 4. "F C"            -> No more "AB" or "CD".
 * Output: 2
 * 
 * Visualizing the Stack approach for "ACDB":
 * 
 * Char | Action                  | Stack State (Bottom -> Top)
 * -------------------------------------------------------------
 * 'A'  | Push 'A'                | [A]
 * 'C'  | Push 'C'                | [A, C]
 * 'D'  | Top is 'C', match "CD"! | 
 *      | Pop 'C'                 | [A]
 * 'B'  | Top is 'A', match "AB"! | 
 *      | Pop 'A'                 | []
 * 
 * Result: Stack is empty, length is 0.
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Confirm the cascading nature of the deletions.
 * 2. Clarify: Ask the clarifying questions above (especially about order mattering).
 * 3. Restate: "I need to greedily remove 'AB' and 'CD' and return the final length."
 * 4. Approach 1 (Brute Force): Mention using `String.replace()` in a loop. Acknowledge 
 *    it's O(N^2) but good to establish a baseline.
 * 5. Approach 2 (Optimal): Explain the LIFO nature and propose a Stack.
 * 6. Code: Write the Stack approach. It's clean and demonstrates good fundamentals.
 * 7. Dry-run: Trace the code with "ACDB".
 * 8. Complexity: State O(N) time and O(N) space.
 * 9. Optimize further (Bonus): If asked to improve space or constant factors, 
 *    mention the Two-Pointer Char Array approach.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - No matches possible: "XYZ" -> length 3.
 * - String completely collapses: "AABB" -> length 0.
 * - Sequential non-cascading matches: "ABCD" -> length 0.
 * - Only single characters: "A" -> length 1.
 * 
 * Common Mistakes:
 * - EmptyStackException: Calling `stack.peek()` or `stack.pop()` without 
 *   checking `!stack.isEmpty()` first.
 * - String Immutability: In the brute force approach, forgetting that `replace()` 
 *   creates a NEW string and doesn't modify the existing one in place.
 * - Using `java.util.Stack`: It's synchronized and slow. Always use `ArrayDeque`.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class ShortestStringAfterRemovals {

    public static void main(String[] args) {
        String test1 = "ABFCACDB"; // Expect 2 ("FC")
        String test2 = "ACBBD";    // Expect 5 (No removals possible)
        String test3 = "AABB";     // Expect 0
        String test4 = "CAB";      // Expect 1 ("C")

        System.out.println("--- Brute Force Approach ---");
        System.out.println("ABFCACDB : " + minLengthBruteForce(test1));
        System.out.println("AABB     : " + minLengthBruteForce(test3));

        System.out.println("\n--- Optimal Stack Approach ---");
        System.out.println("ABFCACDB : " + minLengthStack(test1));
        System.out.println("ACBBD    : " + minLengthStack(test2));

        System.out.println("\n--- Ultra-Optimal Array Approach ---");
        System.out.println("ABFCACDB : " + minLengthArray(test1));
        System.out.println("CAB      : " + minLengthArray(test4));
    }

    /**
     * SOLUTION 1: BRUTE FORCE (String Replacement)
     * ------------------------------------------------------------------------
     * Idea: Continuously search for "AB" and "CD" and replace them with an 
     * empty string. We know we are done when the length of the string stops 
     * changing after a full pass.
     * 
     * Time Complexity: O(N^2) in the worst case. E.g., "AAAA...BBBB". Each 
     * replacement requires copying the rest of the string, which takes O(N) time, 
     * and we do this O(N/2) times. For N=100, this is negligible, but for N=10^6 
     * it would Time Out.
     * 
     * Space Complexity: O(N) to store the new string after each replacement.
     */
    public static int minLengthBruteForce(String s) {
        if (s == null || s.isEmpty()) return 0;

        int prevLength = -1;
        
        // Keep replacing as long as the string length is shrinking
        while (s.length() != prevLength) {
            prevLength = s.length();
            // Strings are immutable in Java, so we reassign the result
            s = s.replace("AB", "").replace("CD", "");
        }
        
        return s.length();
    }

    /**
     * SOLUTION 2: OPTIMAL (Stack)
     * ------------------------------------------------------------------------
     * Idea: We iterate through the string character by character. We use a Stack 
     * to keep track of the characters we've seen so far. If the current character 
     * forms a pair ("AB" or "CD") with the character at the top of the stack, 
     * it means we found a removable substring. We pop the top character and 
     * ignore the current character. Otherwise, we push the current character.
     * 
     * Why it works: The stack naturally handles the "cascading" effect. When we 
     * pop a character, the new top of the stack is exactly the character that 
     * appeared before the removed pair!
     * 
     * Time Complexity: O(N). We process each character exactly once. 
     * Space Complexity: O(N). In the worst case (no pairs), all characters are 
     * pushed onto the stack.
     */
    public static int minLengthStack(String s) {
        if (s == null || s.isEmpty()) return 0;

        // Use ArrayDeque instead of java.util.Stack for better performance
        // (ArrayDeque is not synchronized, avoiding unnecessary lock overhead).
        Deque<Character> stack = new ArrayDeque<>();

        for (char c : s.toCharArray()) {
            // Check for valid pairs ONLY if stack is not empty
            if (!stack.isEmpty()) {
                char top = stack.peek(); // Look at the most recent character
                
                if ((top == 'A' && c == 'B') || (top == 'C' && c == 'D')) {
                    // Pair found! Remove the first half from the stack.
                    // We don't push 'c', effectively deleting both.
                    stack.pop();
                    continue; // Skip the push step below
                }
            }
            // If stack is empty or no pair is formed, push the character
            stack.push(c);
        }

        // The remaining elements in the stack are the final string characters
        return stack.size();
    }

    /**
     * SOLUTION 3: ULTRA-OPTIMAL (Two-Pointer / Char Array)
     * ------------------------------------------------------------------------
     * Idea: We can simulate a stack using an array and a pointer `writeIdx`.
     * `writeIdx` points to where the next character should be written. It acts 
     * exactly like the top of a stack. When we find a pair, we simply decrement 
     * `writeIdx`, effectively "popping" the element.
     * 
     * Detailed Dry Run for "CAB":
     * - Init: arr = ['C', 'A', 'B'], writeIdx = 0
     * - i = 0 ('C'): writeIdx=0. arr[0] = 'C'. writeIdx increments to 1.
     * - i = 1 ('A'): writeIdx=1. arr[1] = 'A'. writeIdx increments to 2.
     * - i = 2 ('B'): writeIdx=2. arr[2] = 'B'. 
     *   Check: writeIdx >= 2? Yes. arr[2-1]=='A' && arr[2]=='B'? YES!
     *   Action: writeIdx -= 2 (writeIdx becomes 0). 
     *   Wait, the code logic below increments writeIdx at the end, so we handle it by
     *   checking the previous element and decrementing if matched.
     * 
     * Let's look at the implementation below for the exact dry run:
     * - Init: arr = ['C', 'A', 'B'], i = 0 (stack top pointer)
     * - j = 0 ('C'): arr[0] = arr[0] ('C'). i > 0 is false. i++ -> i=1.
     * - j = 1 ('A'): arr[1] = arr[1] ('A'). match? No. i++ -> i=2.
     * - j = 2 ('B'): arr[2] = arr[2] ('B'). match? arr[1]=='A' && arr[2]=='B' -> YES.
     *   i -= 2 -> i=0. Then i++ -> i=1.
     * - Result: return i (which is 1). Length is 1!
     * 
     * Time Complexity: O(N). Single pass.
     * Space Complexity: O(N) for the character array (Java strings are immutable).
     * *Note: If we were in C++ where strings are mutable, space would be O(1).*
     */
    public static int minLengthArray(String s) {
        if (s == null || s.isEmpty()) return 0;

        char[] arr = s.toCharArray();
        int i = 0; // 'i' acts as the size of our simulated stack
        
        for (int j = 0; j < arr.length; j++) {
            // "Push" the current character onto our array stack
            arr[i] = arr[j];
            
            // Check if the top two elements form a removable pair
            if (i > 0 && ((arr[i - 1] == 'A' && arr[i] == 'B') || 
                          (arr[i - 1] == 'C' && arr[i] == 'D'))) {
                // "Pop" both elements by moving the stack pointer back by 2
                i -= 2; 
            }
            // Move pointer forward to prepare for the next character
            i++;
        }
        
        // 'i' represents the number of elements left in the valid prefix
        return i;
    }

    /**
     * ========================================================================
     * 8. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time   | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Brute Force    | O(N^2) | O(N)  | Slow, creates many strings | Mention only.
     * Stack (Deque)  | O(N)   | O(N)  | Clear logic, idiomatic     | ⭐ STRONGLY RECOMMENDED. Shows great understanding of data structures.
     * Array/2-Pointer| O(N)   | O(N)  | Fastest, skips object overhead | Great follow-up optimization.
     * 
     * ========================================================================
     * 9. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if we also needed to remove 'BA' and 'DC'?"
     * A1: The logic remains identical, we simply add those conditions to our `if` 
     *     statement: `|| (top == 'B' && c == 'A') || (top == 'D' && c == 'C')`.
     * 
     * Q2: "What if instead of just length, I want you to return the final string?"
     * A2: The Stack approach makes this easy. After the loop, the stack contains 
     *     the exact remaining characters. Since it's a Deque, we can iterate 
     *     through it (which iterates bottom-to-top naturally in Java) and append 
     *     to a StringBuilder:
     *     ```
     *     StringBuilder sb = new StringBuilder();
     *     for(char ch : stack) sb.append(ch); // Deque iterates FIFO!
     *     return sb.toString();
     *     ```
     *     For the Array approach, it's even easier: `return new String(arr, 0, i);`.
     * 
     * Q3: "What if the string is 10 GB and doesn't fit in memory, but we stream it?"
     * A3: The string replacement (Brute Force) and Array approaches fail because 
     *     they require the whole string in memory. The Stack approach works 
     *     perfectly for streaming! We read one character at a time from the stream, 
     *     compare it to our in-memory stack, and push/pop. If the stack grows too 
     *     large for RAM, we can spill the stack to a file on disk.
     * 
     * ========================================================================
     * 10. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: Cascading cancellations or adjacent pairwise removals almost 
     *   ALWAYS require a Stack.
     * - Trick: "Pushing" is just iterating forward, "Popping" resolves the past.
     * - Java Tip: Never use `java.util.Stack`. Use `Deque<T> stack = new ArrayDeque<>()`.
     * - Array Optimization: Simulating a stack with an array and an index pointer 
     *   is a brilliant way to optimize away the constant overhead of object wrappers 
     *   like `Character` in Java.
     */
}
