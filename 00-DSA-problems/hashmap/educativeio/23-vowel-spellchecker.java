/**
 * ============================================================================
 * L E E T C O D E   9 6 6 :   V O W E L   S P E L L C H E C K E R
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "What exactly counts as a vowel?"
 *    (Ensures we don't accidentally treat 'y' as a vowel. The prompt specifies only 'a', 'e', 'i', 'o', 'u').
 * 2. "If multiple words in the wordlist match a query under the capitalization or vowel rules, which do we return?"
 *    (Crucial for correctness: The prompt specifies returning the *first* match in the wordlist).
 * 3. "Are the strings long enough that we need to worry about the overhead of string manipulation?"
 *    (Constraints say max length is 7. This means we can freely use string building/masking without severe performance penalties).
 * 4. "Is it possible for the wordlist to contain duplicates?"
 *    (If it does, the exact match should obviously just return the word, but our capitalization/vowel maps still only care about the *first* time a pattern is seen).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We have three levels of matching (Exact > Capitalization > Vowels), and for the latter 
 * > two, we must prioritize the *first* word in the dictionary that matches. The bottleneck is repeatedly 
 * > comparing a query against up to 5000 dictionary words.
 * 
 * ### Approach 1: Three-Pass Linear Search (Brute Force)
 * 1. What I'd naturally try: For every query, I'd loop through the `wordlist` three times. 
 *    Pass 1: Look for an exact match `query.equals(word)`.
 *    Pass 2: Look for a case-insensitive match `query.toLowerCase().equals(word.toLowerCase())`.
 *    Pass 3: Look for a vowel match by converting both words to lowercase, replacing all vowels with a placeholder 
 *    like '*', and checking equality.
 *    If any pass finds a match, return it and stop.
 * 2. Why it works: It perfectly strictly enforces the precedence rules because we evaluate the highest 
 *    priority match across all words before falling back to lower priorities.
 * 3. Why it's too costly:
 *    - Time Complexity: O(Q * W * L) — where Q is the number of queries (5000), W is the number of words (5000), 
 *      and L is the max length (7). This results in 5000 * 5000 * 3 passes * 7 = ~500 million operations. 
 *      This is dangerously close to Time Limit Exceeded (TLE) limits.
 *    - Space Complexity: O(1) auxiliary space (if we mask on the fly without storing).
 * 4. What work is being repeated: We recalculate the lowercase and masked versions of the dictionary words 
 *    5000 times (once for every query). A dictionary word never changes its lowercase or masked form!
 * 5. What property removes the bottleneck: Precomputation. If we calculate the "fingerprint" of each word 
 *    once and store it in a fast-lookup data structure (HashMap), querying becomes instant.
 * 
 * ### Approach 2: Structural Fingerprinting via HashMaps (The Algorithmic Optimum)
 * 1. What I'd naturally try: I'll iterate over the `wordlist` exactly once. I will maintain three structures:
 *    - A `HashSet` for exact matches.
 *    - A `HashMap` mapping `lowercased_word -> original_word`.
 *    - A `HashMap` mapping `vowel_masked_word -> original_word`.
 * 2. Why it works: To resolve a query, I just check the Set, then the Cap Map, then the Vowel Map. 
 *    But wait, how do I guarantee I return the *first* match? 
 *    *The Trick:* `Map.putIfAbsent()`. If I only insert a mapping if the key doesn't exist, the map inherently 
 *    preserves the very first original word that generated that fingerprint!
 * 3. Cost Breakdown:
 *    - Time Complexity: O((W + Q) * L) — We process the W words once to build the maps, and the Q queries once 
 *      to look them up. String operations take O(L). 5000 + 5000 = 10,000 operations. A massive speedup!
 *    - Space Complexity: O(W * L) — We store each of the W words in a Set and two HashMaps.
 * 
 * > **Interview Strategy:** In a real interview, explicitly name the pattern "Structural Fingerprinting." 
 * > Mention how `putIfAbsent` cleanly solves the "first match" requirement without needing complex 
 * > sorting or secondary lists. Then, write Approach 2.
 * 
 * 
 * ## 3. Edge cases
 * - Wordlist has `["KiTe", "kite"]`. Query is `"kite"`. 
 *   - Exact match triggers first. Returns `"kite"`.
 * - Wordlist has `["KiTe", "kite"]`. Query is `"kIte"`.
 *   - Exact match fails. Cap map looks up `"kite"`. `putIfAbsent` ensured `"kite"` maps to `"KiTe"` (the first one). Returns `"KiTe"`.
 * - Query consists entirely of vowels: e.g., `"uoiea"`. Masked form is `"*****"`. Perfectly valid and handles gracefully.
 * - No match found: Maps return null, code falls through and returns `""`.
 * 
 * 
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight: "The Fallback Cascade"**
 * By creating distinct data structures for each level of strictness, and enforcing a strict query order 
 * (Set -> Map1 -> Map2), the cascading precedence logic writes itself.
 * 
 * **Dry Run:**
 * `wordlist = ["YellOw"]`, `queries = ["yollow"]`
 * 
 * 1. Precomputation (Building the Dictionaries):
 *    - Word: `"YellOw"`
 *    - Exact Set: add `"YellOw"`
 *    - Lowercase: `"yellow"`. Cap Map: `putIfAbsent("yellow", "YellOw")`
 *    - Vowel Mask: Lowercase it -> `"yellow"`. Replace vowels -> `"y*ll*w"`. Vowel Map: `putIfAbsent("y*ll*w", "YellOw")`
 * 
 * 2. Query Processing:
 *    - Query: `"yollow"`
 *    - Exact match? `exactSet.contains("yollow")` -> FALSE
 *    - Cap match? `capMap.get("yollow")` -> NULL
 *    - Vowel match? Lowercase -> `"yollow"`. Mask -> `"y*ll*w"`. 
 *      `vowelMap.get("y*ll*w")` -> Returns `"YellOw"`!
 *    - Output: `"YellOw"`.
 * 
 * 
 * ## 5. Follow-ups
 * - **Q:** What if we had to return *all* valid matches, not just the first one?
 *   **A:** We would change the Maps from `Map<String, String>` to `Map<String, List<String>>`. Instead of `putIfAbsent`, 
 *   we would append to the list.
 * - **Q:** What if the string lengths were huge (e.g., 100,000 characters)?
 *   **A:** Building full string keys for the HashMaps would be too slow and memory-intensive (O(L) hash computation and memory). 
 *   We would instead use custom Hash codes (like a rolling hash) that ignore case/vowels to bucket the words, and only 
 *   do full string comparisons for collision resolution.
 * - **Q:** How can we optimize the vowel masking operation?
 *   **A:** Using `.replaceAll("[aeiou]", "*")` compiles a Regex pattern every time, which is very slow. 
 *   Writing a manual `char[]` loop with a boolean array lookup is magnitudes faster.
 * 
 * ============================================================================
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VowelSpellcheckerStudy {

    interface Spellchecker {
        String[] spellchecker(String[] wordlist, String[] queries);
    }

    // ========================================================================
    // APPROACH 2: FINGERPRINT HASHMAPS (The Optimal Interview Solution)
    // ========================================================================
    static class OptimalSpellchecker implements Spellchecker {
        
        @Override
        public String[] spellchecker(String[] wordlist, String[] queries) {
            Set<String> exactMatches = new HashSet<>();
            Map<String, String> capMatches = new HashMap<>();
            Map<String, String> vowelMatches = new HashMap<>();

            // 1. Precompute fingerprints for all valid words
            for (String word : wordlist) {
                // Precedence 1: Exact match
                exactMatches.add(word);
                
                // Precompute the lowercase version (used for both subsequent maps)
                String lower = word.toLowerCase();
                
                // Precedence 2: Capitalization match
                // putIfAbsent guarantees we ONLY store the first dictionary word 
                // that produces this lowercase fingerprint.
                capMatches.putIfAbsent(lower, word);
                
                // Precedence 3: Vowel match
                String masked = maskVowels(lower);
                vowelMatches.putIfAbsent(masked, word);
            }

            String[] result = new String[queries.length];

            // 2. Resolve queries in strict order of precedence
            for (int i = 0; i < queries.length; i++) {
                String query = queries[i];
                String lowerQuery = query.toLowerCase();
                String maskedQuery = maskVowels(lowerQuery);

                if (exactMatches.contains(query)) {
                    result[i] = query;
                } else if (capMatches.containsKey(lowerQuery)) {
                    result[i] = capMatches.get(lowerQuery);
                } else if (vowelMatches.containsKey(maskedQuery)) {
                    result[i] = vowelMatches.get(maskedQuery);
                } else {
                    result[i] = ""; // No match found
                }
            }

            return result;
        }

        /**
         * Helper method to mask vowels. 
         * Assumes the input string is already lowercased.
         * 
         * Why a manual loop instead of Regex `replaceAll`? 
         * Regex adds massive overhead. A manual char array manipulation is bare-metal fast.
         */
        private String maskVowels(String lower) {
            char[] chars = lower.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (isVowel(chars[i])) {
                    chars[i] = '*'; // Normalize all vowels to a single wildcard character
                }
            }
            return new String(chars);
        }

        private boolean isVowel(char c) {
            return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
        }
    }


    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        Spellchecker checker = new OptimalSpellchecker();

        // Format: {wordlist, queries, expected_results}
        String[][][] testCases = {
            {
                {"KiTe", "kite", "hare", "Hare"}, 
                {"kite", "Kite", "KiTe", "Hare", "HARE", "Hear", "hear", "keti", "keet", "keto"},
                {"kite", "KiTe", "KiTe", "Hare", "hare", "", "", "KiTe", "", "KiTe"}
            },
            {
                {"yellow"},
                {"YellOw", "yollow", "yllw", "yellwo"},
                {"yellow", "yellow", "", ""}
            }
        };

        for (int i = 0; i < testCases.length; i++) {
            System.out.println("Testing Case " + (i + 1) + "...");
            String[] wordlist = testCases[i][0];
            String[] queries = testCases[i][1];
            String[] expected = testCases[i][2];
            
            String[] result = checker.spellchecker(wordlist, queries);
            
            if (Arrays.equals(result, expected)) {
                System.out.println("  [SUCCESS] All queries matched expectations.");
            } else {
                System.out.println("  [FAIL] Mismatch detected.");
                System.out.println("    Expected: " + Arrays.toString(expected));
                System.out.println("    Got:      " + Arrays.toString(result));
            }
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Structural Fingerprinting + Cascading Fallbacks.
 * - Key observation: When matching strings flexibly, normalize them to a "fingerprint" 
 *   (lowercasing, masking). Map the fingerprint back to the original value.
 * - What's worth memorizing: `Map.putIfAbsent(key, value)`. This is the perfect, 
 *   idiomatic way to satisfy the requirement: "Return the first match found in the list."
 * - Most common trap: Using `query.replaceAll("[aeiou]", "*")`. While concise, 
 *   compiling the Regex pattern for every single query causes significant performance 
 *   degradation compared to a raw `char[]` loop.
 * - Mental trigger: "Fuzzy string matching with priorities? Normalization HashMaps."
 * ============================================================================
 */

