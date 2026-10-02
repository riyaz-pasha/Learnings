/**
 * ============================================================================
 * I S O M O R P H I C   S T R I N G S
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "Are the strings guaranteed to be the exact same length?" 
 *    (Prevents immediate OutOfBounds exceptions; though constraints say yes, checking `s.length() != t.length()` is a standard O(1) safety net).
 * 2. "What is the character set? Standard ASCII, Extended ASCII, or full Unicode?"
 *    (Crucial for space allocation: ASCII means we can use a tiny 256-size array. Unicode forces us into HashMaps to avoid massive sparse arrays).
 * 3. "Are the strings case-sensitive?"
 *    (Ensures we don't accidentally treat 'A' and 'a' as the same character).
 * 4. "Can a character map to itself? E.g., 'a' maps to 'a'?"
 *    (Validates the definition of the mapping; usually yes, a bijection allows identity mappings).
 * 5. "Do we need to worry about null inputs?"
 *    (Dictates whether we need `if (s == null || t == null)` guards before processing).
 *
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We need to enforce a *bijection* (a strict 1-to-1 and onto mapping) between the 
 * > characters of string `s` and string `t` as we read them left to right. If 'a' maps to 'x', then 
 * > 'a' can never map to 'y', AND no other character can map to 'x'.
 *
 * ### Approach 1: The "Find and Replace" Simulation (Brute Force)
 * 1. What I'd naturally try: For every character in `s`, find all its occurrences. Check if the 
 *    characters at the exact same positions in `t` are also identical. Then ensure no two different 
 *    characters in `s` map to the same character in `t`.
 * 2. Why it works: It literally verifies the definition of a fixed mapping by checking the entire 
 *    string over and over.
 * 3. Why it's too costly: 
 *    - Time Complexity: O(N^2) — because for each of the N characters, we might scan the rest 
 *      of the string (another N operations) to verify all occurrences match.
 *    - Space Complexity: O(1) auxiliary — because we only use loop pointers, doing it in place.
 * 4. What work is being repeated: We are constantly looking ahead to verify future characters, 
 *    but when the loop reaches those future characters, we process them again.
 * 5. What property removes the bottleneck: We only need to know what a character mapped to *in the past*. 
 *    If we remember past decisions, we never have to look ahead.
 *
 * ### Approach 2: The Double Dictionary (Two HashMaps)
 * 1. What I'd naturally try: Use a `HashMap` to remember mappings. `map.put(s_char, t_char)`.
 * 2. Why it works: As we step through `s[i]` and `t[i]`, we check if `s[i]` is in the map. If it is, 
 *    it better map to `t[i]`. But wait! What if `s = "bad"` and `t = "foo"`? 
 *    'b'->'f', 'a'->'o', 'd'->'o'. The `sToT` map thinks this is fine! We also need to ensure 'o' 
 *    hasn't been claimed by a different letter. So we need TWO maps: `sToT` and `tToS`.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we iterate through the strings exactly once, and HashMap 
 *      lookups are O(1).
 *    - Space Complexity: O(U) where U is the number of unique characters (max 256 for ASCII) — 
 *      because we store at most 256 key-value pairs in each map.
 * 4. What work is being repeated: HashMaps require computing hash codes, handling collisions, 
 *    and allocating Node objects in memory. For just 256 ASCII characters, this is heavy overkill.
 * 5. What property removes the bottleneck: The ASCII character set is small and contiguous (0-255). 
 *    Characters themselves can just be array indices!
 *
 * ### Approach 3: Fixed Arrays (The Optimal ASCII Lookup Table)
 * 1. What I'd naturally try: Replace `HashMap<Character, Character>` with `int[256]` (or `char[256]`).
 * 2. Why it works: `mapS[s_char] = t_char`. Array lookups bypass all hashing overhead. It's the 
 *    exact same logic as Approach 2, just mapped to raw memory addresses.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — strictly one pass, bare-metal array lookups.
 *    - Space Complexity: O(1) — because `int[256]` is exactly 1024 bytes, regardless of whether 
 *      the string length is 10 or 10,000,000. It is strictly bounded.
 *
 * ### Approach 4: Structural Fingerprinting (The Elegant "Index" Trick)
 * 1. What I'd naturally try: Instead of mapping `s_char` to `t_char`, map *both* to the index 
 *    where we last saw them.
 * 2. Why it works: Isomorphic strings share a "structure". For "paper" and "title":
 *    'p' and 't' are seen at idx 0.
 *    'a' and 'i' are seen at idx 1.
 *    'p' and 't' are seen again at idx 2 (their last seen was 0. 0 == 0. Match!).
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — single pass.
 *    - Space Complexity: O(1) — two `int[256]` arrays.
 * 
 * > **Interview Strategy:** Approach 3 (Array char-to-char mapping) or Approach 4 (Array index tracking) 
 * > are both phenomenal. I prefer writing **Approach 4 (Index tracking)** because it naturally handles 
 * > the bidirectional constraint with a single equality check, meaning less code and less chance for bugs.
 *
 * 
 * ## 3. Edge cases
 * - Length mismatch: If strings differ in length, immediately return `false`. (Though the prompt constraints 
 *   guarantee equal length, it's a critical real-world guard).
 * - Empty strings: `""` and `""`. (Loop doesn't run, returns `true`, which is correct).
 * - All duplicates: `"aaaa"` and `"bbbb"`. (Perfectly isomorphic).
 * - One-to-many trap: `"foo"` and `"bar"`. ('o' tries to map to 'o' and 'r' -> caught).
 * - Many-to-one trap: `"bar"` and `"foo"`. ('a' and 'r' try to map to 'o' -> caught).
 *
 * 
 * ## 4. Dry Run & Pitfalls
 * **Pitfall: The Default Zero Trap**
 * If we use an `int[]` for the Index Tracking approach, the default value is `0`. 
 * If we process the 0th character of "foo" ('f'), and we record its index as `0`, our array holds `0`.
 * Next time we look at a character we haven't seen before, its default array value is *also* `0`! 
 * We can't distinguish "seen at index 0" from "never seen".
 * *Solution:* Always store `index + 1`. So index 0 is stored as 1. 0 strictly means "unseen".
 *
 * **Dry Run of Approach 4 (Index Tracking) on "foo" vs "bar":**
 * Initial state: mapS and mapT are all 0s.
 * 
 * 1. i = 0: s='f', t='b'
 *    mapS['f'] (0) == mapT['b'] (0)? YES.
 *    Update: mapS['f'] = 1, mapT['b'] = 1
 * 
 * 2. i = 1: s='o', t='a'
 *    mapS['o'] (0) == mapT['a'] (0)? YES.
 *    Update: mapS['o'] = 2, mapT['a'] = 2
 * 
 * 3. i = 2: s='o', t='r'
 *    mapS['o'] (2) == mapT['r'] (0)? NO! Mismatch. Return FALSE.
 * 
 * 
 * ## 5. Follow-ups
 * - **Q:** What if the string contains thousands of different Unicode characters, not just ASCII?
 *   **A:** We would abandon the `int[256]` arrays and revert to Approach 2 (Two HashMaps). An array 
 *   would need to be size 1,114,112 for full Unicode, which is too sparse and wastes memory.
 * - **Q:** How would you group a List of strings into groups of isomorphic strings? 
 *   **A:** I would write a `encode(String s)` function that translates a string into a structural 
 *   fingerprint. "paper" -> "0.1.0.2.3". "title" -> "0.1.0.2.3". We can then use a 
 *   `HashMap<String, List<String>>` where the key is this fingerprint.
 *
 * ============================================================================
 */

import java.util.HashMap;
import java.util.Map;

public class IsomorphicStringsStudy {

    /**
     * Interface to test all implementations uniformly.
     */
    interface IsomorphicChecker {
        boolean isIsomorphic(String s, String t);
    }

    // ========================================================================
    // APPROACH 4: INDEX TRACKING ARRAYS (Optimal - Recommended for interviews)
    // ========================================================================
    static class ArrayIndexChecker implements IsomorphicChecker {
        @Override
        public boolean isIsomorphic(String s, String t) {
            // Guard clause: By definition, different lengths cannot be a 1-to-1 mapping.
            if (s.length() != t.length()) {
                return false;
            }

            // Using int[] of size 256 for standard ASCII characters.
            // mapS tracks the last seen index (+1) of characters in s.
            // mapT tracks the last seen index (+1) of characters in t.
            int[] mapS = new int[256];
            int[] mapT = new int[256];

            for (int i = 0; i < s.length(); i++) {
                char charS = s.charAt(i);
                char charT = t.charAt(i);

                // If the characters were last seen at different structural positions, 
                // the mapping is broken.
                if (mapS[charS] != mapT[charT]) {
                    return false;
                }

                // Update the last seen position. 
                // CRITICAL: We use i + 1 to avoid conflicts with the default array value of 0.
                // E.g., if i=0, we store 1. Now 0 strictly means "never seen before".
                mapS[charS] = i + 1;
                mapT[charT] = i + 1;
            }

            return true;
        }
    }

    // ========================================================================
    // APPROACH 3: CHAR-TO-CHAR ARRAYS (Alternative Optimal)
    // ========================================================================
    static class ArrayMappingChecker implements IsomorphicChecker {
        @Override
        public boolean isIsomorphic(String s, String t) {
            if (s.length() != t.length()) return false;

            // Arrays to store the exact character mappings.
            // sToT[97] = 120 means 'a' maps to 'x'.
            int[] sToT = new int[256];
            int[] tToS = new int[256];

            for (int i = 0; i < s.length(); i++) {
                char charS = s.charAt(i);
                char charT = t.charAt(i);

                // Check if charS has already been mapped to something else
                if (sToT[charS] != 0 && sToT[charS] != charT) {
                    return false; // Many-to-One violation (e.g., 'b'->'f', 'd'->'f')
                }

                // Check if charT has already been mapped from something else
                if (tToS[charT] != 0 && tToS[charT] != charS) {
                    return false; // One-to-Many violation (e.g., 'f'->'b', 'f'->'a')
                }

                // Establish the bidirectional mapping
                sToT[charS] = charT;
                tToS[charT] = charS;
            }

            return true;
        }
    }

    // ========================================================================
    // APPROACH 2: TWO HASHMAPS (Best if character set is unknown/Unicode)
    // ========================================================================
    static class HashMapChecker implements IsomorphicChecker {
        @Override
        public boolean isIsomorphic(String s, String t) {
            if (s.length() != t.length()) return false;

            Map<Character, Character> sToT = new HashMap<>();
            Map<Character, Character> tToS = new HashMap<>();

            for (int i = 0; i < s.length(); i++) {
                char charS = s.charAt(i);
                char charT = t.charAt(i);

                // Check forward mapping
                if (sToT.containsKey(charS)) {
                    if (sToT.get(charS) != charT) {
                        return false;
                    }
                } else {
                    sToT.put(charS, charT);
                }

                // Check reverse mapping
                if (tToS.containsKey(charT)) {
                    if (tToS.get(charT) != charS) {
                        return false;
                    }
                } else {
                    tToS.put(charT, charS);
                }
            }

            return true;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        IsomorphicChecker[] checkers = {
            new ArrayIndexChecker(), 
            new ArrayMappingChecker(), 
            new HashMapChecker()
        };

        String[] sInputs = {"egg", "foo", "paper", "bad", "abc", ""};
        String[] tInputs = {"add", "bar", "title", "foo", "abc", ""};
        boolean[] expected = {true, false, true, false, true, true};

        for (int i = 0; i < checkers.length; i++) {
            System.out.println("Testing Approach " + (4 - i) + "...");
            boolean allPassed = true;
            
            for (int j = 0; j < sInputs.length; j++) {
                boolean result = checkers[i].isIsomorphic(sInputs[j], tInputs[j]);
                if (result != expected[j]) {
                    System.out.printf("  [FAIL] s=\"%s\", t=\"%s\". Expected: %b, Got: %b%n", 
                        sInputs[j], tInputs[j], expected[j], result);
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
 * - Core pattern: Bidirectional mapping / Structural fingerprinting.
 * - Key observation: Mapping 's' to 't' is only half the battle. You must ensure 
 *   't' hasn't already been claimed by a different character in 's' (bijection).
 * - What's worth memorizing: The "Index Tracking" trick. By comparing the `lastSeen` 
 *   index of both characters, you enforce the bidirectional check simultaneously.
 * - Most common trap: When using arrays for the Index Tracking trick, you MUST 
 *   store `i + 1` (or initialize the array to -1). Otherwise, index `0` is 
 *   indistinguishable from the array's default uninitialized value of `0`.
 * - Mental trigger: "String mapping? Needs TWO dictionaries or index-fingerprinting."
 * ============================================================================
 */


import java.util.*;

public class IsomorphicStrings {

    public static boolean isIsomorphic(String s, String t) {

        // If lengths differ → impossible
        if (s.length() != t.length()) return false;

        // Map from s -> t
        Map<Character, Character> mapST = new HashMap<>();

        // Map from t -> s (to ensure uniqueness)
        Map<Character, Character> mapTS = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {

            char c1 = s.charAt(i);
            char c2 = t.charAt(i);

            // Case 1: mapping already exists → must match
            if (mapST.containsKey(c1)) {
                if (mapST.get(c1) != c2) return false;
            } else {
                mapST.put(c1, c2);
            }

            // Case 2: reverse mapping check (IMPORTANT)
            if (mapTS.containsKey(c2)) {
                if (mapTS.get(c2) != c1) return false;
            } else {
                mapTS.put(c2, c1);
            }
        }

        return true;
    }
}

import java.util.*;

public class IsomorphicStrings_Optimized {

    public static boolean isIsomorphic(String s, String t) {

        if (s.length() != t.length()) return false;

        Map<Character, Character> map = new HashMap<>();
        Set<Character> mapped = new HashSet<>();

        for (int i = 0; i < s.length(); i++) {

            char c1 = s.charAt(i);
            char c2 = t.charAt(i);

            if (map.containsKey(c1)) {
                // must match existing mapping
                if (map.get(c1) != c2) return false;
            } else {
                // ensure no two chars map to same target
                if (mapped.contains(c2)) return false;

                map.put(c1, c2);
                mapped.add(c2);
            }
        }

        return true;
    }
}


import java.util.*;

public class IsomorphicStrings_Optimized {

    public static boolean isIsomorphic(String s, String t) {

        if (s.length() != t.length()) return false;

        Map<Character, Character> map = new HashMap<>();
        Set<Character> mapped = new HashSet<>();

        for (int i = 0; i < s.length(); i++) {

            char c1 = s.charAt(i);
            char c2 = t.charAt(i);

            if (map.containsKey(c1)) {
                // must match existing mapping
                if (map.get(c1) != c2) return false;
            } else {
                // ensure no two chars map to same target
                if (mapped.contains(c2)) return false;

                map.put(c1, c2);
                mapped.add(c2);
            }
        }

        return true;
    }
}

