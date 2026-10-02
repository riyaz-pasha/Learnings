/**
 * ============================================================================
 * L E E T C O D E   2 9 0 :   W O R D   P A T T E R N
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Is the string strictly space-separated with no leading/trailing/multiple spaces?"
 *    (Prevents having to write complex regex splitting logic or trim operations).
 * 2. "Are the characters in the pattern strictly lowercase English letters?"
 *    (Allows us to use an ultra-fast `String[26]` array instead of a heavier `HashMap`).
 * 3. "Can the string `s` or the pattern be empty?"
 *    (Guards against index-out-of-bounds exceptions or zero-length math).
 * 4. "Is memory a critical constraint?"
 *    (Determines if we can use `.split(" ")` which duplicates the entire string into 
 *    an array, or if we must parse the string iteratively with two pointers).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We must enforce a **bijection** (two-way monogamy) between a character 
 * > and a word. If 'a' maps to "dog", 'a' can NEVER map to anything else, AND "dog" can 
 * > NEVER map to any other character.
 * 
 * ### Approach 1: All-Pairs Validation (Brute Force)
 * 1. What I'd naturally try: Split the string. For every character `pattern[i]`, loop through 
 *    all other characters `pattern[j]`. If they are the same character, verify `words[i].equals(words[j])`. 
 *    If they differ, verify `!words[i].equals(words[j])`.
 * 2. Why it works: It strictly applies the bijection definition to every possible pair manually.
 * 3. Why it's too costly: 
 *    - Time Complexity: O(W^2 * L) — where W is the number of words and L is max word length. 
 *      For every word, we scan every other word and do a string comparison.
 *    - Space Complexity: O(W * L) — because we allocate the `split()` array to isolate the words.
 * 4. What work is being repeated: We compare the same word mappings thousands of times. Once we 
 *    establish that 'a' maps to "dog", we don't need to check it against every other 'a'.
 * 5. What property removes the bottleneck: Statefulness. If we simply record the mapping the 
 *    first time we see it, subsequent checks take O(1) time.
 * 
 * ### Approach 2: Two HashMaps (The Literal Translation)
 * 1. What I'd naturally try: Use `Map<Character, String>` to ensure characters don't map to 
 *    multiple words, and `Map<String, Character>` to ensure words don't map to multiple chars.
 * 2. Why it works: It perfectly mirrors the bidirectional constraint of a bijection.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(W * L) — Hash computations and string equality checks take O(L) time. 
 *      We visit each of the W words once.
 *    - Space Complexity: O(W * L) — because we store every unique word twice (once as a value, 
 *      once as a key) plus the full `split()` array.
 * 4. What work is being repeated: Maintaining the reverse dictionary (`String -> Character`) is overkill. 
 *    We only use it to check if a word has *already been claimed*. We don't actually need to 
 *    know *who* claimed it, just that it's taken.
 * 5. What property removes the bottleneck: A `HashSet` only tracks presence, avoiding the 
 *    overhead of storing the character values again.
 * 
 * ### Approach 3: Map and Set (The Pragmatic Standard)
 * 1. What I'd naturally try: Use `Map<Character, String>` for the primary mapping, and a 
 *    `HashSet<String>` to track claimed words.
 * 2. Why it works: When encountering a new character, we check the `HashSet`. If the word is 
 *    there, it's claimed by someone else -> fail. Otherwise, register it in both.
 * 3. Cost Breakdown: 
 *    - Time Complexity: O(W * L) — Traverses the array exactly once.
 *    - Space Complexity: O(W * L) — Array + Map + Set.
 * 4. What work is being repeated: The `HashMap` for characters hashes the keys and handles 
 *    collisions. For a domain of exactly 26 letters, this is unnecessary machinery.
 * 5. What property removes the bottleneck: We can map characters directly to a 26-slot array!
 * 
 * ### Approach 4: Array + Set (The Optimal Interview Solution)
 * 1. What I'd naturally try: Replace `Map<Character, String>` with `String[26]`.
 * 2. Cost Breakdown:
 *    - Time Complexity: O(W * L) — Same Big-O, but much smaller constant factor by skipping hashing.
 *    - Space Complexity: O(W * L) — We save the memory footprint of HashMap Node objects, but 
 *      still allocate the `split()` array.
 * 
 * > **Interview Strategy:** I would confidently write Approach 4 using `split()` and `String[26]`. 
 * > It's fast, idiomatic, avoids HashMap overhead, and is incredibly easy to get right under pressure. 
 * > If the interviewer pushes on memory optimization, I would explain Approach 5 (Stream parsing).
 * 
 * 
 * ## 3. Edge cases
 * - Pattern length differs from string word count: e.g., `pattern = "aba"`, `s = "dog cat"`. 
 *   Must fail immediately before indexing out of bounds.
 * - Single element: `pattern = "a"`, `s = "dog"`. Valid mapping.
 * - The Many-to-One trap: `pattern = "abba"`, `s = "dog dog dog dog"`. (Caught by the Set).
 * - The One-to-Many trap: `pattern = "aaaa"`, `s = "dog cat cat dog"`. (Caught by the Map/Array).
 * 
 * 
 * ## 4. Dry Run, Key Insight & Pitfalls
 * 
 * **Key Insight:** "Bijection is mutual monogamy." A map alone ensures characters are loyal 
 * to words, but a Set is required to ensure words are loyal to characters.
 * 
 * **ASCII Diagram: The Set Catch**
 * a ---> dog
 * b ---/       <-- (Set throws error: 'dog' is already taken!)
 * 
 * **Dry Run of Approach 4 on pattern="abba", s="dog dog dog dog":**
 * 1. i=0, c='a', w="dog"
 *    'a' not in Map. "dog" not in Set.
 *    Map becomes {'a': "dog"}. Set becomes {"dog"}.
 * 2. i=1, c='b', w="dog"
 *    'b' not in Map. 
 *    Is "dog" in Set? YES! 
 *    Conflict detected: 'b' is trying to claim a word already claimed by 'a'.
 *    Return false.
 * 
 * **PITFALL: The "Clever Java Flex" Trap (Integer Caching)**
 * Some candidates try to use a single `Map<Object, Integer>` and rely on Java's `Map.put()` 
 * returning the previous value to check structural fingerprints in one line:
 * 
 *     Map<Object, Integer> map = new HashMap<>();
 *     for (Integer i = 0; i < words.length; i++) {
 *         if (map.put(pattern.charAt(i), i) != map.put(words[i], i)) return false;
 *     }
 * 
 * Why it fails: `put()` returns an `Integer` object. Using `!=` compares object references.
 * Java caches `Integer` objects from -128 to 127. If the string has 128+ words, `put()` 
 * returns non-cached `Integer` instances. `128 != 128` evaluates to TRUE for references, 
 * causing the algorithm to silently fail on large inputs! Always use `Objects.equals()`.
 * 
 * 
 * ## 5. Follow-ups
 * - **Q:** What if the string `s` is 10 GB and we are operating under tight memory constraints?
 *   **A:** Calling `s.split(" ")` duplicates the entire 10 GB string in memory! Instead, we 
 *   can process the string as a stream using two pointers (`start` and `end`). We iterate to 
 *   find the next space, use `substring` to extract just the current word, and validate it. 
 *   The temporary `substring` object gets garbage collected quickly, keeping memory strictly 
 *   limited to the *unique* words in our Set. (See `StreamSolver` below).
 * 
 * ============================================================================
 */

import java.util.*;

public class WordPatternStudy {

    interface WordPatternSolver {
        boolean wordPattern(String pattern, String s);
    }

    // ========================================================================
    // APPROACH 4: ARRAY + SET (The Optimal Interview Solution)
    // ========================================================================
    static class ArrayAndSetSolver implements WordPatternSolver {
        @Override
        public boolean wordPattern(String pattern, String s) {
            String[] words = s.split(" ");
            
            // 1. Guard against domain/range size mismatch
            if (pattern.length() != words.length) {
                return false;
            }
            
            // Map 26 lowercase letters to their corresponding words
            String[] charToWord = new String[26];
            // Track words that have already been claimed by a character
            Set<String> claimedWords = new HashSet<>();
            
            for (int i = 0; i < words.length; i++) {
                // Map 'a' -> 0, 'b' -> 1, etc.
                int charIdx = pattern.charAt(i) - 'a';
                String currentWord = words[i];
                
                if (charToWord[charIdx] != null) {
                    // This character already mapped to something. Does it match the current word?
                    if (!charToWord[charIdx].equals(currentWord)) {
                        return false; // One-to-Many violation (e.g. 'a' -> "dog", then 'a' -> "cat")
                    }
                } else {
                    // This character is new. But is the word already taken by someone else?
                    if (claimedWords.contains(currentWord)) {
                        return false; // Many-to-One violation (e.g. 'b' trying to claim "dog")
                    }
                    // Valid new mapping. Record it.
                    charToWord[charIdx] = currentWord;
                    claimedWords.add(currentWord);
                }
            }
            
            return true;
        }
    }

    // ========================================================================
    // APPROACH 3: HASHMAP + SET (The Standard Developer Way)
    // Good to know if the pattern could contain Unicode or arbitrary characters
    // ========================================================================
    static class MapAndSetSolver implements WordPatternSolver {
        @Override
        public boolean wordPattern(String pattern, String s) {
            String[] words = s.split(" ");
            if (pattern.length() != words.length) return false;
            
            Map<Character, String> map = new HashMap<>();
            Set<String> claimed = new HashSet<>();
            
            for (int i = 0; i < words.length; i++) {
                char c = pattern.charAt(i);
                String w = words[i];
                
                if (map.containsKey(c)) {
                    if (!map.get(c).equals(w)) return false;
                } else {
                    if (claimed.contains(w)) return false;
                    map.put(c, w);
                    claimed.add(w);
                }
            }
            return true;
        }
    }

    // ========================================================================
    // APPROACH 5: TWO-POINTER STREAM (Optimal Memory - Follow-up Flex)
    // Avoids allocating an O(N) array via split()
    // ========================================================================
    static class StreamSolver implements WordPatternSolver {
        @Override
        public boolean wordPattern(String pattern, String s) {
            String[] charToWord = new String[26];
            Set<String> claimedWords = new HashSet<>();
            
            int patternIdx = 0;
            int stringIdx = 0;
            int n = s.length();
            int pLen = pattern.length();
            
            while (stringIdx < n) {
                // If string has more words than the pattern has characters
                if (patternIdx == pLen) return false;
                
                // Fast-forward to find the end of the current word
                int endIdx = stringIdx;
                while (endIdx < n && s.charAt(endIdx) != ' ') {
                    endIdx++;
                }
                
                // Extract just the word we need right now
                String currentWord = s.substring(stringIdx, endIdx);
                int charIdx = pattern.charAt(patternIdx) - 'a';
                
                // Same validation logic as Approach 4
                if (charToWord[charIdx] != null) {
                    if (!charToWord[charIdx].equals(currentWord)) return false;
                } else {
                    if (claimedWords.contains(currentWord)) return false;
                    charToWord[charIdx] = currentWord;
                    claimedWords.add(currentWord);
                }
                
                patternIdx++;
                stringIdx = endIdx + 1; // Move past the space for the next word
            }
            
            // If pattern has more characters than the string has words
            return patternIdx == pLen;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        WordPatternSolver[] solvers = {
            new MapAndSetSolver(),
            new ArrayAndSetSolver(),
            new StreamSolver()
        };

        // Format: {pattern, string, expected_result (true="1", false="0")}
        String[][] testCases = {
            {"abba", "dog cat cat dog", "1"},    // Standard valid case
            {"abba", "dog cat cat fish", "0"},   // Map collision (One-to-Many)
            {"abba", "dog dog dog dog", "0"},    // Set collision (Many-to-One)
            {"aba", "dog cat", "0"},             // Length mismatch (Pattern longer)
            {"a", "dog cat", "0"},               // Length mismatch (String longer)
            {"a", "dog", "1"},                   // Single element valid
            {"aaaa", "dog cat cat dog", "0"}     // Total mismatch
        };

        for (int i = 0; i < solvers.length; i++) {
            System.out.println("Testing " + solvers[i].getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (String[] test : testCases) {
                boolean expected = test[2].equals("1");
                boolean result = solvers[i].wordPattern(test[0], test[1]);
                if (result != expected) {
                    System.out.printf("  [FAIL] pattern=\"%s\", s=\"%s\". Expected: %b, Got: %b%n", 
                        test[0], test[1], expected, result);
                    allPassed = false;
                }
            }
            if (allPassed) {
                System.out.println("  [SUCCESS] All edge cases and examples passed!\n");
            }
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Two-way validation (Bijection). 
 * - Key observation: A Map ensures keys don't have multiple values, but a Set is 
 *   required to ensure values aren't claimed by multiple keys.
 * - What's worth memorizing: ASCII constraint -> Array. When keys are strictly lowercase 
 *   letters, an `int[26]` or `String[26]` array is faster and lighter than a HashMap.
 * - Most common trap: Forgetting the length check before parsing the string!
 * - Mental trigger: "Bijection = Map + Set (or Two Maps)."
 * ============================================================================
 */


import java.util.*;

class Solution {

    public boolean wordPattern(String pattern, String s) {

        String[] words = s.split(" ");

        // ❗ Length mismatch → cannot form bijection
        if (pattern.length() != words.length) {
            return false;
        }

        // Two maps to maintain bijection
        Map<Character, String> charToWord = new HashMap<>();
        Map<String, Character> wordToChar = new HashMap<>();

        for (int i = 0; i < pattern.length(); i++) {

            char ch = pattern.charAt(i);
            String word = words[i];

            // Case 1: mapping already exists
            if (charToWord.containsKey(ch)) {

                // ❌ mismatch → break bijection
                if (!charToWord.get(ch).equals(word)) {
                    return false;
                }

            } else {
                // Case 2: new mapping → check reverse conflict

                if (wordToChar.containsKey(word)) {
                    // ❌ word already mapped to another char
                    return false;
                }

                // ✅ establish mapping
                charToWord.put(ch, word);
                wordToChar.put(word, ch);
            }
        }

        return true;
    }

/*
⏱ Complexity
- Time: O(n)
  (n = number of words / pattern length)
- Space: O(n)
  (maps storing mappings)
*/    
}

import java.util.*;

class Solution {

    public boolean wordPattern(String pattern, String s) {

        // Step 1: Split sentence into words
        String[] words = s.split(" ");

        // Step 2: Length must match (1 char ↔ 1 word)
        if (pattern.length() != words.length) {
            return false;
        }

        /*
         * Step 3: Use a single map to track LAST SEEN INDEX
         * 
         * Key idea:
         * - We store both:
         *      Character → last seen index
         *      Word      → last seen index
         * 
         * Why Object?
         * - Because keys can be both Character and String
         */
        Map<Object, Integer> lastSeenIndex = new HashMap<>();

        for (int i = 0; i < pattern.length(); i++) {

            char ch = pattern.charAt(i);  // current pattern character
            String word = words[i];       // corresponding word

            /*
             * Step 4: Insert both into map
             * 
             * put(key, value) returns PREVIOUS value associated with key
             * (or null if not present)
             */

            Integer prevCharIndex = lastSeenIndex.put(ch, i);
            Integer prevWordIndex = lastSeenIndex.put(word, i);

            /*
             * Step 5: Compare previous indices
             * 
             * If pattern is valid:
             *  - both should have same previous index
             * 
             * Cases:
             * 1. Both null → first time seen → OK
             * 2. Both same index → consistent mapping → OK
             * 3. Different values → mapping conflict ❌
             */

            if (!Objects.equals(prevCharIndex, prevWordIndex)) {
                return false;
            }
        }

        // If no conflicts found → valid bijection
        return true;
    }
}
