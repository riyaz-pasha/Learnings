/**
 * ============================================================================
 * L E E T C O D E   4 0 9 :   L O N G E S T   P A L I N D R O M E
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "Do we need to return the actual palindrome string, or just the length?"
 *    (Crucial distinction: returning the length is just math/counting. Returning the string requires actually constructing the left/right halves and handling tie-breakers).
 * 2. "Are the characters strictly limited to English letters (a-z, A-Z)?"
 *    (Determines our space allocation. If yes, a 128-sized array works perfectly. If full Unicode, we need a HashMap).
 * 3. "Is 'A' considered the same as 'a'?"
 *    (The prompt says 'case-sensitive', meaning 'A' and 'a' are entirely different buckets).
 * 4. "Is there a scenario where the input string is empty or null?"
 *    (Constraints say s.length >= 1, so we can skip `if (s == null || s.length() == 0)` boilerplate).
 *
 *
 * ## 2. The reasoning journey
 * > Core Constraint: A palindrome mirrors itself (e.g., "abccba"). This means every character in it 
 * > MUST appear in pairs (an even number of times). However, there is exactly one exception: a palindrome 
 * > can have a single unpaired character strictly in its dead center (e.g., "abcba"). 
 * > The bottleneck is counting pairs efficiently.
 *
 * ### Approach 1: Frequency HashMap (The Standard Developer Way)
 * 1. What I'd naturally try: I need to know exactly how many times each letter appears. I'll dump 
 *    the string into a `HashMap<Character, Integer>`. Then, I'll iterate through the counts.
 * 2. Why it works: For any character with count `C`, the number of times we can use it in pairs is 
 *    `C / 2 * 2`. (e.g., 5 / 2 = 2. 2 * 2 = 4. We can use 4 of them). If any character has an odd count, 
 *    we flag that a "center" character is available.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we traverse the string of length N once to populate the map, 
 *      and then iterate the map's values (at most 52 items).
 *    - Space Complexity: O(U) where U is unique characters (max 52) — because we allocate heap memory 
 *      for HashMap Nodes and `Character`/`Integer` object wrappers.
 * 4. What work is being repeated: We are using a heavy data structure with hashing overhead to count 
 *    a very restricted, small domain of letters (ASCII).
 * 5. What property removes the bottleneck: ASCII characters evaluate to integers. We can use a raw 
 *    primitive array to count them.
 *
 * ### Approach 2: Integer Array (The Hardware-Friendly Way)
 * 1. What I'd naturally try: Replace the HashMap with an `int[128]` array. `count[s.charAt(i)]++`.
 * 2. Why it works: It maps characters directly to memory addresses. 
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we iterate the string once, and array lookups are instant.
 *    - Space Complexity: O(1) — because we allocate exactly 128 integers (512 bytes), completely 
 *      independent of the string size.
 * 4. What work is being repeated: We are doing a full two-pass algorithm. First we count everything, 
 *    then we do math (`C / 2 * 2`) on every single bucket. What if we just resolved pairs *as they appear*?
 * 5. What property removes the bottleneck: A pair is just seeing the same thing twice. If we track 
 *    "unpaired" elements, the moment we see a second one, we instantly have a pair!
 *
 * ### Approach 3: Boolean Array "Pair Popper" (The Pure Algorithmic Optimum)
 * 1. What I'd naturally try: Use a `boolean[128]` array. When I see 'a', if it's currently `false` 
 *    (unpaired), I set it to `true`. If it's already `true`, I just found a pair! I add 2 to my length 
 *    and set it back to `false`.
 * 2. Why it works: We only ever care about pairs. This completely skips the final division/multiplication 
 *    math loop. At the end, if our calculated length is less than the original string length, it guarantees 
 *    some characters were left unpaired, so we can just add 1 for the center!
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we do exactly one single pass over the string. No secondary loops.
 *    - Space Complexity: O(1) — strictly 128 booleans (128 bytes of memory).
 *
 * > **Interview Strategy:** In an interview, skip the HashMap entirely. Write Approach 2 (Int array) if 
 * > the interviewer wants to see traditional counting. Write Approach 3 (Boolean array) to show exceptional 
 * > algorithmic elegance. I would write Approach 3 and explain the clever "add 1 if length < N" math trick.
 *
 *
 * ## 3. Edge cases
 * - All unique characters: `"abcd"`. Result = 1. (Can just pick 'a').
 * - All identical characters: `"aaaa"`. Result = 4. 
 * - Even and Odd mixes: `"abccccdd"`. Result = 7. (Use all 'c's, all 'd's, and one 'a' or 'b' in the center).
 * - Only one character: `"z"`. Result = 1.
 *
 *
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight:** "The Global Center Check." 
 * You don't need a boolean flag `hasOdd` to know if you can add a center character. 
 * Since every pair consumes 2 characters from the string, if `total_pairs * 2 < s.length()`, 
 * it is mathematically impossible for all characters to have been paired up. You have leftovers. 
 * You can ALWAYS put exactly one leftover in the center.
 * 
 * **Dry Run of Approach 3 (Pair Popper):**
 * Input: `s = "abac"` (Length = 4)
 * Initial state: `boolean[] seen = new boolean[128]`, `length = 0`
 * 
 * 1. i=0, char='a'. seen['a'] is false. 
 *    -> set seen['a'] = true.
 * 2. i=1, char='b'. seen['b'] is false. 
 *    -> set seen['b'] = true.
 * 3. i=2, char='a'. seen['a'] is TRUE! 
 *    -> We found a pair. length += 2 (length = 2). 
 *    -> set seen['a'] = false (reset).
 * 4. i=3, char='c'. seen['c'] is false. 
 *    -> set seen['c'] = true.
 * 
 * Loop ends. 
 * Is `length` (2) < `s.length()` (4)? YES. There are leftovers.
 * Return `length + 1` = 3. 
 * (The palindrome could be "aba" or "aca").
 *
 *
 * ## 5. Follow-ups
 * - **Q:** What if we actually had to return the longest palindrome string itself (e.g., "dccaccd")?
 *   **A:** We would fall back to Approach 2 (Int array). We'd build a `StringBuilder`. We iterate the 
 *   array, appending `count / 2` of each character to the left half. We track the largest odd character 
 *   to put in the center. Finally, we append the reverse of the left half to form the right half.
 * - **Q:** What if this is a real-time data stream and we need to query the max palindrome length at any moment?
 *   **A:** We maintain a running integer `pairsCount` and a running `unpairedCount`. When a character arrives, 
 *   if it was unpaired, `unpairedCount--` and `pairsCount++`. If it was new, `unpairedCount++`. 
 *   The query is always just `(pairsCount * 2) + (unpairedCount > 0 ? 1 : 0)`.
 *
 * ============================================================================
 */

import java.util.HashMap;
import java.util.Map;

public class LongestPalindromeStudy {

    interface PalindromeLengthSolver {
        int longestPalindrome(String s);
    }

    // ========================================================================
    // APPROACH 3: BOOLEAN ARRAY (The Elegant Optimum)
    // ========================================================================
    static class BooleanPairPopperSolver implements PalindromeLengthSolver {
        @Override
        public int longestPalindrome(String s) {
            // boolean array maps ASCII chars (up to 127) to a true/false state.
            // true = we've seen one of this character, waiting for its pair.
            boolean[] unpaired = new boolean[128];
            int palindromeLength = 0;
            
            for (char c : s.toCharArray()) {
                if (unpaired[c]) {
                    // We found a pair! 
                    palindromeLength += 2;
                    // Reset the state so we can find future pairs of this char.
                    unpaired[c] = false; 
                } else {
                    // First time seeing this char (or first time since last pair).
                    unpaired[c] = true;
                }
            }
            
            // If our paired length is smaller than the full string, 
            // it means we have at least one character sitting around without a pair.
            // We can place exactly one of those in the center of the palindrome.
            if (palindromeLength < s.length()) {
                return palindromeLength + 1;
            }
            
            return palindromeLength;
        }
    }

    // ========================================================================
    // APPROACH 2: INTEGER ARRAY (The Standard Hardware Approach)
    // ========================================================================
    static class IntArraySolver implements PalindromeLengthSolver {
        @Override
        public int longestPalindrome(String s) {
            int[] counts = new int[128];
            
            // 1. Count all frequencies
            for (char c : s.toCharArray()) {
                counts[c]++;
            }
            
            int length = 0;
            boolean hasOdd = false;
            
            // 2. Do the math
            for (int count : counts) {
                // If count is 5, 5 / 2 = 2. 2 * 2 = 4. We can use 4 of them.
                length += (count / 2) * 2;
                
                // If it's an odd number, we flag that a center piece is available.
                if (count % 2 == 1) {
                    hasOdd = true;
                }
            }
            
            return hasOdd ? length + 1 : length;
        }
    }

    // ========================================================================
    // APPROACH 1: HASH MAP (Conceptual baseline, avoids if Unicode was required)
    // ========================================================================
    static class HashMapSolver implements PalindromeLengthSolver {
        @Override
        public int longestPalindrome(String s) {
            Map<Character, Integer> counts = new HashMap<>();
            
            for (char c : s.toCharArray()) {
                counts.put(c, counts.getOrDefault(c, 0) + 1);
            }
            
            int length = 0;
            boolean hasOdd = false;
            
            for (int count : counts.values()) {
                length += (count / 2) * 2;
                if (count % 2 != 0) {
                    hasOdd = true;
                }
            }
            
            return hasOdd ? length + 1 : length;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        PalindromeLengthSolver[] solvers = {
            new BooleanPairPopperSolver(),
            new IntArraySolver(),
            new HashMapSolver()
        };

        // Format: {input_string, expected_result}
        String[][] testCases = {
            {"abccccdd", "7"},   // Standard case: 'c'x4, 'd'x2, and one 'a' or 'b'
            {"a", "1"},          // Minimal size
            {"aaaa", "4"},       // All even
            {"abc", "1"},        // All unique (can pick any 1)
            {"AAAAaa", "6"},     // Case sensitivity check ('A'x4, 'a'x2) -> completely paired
            {"ccc", "3"}         // Single odd frequency
        };

        for (PalindromeLengthSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                String input = testCases[i][0];
                int expected = Integer.parseInt(testCases[i][1]);
                int result = solver.longestPalindrome(input);
                
                if (result != expected) {
                    System.out.printf("  [FAIL] Test '%s'. Expected: %d, Got: %d%n", input, expected, result);
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
 * - Core pattern: Pair counting. Palindromes are just mirrored pairs + 1 optional center.
 * - Key observation: You don't need to count the exact frequencies. You only need to know 
 *   when a character forms a pair. A boolean array perfectly acts as a "Pair Popper" state machine.
 * - What's worth memorizing: The trick `if (length < s.length()) return length + 1;` 
 *   This single line replaces the need to track an `odd_found` flag across the entire array.
 * - Most common trap: Trying to actually build the palindrome string, which wastes massive time 
 *   and space, when the question only asks for the mathematical length.
 * - Mental trigger: "Palindrome length? Count pairs, add 1 if there are leftovers."
 * ============================================================================
 */


import java.util.*;

/**
 * Problem:
 * Given a string s (case-sensitive), return the length of the longest palindrome
 * that can be built using its characters.
 *
 * ------------------------------------------------------------
 * 🧠 Core Idea (How to think in interview)
 * ------------------------------------------------------------
 * A palindrome has:
 *   - Characters appearing in PAIRS (left + right)
 *   - At most ONE character with odd frequency (center)
 *
 * So strategy:
 *   1. Count frequency of each character
 *   2. Use ALL even counts fully
 *   3. For odd counts:
 *        - Use (count - 1) → makes it even (usable in pairs)
 *        - Keep track that we saw an odd → can place ONE in center
 *
 * ------------------------------------------------------------
 * Example:
 * ------------------------------------------------------------
 * Input:  "abccccdd"
 *
 * Frequencies:
 *   a → 1
 *   b → 1
 *   c → 4
 *   d → 2
 *
 * Build palindrome:
 *   c → 4  (use all)
 *   d → 2  (use all)
 *   a → 0  (1-1)
 *   b → 0  (1-1)
 *
 * Since we had odd counts → we can place ONE in center
 *
 * Answer = 4 + 2 + 1 = 7
 *
 * Possible palindrome: "dccaccd"
 *
 * ------------------------------------------------------------
 * ⏱ Complexity:
 * ------------------------------------------------------------
 * Time  : O(n)  → one pass for counting + one pass over map
 * Space : O(1)  → at most 52 characters (A-Z, a-z)
 *
 * ------------------------------------------------------------
 * 💡 Why this works:
 * ------------------------------------------------------------
 * - Even counts are fully usable
 * - Odd counts waste 1 character each (except one allowed center)
 * - We maximize usage by converting odd → even (count - 1)
 *
 * ------------------------------------------------------------
 * 🚀 Interview One-Liner:
 * ------------------------------------------------------------
 * "Use all even counts, and from odd counts use (count - 1).
 *  If any odd exists, add one center character."
 */
class Solution {

    public int longestPalindrome(String s) {

        // Step 1: Count frequency of each character
        Map<Character, Integer> freq = new HashMap<>();

        for (char ch : s.toCharArray()) {
            // Increment count for each character
            freq.put(ch, freq.getOrDefault(ch, 0) + 1);
        }

        int length = 0;     // stores max palindrome length
        boolean hasOdd = false; // flag to check if any odd frequency exists

        // Step 2: Build palindrome length using frequencies
        for (int count : freq.values()) {

            if (count % 2 == 0) {
                // Even count → can use all characters
                // Example: 4 → use 4
                length += count;

            } else {
                // Odd count → use (count - 1) to make it even
                // Example: 5 → use 4 (2 pairs)
                length += count - 1;

                // Mark that we saw an odd count
                hasOdd = true;
            }
        }

        // Step 3: Add one center character if any odd count exists
        // Only ONE odd character can be placed in the center
        if (hasOdd) {
            length += 1;
        }

        return length;
    }
}

