/**
 * STANDALONE STUDY NOTE: CHECK IF ONE STRING SWAP CAN MAKE STRINGS EQUAL
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can the strings be of different lengths?" (Constraints state s1.length == s2.length, so they are always equal length).
 * - "What should be returned if the strings are already identical?" (Return true, since 0 swaps are allowed: "at most one swap").
 * - "Can we swap characters within the same string or across strings?" (The prompt states the swap is performed on *exactly one* of the strings, which is equivalent to choosing two indices in one string and swapping them to match the other string).
 * - "What is the alphabet scope?" (Only lowercase English letters).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to check if two strings can be made equal by swapping at most one pair of characters in one string. 
 * The binding constraint is doing this efficiently in a single linear pass with O(1) auxiliary space.
 * 
 * --- APPROACH 1: BRUTE FORCE (Test All Index Pairs) ---
 * 1. What I'd naturally try: Try swapping every possible pair of indices `(i, j)` in string `s1`, and after each swap, 
 *    check if `s1.equals(s2)`.
 * 2. Why it works: It tests every possible single-swap configuration.
 * 3. Why it's too slow/costly: For a string of length N, there are O(N^2) pairs to swap, and each comparison takes O(N) time, 
 *    resulting in O(N^3) total time.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^3) — nested loops for index pairs plus string equality checks.
 *    - Space Complexity: O(N) — string copies or mutable conversions.
 * 
 * --- APPROACH 2: OPTIMAL (Mismatch Tracking & Complement Verification) ---
 * 1. The Observation: If two strings can be made equal with at most one swap, they can only fall into three scenarios:
 *    - Case 0: They are already identical (0 mismatches).
 *    - Case 2: They differ at *exactly* two positions, say index `i` and index `j`, such that `s1.charAt(i) == s2.charAt(j)` 
 *      and `s1.charAt(j) == s2.charAt(i)`.
 *    - Any other number of mismatches (like 1 mismatch, or 3+ mismatches) makes it strictly impossible.
 * 2. How it leads to the next step: Iterate through both strings simultaneously. Count the number of mismatches. 
 *    Store the mismatched characters. If mismatch count exceeds 2, or if the second mismatch doesn't cross-complement 
 *    the first, return false immediately. At the end, ensure we didn't have exactly 1 mismatch.
 * 3. Which one I'd write in an interview: Approach 2 (Mismatch Tracker). It runs in true O(N) time and O(1) space, 
 *    with early termination for invalid cases.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — where N is the length of the strings. We traverse the strings at most once.
 *    - Space Complexity: O(1) — we only store a few scalar variables for mismatch count and tracking characters.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Already identical strings: `s1 = "kelb", s2 = "kelb"` -> Returns true (0 mismatches).
 * - Exactly one mismatch: `s1 = "ab", s2 = "ac"` -> Returns false (1 mismatch cannot be fixed by a swap).
 * - Exactly two mismatches that can be swapped: `s1 = "bank", s2 = "kanb"` -> Returns true.
 * - Two mismatches that *cannot* be swapped: `s1 = "abcd", s2 = "abac"` -> Returns false.
 * - More than two mismatches: `s1 = "attack", s2 = "defend"` -> Returns false.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Mismatch Complementarity:
 * 
 * s1: "bank"
 * s2: "kanb"
 * 
 * Index 0: s1[0]='b', s2[0]='k' (Mismatch #1 -> c1='b', c2='k')
 * Index 3: s1[3]='k', s2[3]='b' (Mismatch #2 -> s1[3] matches c2 ('k'), s2[3] matches c1 ('b')) -> COMPLEMENTARY!
 * 
 * Total mismatches = 2, and they cross-match. Return TRUE!
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: s1 = "bank", s2 = "kanb"
 * 
 * - Init: cnt = 0, c1 = 0, c2 = 0
 * - i = 0: s1[0] ('b') != s2[0] ('k')
 *   - cnt becomes 1.
 *   - c1 = 'b', c2 = 'k'
 * - i = 1: s1[1] ('a') == s2[1] ('a') -> Skip
 * - i = 2: s1[2] ('n') == s2[2] ('n') -> Skip
 * - i = 3: s1[3] ('k') != s2[3] ('b')
 *   - cnt becomes 2.
 *   - Check condition: cnt == 2 and (s1[3] != c2 || s2[3] != c1) -> ('k' != 'k' || 'b' != 'b') is FALSE.
 *   - c1 = 'k', c2 = 'b'
 * - Loop ends.
 * - Return `cnt != 1` -> 2 != 1 -> TRUE!
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To determine if two strings can be made equal with at most one swap, we can track the positions where characters differ. 
 * If the strings are already identical, 0 swaps are needed, which is valid. If they differ, there must be exactly 2 mismatch 
 * positions, and the characters at those positions must be exact reverses of each other so that a single swap fixes both. 
 * If there is only 1 mismatch, it's impossible to fix with a swap since a swap requires two elements. If there are more than 
 * 2 mismatches, one swap is also insufficient. We can implement this in a single linear pass with O(N) time and O(1) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if we were allowed to perform *up to k* swaps instead of just one?
 * A: That transforms the problem into finding the graph distance or minimum swaps (similar to graph cycle decomposition 
 *    or LeetCode 839 Similar String Groups), which typically requires Disjoint Set Union (DSU) or Breadth-First Search.
 */

public class CheckIfOneStringSwapCanMakeStringsEqual {

    /**
     * APPROACH 2: OPTIMAL (Single-Pass Mismatch Tracker)
     * Time: O(N), Space: O(1)
     */
    public boolean areAlmostEqual(String s1, String s2) {
        int mismatchCount = 0;
        char c1 = 0;
        char c2 = 0;

        for (int i = 0; i < s1.length(); i++) {
            char a = s1.charAt(i);
            char b = s2.charAt(i);

            if (a != b) {
                mismatchCount++;

                // [Why needed]: Early exit if more than 2 mismatches occur, or if the second mismatch 
                // does not cross-complement the first mismatch.
                if (mismatchCount > 2 || (mismatchCount == 2 && (a != c2 || b != c1))) {
                    return false;
                }

                // Record the characters of the first mismatch
                c1 = a;
                c2 = b;
            }
        }

        // [Why needed]: Exactly 1 mismatch is impossible to fix with a single swap. 
        // 0 mismatches (already equal) or 2 complementary mismatches are valid.
        return mismatchCount != 1;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Mismatch counting with early termination and cross-complement verification.
     * 2. Key observation: One swap can only fix 0 mismatches (already equal) or exactly 2 cross-complementary mismatches.
     * 3. Memorize vs Understand: Understand why 1 mismatch always returns false (you cannot swap a single element with anything to fix it).
     * 4. Most common trap: Forgetting to check if 0 mismatches should return true (failing to handle already equal strings).
     * 5. Mental trigger: "One string swap to make equal" -> Mismatch count (0 or 2) + cross-complement check.
     */
    public static void main(String[] args) {
        CheckIfOneStringSwapCanMakeStringsEqual solution = new CheckIfOneStringSwapCanMakeStringsEqual();

        // Test Case 1: Standard Success Case
        String s1_1 = "bank", s2_1 = "kanb";
        System.out.println("Test 1 (bank, kanb): " + solution.areAlmostEqual(s1_1, s2_1)); // Expected: true

        // Test Case 2: Standard Failure Case (More than 2 mismatches)
        String s1_2 = "attack", s2_2 = "defend";
        System.out.println("Test 2 (attack, defend): " + solution.areAlmostEqual(s1_2, s2_2)); // Expected: false

        // Test Case 3: Already Equal Case
        String s1_3 = "kelb", s2_3 = "kelb";
        System.out.println("Test 3 (kelb, kelb): " + solution.areAlmostEqual(s1_3, s2_3)); // Expected: true

        // Test Case 4: Single Mismatch Case
        String s1_4 = "abcd", s2_4 = "abcc";
        System.out.println("Test 4 (abcd, abcc): " + solution.areAlmostEqual(s1_4, s2_4)); // Expected: false
    }
}




class Solution {
    public boolean areAlmostEqual(String s1, String s2) {
        // Store the positions where the strings differ.
        int first = -1;
        int second = -1;

        for (int i = 0; i < s1.length(); i++) {
            // Matching characters need no action.
            if (s1.charAt(i) == s2.charAt(i)) {
                continue;
            }

            // Record the first mismatch.
            if (first == -1) {
                first = i;
            }
            // Record the second mismatch.
            else if (second == -1) {
                second = i;
            }
            // Three or more mismatches cannot be fixed by one swap.
            else {
                return false;
            }
        }

        // No mismatches: the strings are already equal.
        if (first == -1) {
            return true;
        }

        // Exactly one mismatch cannot be fixed by swapping.
        if (second == -1) {
            return false;
        }

        // One swap must exchange the two mismatched characters.
        return s1.charAt(first) == s2.charAt(second)
                && s1.charAt(second) == s2.charAt(first);
    }
}


