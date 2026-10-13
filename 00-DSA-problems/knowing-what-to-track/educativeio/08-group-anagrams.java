/**
 * STANDALONE STUDY NOTE: GROUP ANAGRAMS
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can the input list or strings be empty or contain null values?" (Defensive check for empty or null strings).
 * - "Are characters strictly lowercase English letters?" (Confirms constraints allow fixed-size array signatures or sorted string keys).
 * - "Does the order of groups or order of strings within groups matter?" (Prompt explicitly states it does not matter).
 * - "What is the maximum length of strings and the number of strings?" (strs.length <= 10^3, length <= 100, which means N * L log L is extremely small and well within time limits).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to partition a list of strings into buckets where each bucket contains mutual anagrams. 
 * The binding constraint is avoiding an O(N^2) comparison of every string against every other string, 
 * which would be far too slow. We need a canonical representation for an anagram.
 * 
 * --- APPROACH 1: BRUTE FORCE (All-Pairs Comparison) ---
 * 1. What I'd naturally try: Iterate through the list, take the first string, and compare it against 
 *    every subsequent string using our Anagram-checking logic. Group the matches together, and repeat for unvisited strings.
 * 2. Why it works: It correctly identifies all anagram equivalence classes.
 * 3. Why it's too slow/costly: Comparing every string against every other string leads to a quadratic number of 
 *    comparisons.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^2 * L) — where N is number of strings and L is max length of a string, due to repeated comparisons.
 *    - Space Complexity: O(N) — to keep track of visited elements and group structures.
 * 
 * --- APPROACH 2: INTERMEDIATE (Sorted String as Canonical Key) ---
 * 1. The Observation: Any two anagrams, when sorted alphabetically, yield the exact same string (e.g., "eat", "tea", 
 *    "ate" all become "aet"). This sorted string can serve as a unique "signature" or canonical key for the group.
 * 2. How it leads to the next step: Use a `HashMap<String, List<String>>`. For each string, convert it to a char array, 
 *    sort it, convert it back to a String, and use it as the key to append the original string to a list in the map.
 * 3. Why it works: It groups mutual anagrams into the same hash bucket instantly in a single pass over the strings.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N * L log L) — because for each of the N strings, we sort its characters of length L.
 *    - Space Complexity: O(N * L) — to store all strings inside the hash map values and keys.
 * 
 * --- APPROACH 3: OPTIMAL (Frequency Signature as Canonical Key) ---
 * 1. The Observation: Sorting takes O(L log L). Since strings consist entirely of lowercase English letters, we can 
 *    create a frequency signature instead (e.g., "#1#0#1#...#0" representing counts of a, b, c...) in O(L) time.
 * 2. How it leads to the next step: We build a frequency array for each string, serialize it into a string or custom 
 *    object key, and use a `HashMap<String, List<String>>`. 
 * 3. Which one I'd write in an interview: Approach 2 (Sorted String as Key) or Approach 3 (Frequency Key). 
 *    Approach 2 is cleaner and less error-prone under interview pressure because building a delimiter-separated string 
 *    from an `int[26]` array can sometimes have edge cases (e.g., double-digit counts like 12 needing a separator like `#`). 
 *    Approach 2's `Arrays.sort()` is exceptionally clean and fast enough since L <= 100. Let's implement Approach 2 as 
 *    our primary optimal solution.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N * L log L) — because we sort characters of each string.
 *    - Space Complexity: O(N * L) — auxiliary storage for the HashMap groupings.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Empty input list: `strs = []` -> Returns an empty list.
 * - Single-character strings: `strs = ["a", "b", "a"]` -> Handled correctly.
 * - Strings with no anagrams: `strs = ["abc", "def"]` -> Each gets its own group.
 * - Empty string element: `strs = [""]` -> Key becomes `""`, grouped together.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Canonical Representation:
 * 
 * Input List: ["eat", "tea", "tan", "ate", "nat", "bat"]
 * 
 * Step 1: Compute Canonical Key (Sorted Form)
 * "eat" -> "aet"
 * "tea" -> "aet"
 * "tan" -> "ant"
 * "ate" -> "aet"
 * "nat" -> "ant"
 * "bat" -> "abt"
 * 
 * Step 2: HashMap Buckets
 * Key "aet" -> ["eat", "tea", "ate"]
 * Key "ant" -> ["tan", "nat"]
 * Key "abt" -> ["bat"]
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: strs = ["eat", "tea", "tan"]
 * 
 * Init: map = {}
 * 
 * i=0, word = "eat":
 *   char[] arr = ['e', 'a', 't'] -> sorted: ['a', 'e', 't'] -> key = "aet"
 *   map.get("aet") is null. Put new List, add "eat".
 *   map: {"aet": ["eat"]}
 * 
 * i=1, word = "tea":
 *   char[] arr = ['t', 'e', 'a'] -> sorted: ['a', 'e', 't'] -> key = "aet"
 *   map.get("aet") exists: ["eat"]. Add "tea".
 *   map: {"aet": ["eat", "tea"]}
 * 
 * i=2, word = "tan":
 *   char[] arr = ['t', 'a', 'n'] -> sorted: ['a', 'n', 't'] -> key = "ant"
 *   map.get("ant") is null. Put new List, add "tan".
 *   map: {"aet": ["eat", "tea"], "ant": ["tan"]}
 * 
 * Result: new ArrayList<>(map.values()) -> [["eat", "tea"], ["tan"]]
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To group anagrams efficiently, we need a canonical signature for each word so that all anagrams share 
 * the exact same key. Since anagrams share identical character distributions, sorting the characters of 
 * each word alphabetically gives us that exact signature—for example, 'eat', 'tea', and 'ate' all become 'aet'. 
 * We can iterate through the list of strings, compute this sorted string signature for each, and store them 
 * in a HashMap mapping the signature to a list of original words. Finally, we return the values of the map. 
 * This gives us O(N * L log L) time complexity and O(N * L) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: How would you optimize this if strings can be massive (e.g., length 1,000,000)?
 * A: Sorting each string takes O(L log L), which becomes a bottleneck for huge lengths. I would switch 
 *    to the Frequency Signature approach (O(L) per string) by building an `int[26]` array and serializing 
 *    it into a delimiter-safe key format, reducing total time to O(N * L).
 * 
 * Q: What if the input is streamed rather than an array in memory?
 * A: The HashMap-based grouping approach works identically for streams. As each string arrives, we compute 
 *    its key and add it to the map. We just return `map.values()` once the stream closes.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupAnagrams {

    /**
     * APPROACH 2: OPTIMAL (Sorted String Canonical Key via HashMap)
     * Time: O(N * L log L), Space: O(N * L)
     */
    public List<List<String>> groupAnagrams(String[] strs) {
        // [Why needed]: Edge case protection for empty or null inputs.
        if (strs == null || strs.length == 0) {
            return new ArrayList<>();
        }

        // [Why needed]: Maps a canonical signature (sorted string) to all original strings 
        // that share that signature.
        // Example: "aet" -> ["eat", "tea", "ate"]
        Map<String, List<String>> map = new HashMap<>();

        for (String s : strs) {
            // [Why needed]: Convert string to char array so we can sort it to find its canonical form.
            char[] ca = s.toCharArray();
            Arrays.sort(ca);
            String keyStr = String.valueOf(ca);

            // [What it does]: If the canonical signature isn't in our map yet, initialize a new list.
            if (!map.containsKey(keyStr)) {
                map.put(keyStr, new ArrayList<>());
            }

            // Add the original string to its respective anagram bucket.
            map.get(keyStr).add(s);
        }

        // [What it does]: Return all the grouped lists from the map values.
        return new ArrayList<>(map.values());
    }

    /**
     * APPROACH 3: ALTERNATIVE OPTIMAL (Frequency Signature Canonical Key)
     * Time: O(N * L), Space: O(N * L)
     * Note: Avoids L log L sorting by using an int[26] frequency signature.
     */
    public List<List<String>> groupAnagramsFrequencyKey(String[] strs) {
        if (strs == null || strs.length == 0) return new ArrayList<>();

        Map<String, List<String>> map = new HashMap<>();

        for (String s : strs) {
            int[] count = new int[26];
            for (char c : s.toCharArray()) {
                count[c - 'a']++;
            }

            // Build a unique signature string from the frequency array
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 26; i++) {
                sb.append('#').append(count[i]);
            }
            String key = sb.toString();

            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(s);
        }

        return new ArrayList<>(map.values());
    }

    /**
     * SUMMARY
     * 1. Core pattern: Canonical signature hashing (Grouping by equivalence classes).
     * 2. Key observation: Anagrams share the same sorted form ("eat" -> "aet") or frequency fingerprint.
     * 3. Memorize vs Understand: Understand how HashMaps can be used to bucket objects by a derived key.
     * 4. Most common trap: Trying to compare every string against every other string (O(N^2) brute force).
     * 5. Mental trigger: "Group anagrams" -> `HashMap<String, List<String>>` + Sorted String / Frequency Key.
     */
    public static void main(String[] args) {
        GroupAnagrams solution = new GroupAnagrams();

        // Test Case 1: Standard Example
        String[] strs1 = {"eat", "tea", "tan", "ate", "nat", "bat"};
        List<List<String>> res1 = solution.groupAnagrams(strs1);
        System.out.println("Test 1 (Standard): " + res1); 
        // Expected groups: [["bat"], ["nat", "tan"], ["ate", "eat", "tea"]] (order may vary)

        // Test Case 2: Empty string element
        String[] strs2 = {""};
        List<List<String>> res2 = solution.groupAnagrams(strs2);
        System.out.println("Test 2 (Empty string): " + res2); // Expected: [[""]]

        // Test Case 3: Single character strings
        String[] strs3 = {"a"};
        List<List<String>> res3 = solution.groupAnagrams(strs3);
        System.out.println("Test 3 (Single char): " + res3); // Expected: [["a"]]
    }
}



import java.util.*;

class Solution {
    public List<List<String>> groupAnagrams(String[] strs) {
        // Key: character-frequency signature
        // Value: all strings having that signature
        Map<String, List<String>> groups = new HashMap<>();

        for (String str : strs) {
            // Track the frequency of each lowercase English letter.
            int[] freq = new int[26];

            for (char ch : str.toCharArray()) {
                freq[ch - 'a']++;
            }

            // Build an unambiguous key from all 26 frequencies.
            // Example: "eat" and "tea" produce identical keys.
            StringBuilder keyBuilder = new StringBuilder();

            for (int count : freq) {
                keyBuilder.append(count).append('#');
            }

            String key = keyBuilder.toString();

            // Create the group if this key is encountered for the first time.
            // Otherwise, append the string to its existing anagram group.
            groups.computeIfAbsent(key, k -> new ArrayList<>())
                  .add(str);
        }

        // Return all groups. Their order does not matter.
        return new ArrayList<>(groups.values());
    }
}


