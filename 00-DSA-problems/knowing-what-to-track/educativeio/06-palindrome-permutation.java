/**
 * STANDALONE STUDY NOTE: PALINDROME PERMUTATION
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Do we need to return the actual palindrome string, or just a boolean?" (Crucial: returning a boolean means we can just use math/counting instead of string building).
 * - "Is the input strictly lowercase English letters?" (Confirms we can use an O(1) fixed-size array or a bitmask).
 * - "Can the string contain spaces or punctuation?" (If yes, we'd need to clarify if spaces count towards the palindrome or should be ignored).
 * - "Can the string be empty?" (Constraints say >= 1, but asking shows defensive programming).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * The problem asks if *any* rearrangement of a string reads the same forwards and backwards.
 * The binding constraint is that generating actual permutations is mathematically explosive (factorial time), 
 * so we must find a structural property that guarantees a palindrome can be formed without actually forming it.
 * 
 * --- APPROACH 1: BRUTE FORCE (Generate All Permutations) ---
 * 1. What I'd naturally try: I could write a recursive backtracking function to generate every possible 
 *    permutation of the string. For each one, I'd check if `str.equals(reverse(str))`.
 * 2. Why it works: It exhaustively checks the entire possibility space. If a valid configuration exists, it will find it.
 * 3. Why it's too slow/costly: Permutations grow factorially. A string of length 15 has 15! (1.3 trillion) permutations.
 * 4. What work is repeated: We build massive amounts of completely invalid strings (like "abcdeedcba" vs "bacdeedcab") 
 *    when we could just count the ingredients.
 * 5. Cost Breakdown:
 *    - Time Complexity: O(N! * N) — because there are N! permutations, and checking each takes O(N) time.
 *    - Space Complexity: O(N) — because the recursion stack goes N levels deep, and we allocate new strings of length N.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Frequency Parity) ---
 * 1. The Observation: What structurally makes a palindrome? It's a mirror. Every character on the left 
 *    must have a matching character on the right. This means characters must appear in *pairs* (even frequency). 
 *    The only exception is the exact center character, which can appear without a pair (odd frequency).
 * 2. How it leads to the next step: We just need to count frequencies! Iterate through the string, populate a 
 *    `HashMap<Character, Integer>`. Then, iterate through the map's values. If more than ONE character has an 
 *    odd frequency, a palindrome is impossible.
 * 3. Why it works: It perfectly abstracts the problem from "string manipulation" to "parity counting".
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we scan the string once O(N), and iterate through the map O(U).
 *    - Space Complexity: O(U) where U is unique characters. For Java, this means allocating Object wrappers 
 *      (Character, Integer) and Map Node objects.
 * 
 * --- APPROACH 3: OPTIMAL (Array Frequency Map) ---
 * 1. The Observation: The domain is limited to 26 lowercase English letters. A HashMap is bloated overkill.
 * 2. How it leads to the next step: Replace the Map with an `int[26]` array. 
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we scan the string once, using O(1) primitive array lookups, then scan 
 *      the 26-element array.
 *    - Space Complexity: O(1) — because we allocate exactly one array of 26 integers (104 bytes).
 * 
 * --- APPROACH 4: NINJA / BIT MANIPULATION (The ultimate flex) ---
 * 1. The Observation: We don't even care about the *actual* count (2, 4, 6). We only care about the *parity* 
 *    (Even vs Odd). Even = 0, Odd = 1. This is a binary state.
 * 2. How it leads to the next step: We can use a single 32-bit integer as a bitmask. For each character, we flip 
 *    its corresponding bit (using XOR). At the end, the integer will have bits set to 1 *only* for characters 
 *    that appeared an odd number of times. We can check if a number has at most one bit set using `(mask & (mask - 1)) == 0`.
 * 3. Which one I'd write in an interview: Approach 3 (Array) is the safest and most readable. I would *mention* 
 *    Approach 4 (Bitmask) to show mastery of low-level optimization.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Length 1: "a" -> Always TRUE (0 pairs, 1 center).
 * - All identical: "aaaa" -> TRUE (2 pairs, 0 center).
 * - Exactly one of everything: "abcdef" -> FALSE (Multiple odds).
 * - Already a palindrome: "racecar" -> TRUE.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * The Anatomy of a Palindrome:
 * [ MATCHING LEFT HALF ] + [ OPTIONAL CENTER CHAR ] + [ MATCHING RIGHT HALF ]
 *          (Even)                    (Odd)                    (Even)
 * 
 * Example: "carrace"
 * Characters: c:2, a:2, r:2, e:1
 * Parities:   c:Even, a:Even, r:Even, e:Odd
 * Total Odds: 1. (<= 1, so TRUE!)
 * Layout:     "car" + "e" + "rac"
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Array Approach)
 * Target: st = "code" (The "Too Many Odds" case)
 * 
 * Init: counts = [0,0...0] (size 26)
 * 
 * Pass 1: Build frequencies
 * i=0, 'c' -> counts[2]=1
 * i=1, 'o' -> counts[14]=1
 * i=2, 'd' -> counts[3]=1
 * i=3, 'e' -> counts[4]=1
 * 
 * Pass 2: Count odds
 * counts[2] is 1 (odd) -> oddCount = 1
 * counts[3] is 1 (odd) -> oddCount = 2
 * counts[4] is 1 (odd) -> oddCount = 3
 * counts[14] is 1 (odd) -> oddCount = 4
 * 
 * Result: oddCount (4) > 1, so return FALSE.
 * 
 * =================================================================================================
 * 
 * 6. FOLLOW-UPS
 * Q: What if the string contains all possible Unicode characters?
 * A: The `int[26]` array fails. The bitmask also fails because a 32-bit int can't hold 65k+ flags. 
 *    I'd revert to Approach 2, but instead of a HashMap counting frequencies, I'd use a `HashSet`. 
 *    Add the char if it's not in the set, remove it if it is. At the end, `set.size() <= 1` gives the answer.
 * 
 * Q: How does the bitmask `(mask & (mask - 1)) == 0` trick work?
 * A: `mask - 1` flips the lowest set bit and all bits to the right of it. ANDing it with `mask` erases 
 *    that lowest set bit. If the number only had ONE bit set in the first place, erasing it leaves 0.
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PalindromePermutation {

    /**
     * APPROACH 3: OPTIMAL (Array Frequency Map)
     * Time: O(N), Space: O(1)
     */
    public boolean canPermutePalindromeOptimal(String st) {
        // [Why needed]: Acts as our frequency counter for 'a' to 'z'.
        int[] counts = new int[26];
        
        // Count all character frequencies
        for (int i = 0; i < st.length(); i++) {
            counts[st.charAt(i) - 'a']++;
        }
        
        int oddCount = 0;
        
        // [What it does]: Scans the 26 buckets to check parity rule.
        for (int count : counts) {
            // [Why needed]: We only care about characters that appear an odd number of times.
            if (count % 2 != 0) {
                oddCount++;
            }
            // Early exit optimization
            if (oddCount > 1) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * APPROACH 4: NINJA OPTIMAL (Bitmask)
     * Time: O(N), Space: O(1) - Literally a single primitive integer.
     */
    public boolean canPermutePalindromeBitmask(String st) {
        int mask = 0;
        
        for (int i = 0; i < st.length(); i++) {
            // Calculate the bit position (0 to 25)
            int bitOffset = st.charAt(i) - 'a';
            
            // [What it does]: XOR operator (^) flips the bit at the given offset.
            // If we see 'a', we flip the 0th bit to 1. 
            // If we see 'a' again, we flip it back to 0 (canceling out the pair).
            mask ^= (1 << bitOffset);
        }
        
        // [Why needed]: A valid palindrome permutation has AT MOST one character with odd frequency.
        // That means our mask should have AT MOST one bit set to 1.
        // mask == 0 handles the case where all characters appear evenly (0 bits set).
        // (mask & (mask - 1)) == 0 is the classic bit-hack to check if exactly one bit is set.
        return mask == 0 || (mask & (mask - 1)) == 0;
    }

    /**
     * APPROACH 2B: INTERMEDIATE (HashSet Parity Tracking)
     * Time: O(N), Space: O(U)
     * Note: This is the best approach if the character set is unbounded (Unicode).
     */
    public boolean canPermutePalindromeHashSet(String st) {
        Set<Character> unpairedChars = new HashSet<>();
        
        for (int i = 0; i < st.length(); i++) {
            char c = st.charAt(i);
            // If it's already in the set, we found a pair! Remove it.
            if (!unpairedChars.add(c)) {
                unpairedChars.remove(c);
            }
        }
        
        // If 1 or 0 characters are left without a pair, it's a valid palindrome.
        return unpairedChars.size() <= 1;
    }

    /**
     * APPROACH 2A: INTERMEDIATE (HashMap Frequency)
     * Time: O(N), Space: O(U)
     */
    public boolean canPermutePalindromeHashMap(String st) {
        Map<Character, Integer> counts = new HashMap<>();
        
        for (char c : st.toCharArray()) {
            counts.put(c, counts.getOrDefault(c, 0) + 1);
        }
        
        int odds = 0;
        for (int count : counts.values()) {
            if (count % 2 != 0) {
                odds++;
            }
        }
        
        return odds <= 1;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Frequency Counting / Parity checking.
     * 2. Key observation: Palindromes require mirror symmetry. Every character must be paired (even), 
     *    with a maximum of ONE unpaired character (odd) allowed in the dead center.
     * 3. Memorize vs Understand: Understand that "permutation" + "palindrome" = "count evens and odds".
     * 4. Mental trigger: "Palindrome permutation" -> `int[26]` array (or Bitmask), `oddCount <= 1`.
     */
    public static void main(String[] args) {
        PalindromePermutation solution = new PalindromePermutation();

        // Test Case 1: Standard Valid ("racecar" rearranged)
        String s1 = "carrace";
        System.out.println("Test 1 (carrace): " + solution.canPermutePalindromeOptimal(s1)); // Expected: true

        // Test Case 2: Standard Invalid
        String s2 = "code";
        System.out.println("Test 2 (code): " + solution.canPermutePalindromeOptimal(s2)); // Expected: false

        // Test Case 3: All Evens
        String s3 = "aabb";
        System.out.println("Test 3 (aabb): " + solution.canPermutePalindromeOptimal(s3)); // Expected: true

        // Test Case 4: Length 1 Edge Case
        String s4 = "a";
        System.out.println("Test 4 (a): " + solution.canPermutePalindromeOptimal(s4)); // Expected: true
        
        // Test Case 5: Verify Bitmask Approach
        System.out.println("Test 5 (Bitmask on carrace): " + solution.canPermutePalindromeBitmask(s1)); // Expected: true
    }
}


import java.util.HashSet;
import java.util.Set;

class Solution {
    public boolean canPermutePalindrome(String st) {
        Set<Character> oddCharacters = new HashSet<>();

        for (char ch : st.toCharArray()) {
            // First occurrence (or any transition to odd): add it.
            // Next occurrence (transition to even): remove it.
            if (!oddCharacters.add(ch)) {
                oddCharacters.remove(ch);
            }
        }

        // Zero or one character may have an odd frequency.
        return oddCharacters.size() <= 1;
    }
}
/*
Complexity
- Time: \(O(n)\) expected.
- Auxiliary space: \(O(k)\), where \(k\) is the number of distinct characters, at most 26 under the given constraints. Thus, it is also \(O(1)\) for this problem.
*/


