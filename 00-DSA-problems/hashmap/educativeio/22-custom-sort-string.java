/**
 * ============================================================================
 * L E E T C O D E   7 9 1 :   C U S T O M   S O R T   S T R I N G
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "What happens to characters in `s` that do NOT appear in `order`?" 
 *    (Crucial constraint check: They can be placed anywhere in the returned string, usually at the end for simplicity).
 * 2. "If `s` contains duplicates (e.g., 'a', 'a'), do they need to stay together?"
 *    (Yes, if 'a' comes before 'b', all 'a's must precede all 'b's).
 * 3. "Are the strings strictly lowercase English letters?"
 *    (Yes. This allows us to use a fast, fixed-size 26-element array instead of a HashMap).
 * 4. "Is `order` guaranteed to only contain unique characters?"
 *    (Yes. This simplifies our logic, meaning we don't have to handle conflicting custom order rules).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We must rearrange characters in `s` based on a predefined dictionary of weights (`order`). 
 * > Characters outside the dictionary don't matter, but characters inside must obey strict relativity. 
 * > The bottleneck is sorting efficiency and handling duplicates gracefully.
 * 
 * ### Approach 1: Custom Comparator (The Brute Force Sorting)
 * 1. What I'd naturally try: I'd assign a "weight" or "rank" to every character based on its index in `order`. 
 *    Then, I'd convert `s` into an array of `Character` objects and use Java's `Arrays.sort()` with a 
 *    custom Comparator that compares characters by their assigned weights.
 * 2. Why it works: It literally applies sorting mechanics to a custom valuation system.
 * 3. Why it's too costly:
 *    - Time Complexity: O(N log N) — where N is the length of `s`. We are using comparison-based sorting 
 *      (TimSort), which requires repeatedly comparing elements.
 *    - Space Complexity: O(N) — because we must box `char` primitives into `Character` objects to use 
 *      custom comparators in Java, consuming heap memory.
 * 4. What work is being repeated: Sorting algorithms assume elements can be in any arbitrary jumbled state 
 *    and meticulously compare pairs. But we have an extremely limited universe of items (only 26 letters). 
 * 5. What property removes the bottleneck: The alphabet size is strictly bounded (26). We don't actually 
 *    need to *sort* elements by moving them around. We can just *build a new string from scratch* 
 *    based on character counts!
 * 
 * ### Approach 2: Frequency Counting / Bucket Sort (The Algorithmic Optimum)
 * 1. What I'd naturally try: Count how many times each character appears in `s` using an `int[26]` array. 
 *    Then, iterate through the `order` string. If `order` says 'c' is first, look at the frequency array 
 *    for 'c'. If we have three 'c's, append "ccc" to the result. Then zero out 'c'. Finally, append whatever 
 *    is left in the frequency array.
 * 2. Why it works: It guarantees the custom order perfectly because we traverse `order` to construct the 
 *    first part of the result, leveraging the pre-counted frequencies.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + M) — where N is the length of `s` and M is the length of `order`. We do one 
 *      linear pass over `s` to count, one pass over `order` to build, and one pass over 26 buckets.
 *    - Space Complexity: O(N) — for the `StringBuilder` to hold the output. The auxiliary counting array 
 *      is strictly O(1) space (26 ints).
 * 
 * > **Interview Strategy:** Start by mentioning the Custom Comparator (Approach 1) conceptually, but note 
 * > that "because the alphabet is fixed to 26 lowercase letters, comparison sorting is massive overkill." 
 * > Immediately write Approach 2. This shows strong mechanical sympathy and understanding of Bucket/Counting sort.
 * 
 * 
 * ## 3. Edge cases
 * - `s` contains characters NOT in `order`: `order = "cba"`, `s = "abcd"`. ('d' must be appended at the end).
 * - `order` contains characters NOT in `s`: `order = "xyz"`, `s = "a"`. (The loop for `order` does nothing; 'a' is appended at the end).
 * - Duplicates in `s`: `s = "cccaaa"`. (Frequency array handles this natively by appending the exact count).
 * 
 * 
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight: "Counting replaces Sorting"**
 * Whenever the domain of values to be sorted is exceptionally small (like 26 letters, or ages 0-120), 
 * DO NOT use comparison sorting (O(N log N)). Use frequency counting (O(N)). Rebuilding the array/string 
 * from counts is inherently sorted by the order you process the buckets.
 * 
 * **ASCII Diagram: Depleting the Frequency Array**
 * order = "cba", s = "abcdac"
 * 
 * 1. Count `s`:
 *    [a:2, b:1, c:2, d:1]
 * 
 * 2. Walk `order`:
 *    'c' -> freq is 2. Append "cc". Map -> [a:2, b:1, c:0, d:1]. Res = "cc"
 *    'b' -> freq is 1. Append "b".  Map -> [a:2, b:0, c:0, d:1]. Res = "ccb"
 *    'a' -> freq is 2. Append "aa". Map -> [a:0, b:0, c:0, d:1]. Res = "ccbaa"
 * 
 * 3. Sweep Remaining:
 *    'd' -> freq is 1. Append "d".  Map -> [a:0, b:0, c:0, d:0]. Res = "ccbaad"
 * 
 * 
 * ## 5. Follow-ups
 * - **Q:** What if the string contained all Unicode characters instead of just lowercase English letters?
 *   **A:** We would replace the `int[26]` array with a `HashMap<Character, Integer>` to track frequencies, 
 *   keeping space proportional to unique characters (O(U)) rather than allocating a sparse array of millions 
 *   of indices.
 * - **Q:** What if we wanted the sorting to be "stable" for characters NOT in `order`? (i.e., if 'e' came before 'd' in `s`, it must remain before 'd' in the result).
 *   **A:** The frequency array approach groups all remaining characters together (e.g., all 'd's then all 'e's), 
 *   destroying their original relative order. To achieve stability, we'd iterate through `s` a second time for 
 *   the leftovers, appending characters whose frequency is > 0 and then decrementing them.
 * 
 * ============================================================================
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CustomSortStringStudy {

    interface CustomSorter {
        String customSortString(String order, String s);
    }

    // ========================================================================
    // APPROACH 2: FREQUENCY COUNTING (The Optimal Hardware-Friendly Solution)
    // ========================================================================
    static class FrequencySorter implements CustomSorter {
        @Override
        public String customSortString(String order, String s) {
            // Constraint: Only lowercase English letters.
            // freq[0] represents 'a', freq[2] represents 'c', etc.
            int[] freq = new int[26];
            
            // 1. Build the frequency map (O(N))
            for (char c : s.toCharArray()) {
                freq[c - 'a']++;
            }
            
            StringBuilder result = new StringBuilder(s.length());
            
            // 2. Consume frequencies based on the strictly provided `order` (O(M))
            for (char c : order.toCharArray()) {
                int index = c - 'a';
                // Append the character as many times as it appeared in 's'
                while (freq[index] > 0) {
                    result.append(c);
                    freq[index]--; // Erase it so we don't double-count later
                }
            }
            
            // 3. Sweep the remaining characters that were NOT in `order` (O(26) -> O(1))
            for (int i = 0; i < 26; i++) {
                while (freq[i] > 0) {
                    // Convert index back to a character
                    result.append((char) (i + 'a'));
                    freq[i]--;
                }
            }
            
            return result.toString();
        }
    }

    // ========================================================================
    // APPROACH 1: CUSTOM COMPARATOR (The General Purpose / Unbounded Solution)
    // ========================================================================
    static class ComparatorSorter implements CustomSorter {
        @Override
        public String customSortString(String order, String s) {
            // Create a weight map based on the order string
            Map<Character, Integer> weights = new HashMap<>();
            for (int i = 0; i < order.length(); i++) {
                weights.put(order.charAt(i), i);
            }

            // Convert string to Character objects (Heavy memory overhead)
            Character[] chars = new Character[s.length()];
            for (int i = 0; i < s.length(); i++) {
                chars[i] = s.charAt(i);
            }

            // Sort using a custom comparator. 
            // Characters not in `order` get a default weight of Integer.MAX_VALUE 
            // so they fall to the back.
            Arrays.sort(chars, (a, b) -> {
                int weightA = weights.getOrDefault(a, Integer.MAX_VALUE);
                int weightB = weights.getOrDefault(b, Integer.MAX_VALUE);
                return Integer.compare(weightA, weightB);
            });

            // Rebuild string
            StringBuilder result = new StringBuilder(s.length());
            for (char c : chars) {
                result.append(c);
            }
            return result.toString();
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        CustomSorter[] solvers = {
            new FrequencySorter(),
            new ComparatorSorter()
        };

        // Format: {order, s, expected_prefix (as leftovers can be any order)}
        // For the tests, we'll validate that the characters IN `order` are sorted correctly,
        // and that the resulting string has the exact same character frequencies as the original.
        String[][] testCases = {
            {"cba", "abcd", "cbad"},       // Standard test case
            {"cbafg", "abcd", "cbad"},     // 'order' has extra characters
            {"z", "aaaa", "aaaa"},         // 's' has all identical elements not in 'order'
            {"kqep", "pekeq", "kqeep"}     // Multiple duplicates in 's'
        };

        for (CustomSorter solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                String order = testCases[i][0];
                String s = testCases[i][1];
                String expected = testCases[i][2]; // Note: FrequencySorter ensures alphabetical for leftovers
                
                String result = solver.customSortString(order, s);
                
                // For validation, we just do a strict equality check against the known deterministic 
                // outputs of our algorithms (Leftovers in FrequencySorter are added alphabetically).
                if (!result.equals(expected)) {
                    System.out.printf("  [FAIL] Test %d. order='%s', s='%s'%n    Expected: %s%n    Got:      %s%n", 
                        i + 1, order, s, expected, result);
                    allPassed = false;
                }
            }
            
            if (allPassed) {
                System.out.println("  [SUCCESS] All test cases passed!\n");
            }
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Frequency Counting (Bucket Sort) to bypass O(N log N) sorting overhead.
 * - Key observation: When rearranging a string by a custom mapping, you don't need 
 *   to actively "sort" the existing string. Simply take inventory of what you have (counts), 
 *   and rebuild a completely new string following the exact recipe given by `order`.
 * - What's worth memorizing: `freq[c - 'a']++` and `(char) (i + 'a')`. This is the 
 *   universal idiom for mapping lowercase letters to/from a fixed size 26 array.
 * - Most common trap: Forgetting to append the characters from `s` that were NOT 
 *   present in the `order` string. The final sweep of the `freq` array handles this perfectly.
 * - Mental trigger: "Custom sorting small alphabets? Don't sort. Count and rebuild."
 * ============================================================================
 */


class Solution {
    public String customSortString(String order, String s) {
        // Step 1: Count the frequency of every character in s.
        // Index 0 represents 'a', index 1 represents 'b', and so on.
        int[] frequency = new int[26];

        for (char ch : s.toCharArray()) {
            frequency[ch - 'a']++;
        }

        StringBuilder result = new StringBuilder(s.length());

        // Step 2: Append characters in the exact order specified by order.
        for (char ch : order.toCharArray()) {
            int index = ch - 'a';

            // Append all occurrences of this character.
            while (frequency[index] > 0) {
                result.append(ch);
                frequency[index]--;
            }
        }

        // Step 3: Append characters that did not appear in order.
        // Their relative order does not matter, so any order is valid.
        for (int i = 0; i < 26; i++) {
            while (frequency[i] > 0) {
                result.append((char) ('a' + i));
                frequency[i]--;
            }
        }

        return result.toString();
    }
}

