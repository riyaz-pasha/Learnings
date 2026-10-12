/**
 * STANDALONE STUDY NOTE: FIRST UNIQUE CHARACTER IN A STRING
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can the string be empty?" (Constraint says length >= 1, but asking proves defensive programming mindset).
 * - "Are we strictly dealing with lowercase English letters?" (Confirming this unlocks the O(1) space integer array optimization over a HashMap).
 * - "Is the input coming as a complete string in memory, or is it a stream of characters over time?" (A stream fundamentally changes the approach to a Doubly-Linked-List + Map since we can't do a traditional 'two-pass' over a stream).
 * - "What should we return if the string is huge and no unique character exists?" (Validates returning -1 as requested, and sets up expectations for worst-case O(N) traversal).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * The problem asks us to find the *first* character that appears *exactly once*. The binding constraint 
 * is that we cannot know if a character is truly unique until we have seen the entire string. Therefore, 
 * any solution requires global knowledge of the string before making a final decision.
 * 
 * --- APPROACH 1: BRUTE FORCE (Re-scanning) ---
 * 1. What I'd naturally try: I'd look at the first character, then scan the rest of the string to see 
 *    if it appears again. If it doesn't, I'm done! If it does, I move to the second character and repeat.
 * 2. Why it works: It perfectly satisfies the "first" and "non-repeating" criteria by checking characters 
 *    in their original order and exhaustively verifying uniqueness.
 * 3. Why it's too slow/costly: For every character, we potentially scan the entire string. If the string 
 *    is "aaaa...a", we do N checks for the first 'a', N checks for the second 'a', and so on.
 * 4. What work is repeated: We read the same characters over and over again to answer a question we could 
 *    have memorized ("Have I seen 'c' before?").
 * 5. Cost Breakdown:
 *    - Time Complexity: O(N^2) — because for each of the N characters, we do an O(N) scan of the string.
 *    - Space Complexity: O(1) — because we only use two pointers (i and j) and no extra data structures.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Frequency Counting - Two Passes) ---
 * 1. The Observation: We are repeating scans because we lack memory. If we do one complete pass to tally up 
 *    the frequencies of every character, a second pass can just ask the tally, "Is my frequency exactly 1?".
 * 2. How it leads to the next step: We iterate through the string and populate a `HashMap<Character, Integer>`. 
 *    Then, we iterate through the string *again* (from index 0 to N-1 to guarantee we find the *first* one) 
 *    and check the map.
 * 3. Why it works: The first pass provides the required global knowledge. The second pass enforces the order.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we do exactly two passes over the string. O(N) + O(N) = O(N).
 *    - Space Complexity: O(U) where U is unique characters (bounded to 26). However, in Java, Map nodes 
 *      and autoboxing primitives (char -> Character, int -> Integer) consume excess memory and trigger 
 *      garbage collection.
 * 
 * --- APPROACH 3: OPTIMAL (Array-based Frequency Map) ---
 * 1. The Observation: The domain of characters is strictly lowercase English letters ('a' to 'z'). 
 *    There are only 26 possible keys. We do not need a hashing algorithm or collision resolution.
 * 2. How it leads to the next step: Replace the HashMap with an `int[26]` array. 
 * 3. Which one I'd write in an interview: Approach 3 (Array). It's the industry standard for bounded-alphabet 
 *    string problems, incredibly fast, and demonstrates an understanding of how to map ASCII values to 
 *    array indices.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we do exactly two passes over the string (Pass 1: count, Pass 2: check), 
 *      and array lookups are instant.
 *    - Space Complexity: O(1) — because we allocate a fixed 26-element array (104 bytes) regardless of 
 *      whether the string has 10 characters or 10 million.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - All unique characters: "abcd" (Should return 0, the first char).
 * - All identical characters: "aaaa" (Should return -1).
 * - Unique character at the very end: "aabbc" (Must traverse the whole string twice to find it).
 * - String of length 1: "z" (Should return 0 instantly).
 * - Interleaved duplicates: "abacbd" (Should return 2 for 'c').
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * The "Two Pass" pattern is the golden rule here. 
 * Pass 1: Build the Truth. 
 * Pass 2: Query the Truth in order.
 * 
 * String: "l e e t c o d e"
 * 
 * PASS 1: Build Array (Alphabet mapped 0-25)
 * [ ... c:1, d:1, e:3, ..., l:1, ..., o:1, ..., t:1 ... ]
 * 
 * PASS 2: Iterate String and check Array
 * i=0 -> 'l' -> check map -> count is 1? YES -> RETURN 0.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: s = "aabbc"
 * 
 * Initialization: counts = [0,0,0...,0] (length 26)
 * 
 * --- PASS 1: Count ---
 * i=0, char='a', counts['a'-'a']++ -> counts[0]=1
 * i=1, char='a', counts['a'-'a']++ -> counts[0]=2
 * i=2, char='b', counts['b'-'a']++ -> counts[1]=1
 * i=3, char='b', counts['b'-'a']++ -> counts[1]=2
 * i=4, char='c', counts['c'-'a']++ -> counts[2]=1
 * Array state: [2, 2, 1, 0, 0...]
 * 
 * --- PASS 2: Find First Unique ---
 * i=0, char='a', check counts[0] -> 2. Not unique.
 * i=1, char='a', check counts[0] -> 2. Not unique.
 * i=2, char='b', check counts[1] -> 2. Not unique.
 * i=3, char='b', check counts[1] -> 2. Not unique.
 * i=4, char='c', check counts[2] -> 1. UNIQUE!
 * Return i = 4.
 * 
 * =================================================================================================
 * 
 * 6. PITFALLS
 * - Returning the *character* instead of the *index*. Always re-read the prompt!
 * - Iterating over the *HashMap/Array* in Pass 2 instead of the *String*. If you iterate over the 
 *   Array (a to z), you will find the unique character that comes first *alphabetically*, not the 
 *   one that appears first *in the string*. You MUST iterate over the string for Pass 2.
 * 
 * =================================================================================================
 * 7. INTERVIEW SCRIPT
 * "Since we only have lowercase English letters, a 26-element integer array is the most efficient 
 * way to store character frequencies. We can't know if a character is unique until we see the whole 
 * string, so this requires two passes. In the first pass, I'll count occurrences of each character. 
 * In the second pass, I'll iterate through the string again, from index 0 upwards, and return the 
 * index of the first character whose count in our array is exactly 1. If the loop finishes, I'll 
 * return -1. This gives us O(N) time complexity and O(1) space."
 * 
 * =================================================================================================
 * 
 * 8. FOLLOW-UPS
 * Q: What if the string is incredibly long (e.g., 100 million chars), and we want to minimize memory bandwidth?
 * A: In Java, we can do an "Alphabet Pass" approach. Instead of iterating the string twice, we iterate 
 *    the alphabet ('a' to 'z'). For each letter, we call `s.indexOf(char)` and `s.lastIndexOf(char)`. 
 *    If they are equal and not -1, that character is unique. We track the minimum index found. 
 *    Because `indexOf` uses highly optimized C-level CPU intrinsics, this can actually be faster in 
 *    practice for massive strings, taking O(26 * N) time which is strictly O(N), but with only 
 *    one pass per character globally.
 * 
 * Q: What if this was a continuous data stream, and you needed to return the first unique character 
 *    at any given moment? (You can't do two passes on a stream).
 * A: I would use a doubly-linked list combined with a HashMap (similar to an LRU Cache). The HashMap 
 *    maps the character to its Node in the linked list. When a character arrives for the first time, 
 *    we append it to the tail of the list and add it to the map. If it arrives again, we use the map 
 *    to find the Node in O(1) time and remove it from the list. The "first unique character" is always 
 *    just the head of the linked list (O(1) retrieval).
 */

import java.util.HashMap;
import java.util.Map;

public class FirstUniqueCharacter {

    /**
     * APPROACH 3: OPTIMAL (Array Frequency Map)
     * Time: O(N), Space: O(1)
     */
    public int firstUniqCharOptimal(String s) {
        // [Why needed]: Acts as our character frequency map for 'a' to 'z'.
        // Example: counts[0] holds the tally for 'a', counts[25] for 'z'.
        int[] counts = new int[26];
        
        // Pass 1: Build global knowledge (the frequencies)
        for (int i = 0; i < s.length(); i++) {
            // [What it does]: Subtracting 'a' maps the char to a 0-25 index.
            counts[s.charAt(i) - 'a']++;
        }
        
        // Pass 2: Query the string in order to find the *first* unique
        for (int i = 0; i < s.length(); i++) {
            // [Why needed]: If the tally for this specific character is 1, 
            // it is non-repeating. Because we loop starting from 0, it is 
            // guaranteed to be the FIRST non-repeating character.
            if (counts[s.charAt(i) - 'a'] == 1) {
                return i;
            }
        }
        
        // [Why needed]: If no character had a frequency of 1, all repeat.
        return -1;
    }

    /**
     * APPROACH 2: INTERMEDIATE (HashMap)
     * Time: O(N), Space: O(1) technically bounded by 26, but high overhead.
     */
    public int firstUniqCharHashMap(String s) {
        Map<Character, Integer> counts = new HashMap<>();
        
        // Pass 1
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            counts.put(c, counts.getOrDefault(c, 0) + 1);
        }
        
        // Pass 2
        for (int i = 0; i < s.length(); i++) {
            if (counts.get(s.charAt(i)) == 1) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * APPROACH 1: BRUTE FORCE (Nested Loops)
     * Time: O(N^2), Space: O(1)
     */
    public int firstUniqCharBruteForce(String s) {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            boolean isUnique = true;
            
            // Check every other character in the string
            for (int j = 0; j < n; j++) {
                if (i != j && s.charAt(i) == s.charAt(j)) {
                    isUnique = false;
                    break; // Found a duplicate, stop searching for this char
                }
            }
            
            if (isUnique) {
                return i;
            }
        }
        return -1;
    }

    /**
     * APPROACH 4: JAVA SPECIFIC OPTIMIZATION (Alphabet Pass via indexOf)
     * Time: O(26 * N) -> O(N), Space: O(1)
     * Note: Often beats Approach 3 in LeetCode/Java benchmarks because 
     * String.indexOf uses CPU-optimized intrinsics (SIMD instructions).
     */
    public int firstUniqCharAlphabetTrick(String s) {
        // Assume the first unique char is out of bounds
        int minIndex = Integer.MAX_VALUE; 
        
        // Iterate over the alphabet, not the string!
        for (char c = 'a'; c <= 'z'; c++) {
            int firstIdx = s.indexOf(c);
            
            // If the character exists AND its first occurrence is exactly its last occurrence...
            if (firstIdx != -1 && firstIdx == s.lastIndexOf(c)) {
                // Update our tracker for the earliest unique character found
                minIndex = Math.min(minIndex, firstIdx);
            }
        }
        
        return minIndex == Integer.MAX_VALUE ? -1 : minIndex;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Two-pass Frequency Counting.
     * 2. Key observation: Uniqueness requires global knowledge (Pass 1), 
     *    but "first" requires sequential reading (Pass 2).
     * 3. Memorize vs Understand: Understand why you MUST loop over the string 
     *    in Pass 2, not the array/map. Looping the array gives you the 
     *    alphabetically first unique character, not the index-first.
     * 4. Most common trap: Trying to solve it in one pass without extra data 
     *    structures (e.g. state machine), which is impossible for finding an index.
     * 5. Mental trigger: "First unique/non-repeating" -> `int[26]` array + Two Passes.
     */
    public static void main(String[] args) {
        FirstUniqueCharacter solution = new FirstUniqueCharacter();
        
        // Test Case 1: Standard Case
        String s1 = "leetcode";
        // 'l' is at 0, unique.
        System.out.println("Test 1 (leetcode): " + solution.firstUniqCharOptimal(s1)); // Expected: 0
        
        // Test Case 2: Duplicate at start, unique in middle
        String s2 = "loveleetcode";
        // l(repeat), o(repeat), v(unique, index 2)
        System.out.println("Test 2 (loveleetcode): " + solution.firstUniqCharOptimal(s2)); // Expected: 2
        
        // Test Case 3: All repeating
        String s3 = "aabb";
        System.out.println("Test 3 (aabb): " + solution.firstUniqCharOptimal(s3)); // Expected: -1
        
        // Test Case 4: Unique at the very end
        String s4 = "aabbc";
        System.out.println("Test 4 (aabbc): " + solution.firstUniqCharOptimal(s4)); // Expected: 4
        
        // Test Case 5: Single character
        String s5 = "z";
        System.out.println("Test 5 (z): " + solution.firstUniqCharOptimal(s5)); // Expected: 0

        // Test Case 6: Cross-check with Java trick
        System.out.println("Test 6 (Java Trick on loveleetcode): " + solution.firstUniqCharAlphabetTrick(s2)); // Expected: 2
    }
}


import java.util.HashMap;
import java.util.Map;

class Solution {
    public int firstUniqChar(String s) {
        Map<Character, Integer> frequency = new HashMap<>();

        // Count each character.
        for (char ch : s.toCharArray()) {
            frequency.merge(ch, 1, Integer::sum);
        }

        // Preserve the original string order when finding the answer.
        for (int i = 0; i < s.length(); i++) {
            if (frequency.get(s.charAt(i)) == 1) {
                return i;
            }
        }

        return -1;
    }
}
/*
Time: \(O(n)\) expected.
Auxiliary space: \(O(k)\), where \(k\) is the number of distinct characters; worst case \(O(n)\).
Useful when the input character set is not restricted to lowercase English letters.
*/


