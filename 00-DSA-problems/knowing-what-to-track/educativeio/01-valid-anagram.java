/**
 * STANDALONE STUDY NOTE: VALID ANAGRAM
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Are the strings guaranteed to be non-null, or should I handle null inputs?" (Prevents NullPointerExceptions during length checks).
 * - "Does capitalization matter, and is the input strictly lowercase?" (The constraints say lowercase English, but confirming prevents bugs if they throw in 'A' vs 'a').
 * - "Could there be whitespace, punctuation, or Unicode characters?" (Changes the data structure from a fixed int[26] array to a HashMap or larger array).
 * - "Do we consider two identical strings to be anagrams of each other?" (Clarifies the strictness of the definition; usually yes, but good to confirm).
 * - "Is memory tightly constrained?" (Helps decide if creating multiple new string objects or arrays is a dealbreaker, guiding us away from sorting).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * The problem asks us to verify if two strings share the exact same characters with the exact same 
 * frequencies. The binding constraint on naive approaches is doing "too much work" — specifically, 
 * establishing an absolute alphabetical order (sorting) when we only need to verify frequency parity.
 * 
 * --- APPROACH 1: BRUTE FORCE (Sorting) ---
 * 1. What I'd naturally try: If the strings are anagrams, sorting their characters will yield two 
 *    identical strings. I can convert to char arrays, sort them, and compare.
 * 2. Why it works: Sorting acts as a canonical representation. Any two anagrams normalize to the 
 *    exact same sorted sequence.
 * 3. Why it's too slow/costly: Sorting requires O(N log N) operations. We are paying for the overhead 
 *    of figuring out that 'a' comes before 'b', which is irrelevant to the question "do they have the 
 *    same characters?".
 * 4. What work is repeated: Comparisons between characters to establish their absolute order.
 * 5. Cost Breakdown:
 *    - Time Complexity: O(N log N) — because the sorting algorithm (usually Dual-Pivot Quicksort or 
 *      TimSort) must perform N log N comparisons to establish total alphabetical order.
 *    - Space Complexity: O(N) — because strings are immutable in Java, forcing us to allocate O(N) 
 *      extra memory for the `toCharArray()` copies before we can sort them.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Frequency Counting) ---
 * 1. The Observation: We don't need order, we just need counts. This is a classic "histogram" or 
 *    "frequency map" problem.
 * 2. How it leads to the next step: We can iterate through str1 and count character occurrences in a 
 *    HashMap<Character, Integer>. Then iterate through str2 and decrement the counts. If any count 
 *    goes below zero or remains non-zero, they aren't anagrams.
 * 3. Why it works: It directly answers the core requirement—matching multiplicities of elements.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we visit each character exactly once for str1 and once for str2, 
 *      doing O(1) hash map operations per character.
 *    - Space Complexity: O(U) where U is unique characters — because we store distinct characters and 
 *      their Integer counts. Bounded at O(1) since U <= 26, but carries heavy Java object overhead (boxing).
 * 
 * --- APPROACH 3: OPTIMAL (Array-based Frequency Counting) ---
 * 1. The Observation: A HashMap is overkill. We know the "keys" are strictly lowercase English letters ('a'-'z').
 *    This is a dense, contiguous, tiny integer domain (0 to 25).
 * 2. How it leads to the next step: We map 'a' to index 0, 'b' to 1, etc. A simple `int[26]` array 
 *    replaces the HashMap, bypassing all hashing algorithms, collision resolution, and Integer object boxing.
 * 3. Which one I'd write in an interview: Approach 3 (Array). It's incredibly fast, showcases an 
 *    understanding of ASCII manipulation (char math), has minimal code risk, and avoids the heavy 
 *    autoboxing penalty of HashMaps in Java.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we linearly scan the strings doing constant-time primitive array 
 *      lookups and increments. Fast enough for massive strings.
 *    - Space Complexity: O(1) — because we only ever allocate an array of exactly 26 ints (104 bytes), 
 *      which does not grow regardless of how long the input strings get (N = 10^3 or N = 10^9).
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Length mismatch: str1="abc", str2="ab". (Must be caught immediately; they can't be anagrams).
 * - Same letters, different counts: str1="aab", str2="abb". (Tests if our logic actually respects 
 *   frequencies, not just the presence of a character).
 * - All identical characters: str1="aaaa", str2="aaaa".
 * - Strings of length 1: str1="a", str2="a" or str1="a", str2="b".
 * 
 * =================================================================================================
 * 
 * 4. DRY RUN (Optimal Approach on "aab" vs "abb" - The Tempting Pitfall Case)
 * 
 * Initialization: counts = [0,0,0,...,0] (length 26)
 * Step 1: Iterate through str1 ("aab") and str2 ("abb") simultaneously.
 * 
 * i=0: str1[0]='a' -> counts['a'-'a']++ (counts[0] becomes 1)
 *      str2[0]='a' -> counts['a'-'a']-- (counts[0] becomes 0)
 *      State: [0, 0, 0...]
 * 
 * i=1: str1[1]='a' -> counts['a'-'a']++ (counts[0] becomes 1)
 *      str2[1]='b' -> counts['b'-'a']-- (counts[1] becomes -1)
 *      State: [1, -1, 0...]
 * 
 * i=2: str1[2]='b' -> counts['b'-'a']++ (counts[1] becomes 0)
 *      str2[2]='b' -> counts['b'-'a']-- (counts[1] becomes -1)
 *      State: [1, -1, 0...]
 * 
 * End of loop. Now we check the `counts` array.
 * We see counts[0] = 1 (meaning str1 had one more 'a' than str2).
 * We return false.
 * 
 * *Pitfall*: A common mistake is just XORing all characters or summing their ASCII values. 
 * E.g., 'a'+'d' (97+100=197) and 'b'+'c' (98+99=197). If you just sum characters, "ad" and "bc" 
 * look like anagrams! The frequency array explicitly maps distinct characters to distinct buckets, 
 * avoiding this collision.
 * 
 * =================================================================================================
 * 
 * 5. FOLLOW-UPS
 * Q: What if the inputs contain Unicode characters (e.g., emojis, foreign alphabets)?
 * A: The `int[26]` array fails because the character space is vast and sparse. I would switch back 
 *    to the HashMap approach (Approach 2) to maintain O(N) time without allocating an impossibly 
 *    huge array.
 * 
 * Q: How would you optimize if the strings are extremely large and stored on disk?
 * A: We can't load them entirely into memory. We would stream the files chunk by chunk. We can read 
 *    file 1, building the frequency map in memory (which only takes 26 ints), then stream file 2, 
 *    decrementing the map. If counts dip below zero during stream 2, we can early-exit.
 * 
 * Q: What if you had to group an array of strings into anagram sets?
 * A: I would use a `HashMap<String, List<String>>`. The key would be the serialized frequency array 
 *    (e.g., "1#0#2#..." representing counts of a,b,c) or the sorted string. 
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ValidAnagram {

    /**
     * APPROACH 3: OPTIMAL (Array Frequency Map)
     * Time: O(N), Space: O(1)
     */
    public boolean isAnagramOptimal(String str1, String str2) {
        // [Why needed]: If lengths differ, it's mathematically impossible for them to be anagrams.
        // [What it does]: O(1) early exit preventing useless O(N) iteration.
        if (str1.length() != str2.length()) {
            return false;
        }

        // [Why needed]: We need a data structure to hold character tallies.
        // [What it does]: Allocates exactly 26 integers. 
        // Example: if we process 'c', it maps to index ('c' - 'a') = (99 - 97) = 2.
        // So counts[2] tracks occurrences of 'c'.
        int[] counts = new int[26];

        // Single-pass optimization: since lengths are equal, we can iterate once.
        // We add for str1's characters and subtract for str2's characters.
        for (int i = 0; i < str1.length(); i++) {
            
            // [Why needed]: Tracks a character from str1.
            // char - 'a' computes the exact offset from 0 to 25.
            counts[str1.charAt(i) - 'a']++;
            
            // [Why needed]: Offsets the count using str2's character.
            // If they are anagrams, every increment is perfectly offset by a decrement eventually.
            counts[str2.charAt(i) - 'a']--;
        }

        // [Why needed]: We must verify the net result is zero everywhere.
        for (int count : counts) {
            if (count != 0) {
                return false; // Found an imbalance.
            }
        }

        return true;
    }

    /**
     * APPROACH 1: BRUTE FORCE (Sorting)
     * Time: O(N log N), Space: O(N)
     */
    public boolean isAnagramSorting(String str1, String str2) {
        if (str1.length() != str2.length()) return false;

        // [Why needed]: Strings in Java are immutable; we cannot sort them in-place.
        // Space overhead incurred here: O(N) memory allocated for the new char arrays.
        char[] arr1 = str1.toCharArray();
        char[] arr2 = str2.toCharArray();

        // Time overhead incurred here: O(N log N) comparison-based sorting.
        Arrays.sort(arr1);
        Arrays.sort(arr2);

        // [What it does]: Linearly checks if the sorted structures match exactly.
        return Arrays.equals(arr1, arr2);
    }

    /**
     * APPROACH 2: INTERMEDIATE (HashMap)
     * Time: O(N), Space: O(U) -> Technically O(1) since U <= 26, but high constant overhead.
     */
    public boolean isAnagramHashMap(String str1, String str2) {
        if (str1.length() != str2.length()) return false;

        // HashMap involves boxing char to Character, and int to Integer.
        // This is memory-heavy compared to primitives.
        Map<Character, Integer> freqMap = new HashMap<>();

        for (char c : str1.toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }

        for (char c : str2.toCharArray()) {
            int currentCount = freqMap.getOrDefault(c, 0);
            if (currentCount == 0) {
                // If str2 has a character not in str1, or too many of a character.
                return false;
            }
            freqMap.put(c, currentCount - 1);
        }

        return true;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Frequency counting (Histogram).
     * 2. Key observation: We only care about character multiplicity, not canonical order. 
     *    Sorting wastes time establishing order.
     * 3. Memorize vs Understand: Memorize the char-to-index trick (`c - 'a'`). Understand *why* 
     *    we do it (to avoid HashMap object overhead for dense, bounded key spaces).
     * 4. Common trap: Summing ASCII values instead of using distinct buckets, which causes 
     *    false positives (e.g. "ad" == "bc").
     * 5. Mental trigger: When you see "anagram" or "permutation" + "lowercase letters", immediately 
     *    think: `int[] map = new int[26]`.
     */
    public static void main(String[] args) {
        ValidAnagram solution = new ValidAnagram();

        // Test Case 1: Standard Anagram
        String s1 = "anagram";
        String t1 = "nagaram";
        System.out.println("Test 1 (Standard): " + solution.isAnagramOptimal(s1, t1)); // Expected: true

        // Test Case 2: Length Mismatch Edge Case
        String s2 = "rat";
        String t2 = "car";
        System.out.println("Test 2 (Different Chars): " + solution.isAnagramOptimal(s2, t2)); // Expected: false

        // Test Case 3: Pitfall Edge Case (Same chars, diff frequency)
        String s3 = "aab";
        String t3 = "abb";
        System.out.println("Test 3 (Freq Mismatch): " + solution.isAnagramOptimal(s3, t3)); // Expected: false

        // Test Case 4: Identical Strings Edge Case
        String s4 = "aaaa";
        String t4 = "aaaa";
        System.out.println("Test 4 (Identical): " + solution.isAnagramOptimal(s4, t4)); // Expected: true
        
        // Test Case 5: Single Character
        String s5 = "a";
        String t5 = "b";
        System.out.println("Test 5 (Single Char Mismatch): " + solution.isAnagramOptimal(s5, t5)); // Expected: false
    }
}
