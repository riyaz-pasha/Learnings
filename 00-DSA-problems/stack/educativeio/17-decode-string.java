/**
 * ============================================================================
 * DECODE STRING - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given an encoded string where `k[encoded_string]` means the 
 * `encoded_string` is repeated `k` times, return the decoded string.
 * 
 * Simple Explanation for Interviewer:
 * "We are decompressing a string. Every time we see a number followed by a 
 * bracket, it means the contents inside the bracket need to be repeated that 
 * many times. Because brackets can be nested (e.g., 2[a3[b]]), we must 
 * evaluate the innermost brackets first, just like algebraic equations."
 * 
 * Core Idea & Intuition:
 * Nested structures with an "inside-out" evaluation priority are a textbook 
 * use case for a Stack. 
 * When we encounter an opening bracket `[`, we "pause" what we were currently 
 * building, save our progress to a stack, and start building the new inner 
 * string. When we encounter a closing bracket `]`, we "resume" the previous 
 * string by popping it from the stack, multiplying our current inner string, 
 * and appending it to the previous string.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can the multiplier `k` be a multi-digit number?"
 *    Why: To ensure we don't just read a single character but accumulate digits 
 *    (e.g., `12[a]` means `k = 12`).
 *    Impact: We need `k = k * 10 + (char - '0')` when scanning digits.
 * 
 * 2. Q: "Are the brackets guaranteed to be balanced and correctly formatted?"
 *    Why: Determines if we need error handling for malformed strings.
 *    Impact: Prompt guarantees validity, saving us from checking mismatched brackets.
 * 
 * 3. Q: "Can there be letters outside of any brackets, like `abc3[cd]xyz`?"
 *    Why: Clarifies the structure of the input.
 *    Impact: Yes, we must be able to handle plain strings at the base level.
 * 
 * 4. Q: "What is the maximum length of the output string?"
 *    Why: Nested multipliers can grow exponentially (e.g., `100[100[a]]` is 10,000 chars).
 *    Impact: Helps justify using `StringBuilder` over `String` concatenation, 
 *    and confirms the result will fit in memory.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: "3[a]2[bc]" -> Output: "aaabcbc"
 * 
 * Example 2 (Nested):
 * Input: "2[a3[c]]"
 * 
 * Visualizing the Two-Stack Approach:
 * (countStack holds the multipliers, stringStack holds the "paused" prefixes)
 * 
 * Char | Action & State (k, currentString)     | countStack | stringStack
 * -----------------------------------------------------------------------
 * '2'  | k = 2, cur = ""                       | []         | []
 * '['  | Push k, Push cur. Reset k=0, cur=""   | [2]        | [""]
 * 'a'  | cur = "a"                             | [2]        | [""]
 * '3'  | k = 3                                 | [2]        | [""]
 * '['  | Push k, Push cur. Reset k=0, cur=""   | [2, 3]     | ["", "a"]
 * 'c'  | cur = "c"                             | [2, 3]     | ["", "a"]
 * ']'  | POP k=3, POP prev="a".                | [2]        | [""]
 *      | cur = prev + 3 * cur = "a" + "ccc"    |            |
 *      | cur = "accc"                          |            |
 * ']'  | POP k=2, POP prev="".                 | []         | []
 *      | cur = prev + 2 * cur = "" + "acccaccc"|            |
 *      | cur = "acccaccc"                      |            |
 * 
 * Final Output: "acccaccc"
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Acknowledge the nested parsing and "inside-out" evaluation.
 * 2. Clarify: Ask about multi-digit numbers and characters outside brackets.
 * 3. Identify Pattern: State clearly that nested brackets = Stack or Recursion.
 * 4. Approach 1 (Iterative): Explain the Two-Stack method (one for numbers, 
 *    one for pending strings). It's robust and easy to dry-run.
 * 5. Approach 2 (Recursive): Mention that function call stacks can replace 
 *    explicit stacks.
 * 6. Code: Write the Two-Stack approach first, as it is iterative and safe.
 * 7. Dry-run: Trace "2[a3[c]]" to prove your logic works on nesting.
 * 8. Complexity: State O(Output Length) Time and Space.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - No brackets at all: "abc" -> "abc".
 * - Empty string inside (if allowed): "3[]" -> "".
 * - Massive multipliers: "100[a]" -> Need efficient string building.
 * - Leading/Trailing text: "x2[y]z" -> "xyyz".
 * 
 * Common Mistakes:
 * - String Concatenation: Using `string += newString` inside the loop. This 
 *   creates a new string object every time, changing time complexity from O(N) 
 *   to O(N^2). ALWAYS use `StringBuilder`.
 * - Pushing the wrong string: Pushing the *inner* string instead of the 
 *   *previous outer* string when hitting `[`.
 * - Multi-digit numbers: Processing `1` and `2` as separate multipliers instead 
 *   of `12`.
 */

import java.util.*;

public class DecodeString {

    public static void main(String[] args) {
        String test1 = "3[a]2[bc]";
        String test2 = "2[a3[c]]";
        String test3 = "abc3[cd]xyz";

        System.out.println("--- Iterative Two-Stack Approach ---");
        System.out.println(test1 + " -> " + decodeStringIterative(test1)); // aaabcbc
        System.out.println(test2 + " -> " + decodeStringIterative(test2)); // acccaccc
        System.out.println(test3 + " -> " + decodeStringIterative(test3)); // abccdcdcdxyz

        System.out.println("\n--- Recursive DFS Approach ---");
        System.out.println(test1 + " -> " + decodeStringRecursive(test1)); // aaabcbc
        System.out.println(test2 + " -> " + decodeStringRecursive(test2)); // acccaccc
    }

    /**
     * SOLUTION 1: ITERATIVE TWO-STACK APPROACH (Optimal)
     * ------------------------------------------------------------------------
     * Idea: We maintain two stacks:
     * - `countStack`: Stores the repetition multipliers (k).
     * - `stringStack`: Stores the strings we were building BEFORE we entered 
     *   a new bracket context.
     * When we see '[', we save our current state and start fresh.
     * When we see ']', we pop our saved state, multiply the inner string we 
     * just finished building, and attach it to the saved state.
     * 
     * Time Complexity: O(M), where M is the length of the DECODED output. We 
     * touch each character of the output a constant number of times.
     * Space Complexity: O(M) for the final string, and O(D) where D is the 
     * maximum depth of nested brackets for the stacks.
     */
    public static String decodeStringIterative(String s) {
        if (s == null || s.isEmpty()) return "";

        // ArrayDeque is the modern, fast, non-synchronized stack in Java
        Deque<Integer> countStack = new ArrayDeque<>();
        Deque<StringBuilder> stringStack = new ArrayDeque<>();
        
        StringBuilder currentString = new StringBuilder();
        int k = 0;

        for (char ch : s.toCharArray()) {
            if (Character.isDigit(ch)) {
                // Accumulate multi-digit numbers (e.g., '1' then '2' becomes 12)
                k = k * 10 + (ch - '0');
                
            } else if (ch == '[') {
                // Pause current context: Save the multiplier and string built so far
                countStack.push(k);
                stringStack.push(currentString);
                
                // Reset variables to start parsing the new inner context
                currentString = new StringBuilder();
                k = 0;
                
            } else if (ch == ']') {
                // Resume previous context:
                // 1. Get the previous string we were building
                StringBuilder decodedString = stringStack.pop();
                
                // 2. Get the multiplier for the inner string we just finished
                int repeatTimes = countStack.pop();
                
                // 3. Append the inner string to the previous string `repeatTimes` times
                for (int i = 0; i < repeatTimes; i++) {
                    decodedString.append(currentString);
                }
                
                // 4. The combined string becomes our new current context
                currentString = decodedString;
                
            } else {
                // Normal character, just append it to our current context
                currentString.append(ch);
            }
        }

        return currentString.toString();
    }

    /**
     * SOLUTION 2: RECURSIVE (DFS) APPROACH
     * ------------------------------------------------------------------------
     * Idea: Every pair of `[...]` can be seen as a sub-problem. 
     * When we hit `[`, we make a recursive call to decode the inner string. 
     * The recursive call returns when it hits `]`. 
     * We pass an array `index` to keep track of our global position in the 
     * string across all recursive calls.
     * 
     * Time Complexity: O(M) where M is the length of the decoded output.
     * Space Complexity: O(M) for the result and O(D) for the call stack.
     */
    public static String decodeStringRecursive(String s) {
        // We use a 1-element array to maintain a mutable global index reference 
        // without relying on class-level fields, ensuring thread-safety.
        return decodeHelper(s, new int[]{0});
    }

    private static String decodeHelper(String s, int[] index) {
        StringBuilder currentString = new StringBuilder();
        int k = 0;

        while (index[0] < s.length()) {
            char ch = s.charAt(index[0]);

            if (Character.isDigit(ch)) {
                k = k * 10 + (ch - '0');
                index[0]++;
                
            } else if (ch == '[') {
                index[0]++; // Skip the '['
                
                // Recursively decode the inner bracket contents
                String innerDecoded = decodeHelper(s, index);
                
                // Multiply and append the result
                for (int i = 0; i < k; i++) {
                    currentString.append(innerDecoded);
                }
                
                k = 0; // Reset multiplier for next potential sequence
                
            } else if (ch == ']') {
                index[0]++; // Skip the ']'
                // We reached the end of this bracket's scope, return to caller
                return currentString.toString();
                
            } else {
                currentString.append(ch);
                index[0]++;
            }
        }

        return currentString.toString();
    }

    /**
     * ========================================================================
     * 6. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time   | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Iterative Stack| O(M)*  | O(M)* | Explicit state, no overflow| ⭐ STRONGLY REC.
     * Recursive DFS  | O(M)*  | O(M)* | Elegant, heavily uses heap | Great alternative.
     * 
     * *M = Length of the final decoded string.
     * 
     * Recommendation: Write the Iterative Two-Stack solution. It clearly 
     * demonstrates your ability to manage state manually without relying on 
     * language-level call stacks. 
     * 
     * ========================================================================
     * 7. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if the decoded string is extremely large (e.g., millions of characters)
     *      and we cannot fit it into memory?"
     * A1: Returning a `String` would throw an `OutOfMemoryError`. Instead, we 
     *     would modify the function to accept an `OutputStream` or `Writer` and 
     *     stream the characters directly to a file/network as we decode them, 
     *     avoiding accumulating the entire string in RAM.
     * 
     * Q2: "Can you solve this with only one stack instead of two?"
     * A2: Yes, by using a `Deque<Object>` or `Deque<String>` where we push both 
     *     the integers (as Strings or wrapped objects) and the intermediate 
     *     strings onto the same stack. However, checking types with `instanceof` 
     *     or parsing strings back to ints makes the code messy. Two stacks is 
     *     type-safe and much cleaner.
     * 
     * ========================================================================
     * 8. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: Nested elements with opening and closing tags (brackets, 
     *   HTML tags, parentheses) almost always require a LIFO Stack.
     * - Best Practice: When parsing numbers from characters, `num = num * 10 + (c - '0')` 
     *   is the universal, fastest pattern.
     * - Performance: In string manipulation problems, allocating Strings in loops 
     *   is the #1 cause of performance failure. Rely strictly on `StringBuilder`.
     */
}


/**
 * ============================================================
 * 🔥 Decode String — Single Stack + Record (Clean Design)
 * ============================================================
 */
public class DecodeStringSingleStack {

    public static void main(String[] args) {
        String s = "3[a2[c]]";
        System.out.println(decodeString(s)); // accaccacc
    }

    /**
     * 📦 Record to store state when we encounter '['
     *
     * count        -> how many times to repeat
     * prevString   -> string built before '['
     */
    private record State(int count, StringBuilder prevString) {}

    /**
     * ✅ Single Stack Solution
     *
     * Time Complexity  : O(n * k)
     *  - n = length of string
     *  - k = max repetition (string expansion cost)
     *
     * Space Complexity : O(n)
     *
     * ------------------------------------------------------------
     * 🧠 Core Idea:
     * Instead of 2 stacks:
     *   - combine both into ONE using a record
     *
     * When '[':
     *   -> push (k, currentString)
     *   -> reset currentString
     *
     * When ']':
     *   -> pop state
     *   -> repeat currentString k times
     *   -> append to previous string
     * ------------------------------------------------------------
     */
    public static String decodeString(String s) {

        // Stack holding (count + previous string)
        Deque<State> stack = new ArrayDeque<>();

        StringBuilder current = new StringBuilder();
        int k = 0;

        for (char ch : s.toCharArray()) {

            // 1️⃣ Build number (handles multi-digit like 12[a])
            if (Character.isDigit(ch)) {
                k = k * 10 + (ch - '0');
            }

            // 2️⃣ Opening bracket → push state
            else if (ch == '[') {
                stack.push(new State(k, current));

                // Reset for new substring
                k = 0;
                current = new StringBuilder();
            }

            // 3️⃣ Closing bracket → resolve
            else if (ch == ']') {

                State state = stack.pop();

                // Repeat current substring k times
                String repeated = current.toString().repeat(state.count());

                // Merge with previous string
                current = state.prevString().append(repeated);
            }

            // 4️⃣ Normal character
            else {
                current.append(ch);
            }
        }

        return current.toString();
    }
}

