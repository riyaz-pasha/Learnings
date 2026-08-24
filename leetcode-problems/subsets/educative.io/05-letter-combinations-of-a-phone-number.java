/**
 * ============================================================================
 * PROBLEM STATEMENT: Letter Combinations of a Phone Number
 * ============================================================================
 * Given a string containing digits from 2 to 9 inclusive, return all possible 
 * letter combinations that the number could represent. Return the answer in 
 * any order.
 * 
 * Mapping of digits to letters (standard telephone dial pad):
 * 2 -> a, b, c
 * 3 -> d, e, f
 * 4 -> g, h, i
 * 5 -> j, k, l
 * 6 -> m, n, o
 * 7 -> p, q, r, s
 * 8 -> t, u, v
 * 9 -> w, x, y, z
 * 
 * Constraints:
 * - 0 <= digits.length <= 4
 * - digits[i] is a digit in the range ['2', '9'].
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS (To ask in an interview):
 * ============================================================================
 * 1. What should be returned if the input string is empty?
 *    (Crucial edge case: An empty string should return an empty list [], 
 *    not a list containing an empty string [""]).
 * 2. Can the input contain invalid characters like '1', '0', or '*'?
 *    (The constraint states digits will only be from '2' to '9', but it's 
 *    good to confirm if validation is required).
 * 3. Does the order of the output matter?
 *    (Problem specifies "any order").
 * 
 * ============================================================================
 * INTERVIEW APPROACH:
 * ============================================================================
 * 1. Acknowledge constraints: digits.length <= 4. The maximum possible 
 *    combinations would be for "7979", which is 4 * 4 * 4 * 4 = 256. 
 *    This is a very small state space, so O(4^N) approaches are optimal.
 * 2. Discuss the Dictionary/Mapping setup: Using an array of Strings 
 *    is the fastest way to map a digit to its corresponding letters.
 * 3. Discuss the Approaches:
 *    - Backtracking (DFS): The standard, most expected combinatorial solution.
 *    - Iterative Queue (BFS): A neat level-by-level approach.
 *    - Functional / Streams: A modern, concise way using flatMap.
 * 
 * ============================================================================
 * TIME & SPACE COMPLEXITY:
 * ============================================================================
 * - Time Complexity: O(4^N * N), where N is the length of digits. 
 *   The worst-case branching factor is 4 (for digits 7 and 9). We generate 
 *   up to 4^N combinations, and building each string takes O(N) time.
 * - Space Complexity: O(N) for the recursion stack in DFS, plus O(4^N * N) 
 *   to store the output strings.
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Stream;

class PhoneCombinationsSolver {

    // A static array mapping digits to their corresponding letters.
    // Index 2 maps to "abc", 3 maps to "def", etc. 
    // Indices 0 and 1 are empty strings since they don't map to letters.
    private static final String[] KEYPAD = {
        "",     // 0
        "",     // 1
        "abc",  // 2
        "def",  // 3
        "ghi",  // 4
        "jkl",  // 5
        "mno",  // 6
        "pqrs", // 7
        "tuv",  // 8
        "wxyz"  // 9
    };

    public static void main(String[] args) {
        String digits = "23";
        
        System.out.println("Input digits: \"23\"\n");

        System.out.println("1. Backtracking (DFS) Approach:");
        System.out.println(letterCombinationsDFS(digits));
        
        System.out.println("\n2. Iterative (Queue/BFS) Approach:");
        System.out.println(letterCombinationsBFS(digits));
        
        System.out.println("\n3. Modern Java (Streams & flatMap) Approach:");
        System.out.println(letterCombinationsStreams(digits));
    }

    /**
     * ========================================================================
     * SOLUTION 1: BACKTRACKING (DFS)
     * ========================================================================
     * Idea & Intuition:
     * Traverse the input string character by character. For the current digit, 
     * fetch its letters. For each letter, append it to a temporary string 
     * (StringBuilder) and recursively move to the next digit.
     * 
     * Visual Decision Tree for "23":
     *                       (Start)
     *                  /       |       \
     *               'a'       'b'       'c'     <-- Digit '2'
     *              / | \     / | \     / | \
     *             d  e  f   d  e  f   d  e  f   <-- Digit '3'
     * 
     * Result paths: "ad", "ae", "af", "bd", "be", "bf", "cd", "ce", "cf".
     */
    public static List<String> letterCombinationsDFS(String digits) {
        var result = new ArrayList<String>();
        if (digits == null || digits.isEmpty()) {
            return result;
        }
        
        backtrack(digits, 0, new StringBuilder(), result);
        return result;
    }

    private static void backtrack(String digits, int index, StringBuilder current, List<String> result) {
        // Base case: If the current string is the same length as digits, it's complete
        if (index == digits.length()) {
            result.add(current.toString());
            return;
        }

        // Get the letters that the current digit maps to
        char digit = digits.charAt(index);
        String letters = KEYPAD[digit - '0'];

        // Iterate through all possible letters for this digit
        for (char letter : letters.toCharArray()) {
            current.append(letter);                  // Choose
            backtrack(digits, index + 1, current, result); // Explore
            current.deleteCharAt(current.length() - 1); // Un-choose (Backtrack)
        }
    }

    /**
     * ========================================================================
     * SOLUTION 2: ITERATIVE WITH QUEUE (BFS)
     * ========================================================================
     * Idea & Intuition:
     * Start with a queue containing an empty string. Process the digits one 
     * by one. For each digit, pop elements from the queue (which represent 
     * combinations built so far), append every new possible letter to them, 
     * and push the new combinations back into the queue.
     * 
     * Visual Example for "23":
     * Initial Queue: [""]
     * Digit '2' ('a','b','c'): Pop "", Push "a", "b", "c". 
     * Queue: ["a", "b", "c"]
     * Digit '3' ('d','e','f'): Pop "a", Push "ad", "ae", "af".
     *                          Pop "b", Push "bd", "be", "bf". 
     *                          ...and so on.
     */
    public static List<String> letterCombinationsBFS(String digits) {
        if (digits == null || digits.isEmpty()) {
            return new ArrayList<>();
        }

        // LinkedList acts as a FIFO queue
        Queue<String> queue = new LinkedList<>();
        queue.offer("");

        for (int i = 0; i < digits.length(); i++) {
            // Number of combinations formed up to the previous digit
            int size = queue.size();
            String letters = KEYPAD[digits.charAt(i) - '0'];

            // Process all elements currently in the queue
            for (int j = 0; j < size; j++) {
                String prefix = queue.poll();
                
                // Append all letters for the current digit to the prefix
                for (char letter : letters.toCharArray()) {
                    queue.offer(prefix + letter);
                }
            }
        }
        
        // The queue now contains all completed combinations
        return new ArrayList<>(queue);
    }

    /**
     * ========================================================================
     * SOLUTION 3: MODERN JAVA STREAMS (FUNCTIONAL FOLDING)
     * ========================================================================
     * Idea & Intuition:
     * This takes the iterative concept but applies functional programming 
     * paradigms. We start with a Stream of a single empty string. As we iterate 
     * over the digits, we map (flatMap) each existing combination to a new 
     * set of combinations by appending the characters of the current digit.
     * 
     * Why use this?
     * It highlights knowledge of modern Java APIs (Stream, flatMap, toList). 
     * It's immutable and concise.
     */
    public static List<String> letterCombinationsStreams(String digits) {
        if (digits == null || digits.isEmpty()) {
            return List.of(); 
        }

        // Start with a list containing just the empty string
        var combinations = List.of("");

        for (char digit : digits.toCharArray()) {
            String letters = KEYPAD[digit - '0'];
            
            // Reassign the list by flatMapping the current prefixes with the new letters
            combinations = combinations.stream()
                .flatMap(prefix -> letters.chars()
                    .mapToObj(c -> prefix + (char) c))
                .toList(); // Java 16+ feature for immutable lists
        }

        return combinations;
    }
}
