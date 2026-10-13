/**
 * STANDALONE STUDY NOTE: FIND ALL ANAGRAMS IN A STRING
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can string b be longer than string a?" (If yes, no anagrams can exist, so we should immediately return an empty list).
 * - "Are characters strictly lowercase English letters?" (Confirming this allows us to use fixed-size `int[26]` arrays instead of HashMaps for window counting).
 * - "In what order should the start indices be returned?" (The prompt states we can return them in any order).
 * - "What should be returned if no anagrams of b are found in a?" (Return an empty list).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to find all starting indices in string `a` where a substring of length `b.length()` is an anagram of `b`. 
 * The binding constraint is that re-calculating the frequency count for every overlapping window of length `|b|` 
 * from scratch leads to a redundant, slow brute-force solution.
 * 
 * --- APPROACH 1: BRUTE FORCE (Re-counting Every Window) ---
 * 1. What I'd naturally try: Slide a window of length `b.length()` across every possible starting index in `a`. 
 *    For each window, build a frequency map or sort the characters, and compare it against `b`.
 * 2. Why it works: It exhaustively checks every single substring window.
 * 3. Why it's too slow/costly: For a string `a` of length N and `b` of length M, checking each window takes O(M) time, 
 *    resulting in O(N * M) total time. If N and M are large, this becomes inefficient.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N * M) — because we re-scan or re-sort every window of length M across N possible start positions.
 *    - Space Complexity: O(1) or O(M) depending on temporary storage.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Sliding Window) ---
 * 1. The Observation: As the window slides one character to the right, we don't need to recount everything. 
 *    We just drop the outgoing character on the left and add the incoming character on the right. 
 *    This is the classic *Sliding Window* technique.
 * 2. How it leads to the next step: Maintain a frequency map of `b`, and a rolling frequency map for the current window 
 *    in `a`. Compare the maps at each step.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because map comparisons and updates happen in constant time per window shift.
 *    - Space Complexity: O(U) where U is unique characters (up to 26), but incurs heavy HashMap object overhead.
 * 
 * --- APPROACH 3: OPTIMAL (Fixed-Size Array Sliding Window with Match Counter) ---
 * 1. The Observation: Since characters are strictly lowercase English letters ('a'-'z'), we can replace HashMaps 
 *    with fixed `int[26]` frequency arrays. Furthermore, instead of comparing two arrays of size 26 at every step 
 *    (which takes 26 operations per shift), we can maintain a `matches` counter or check exact array equality 
 *    efficiently by tracking how many of the 26 character frequencies match.
 * 2. How it leads to the next step:
 *    - Tally characters of `b` into `pCount` (or `bCount`).
 *    - Tally the first `b.length()` characters of `a` into `windowCount`.
 *    - Slide the window from left to right: at each step, add the new incoming character on the right, remove the 
 *      outgoing character on the left, and check if `windowCount` matches `bCount`.
 * 3. Which one I'd write in an interview: Approach 3 (Array Sliding Window). It is clean, avoids map allocation, 
 *    runs in true O(N) time, and demonstrates mastery of sliding window optimization.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — where N is the length of string `a`. We make a single pass, updating array indices 
 *      and checking array equality in O(26) = O(1) time per shift.
 *    - Space Complexity: O(1) auxiliary space — because we allocate fixed `int[26]` arrays (104 bytes each), completely 
 *      independent of string lengths.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - `b` is longer than `a`: `a = "ab", b = "abc"` -> Returns an empty list immediately.
 * - Single-character strings: `a = "a", b = "a"` -> Returns `[0]`.
 * - No anagrams exist: `a = "abcdef", b = "xyz"` -> Returns an empty list.
 * - Multiple overlapping anagrams: `a = "abab", b = "ab"` -> Returns `[0, 1, 2]`.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Sliding Window Mechanics:
 * 
 * String a: "c b a e b a b a c d"
 * String b: "a b c" (Length = 3)
 * 
 * Window slides across `a`:
 * Index 0: ["c", "b", "a"] -> Anagram of "abc"? YES! (Add index 0)
 * Index 1:   ["b", "a", "e"] -> Anagram of "abc"? NO ('e' instead of 'c')
 * Index 2:     ["a", "e", "b"] -> Anagram of "abc"? NO
 * Index 3:       ["e", "b", "a"] -> Anagram of "abc"? NO
 * Index 4:         ["b", "a", "b"] -> Anagram of "abc"? NO
 * Index 5:           ["a", "b", "a"] -> Anagram of "abc"? NO
 * Index 6:             ["b", "a", "c"] -> Anagram of "abc"? YES! (Add index 6)
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Array Approach)
 * Target: a = "cbaebabacd", b = "abc" (Length = 3)
 * 
 * - Step 1: Check early exit. b.length() (3) > a.length() (10)? No.
 * - Step 2: Build target frequency array for `b` ("abc"):
 *   `bCount` = [1, 1, 1, 0, ..., 0] ('a':1, 'b':1, 'c':1)
 * - Step 3: Build first window frequency array for `a` of length 3 ("cba"):
 *   `windowCount` = [1, 1, 1, 0, ..., 0] ('c':1, 'b':1, 'a':1)
 * - Step 4: Compare `windowCount` and `bCount`. They match! Add start index `0` to result list.
 * - Step 5: Slide window:
 *   - i = 3 (incoming char = 'e', outgoing char at i - len = 0 is 'c'):
 *     - Add 'e': windowCount['e'-'a']++
 *     - Remove 'c': windowCount['c'-'a']--
 *     - Compare with `bCount` -> No match.
 *   - ... continue sliding until index 6 ("bac"):
 *     - windowCount matches `bCount` -> Add start index `6` to result list.
 * 
 * Result: [0, 6]
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To find all anagram starting indices efficiently, we can use a fixed-size sliding window of length `b.length()` 
 * over string `a`. Since the character set is restricted to lowercase English letters, we can represent character 
 * frequencies using fixed `int[26]` arrays. First, we populate the frequency count for string `b` and the initial 
 * window of string `a`. Then, as the window slides one character to the right at each step, we add the incoming 
 * character and remove the outgoing character from our window frequency array in O(1) time. We compare the window 
 * array to `b`'s array at each shift. This reduces our time complexity to O(N) and auxiliary space complexity to O(1)."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if the character set was full Unicode instead of lowercase English letters?
 * A: The fixed `int[26]` array would fail. We would switch to a `HashMap` or a larger array combined with a 
 *    variable mismatch counter to maintain O(N) sliding window performance.
 * 
 * Q: How would you modify this if you needed to find the *minimum window substring* containing all characters of b?
 * A: That would require a variable-length sliding window (expanding right until valid, then contracting left 
 *    to minimize), which is a different pattern (Minimum Window Substring).
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FindAllAnagramsInAString {

    /**
     * APPROACH 3: OPTIMAL (Fixed-Size Array Sliding Window)
     * Time: O(N), Space: O(1)
     */
    public List<Integer> findAnagrams(String a, String b) {
        List<Integer> result = new ArrayList<>();
        
        int n = a.length();
        int m = b.length();

        // [Why needed]: Physical impossibility check. If b is longer than a, no anagram can exist.
        if (n < m) {
            return result;
        }

        // [Why needed]: Frequency arrays for string b and the current sliding window in string a.
        // Size 26 maps 'a' through 'z'.
        int[] bCount = new int[26];
        int[] windowCount = new int[26];

        // Phase 1: Populate target frequencies for b and the first window of a
        for (int i = 0; i < m; i++) {
            bCount[b.charAt(i) - 'a']++;
            windowCount[a.charAt(i) - 'a']++;
        }

        // Check if the very first window (starting at index 0) is an anagram
        if (Arrays.equals(bCount, windowCount)) {
            result.add(0);
        }

        // Phase 2: Slide the window across the rest of string a
        for (int i = m; i < n; i++) {
            // [What it does]: Add the new incoming character on the right side of the window
            windowCount[a.charAt(i) - 'a']++;

            // [What it does]: Remove the old outgoing character from the left side of the window
            // The outgoing character is at index `i - m`.
            windowCount[a.charAt(i - m) - 'a']--;

            // [Why needed]: Compare the updated window frequency array against b's frequency array.
            // Since arrays are length 26, Arrays.equals executes in extremely fast O(26) = O(1) time.
            if (Arrays.equals(bCount, windowCount)) {
                // The start index of the current window is `i - m + 1`
                result.add(i - m + 1);
            }
        }

        return result;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Fixed-size sliding window with frequency array comparison.
     * 2. Key observation: Sliding windows avoid re-counting characters from scratch by adding the incoming 
     *    right character and dropping the outgoing left character in O(1) time.
     * 3. Memorize vs Understand: Understand how window start indices are calculated during a slide: `i - m + 1`.
     * 4. Most common trap: Off-by-one errors when managing the window boundaries during the slide loop.
     * 5. Mental trigger: "Find all anagram substrings" -> Fixed-size sliding window + `int[26]` frequency arrays + `Arrays.equals`.
     */
    public static void main(String[] args) {
        FindAllAnagramsInAString solution = new FindAllAnagramsInAString();

        // Test Case 1: Standard Example
        String a1 = "cbaebabacd";
        String b1 = "abc";
        System.out.println("Test 1 (Standard): " + solution.findAnagrams(a1, b1)); // Expected: [0, 6]

        // Test Case 2: Overlapping Anagrams
        String a2 = "abab";
        String b2 = "ab";
        System.out.println("Test 2 (Overlapping): " + solution.findAnagrams(a2, b2)); // Expected: [0, 1, 2]

        // Test Case 3: b longer than a
        String a3 = "ab";
        String b3 = "abc";
        System.out.println("Test 3 (b longer than a): " + solution.findAnagrams(a3, b3)); // Expected: []

        // Test Case 4: Single Character Match
        String a4 = "a";
        String b4 = "a";
        System.out.println("Test 4 (Single char): " + solution.findAnagrams(a4, b4)); // Expected: [0]
    }
}

