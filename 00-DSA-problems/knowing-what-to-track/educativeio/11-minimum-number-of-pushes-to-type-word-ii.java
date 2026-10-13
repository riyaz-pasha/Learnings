/**
 * STANDALONE STUDY NOTE: MINIMUM NUMBER OF PUSHES TO TYPE WORD II
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Are we allowed to map multiple characters to the same key, up to any capacity?" (Yes, keys 2 to 9 can hold multiple letters).
 * - "Does the cost per character depend on its position on the key (1 push for 1st letter, 2 pushes for 2nd, etc.)?" (Yes, exactly how standard telephone keypads work).
 * - "Can we leave some keys unmapped if there are fewer than 8 distinct characters?" (Yes, and we should map frequent characters to lower-cost positions first).
 * - "Is the input string guaranteed to contain only lowercase English letters?" (Yes, simplifying the alphabet domain to 26 characters).
 * - "Can the string length be large enough to cause integer overflow during cost summation?" (With length up to 10^5, max cost fits comfortably inside a standard 32-bit integer).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to assign 26 lowercase letters to 8 available keys (keys 2-9) such that the total number of key presses 
 * required to type the given string is minimized. The binding constraint is that each key can hold multiple letters, 
 * but each subsequent letter added to a key costs +1 additional press (1st position = 1 press, 2nd = 2 presses, etc.).
 * 
 * --- APPROACH 1: BRUTE FORCE (Permutation Assignment) ---
 * 1. What I'd naturally try: Try every possible way to group and assign the 26 lowercase letters across the 8 keys, 
 *    and calculate the total cost for the word under each configuration to find the minimum.
 * 2. Why it works: It searches the entire state space of possible keypad configurations.
 * 3. Why it's too slow/costly: The number of ways to partition 26 letters into 8 key buckets is combinatorially 
 *    explosive (Stirling numbers of the second kind), making exhaustive search completely intractable.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(K^26 * N) — where K is the number of keys (8) and N is the length of the word, exploring 
 *      an exponential configuration space.
 *    - Space Complexity: O(26) — recursion and mapping state storage.
 * 
 * --- APPROACH 2: INTERMEDIATE (Greedy Frequency Sorting with HashMaps) ---
 * 1. The Observation: By Greedy Choice Property, to minimize total presses, the most frequently occurring letters 
 *    must be assigned to the cheapest available positions (1 press per push), followed by the next most frequent letters 
 *    getting 2 presses, and so on. We don't need to try all configurations; we just need to sort unique characters 
 *    by their frequency in descending order.
 * 2. How it leads to the next step: Count frequencies using a `HashMap`, extract and sort the frequency values, 
 *    and assign them sequentially to tiers of 8 (first 8 get 1 press, next 8 get 2 presses, etc.).
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + U log U) — where N is the length of the string and U is the number of unique characters (at most 26). 
 *      Sorting 26 elements takes negligible time.
 *    - Space Complexity: O(U) — HashMap and list storage for unique characters, incurring Java object boxing overhead.
 * 
 * --- APPROACH 3: OPTIMAL (Array-based Frequency Sorting) ---
 * 1. The Observation: Since characters are strictly lowercase English letters ('a'-'z'), a HashMap is an unnecessary 
 *    overhead. We can use a primitive `int[26]` frequency array.
 * 2. How it leads to the next step: Tally character counts in the array, sort the array in ascending order, and iterate 
 *    from the end (highest frequencies first). For each frequency, multiply it by its required push multiplier `(i / 8 + 1)`.
 * 3. Which one I'd write in an interview: Approach 3 (Array + Sorting). It runs in true O(N) time (since sorting 26 elements 
 *    is an O(1) constant operation), uses O(1) auxiliary space, and avoids all object allocation.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we make a single linear pass to count character frequencies, and sort a fixed 
 *      array of size 26 in O(26 log 26) time (which is effectively constant time).
 *    - Space Complexity: O(1) — because we allocate a fixed 26-element integer array (104 bytes), completely independent 
 *      of the input string length N.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Short strings with few unique characters: `word = "abcde"` -> All 5 unique characters fit in the first tier (1 push each).
 * - Exactly 8 unique characters: Each gets the prime position (1 push each).
 * - More than 8 unique characters: The 9th and subsequent unique characters wrap around to the second tier (2 pushes each).
 * - All characters identical: `word = "aaaaa"` -> Handled instantly since there's only 1 unique character.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * The 8-Key Tier Assignment Strategy:
 * 
 * Available Keys: [2, 3, 4, 5, 6, 7, 8, 9] (Total = 8 keys)
 * 
 * Sorted Frequencies (Descending): [f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, ...]
 * 
 * Tier 1 (Indices 0-7): Assigned as 1st letter on keys 2-9 -> Requires 1 push each.
 * Tier 2 (Indices 8-15): Assigned as 2nd letter on keys 2-9 -> Requires 2 pushes each.
 * Tier 3 (Indices 16-23): Assigned as 3rd letter on keys 2-9 -> Requires 3 pushes each.
 * Tier 4 (Indices 24-25): Assigned as 4th letter on keys -> Requires 4 pushes each.
 * 
 * Formula for multiplier at sorted index `i`: `(i / 8) + 1`
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: word = "aabbccc"
 * 
 * Step 1: Count Frequencies
 *   count['a'-'a'] = 2
 *   count['b'-'a'] = 2
 *   count['c'-'a'] = 3
 * 
 * Step 2: Sort the frequency array (`Arrays.sort(count)`)
 *   The array of size 26 will have zeros everywhere except the top 3 slots:
 *   [0, 0, ..., 2, 2, 3] (sorted ascending)
 * 
 * Step 3: Iterate backwards (highest frequencies first) with index `i` from 0 to 25:
 *   - i = 0 (freq = 3): multiplier = (0 / 8) + 1 = 1. Cost += 3 * 1 = 3.
 *   - i = 1 (freq = 2): multiplier = (1 / 8) + 1 = 1. Cost += 2 * 1 = 2.
 *   - i = 2 (freq = 2): multiplier = (2 / 8) + 1 = 1. Cost += 2 * 1 = 2.
 *   - Remaining 23 slots have frequency 0, adding 0 cost.
 * 
 * Total Minimum Pushes = 3 + 2 + 2 = 7.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To minimize the total number of key pushes, we must apply a greedy strategy: the most frequently 
 * occurring characters in the word should be assigned to the cheapest positions on the keypad—meaning 
 * they require only 1 push. Since we have 8 available keys (2 through 9), the top 8 most frequent characters 
 * get 1 push each. The next 8 characters require 2 pushes, and so on. We can implement this efficiently 
 * by counting character frequencies into a fixed-size integer array of size 26, sorting the frequencies, 
 * and then iterating through them from highest to lowest while multiplying each frequency by `(i / 8 + 1)`. 
 * This runs in O(N) time and O(1) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if the telephone keypad had a different number of assignable keys (e.g., 10 keys instead of 8)?
 * A: We would simply change the divisor in our multiplier formula from `8` to `10` (`i / 10 + 1`), reflecting 
 *    that each tier now holds 10 characters before requiring additional pushes.
 * 
 * Q: What if the character set included full Unicode characters or uppercase letters?
 * A: The fixed-size `int[26]` array would no longer suffice. We would switch to a `HashMap<Character, Integer>` 
 *    to tally frequencies, extract the values into a list, sort them descendingly, and apply the exact same 
 *    `i / 8 + 1` tier formula.
 */

import java.util.Arrays;

public class MinimumNumberOfPushesToTypeWordII {

    /**
     * APPROACH 3: OPTIMAL (Array Frequency + Greedy Tier Assignment)
     * Time: O(N), Space: O(1)
     */
    public int minimumPushes(String word) {
        // [Why needed]: Tracks frequency of each lowercase English letter ('a' through 'z').
        int[] count = new int[26];

        // Phase 1: Count occurrences of each character in the word
        for (int i = 0; i < word.length(); i++) {
            count[word.charAt(i) - 'a']++;
        }

        // Phase 2: Sort frequencies in ascending order. 
        // Since the array is always length 26, this takes O(26 log 26) time, which is O(1) constant time.
        Arrays.sort(count);

        int totalPushes = 0;
        
        // Phase 3: Iterate backwards (from highest frequency to lowest frequency)
        // i represents the rank of the frequency from 0 (most frequent) to 25 (least frequent).
        for (int i = 0; i < 26; i++) {
            // [What it does]: Extract frequencies starting from the highest end of the sorted array.
            int frequency = count[26 - 1 - i];

            // If frequency is 0, there are no more unique characters to process.
            if (frequency == 0) {
                break;
            }

            // [Why needed]: The tier multiplier formula. 
            // Indices 0-7 (first 8 chars) require 1 push: (0..7)/8 + 1 = 1.
            // Indices 8-15 (next 8 chars) require 2 pushes: (8..15)/8 + 1 = 2.
            // Indices 16-23 (next 8 chars) require 3 pushes: (16..23)/8 + 1 = 3.
            // Indices 24-25 (remaining chars) require 4 pushes: (24..25)/8 + 1 = 4.
            int pushesPerChar = (i / 8) + 1;

            totalPushes += frequency * pushesPerChar;
        }

        return totalPushes;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Greedy frequency sorting with tier-based cost distribution (`i / 8 + 1`).
     * 2. Key observation: Assigning the highest frequency characters to the cheapest key positions 
     *    minimizes the overall total press count.
     * 3. Memorize vs Understand: Understand the tier formula `(i / 8 + 1)` where `i` is the 0-indexed 
     *    rank of sorted frequencies in descending order.
     * 4. Most common trap: Forgetting to sort frequencies in descending order before applying the tier multiplier, 
     *    which would assign high costs to frequent letters.
     * 5. Mental trigger: "Minimum key pushes with grouped capacity" -> Frequency array + Sort descending + `(i / 8 + 1) * freq`.
     */
    public static void main(String[] args) {
        MinimumNumberOfPushesToTypeWordII solution = new MinimumNumberOfPushesToTypeWordII();

        // Test Case 1: Standard Example
        String word1 = "abcde";
        System.out.println("Test 1 (abcde): " + solution.minimumPushes(word1)); // Expected: 5

        // Test Case 2: Repeating characters
        String word2 = "xyzxyzxyzxyz";
        System.out.println("Test 2 (xyzxyzxyzxyz): " + solution.minimumPushes(word2)); // Expected: 12

        // Test Case 3: More than 8 unique characters (triggers tier 2)
        String word3 = "aabbccddeeffgghhiiiiii";
        System.out.println("Test 3 (Large word): " + solution.minimumPushes(word3)); // Expected: 24

        // Test Case 4: Single unique character repeated
        String word4 = "aaaaa";
        System.out.println("Test 4 (All 'a'): " + solution.minimumPushes(word4)); // Expected: 5
    }
}

/*
 * Problem: Minimum Number of Pushes to Type a Word
 * Language: Java 24 (the solution also works on earlier modern Java versions)
 *
 * ---------------------------------------------------------------------------
 * 1. PROBLEM STATEMENT
 * ---------------------------------------------------------------------------
 * We are given a lowercase English word. We may remap letters to the eight
 * telephone keys 2 through 9 however we like.
 *
 * On any one key:
 *   - the first assigned letter costs 1 push,
 *   - the second assigned letter costs 2 pushes,
 *   - the third assigned letter costs 3 pushes,
 *   - and so on.
 *
 * Return the minimum total number of pushes required to type the word.
 *
 * ---------------------------------------------------------------------------
 * 2. INTUITION: WHAT SHOULD WE TRACK?
 * ---------------------------------------------------------------------------
 * We only need to know how often each letter occurs.
 *
 * For example, in "aabaa":
 *   a occurs 4 times
 *   b occurs 1 time
 *
 * If 'a' costs 1 push each time, it contributes 4 * 1 = 4 pushes.
 * If 'a' costs 2 pushes each time, it contributes 4 * 2 = 8 pushes.
 *
 * Therefore, frequent letters should get cheap positions, while rare letters
 * can be placed in more expensive positions.
 *
 * There are 8 keys, so there are:
 *   - 8 positions costing 1 push,
 *   - 8 positions costing 2 pushes,
 *   - 8 positions costing 3 pushes,
 *   - and so on.
 *
 * Greedy rule:
 *   Sort letter frequencies from largest to smallest and assign them in that
 *   order to positions costing 1, 1, ..., 1, 2, 2, ..., 2, 3, ...
 *   (eight positions at each cost).
 *
 * ---------------------------------------------------------------------------
 * 3. WHY IS THIS GREEDY STRATEGY OPTIMAL?
 * ---------------------------------------------------------------------------
 * Imagine a frequent letter appears 10 times and a rare letter appears once.
 * If the frequent letter costs 2 pushes and the rare letter costs 1 push:
 *
 *   cost = 10 * 2 + 1 * 1 = 21
 *
 * Swap their positions:
 *
 *   cost = 10 * 1 + 1 * 2 = 12
 *
 * The total decreases. More generally, if frequency A >= frequency B and
 * push cost x <= push cost y, assigning A to x and B to y is never worse than
 * the opposite assignment:
 *
 *   A*x + B*y <= A*y + B*x
 *
 * This is why descending frequencies paired with ascending costs are optimal.
 *
 * ---------------------------------------------------------------------------
 * 4. EXAMPLE WALKTHROUGH
 * ---------------------------------------------------------------------------
 * Example: word = "abcde"
 *
 * Frequencies:
 *   a: 1, b: 1, c: 1, d: 1, e: 1
 *
 * There are only five distinct letters. Since eight letters can occupy
 * one-push positions, every letter costs 1 push.
 *
 *   a -> 1 * 1 = 1
 *   b -> 1 * 1 = 1
 *   c -> 1 * 1 = 1
 *   d -> 1 * 1 = 1
 *   e -> 1 * 1 = 1
 *
 * Total = 5.
 *
 * A second illustrative frequency list:
 *   Letter frequencies, already sorted descending:
 *   [10, 8, 6, 4, 3, 2, 2, 1, 1]
 *
 * Assignment:
 *   ranks 1 through 8 -> 1 push each
 *   rank 9            -> 2 pushes
 *
 * Total = 10+8+6+4+3+2+2+1 + (1*2) = 38.
 *
 * Note: this second list illustrates the greedy grouping rule. It is a
 * frequency list, not a claim about a particular word in the first example.
 *
 * ---------------------------------------------------------------------------
 * 5. TRACE THE ACTUAL CODE
 * ---------------------------------------------------------------------------
 * The code sorts all 26 frequency slots in ascending order, including zeros.
 * It then scans from index 25 down to index 0.
 *
 * Formula:
 *   pushCount = (25 - i) / 8 + 1
 *
 * Why does this formula work?
 *   - At i = 25, (25 - 25) / 8 + 1 = 1
 *   - At i = 24, (25 - 24) / 8 + 1 = 1
 *   - ...
 *   - At i = 18, the result is 1
 *   - At i = 17, (25 - 17) / 8 + 1 = 2
 *
 * Java integer division truncates the fractional part, so each group of eight
 * processed slots receives the same push count.
 *
 * Important detail:
 *   The index i includes zero-frequency entries. Because we scan from the
 *   largest end and stop at the first zero, only nonzero frequencies are
 *   processed. Therefore, the first actual letter always gets one push,
 *   regardless of how many zero entries exist before the nonzero frequencies.
 *
 * ---------------------------------------------------------------------------
 * 6. DETAILED CODE EXPLANATION
 * ---------------------------------------------------------------------------
 * int[] frequency = new int[26];
 *   Creates one counter for each lowercase English letter. All values start
 *   at zero.
 *
 * frequency[ch - 'a']++;
 *   Maps 'a' to index 0, 'b' to index 1, ..., 'z' to index 25.
 *   For example, 'c' - 'a' equals 2, so frequency[2] counts 'c'.
 *
 * Arrays.sort(frequency);
 *   Sorts the frequencies in ascending order. The most frequent letter
 *   frequencies end up at the right-hand side of the array.
 *
 * for (int i = 25; i >= 0; i--)
 *   Walks from the largest frequency to the smallest.
 *
 * if (frequency[i] == 0) break;
 *   Once we reach zero, all remaining entries are also zero because the array
 *   is sorted. No more letters from the word remain to process.
 *
 * int pushCount = (25 - i) / 8 + 1;
 *   Calculates the cost for the current frequency slot: 1 for the first eight
 *   nonzero frequencies, 2 for the next eight, 3 for the next eight, etc.
 *
 * totalPushes += frequency[i] * pushCount;
 *   Adds the contribution of this letter: occurrence count multiplied by the
 *   number of pushes required for each occurrence.
 *
 * return totalPushes;
 *   Returns the minimum total.
 *
 * ---------------------------------------------------------------------------
 * 7. COMPLETE SOLUTION
 * ---------------------------------------------------------------------------
 */

import java.util.Arrays;

class Solution {
    public int minimumPushes(String word) {
        // Step 1: Count the frequency of each lowercase letter.
        // Index 0 represents 'a', index 1 represents 'b', ..., index 25 'z'.
        int[] frequency = new int[26];

        for (char ch : word.toCharArray()) {
            frequency[ch - 'a']++;
        }

        // Step 2: Sort frequencies in ascending order.
        // The highest frequencies will be at the end of the array.
        Arrays.sort(frequency);

        int totalPushes = 0;

        // Step 3: Process the highest frequency first.
        for (int i = 25; i >= 0; i--) {
            // Since the array is sorted, all remaining entries are zero too.
            // These entries represent letters that do not appear in the word.
            if (frequency[i] == 0) {
                break;
            }

            // There are 8 keys, so each push-cost level has 8 positions:
            // positions 1-8 cost 1 push, positions 9-16 cost 2 pushes, etc.
            //
            // (25 - i) is how many slots we have moved from the right edge.
            // Integer division by 8 groups slots into sets of eight.
            int pushCount = (25 - i) / 8 + 1;

            // Every occurrence of this letter costs pushCount pushes.
            totalPushes += frequency[i] * pushCount;
        }

        return totalPushes;
    }
}

/*
 * ---------------------------------------------------------------------------
 * 8. COMPLEXITY
 * ---------------------------------------------------------------------------
 * Let n be word.length().
 *
 * Time:
 *   - Count letters: O(n)
 *   - Sort 26 counters: O(26 log 26)
 *   - Scan at most 26 counters: O(26)
 *
 *   Overall: O(n + 26 log 26), which simplifies to O(n) because the alphabet
 *   has a fixed size of 26.
 *
 * Auxiliary space:
 *   O(26) for the frequency array, which simplifies to O(1) for a fixed
 *   lowercase English alphabet. The toCharArray() call also creates a
 *   temporary O(n) character array; to avoid that temporary array, iterate
 *   directly over the string using charAt().
 *
 * ---------------------------------------------------------------------------
 * 9. INTERVIEW SUMMARY
 * ---------------------------------------------------------------------------
 * Pattern: Greedy + Frequency Counting + Sorting
 *
 * Remember:
 *   1. Count the frequency of each letter.
 *   2. Sort frequencies in descending order (or sort ascending and scan back).
 *   3. Assign the first 8 distinct letters 1 push, the next 8 letters 2 pushes,
 *      and so on.
 *   4. Add frequency * pushCount for every present letter.
 *
 * The actual letters do not matter; only their frequencies matter.
 */

