/**
 * STANDALONE STUDY NOTE: LONGEST PALINDROME BY CONCATENATING TWO-LETTER WORDS
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Are all words strictly 2 lowercase English letters?" (Constraints confirm this, which allows us to optimize using a 26x26 frequency matrix).
 * - "Can a self-reversing word like 'gg' be used multiple times, and can more than one unpaired self-reversing word be placed in the center?" (Each word can be used at most once, and only *one* unpaired self-reversing word can occupy the center).
 * - "What if no palindrome can be formed at all?" (Return 0, as stated in the prompt).
 * - "Do we need to return the actual palindrome string, or just its maximum length?" (Just the length, which lets us use counting/combinatorics instead of string building).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to find the maximum length palindrome by pairing up 2-letter words (where word A matches word B's reverse). 
 * The binding constraint is that generating all permutations of word concatenations to test for palindromes is 
 * factorially or exponentially explosive, so we must analyze the symmetry properties of 2-letter words.
 * 
 * --- APPROACH 1: BRUTE FORCE (Permutation Generation) ---
 * 1. What I'd naturally try: Generate all valid subsets and permutations of the words array, concatenate them, 
 *    and check if the resulting string is a palindrome while keeping track of the max length.
 * 2. Why it works: It exhaustively tests every possible combination.
 * 3. Why it's too slow/costly: With up to 1000 words, the number of permutations is astronomical (N!). 
 *    Checking each for palindrome property takes O(N * L) time.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N! * N * L) — exponential/factorial time due to permutation generation.
 *    - Space Complexity: O(N * L) — recursion stack and string allocation storage.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Frequency Counting) ---
 * 1. The Observation: Words fall into two distinct categories:
 *    - Type A: Non-symmetric words (e.g., "ab"). To form a palindrome, they *must* be paired with their exact 
 *      reverse ("ba"). Each matched pair contributes 4 characters to the length.
 *    - Type B: Symmetric/Self-reversing words (e.g., "gg", "bb"). They can be paired with themselves (two "gg"s 
 *      contribute 4 characters), OR a single unpaired one can be placed right in the dead center of the palindrome 
 *      to add 2 characters.
 * 2. How it leads to the next step: We can tally the frequencies of all words in a data structure, then process 
 *    pairs and center candidates mathematically.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N * L) — linear pass to count frequencies, followed by checking 676 possible word pairs.
 *    - Space Complexity: O(U) where U is unique words (max 676). However, HashMap object overhead is heavy.
 * 
 * --- APPROACH 3: OPTIMAL (2D Frequency Array Matrix) ---
 * 1. The Observation: Since words are always 2 lowercase English letters, there are only 26 * 26 = 676 possible 
 *    unique words in total. A HashMap is bloated overkill. We can use a 2D integer array `int[26][26]` where 
 *    row index is the first character and column index is the second character.
 * 2. How it leads to the next step: 
 *    - For asymmetric words like "ab" (row 'a', col 'b'), we look for its reverse "ba" (row 'b', col 'a'). 
 *      The number of usable pairs is `min(count[a][b], count[b][a])`. Each pair adds 4 to length. 
 *      (To avoid double counting, we only process when row < col, or handle them symmetrically).
 *    - For symmetric words like "gg" (row 'gg'), pairs are `count[i][i] / 2 * 2`, adding 4 per pair. 
 *      If any symmetric word has an *odd* count remaining, exactly one of them can be used as the center (+2 length).
 * 3. Which one I'd write in an interview: Approach 3 (2D Array). It demonstrates an elite understanding of 
 *    bounded problem domains, runs in O(N) time, uses O(1) space, and completely avoids object allocations.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N * L) — where N is the number of words and L = 2. We make one pass to fill the 26x26 
 *      matrix, and a constant 26x26 double loop to calculate lengths.
 *    - Space Complexity: O(1) — because a 26x26 integer matrix takes fixed memory (2704 bytes), completely independent 
 *      of how large `words.length` is.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - No matching pairs at all: `words = ["ab", "cd", "ef"]` -> Returns 0.
 * - Only symmetric words with odd counts: `words = ["gg"]` -> Returns 2 (placed in center).
 * - Mixed asymmetric and symmetric: `words = ["lc","cl","gg"]` -> "lc" and "cl" pair (4), "gg" goes in center (2) -> Total 6.
 * - Multiple symmetric words with odd counts: `words = ["gg", "bb"]` -> Only ONE can go in the center.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Pairing Mechanics:
 * 
 * Asymmetric ("ab" + "ba"):
 * [ "ab" ] + [ ... inner palindrome ... ] + [ "ba" ]  --> Contributes 4 length
 * 
 * Symmetric Center Exception:
 * [ "gg" ] + [ "gg" ]  --> Contributes 4 length (Even count pair)
 * [ "gg" ]             --> Contributes 2 length (Single center candidate, allowed ONCE)
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal 2D Array Approach)
 * Target: words = ["lc", "cl", "gg"]
 * 
 * Phase 1: Populate 26x26 matrix
 *   - "lc": count['l'-'a']['c'-'a']++  --> count[11][2] = 1
 *   - "cl": count['c'-'a']['l'-'a']++  --> count[2][11] = 1
 *   - "gg": count['g'-'a']['g'-'a']++  --> count[6][6] = 1
 * 
 * Phase 2: Compute Length
 *   - Loop through matrix (i from 0 to 25, j from 0 to 25):
 *     - If i == j (Symmetric words like "gg"):
 *       - count[6][6] = 1. Is it even? No (1 % 2 != 0).
 *       - We take `(1 / 2) * 2` = 0 pairs. 
 *       - Since it's odd, we set `hasCenter = true`.
 *     - If i < j (Asymmetric words like "lc" and "cl"):
 *       - count[11][2] = 1, count[2][11] = 1.
 *       - Pairs = `Math.min(1, 1)` = 1 pair.
 *       - Length += 1 * 4 = 4.
 * 
 * Phase 3: Center Check
 *   - `hasCenter` is true, so add 2 to length.
 * Total Length = 4 + 2 = 6.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "Since every word is exactly two lowercase letters, there are only 676 possible unique words. 
 * Instead of checking permutations which would be factorial, we can map this into a 26-by-26 frequency matrix. 
 * For asymmetric words like 'ab', we look for its reverse 'ba' and take the minimum count to form pairs 
 * that wrap around the palindrome, contributing 4 characters each. For symmetric words like 'gg', pairs of them 
 * also contribute 4 characters, and any leftover odd symmetric word can be placed exactly once in the center 
 * to add 2 characters. This gives us O(N) time complexity and O(1) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if words could be of length K instead of 2?
 * A: The 26x26 array approach breaks down because the number of possible words grows as 26^K. We would 
 *    switch to a `HashMap<String, Integer>` to track word frequencies dynamically, while keeping the same 
 *    pairing logic.
 * 
 * Q: How would you actually construct and return the palindrome string instead of just its length?
 * A: We would maintain a deque or build the string outwards. We push the left halves (`"ab"`) to the front 
 *    or back builder, place the single center word in the middle if `hasCenter` is true, and mirror the right halves.
 */

public class LongestPalindromeByConcatenatingTwoLetterWords {

    /**
     * APPROACH 3: OPTIMAL (2D Frequency Matrix)
     * Time: O(N), Space: O(1)
     */
    public int longestPalindrome(String[] words) {
        // [Why needed]: 26x26 grid covers all possible combinations of two lowercase English letters.
        // Example: count[0][1] tracks frequency of "ab", count[1][0] tracks "ba".
        int[][] count = new int[26][26];

        // Phase 1: Populate frequency matrix
        for (String word : words) {
            int r = word.charAt(0) - 'a';
            int c = word.charAt(1) - 'a';
            count[r][c]++;
        }

        int length = 0;
        boolean hasCenter = false;

        // Phase 2: Compute palindrome length
        for (int i = 0; i < 26; i++) {
            // Case 1: Symmetric words (e.g., "gg", "aa", "zz")
            if (count[i][i] % 2 == 0) {
                // If even, all of them can be used in pairs
                length += count[i][i] * 2;
            } else {
                // If odd, we can use the maximum even portion in pairs
                length += (count[i][i] - 1) * 2;
                // And save the single leftover odd one for the dead center
                hasCenter = true;
            }

            // Case 2: Asymmetric words (e.g., "lc" and "cl")
            // [Why needed]: To avoid double-counting (processing "lc" then "cl"), 
            // we only iterate where i < j (upper triangle of the matrix).
            for (int j = i + 1; j < 26; j++) {
                // A valid pair requires matching the reverse word (count[j][i])
                int pairs = Math.min(count[i][j], count[j][i]);
                length += pairs * 4; // Each pair contributes 2 words of length 2 = 4 chars
            }
        }

        // [Why needed]: If we found at least one unpaired symmetric word, we can place it 
        // right in the center of our palindrome structure, adding 2 characters.
        if (hasCenter) {
            length += 2;
        }

        return length;
    }

    /**
     * SUMMARY
     * 1. Core pattern: 2D Frequency Matrix pairing (Asymmetric min-matching + Symmetric parity/center logic).
     * 2. Key observation: Fixed small alphabets (2 lowercase letters) map perfectly to 26x26 dense matrices 
     *    instead of HashMaps.
     * 3. Memorize vs Understand: Understand why asymmetric words require `Math.min(count[i][j], count[j][i])` 
     *    and why symmetric words allow at most *one* odd center piece.
     * 4. Most common trap: Double-counting asymmetric pairs by scanning the entire 26x26 matrix instead of `i < j`.
     * 5. Mental trigger: "Concatenate 2-letter words into a palindrome" -> `int[26][26]` matrix + Upper-triangle pairing.
     */
    public static void main(String[] args) {
        LongestPalindromeByConcatenatingTwoLetterWords solution = new LongestPalindromeByConcatenatingTwoLetterWords();

        // Test Case 1: Standard Mixed Example
        String[] words1 = {"lc", "cl", "gg"};
        // "lc" + "gg" + "cl" = "lcggcl" (Length 6)
        System.out.println("Test 1 (lc, cl, gg): " + solution.longestPalindrome(words1)); // Expected: 6

        // Test Case 2: Multiple Symmetric Center Conflict
        String[] words2 = {"ab", "ty", "yt", "lc", "cl", "ab"};
        // "ab" + "ty" + "yt" + "ba" -> "abtyytba" (Length 8)
        System.out.println("Test 2 (ab, ty, yt, lc, cl, ab): " + solution.longestPalindrome(words2)); // Expected: 8

        // Test Case 3: Only Center Candidate
        String[] words3 = {"cc", "ll", "xx"};
        // Only one can go in center -> "cc" (Length 2)
        System.out.println("Test 3 (cc, ll, xx): " + solution.longestPalindrome(words3)); // Expected: 2

        // Test Case 4: No matches
        String[] words4 = {"ab", "cd", "ef"};
        System.out.println("Test 4 (No matches): " + solution.longestPalindrome(words4)); // Expected: 0
    }
}


import java.util.HashMap;
import java.util.Map;

class Solution {
    public int longestPalindrome(String[] words) {
        Map<String, Integer> freq = new HashMap<>();

        // Step 1: Count the frequency of every two-letter word.
        for (String word : words) {
            freq.merge(word, 1, Integer::sum);
        }

        int length = 0;
        boolean centerUsed = false;

        // Step 2: Match each word with its reverse.
        for (var entry : freq.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue();

            String reverse = new StringBuilder(word).reverse().toString();

            // Special case: "aa", "bb", etc. are palindromes themselves.
            if (word.equals(reverse)) {
                // Use pairs of identical words on the left and right.
                length += (count / 2) * 4;

                // One unused word can occupy the center.
                if (count % 2 == 1) {
                    centerUsed = true;
                }
            } else {
                // Pair only one direction to avoid counting "ab" + "ba"
                // and "ba" + "ab" twice.
                if (word.compareTo(reverse) < 0) {
                    int reverseCount = freq.getOrDefault(reverse, 0);

                    // Every matched pair contributes 4 characters.
                    length += Math.min(count, reverseCount) * 4;
                }
            }
        }

        // A single identical-letter word contributes 2 center characters.
        if (centerUsed) {
            length += 2;
        }

        return length;
    }
}


class Solution {
    public int longestPalindrome(String[] words) {
        // freq[a][b] counts the occurrences of the word "ab".
        int[][] freq = new int[26][26];

        for (String word : words) {
            int a = word.charAt(0) - 'a';
            int b = word.charAt(1) - 'a';
            freq[a][b]++;
        }

        int length = 0;
        boolean centerUsed = false;

        for (int a = 0; a < 26; a++) {
            for (int b = 0; b < 26; b++) {
                if (a == b) {
                    // Identical-letter words can be paired with themselves.
                    length += (freq[a][a] / 2) * 4;

                    // At most one unpaired identical-letter word can be central.
                    if (freq[a][a] % 2 == 1) {
                        centerUsed = true;
                    }
                } else if (a < b) {
                    // Process each reverse pair only once:
                    // "ab" and "ba".
                    int pairs = Math.min(freq[a][b], freq[b][a]);
                    length += pairs * 4;
                }
            }
        }

        if (centerUsed) {
            length += 2;
        }

        return length;
    }
}

