/**
 * ============================================================================
 * REMOVE ALL ADJACENT DUPLICATES IN STRING II - INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given a string `s` and an integer `k`, repeatedly remove exactly 
 * `k` adjacent and equal characters from `s`. After a removal, the left and 
 * right sides of the string concatenate. Continue until no more removals of 
 * length `k` can be made. Return the final string.
 * 
 * Simple Explanation for Interviewer:
 * "We are looking for contiguous blocks of exactly `k` identical characters. 
 * When we find one, we delete it. Because deleting a block causes the characters 
 * before and after to touch, this might create a new block of `k` identical 
 * characters. We keep collapsing the string until no `k`-sized blocks remain."
 * 
 * Core Idea & Intuition:
 * The "cascading" or "collapsing" nature of this problem strongly suggests a Stack 
 * (Last-In-First-Out). However, unlike the 2-duplicate version where we just pop 
 * if the top character matches, here we need to know exactly *how many* times we 
 * have seen the character sequentially. Thus, instead of just storing characters, 
 * our Stack must store an object containing both the character AND its current 
 * contiguous count.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFICATION QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can k be greater than the length of the string?"
 *    Why: If k > length, no removals are possible. We can return `s` immediately.
 * 2. Q: "Are the characters strictly lowercase English letters?"
 *    Why: Ensures we don't have to worry about case sensitivity (e.g., 'A' vs 'a').
 * 3. Q: "Can the string be completely collapsed to an empty string?"
 *    Why: Edge case checking. (Yes, it can. e.g., "aabbcc", k=2 -> "").
 * 4. Q: "If I have k+1 adjacent duplicates, do I remove exactly k and leave 1?"
 *    Why: Clarifies the greedy nature of the removal. 
 *    Impact: Yes, "aaa", k=2 -> leaves "a". 
 * 5. Q: "Is modifying the input array in-place acceptable if we convert it to char[]?"
 *    Why: Java strings are immutable, so we must use O(N) space. But an in-place 
 *    array simulation of the stack can heavily optimize constant factors.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: s = "abcd", k = 2
 * Output: "abcd" (No adjacent duplicates of length 2).
 * 
 * Example 2:
 * Input: s = "deeedbbcccbdaa", k = 3
 * Output: "aa"
 * 
 * Visualizing the Stack approach for "deeed", k=3:
 * 
 * Char | Stack State (Char, Count)       | Action
 * ---------------------------------------------------------------------
 * 'd'  | [(d, 1)]                        | Push new char
 * 'e'  | [(d, 1), (e, 1)]                | Push new char
 * 'e'  | [(d, 1), (e, 2)]                | Match top! Increment count.
 * 'e'  | [(d, 1), (e, 3)] -> [(d, 1)]    | Match top! Count == 3. POP block!
 * 'd'  | [(d, 2)]                        | Match top! Increment count.
 * 
 * End of string. Rebuild string from stack: 'd' appears 2 times -> "dd".
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Emphasize the "collapsing" effect requiring memory of the past.
 * 2. Clarify: Ask about k relative to string length and exactly what to do with k+1.
 * 3. Brute Force: Briefly mention repeatedly finding and replacing sequences of 
 *    length k. Note that it's O(N^2 / k) time due to string re-allocations.
 * 4. Better (Stack of Objects): Explain using a Stack that stores `(char, count)`.
 *    This gives O(N) time and O(N) space and is very easy to explain.
 * 5. Optimal (Two-Pointer Array): Propose simulating the stack using two arrays 
 *    (or one char array and one count array) to avoid object creation overhead.
 * 6. Code: Write the Optimal or Stack approach (both are acceptable O(N) solutions, 
 *    but the array one shows strong optimization skills).
 * 7. Dry-run: Trace through the "deeed" example.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - k > s.length(): Return s.
 * - Entire string collapses: "aaaa", k=4 -> "".
 * - Multiple overlapping collapses: "pbbcggttciiippooaais", k=2.
 * 
 * Common Mistakes:
 * - Rebuilding the string by popping from the stack in Java `java.util.Stack` 
 *   iterates LIFO. If you pop to a StringBuilder, it will be backwards. Using 
 *   `ArrayDeque` and a standard enhanced for-loop iterates FIFO, which is perfect.
 * - Forgetting to push the remaining elements *multiple* times if reconstructing 
 *   from a custom object stack (e.g., if top is `('a', 2)`, you must append 'a' twice).
 */

import java.util.*;

public class RemoveKAdjacentDuplicates {

    public static void main(String[] args) {
        String s1 = "abcd", s2 = "deeedbbcccbdaa", s3 = "pbbcggttciiippooaais";
        int k1 = 2, k2 = 3, k3 = 2;

        System.out.println("--- Stack of Objects Approach ---");
        System.out.println(s1 + " (k=" + k1 + ") -> " + removeDuplicatesStack(s1, k1));
        System.out.println(s2 + " (k=" + k2 + ") -> " + removeDuplicatesStack(s2, k2));

        System.out.println("\n--- Optimal Array (Simulated Stack) Approach ---");
        System.out.println(s2 + " (k=" + k2 + ") -> " + removeDuplicatesOptimal(s2, k2));
        System.out.println(s3 + " (k=" + k3 + ") -> " + removeDuplicatesOptimal(s3, k3));
    }

    /**
     * SOLUTION 1: BETTER APPROACH (Stack of Objects)
     * ------------------------------------------------------------------------
     * Idea: Use a Stack to store custom objects (or records) holding the character 
     * and its continuous frequency. If the incoming character matches the top of 
     * the stack, we increment the count. If the count hits `k`, we pop the object, 
     * effectively deleting the sequence.
     * 
     * Time Complexity: O(N) - One pass over the string. Rebuilding takes O(N).
     * Space Complexity: O(N) - Stack can hold up to N elements in the worst case 
     * (e.g., "abcdef" with k=2).
     */
    
    // Simple helper class to store state. Mutable so we don't have to pop/push 
    // repeatedly just to update the count.
    static class CharNode {
        char c;
        int count;

        CharNode(char c, int count) {
            this.c = c;
            this.count = count;
        }
    }

    public static String removeDuplicatesStack(String s, int k) {
        if (s == null || s.length() < k) return s;

        Deque<CharNode> stack = new ArrayDeque<>();

        for (char c : s.toCharArray()) {
            if (!stack.isEmpty() && stack.peek().c == c) {
                // Character matches top, increment count
                stack.peek().count++;
                
                // If count reaches k, destroy the block
                if (stack.peek().count == k) {
                    stack.pop();
                }
            } else {
                // New character, push with count 1
                stack.push(new CharNode(c, 1));
            }
        }

        // Reconstruct string. 
        // Note: ArrayDeque iterator processes elements from head (top) to tail (bottom).
        // Since we want the string in original order, we must either iterate backwards 
        // (using descendingIterator) or append and reverse at the end.
        StringBuilder sb = new StringBuilder();
        java.util.Iterator<CharNode> it = stack.descendingIterator();
        while (it.hasNext()) {
            CharNode node = it.next();
            for (int i = 0; i < node.count; i++) {
                sb.append(node.c);
            }
        }

        return sb.toString();
    }

    /**
     * SOLUTION 2: OPTIMAL APPROACH (Two-Pointer Array Simulation)
     * ------------------------------------------------------------------------
     * Idea: We can simulate the stack using the input string's character array 
     * itself and an auxiliary `count` array of the same length. 
     * We use a pointer `i` which represents the top of our "stack" (the length of 
     * the valid string built so far). As we scan the string with pointer `j`, 
     * we write `s[j]` to `res[i]`. We update the count array accordingly.
     * If `count[i]` reaches `k`, we simply decrement `i` by `k`, effectively 
     * "popping" those characters off the simulated stack.
     * 
     * Detailed Dry Run for "deeed", k=3:
     * - Init: res = ['d','e','e','e','d'], count = [0,0,0,0,0]
     * - i = 0, j = 0 ('d'): 
     *   res[0]='d'. count[0]=1. i becomes 1.
     * - i = 1, j = 1 ('e'): 
     *   res[1]='e'. count[1]=1 (since 'e' != 'd'). i becomes 2.
     * - i = 2, j = 2 ('e'): 
     *   res[2]='e'. count[2] = count[1] + 1 = 2. i becomes 3.
     * - i = 3, j = 3 ('e'): 
     *   res[3]='e'. count[3] = count[2] + 1 = 3. 
     *   Count hits k (3)! i -= 3 -> i becomes 0. (We popped "eee"!)
     * - i = 0, j = 4 ('d'): 
     *   res[0]='d'. count[0]=1 (since i=0, no prev). i becomes 1.
     * 
     * Return new String(res, 0, 1) -> "d". Wait, the previous dry-run left 'd' 
     * unresolved? Let's check: "deeed", k=3. 
     * 'd' then 'eee' (deleted), leaving 'd' and the next 'd' -> "dd".
     * Let's trace j=4 exactly:
     * Previous step: i became 0. 
     * Next step: j=4. We write 'd' to res[0]. What is res[-1]? It doesn't exist.
     * Wait, my manual trace of the string missed that "deeed" only has ONE 'd' at 
     * the end. The string in Example 2 is "deeedbbcccbdaa". 
     * Ah, if the string was "ddeeed", the first 'd' would be at res[0] and res[1], 
     * i would become 2.
     * 
     * Time Complexity: O(N) - Exactly one pass over the string array.
     * Space Complexity: O(N) - Arrays for characters and counts. Object overhead 
     * is completely eliminated, making this blazingly fast in Java.
     */
    public static String removeDuplicatesOptimal(String s, int k) {
        if (s == null || s.length() < k) return s;

        // The char array doubles as both the input and the simulated stack
        char[] res = s.toCharArray();
        // The count array tracks the continuous count of the character at res[i]
        int[] count = new int[s.length()];
        
        int i = 0; // 'i' is the write pointer, acting as the size/top of the stack
        
        for (int j = 0; j < s.length(); j++, i++) {
            // Write the current character to the "top" of the stack
            res[i] = res[j];
            
            // If it matches the previous character, increment its count based on the previous count
            if (i > 0 && res[i - 1] == res[i]) {
                count[i] = count[i - 1] + 1;
            } else {
                // First time seeing this block of characters
                count[i] = 1;
            }
            
            // If the count reaches k, we "pop" the block off the stack
            if (count[i] == k) {
                // Moving the pointer back by 'k' effectively deletes the characters.
                // We do 'i -= k', and the loop's 'i++' will increment it back to the 
                // correct insertion point for the NEXT character.
                i -= k;
            }
        }
        
        // The valid string remains in the array from index 0 to i
        return new String(res, 0, i);
    }

    /**
     * ========================================================================
     * 8. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time   | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Brute Force    | O(N^2) | O(N)  | Modifies strings heavily   | Avoid.
     * Object Stack   | O(N)   | O(N)  | Easy to explain & read     | Good starting point.
     * Array Pointers | O(N)   | O(N)  | Extremely fast, skips OOP  | ⭐ OPTIMAL. Present this for full marks.
     * 
     * ========================================================================
     * 9. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "How would this change if we only needed to delete exactly `k` characters, 
     *      but if there are `k+1`, the remaining 1 gets deleted too?"
     * A1: If the rule changes so that ANY contiguous block of size >= k is deleted, 
     *     our stack approach changes slightly. Instead of popping immediately when 
     *     count == k, we wait until the NEXT character is different. If the count 
     *     is >= k, we pop it then.
     * 
     * Q2: "Can we achieve O(1) space?"
     * A2: In Java, no, because strings are immutable and converting them to a 
     *     character array takes O(N) space. In languages with mutable strings like C++, 
     *     we can use the two-pointer array simulation directly on the input string, 
     *     but we would still need an auxiliary `count` array of size N, keeping it O(N).
     * 
     * ========================================================================
     * 10. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: Cascading cancellations of variable length require tracking states.
     * - Trick: When you need to remember properties (like counts) alongside characters 
     *   in a Stack, push a custom Object/Record OR use parallel arrays.
     * - Performance: The Two-Pointer array simulation of a Stack is an incredibly 
     *   powerful optimization in Java that avoids Object creation and Garbage Collection overhead.
     */
}



/**
 * ================================================================
 * 🔥 REMOVE ALL ADJACENT K DUPLICATES IN STRING
 * ================================================================
 *
 * Problem:
 * --------
 * Given a string s and integer k:
 * - Remove k adjacent equal characters repeatedly
 * - Return final string
 *
 * Example:
 * --------
 * s = "deeedbbcccbdaa", k = 3
 *
 * Output:
 * "aa"
 *
 * ================================================================
 *
 * 🧠 CORE IDEA:
 * ------------
 * Instead of repeatedly removing substrings (costly ❌),
 * we SIMULATE removals using a stack.
 *
 * Stack stores:
 *     (character, consecutive count)
 *
 * WHY?
 * ----
 * Because we only care about:
 * 1. Adjacent characters
 * 2. Their frequency
 *
 * ================================================================
 */

public class RemoveKDuplicatesDetailed {

    /**
     * 🔹 Java 24 record (immutable pair)
     * Each element in stack stores:
     * - character
     * - how many times it appeared consecutively
     */
    record Pair(char ch, int count) {}

    public static String removeDuplicates(String s, int k) {

        /**
         * Using Deque as stack (better than Stack class)
         * - push()  -> add first
         * - pop()   -> remove first
         * - peek()  -> look at top
         */
        Deque<Pair> stack = new ArrayDeque<>();

        /**
         * ============================================================
         * 🔁 Traverse each character
         * ============================================================
         */
        for (char currentChar : s.toCharArray()) {

            /**
             * CASE 1:
             * Stack is NOT empty AND top character == current character
             *
             * → We found a consecutive duplicate
             */
            if (!stack.isEmpty() && stack.peek().ch == currentChar) {

                // Remove top element to update its count
                Pair top = stack.pop();

                // Increase frequency
                int updatedCount = top.count + 1;

                /**
                 * 🔥 KEY CONDITION:
                 * If count reaches k → REMOVE this group
                 *
                 * Why?
                 * Because k duplicates should be deleted
                 */
                if (updatedCount == k) {
                    // ❌ Do NOT push back → effectively removed
                } else {
                    // ✅ Put back with updated count
                    stack.push(new Pair(currentChar, updatedCount));
                }

            } else {
                /**
                 * CASE 2:
                 * New character OR different from top
                 *
                 * → Start new sequence with count = 1
                 */
                stack.push(new Pair(currentChar, 1));
            }
        }

        /**
         * ============================================================
         * 🏗️ BUILD FINAL RESULT
         * ============================================================
         *
         * Stack contains remaining characters in REVERSE order
         *
         * Example stack (top → bottom):
         *   (a,2), (d,1)
         *
         * Means:
         *   "daa" but reversed → need to reverse later
         */
        StringBuilder result = new StringBuilder();

        for (Pair p : stack) {

            /**
             * Append character 'count' times
             *
             * Example:
             * (a,2) → "aa"
             */
            result.append(String.valueOf(p.ch).repeat(p.count));
        }

        /**
         * Reverse because stack is LIFO
         */
        return result.reverse().toString();
    }

    public static void main(String[] args) {

        String s = "deeedbbcccbdaa";
        int k = 3;

        String answer = removeDuplicates(s, k);

        System.out.println("Final Answer = " + answer); // "aa"
    }
}
