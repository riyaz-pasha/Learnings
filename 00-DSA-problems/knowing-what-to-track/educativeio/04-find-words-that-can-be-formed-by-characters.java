/**
 * STANDALONE STUDY NOTE: FIND WORDS THAT CAN BE FORMED BY CHARACTERS
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Does each valid word consume characters from `chars` permanently, or does the inventory reset for every word?" (The prompt says 'per word', meaning they are checked independently. This is the most critical assumption to verify).
 * - "Are we strictly dealing with lowercase English letters?" (Confirming this unlocks the O(1) space integer array optimization).
 * - "Can words be longer than the `chars` string?" (If so, we can instantly skip them without checking individual letters).
 * - "What if `words` is empty?" (Return 0).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * The problem is essentially checking if multiple "ransom notes" (the words) can be independently 
 * built from a single "magazine" (`chars`). The binding constraint is doing this validation repeatedly 
 * for up to 1000 words without causing massive garbage collection overhead from recreating data structures.
 * 
 * --- APPROACH 1: BRUTE FORCE (String Manipulation) ---
 * 1. What I'd naturally try: For every word, I will make a mutable copy of `chars` (like a StringBuilder). 
 *    For each letter in the word, I'll search for it in the builder. If found, I delete it. If not, the word fails.
 * 2. Why it works: It perfectly simulates picking letters out of a pool one by one.
 * 3. Why it's too slow/costly: Finding and deleting a character in a String/StringBuilder is an O(C) operation 
 *    because the underlying array has to shift elements. We do this for every letter of every word.
 * 4. What work is repeated: We are repeatedly scanning the remaining characters and shifting arrays in memory.
 * 5. Cost Breakdown:
 *    - Time Complexity: O(W * L * C) — where W is number of words, L is max length of a word, and C is length of chars. 
 *      For each letter of each word, we scan and shift up to C characters.
 *    - Space Complexity: O(C) — because we allocate a new StringBuilder of size C for every single word we check.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Frequency Counting) ---
 * 1. The Observation: We don't need to actually "delete" characters. We just need to know if the required 
 *    frequency of each letter in the word is less than or equal to the frequency in `chars`.
 * 2. How it leads to the next step: We can count the frequencies of `chars` once into a `HashMap<Character, Integer>`. 
 *    Then, for each word, we count its frequencies into a *new* HashMap, and verify that for every key, 
 *    the word's count <= chars' count.
 * 3. Why it works: We stop shifting arrays and reduce the check to a mathematical subset comparison.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(C + W * L) — because we scan `chars` once O(C), and then for each word, we scan its 
 *      characters O(L) and compare maps O(U) where U is unique chars.
 *    - Space Complexity: O(U) — where U is unique characters (bounded to 26). However, in Java, allocating a new 
 *      HashMap, Character objects, and Integer objects for every word is very heavy on the garbage collector.
 * 
 * --- APPROACH 3: OPTIMAL (Array-based Frequency Map) ---
 * 1. The Observation: The domain is strictly lowercase English letters ('a' to 'z'). A HashMap is overkill.
 * 2. How it leads to the next step: We replace the HashMap with an `int[26]` array for `chars`. For each word, 
 *    we can either build a temporary `int[26]` for the word and compare, or clone the `chars` array and deduct.
 * 3. Which one I'd write in an interview: Approach 3 (Array). Building a temporary `int[26]` for each word is 
 *    blazing fast, allocates almost no memory, and completely avoids autoboxing.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(C + W * L) — We tally the `chars` string once O(C). For each word O(W), we tally its 
 *      letters O(L) and do a fixed 26-step comparison O(1). 
 *    - Space Complexity: O(1) — We only ever allocate arrays of exactly 26 integers, regardless of the input size.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Word is longer than chars: `word="hello"`, `chars="hi"`. (Instantly skip).
 * - Exact match: `word="cat"`, `chars="tac"`.
 * - Insufficient quantity: `word="good"`, `chars="god"`. (Fails because it needs two 'o's but only has one).
 * - Chars has excess: `word="a"`, `chars="abcdefghijklmnopqrstuvwxyz"`. (Succeeds).
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * This is "Ransom Note" applied in a loop. The master inventory never changes.
 * 
 * Master Inventory (`chars`): [ a:2, b:1, c:1, ..., z:0 ]
 * 
 * Word 1 ("cab"): 
 *   Needs: [ a:1, b:1, c:1 ] -> All <= Master? YES. (Add length 3 to sum)
 * Word 2 ("bad"): 
 *   Needs: [ a:1, b:1, c:0, d:1 ] -> 'd' needs 1, Master has 0. NO. (Skip)
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: words = ["cat", "bt", "hat", "tree"], chars = "atach"
 * 
 * Init: sum = 0
 * Master Array for "atach": [ a:2, c:1, h:1, t:1, (rest 0) ]
 * 
 * Word "cat": 
 *   Build local array: [ a:1, c:1, t:1 ]
 *   Compare: a(1)<=2(yes), c(1)<=1(yes), t(1)<=1(yes). Valid! sum += 3.
 * Word "bt":
 *   Build local array: [ b:1, t:1 ]
 *   Compare: b(1)<=0(NO!). Invalid!
 * Word "hat":
 *   Build local array: [ a:1, h:1, t:1 ]
 *   Compare: a(1)<=2(yes), h(1)<=1(yes), t(1)<=1(yes). Valid! sum += 3.
 * Word "tree":
 *   Length 4 > 5? No.
 *   Build local array: [ e:2, r:1, t:1 ]
 *   Compare: e(2)<=0(NO!). Invalid!
 * 
 * Result: sum = 6.
 * 
 * =================================================================================================
 * 
 * 6. FOLLOW-UPS
 * Q: What if `words` contained millions of entries?
 * A: Because the validation of each word is completely independent and only relies on reading the 
 *    shared `chars` array, we could process the `words` array in parallel using Java Streams 
 *    (`Arrays.stream(words).parallel()`).
 * 
 * Q: What if the character set was all Unicode characters?
 * A: We would have to abandon the `int[26]` array. We'd use a `HashMap` for the master inventory, 
 *    and for each word, we'd build a local `HashMap` or clone the master and decrement.
 */

import java.util.HashMap;
import java.util.Map;

public class FindWordsThatCanBeFormed {

    /**
     * APPROACH 3: OPTIMAL (Array Frequency Map)
     * Time: O(C + W * L), Space: O(1)
     */
    public int countCharactersOptimal(String[] words, String chars) {
        // [Why needed]: Acts as our permanent master inventory for 'a' to 'z'.
        int[] masterCount = new int[26];
        for (int i = 0; i < chars.length(); i++) {
            // [What it does]: Maps 'a' to 0, 'z' to 25 and increments the tally.
            masterCount[chars.charAt(i) - 'a']++;
        }

        int totalLength = 0;

        for (String word : words) {
            // [Why needed]: Physical impossibility check. Saves O(L) time.
            if (word.length() > chars.length()) {
                continue;
            }

            // [Why needed]: We need a fresh inventory for this specific word to compare against the master.
            int[] wordCount = new int[26];
            boolean isGood = true;

            for (int i = 0; i < word.length(); i++) {
                int charIdx = word.charAt(i) - 'a';
                wordCount[charIdx]++;
                
                // [Why needed]: Early exit. If at any point this word requires more of a letter 
                // than the master has, it's invalid. We don't even need to finish tallying the word.
                if (wordCount[charIdx] > masterCount[charIdx]) {
                    isGood = false;
                    break;
                }
            }

            // [What it does]: If the word successfully passed all checks, add its length.
            if (isGood) {
                totalLength += word.length();
            }
        }

        return totalLength;
    }

    /**
     * APPROACH 2: INTERMEDIATE (HashMap)
     * Time: O(C + W * L), Space: O(U) -> High overhead from object creation
     */
    public int countCharactersHashMap(String[] words, String chars) {
        Map<Character, Integer> masterMap = new HashMap<>();
        for (char c : chars.toCharArray()) {
            masterMap.put(c, masterMap.getOrDefault(c, 0) + 1);
        }

        int totalLength = 0;

        for (String word : words) {
            if (word.length() > chars.length()) continue;

            // Allocating a new map per word is heavy on GC
            Map<Character, Integer> wordMap = new HashMap<>();
            boolean isGood = true;

            for (char c : word.toCharArray()) {
                wordMap.put(c, wordMap.getOrDefault(c, 0) + 1);
                
                if (wordMap.get(c) > masterMap.getOrDefault(c, 0)) {
                    isGood = false;
                    break;
                }
            }

            if (isGood) {
                totalLength += word.length();
            }
        }
        return totalLength;
    }

    /**
     * APPROACH 1: BRUTE FORCE (String Manipulation)
     * Time: O(W * L * C), Space: O(C)
     */
    public int countCharactersBruteForce(String[] words, String chars) {
        int totalLength = 0;

        for (String word : words) {
            if (word.length() > chars.length()) continue;

            // Make a mutable copy of chars for this specific word
            StringBuilder availableChars = new StringBuilder(chars);
            boolean isGood = true;

            for (char c : word.toCharArray()) {
                // O(C) search
                int index = availableChars.indexOf(String.valueOf(c));
                if (index == -1) {
                    isGood = false;
                    break; // Missing a required character
                }
                // O(C) deletion because of array shifting
                availableChars.deleteCharAt(index);
            }

            if (isGood) {
                totalLength += word.length();
            }
        }
        return totalLength;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Frequency counting & validation against a master inventory.
     * 2. Key observation: The master inventory (`chars`) does not change. We can validate 
     *    each word independently using an early-exit inner loop.
     * 3. Memorize vs Understand: Understand that `int[26]` arrays are incredibly cheap to 
     *    instantiate. Building a new one for every word is faster than cloning maps.
     * 4. Most common trap: Modifying the master inventory directly and forgetting to reset 
     *    it for the next word.
     * 5. Mental trigger: "form strings from characters" -> Master `int[26]` + Local `int[26]`.
     */
    public static void main(String[] args) {
        FindWordsThatCanBeFormed solution = new FindWordsThatCanBeFormed();

        // Test Case 1: Standard Example
        String[] words1 = {"cat", "bt", "hat", "tree"};
        String chars1 = "atach";
        // cat (3) + hat (3) = 6
        System.out.println("Test 1 (Standard): " + solution.countCharactersOptimal(words1, chars1)); // Expected: 6

        // Test Case 2: Insufficient Quantity
        String[] words2 = {"hello", "world", "leetcode"};
        String chars2 = "welldonehoneyr";
        // hello (5) + world (5) = 10. leetcode fails on 't','c'
        System.out.println("Test 2 (Insufficient): " + solution.countCharactersOptimal(words2, chars2)); // Expected: 10

        // Test Case 3: Length Edge Case
        String[] words3 = {"toolong"};
        String chars3 = "short";
        System.out.println("Test 3 (Word too long): " + solution.countCharactersOptimal(words3, chars3)); // Expected: 0

        // Test Case 4: Multiple identical words
        String[] words4 = {"a", "a", "a"};
        String chars4 = "a";
        // All independent. Each "a" is valid against master "a".
        System.out.println("Test 4 (Independent checks): " + solution.countCharactersOptimal(words4, chars4)); // Expected: 3
    }
}



class Solution {

    public int countCharacters(String[] words, String chars) {

        // Step 1: Count how many times each character is available in chars.
        // Index 0 represents 'a', index 1 represents 'b', ..., index 25 represents 'z'.
        int[] available = new int[26];

        for (char ch : chars.toCharArray()) {
            available[ch - 'a']++;
        }

        int totalLength = 0;

        // Step 2: Check whether each word can be formed using available characters.
        for (String word : words) {

            // Clone the original frequencies because each word gets its own supply.
            // Using one shared array would incorrectly consume characters across words.
            int[] remaining = available.clone();

            boolean isGood = true;

            for (char ch : word.toCharArray()) {

                int index = ch - 'a';

                // No copies of this character remain for the current word.
                if (remaining[index] == 0) {
                    isGood = false;
                    break;
                }

                // Consume exactly one occurrence of this character.
                remaining[index]--;
            }

            // Add the word's length only if every character was available.
            if (isGood) {
                totalLength += word.length();
            }
        }

        return totalLength;
    }
}

