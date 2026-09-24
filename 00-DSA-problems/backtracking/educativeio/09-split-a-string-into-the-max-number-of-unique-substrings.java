/**
 * ============================================================================
 * SPLIT A STRING INTO THE MAX NUMBER OF UNIQUE SUBSTRINGS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * Imagine you have a ribbon with a word printed on it. You have a pair of 
 * scissors, and you want to cut this ribbon into as many smaller pieces as 
 * possible. The only rule is that no two pieces can have the exact same text.
 * When you place all the pieces side-by-side in order, they must perfectly 
 * form the original word. Our goal is to find the maximum number of pieces 
 * we can create.
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Do the substrings need to be contiguous?
 * A: Yes, splitting a string inherently means the parts are contiguous chunks 
 *    that concatenate back to the original.
 * 
 * Q: What is the maximum length of the string?
 * A: The constraints state the string length is <= 16. This is the biggest 
 *    hint! A length of 16 means exponential time complexity O(2^N) is perfectly 
 *    acceptable. We can try all possible splits.
 * 
 * Q: What if the string is made of the same repeating character (e.g., "aaaa")?
 * A: We must group them to be unique. "aaaa" can be split into "a" and "aaa" (2),
 *    or "a", "aa", and remaining "a" (invalid, duplicate). Max is 2.
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - DECISION TREE: At every character, we have a choice: do we place a cut 
 *   here, or do we continue building the current substring?
 * - BACKTRACKING: Because we need to enforce uniqueness, we can maintain a 
 *   Set of "seen" substrings. If a newly cut piece is already in the Set, 
 *   that path is dead, and we backtrack.
 * - BITMASKING ALTERNATIVE: Since max length is 16, there are 15 possible 
 *   "cut points" between characters. This means there are exactly 2^15 (32,768) 
 *   ways to split the string. We can literally check all of them using a loop!
 * - PRUNING (THE SECRET WEAPON): If we are currently at index 'i', we have 
 *   'seen' 3 substrings. The remaining string has 4 characters. Even if we cut 
 *   the remaining string into individual letters, the absolute maximum total 
 *   substrings we could ever reach is 3 + 4 = 7. If we have already found a 
 *   valid split of 8 elsewhere, we should instantly stop exploring this path!
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Immediately highlight the constraint "s.length <= 16". State that 
 *   this guarantees Backtracking or Bitmasking is the expected approach.
 * - Step 2: Write the standard Backtracking solution using a HashSet.
 * - Step 3: Stop and say, "We can optimize this significantly with pruning." 
 *   Add the mathematical check to fail-fast if the current path mathematically 
 *   cannot beat the current maximum.
 * - Step 4: Mention the Bitmask approach as a fun, iterative alternative. 
 *   Interviewers love candidates who can switch between recursion and iteration.
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * String: "ababccc"
 * 
 * Backtracking Tree:
 * Cut "a" -> Remaining "babccc" | Seen: ["a"]
 *   Cut "b" -> Remaining "abccc" | Seen: ["a", "b"]
 *     Cut "a" -> INVALID (Duplicate!)
 *     Cut "ab" -> Remaining "ccc" | Seen: ["a", "b", "ab"]
 *       Cut "c" -> Remaining "cc" | Seen: ["a", "b", "ab", "c"]
 *         Cut "c" -> INVALID (Duplicate!)
 *         Cut "cc" -> Remaining "" | Seen: ["a", "b", "ab", "c", "cc"] -> Valid! Count = 5.
 */

import java.util.HashSet;
import java.util.Set;

public class MaxUniqueSubstrings {

    /**
     * SOLUTION 1: Standard DFS Backtracking
     * ------------------------------------------------------------------------
     * Pros: Intuitive, directly models the "cut or don't cut" logic.
     * Cons: Explores every single valid path to the end, even if it's obvious
     * we won't beat the current maximum.
     * 
     * Time Complexity: O(2^N). In the worst case, we explore every combination 
     * of substrings. Extracting substrings and hashing takes O(N), so O(N * 2^N).
     * Space Complexity: O(N) for the HashSet and recursion stack.
     */
    public int maxUniqueSplitStandard(String s) {
        return backtrackStandard(s, 0, new HashSet<>());
    }

    private int backtrackStandard(String s, int start, Set<String> seen) {
        // Base Case: We've reached the end of the string
        if (start == s.length()) {
            return 0;
        }

        int maxSplits = -1;

        // Try cutting the string at every possible point after 'start'
        for (int i = start + 1; i <= s.length(); i++) {
            String substring = s.substring(start, i);

            // Only proceed if this substring hasn't been used yet
            if (!seen.contains(substring)) {
                seen.add(substring); // Choose
                
                // Explore
                int splits = backtrackStandard(s, i, seen); 
                
                // If the rest of the string was successfully split
                if (splits != -1) {
                    maxSplits = Math.max(maxSplits, 1 + splits);
                }
                
                seen.remove(substring); // Un-choose (Backtrack)
            }
        }
        
        return maxSplits;
    }

    /**
     * SOLUTION 2: Optimized DFS Backtracking (With Pruning)
     * ------------------------------------------------------------------------
     * Pros: Fails incredibly fast. Bypasses massive sections of the recursion 
     * tree when a better solution has already been found. This is the optimal 
     * answer for an interview.
     * 
     * Time Complexity: O(N * 2^N) theoretical, but practically far smaller 
     * due to aggressive pruning.
     * Space Complexity: O(N) for the recursion stack and HashSet.
     */
    
    // We use a class-level/instance variable to track the global maximum,
    // which makes the pruning logic much easier to write and read.
    private int maxCount = 0;

    public int maxUniqueSplitPruned(String s) {
        maxCount = 0; // Reset for multiple runs
        backtrackPruned(s, 0, new HashSet<>());
        return maxCount;
    }

    private void backtrackPruned(String s, int start, Set<String> seen) {
        // PRUNING MAGIC: 
        // seen.size() = pieces we have cut so far.
        // s.length() - start = remaining characters.
        // Even if every remaining character is cut into a length-1 string, 
        // the max total pieces we can get is (seen.size() + remaining).
        // If this theoretical maximum is less than or equal to our already-found 
        // maxCount, there is absolutely no reason to continue this branch!
        if (seen.size() + (s.length() - start) <= maxCount) {
            return; 
        }

        if (start == s.length()) {
            maxCount = Math.max(maxCount, seen.size());
            return;
        }

        for (int i = start + 1; i <= s.length(); i++) {
            String substring = s.substring(start, i);

            if (!seen.contains(substring)) {
                seen.add(substring);
                backtrackPruned(s, i, seen);
                seen.remove(substring);
            }
        }
    }

    /**
     * SOLUTION 3: Iterative Bitmasking (No Recursion)
     * ------------------------------------------------------------------------
     * Pros: Demonstrates deep system-level understanding. Bypasses recursion 
     * overhead entirely. 
     * How it works: A string of length N has N-1 gaps between letters. We use 
     * an integer mask where each bit represents a gap. If the bit is 1, we cut. 
     * If 0, we don't. 
     * 
     * Time Complexity: O(N * 2^(N-1)). We loop 2^(N-1) times, doing O(N) work each.
     * Space Complexity: O(N) for the HashSet.
     */
    public int maxUniqueSplitBitmask(String s) {
        int n = s.length();
        int maxSplits = 1;
        // Number of gaps is n - 1. So total combinations is 2^(n-1).
        int totalCombinations = 1 << (n - 1); 

        for (int mask = 0; mask < totalCombinations; mask++) {
            // Pruning for Bitmask: 
            // Integer.bitCount(mask) is the number of cuts. 
            // Cuts + 1 = total substrings. 
            // If the total substrings this mask generates is <= our max, skip it!
            if (Integer.bitCount(mask) + 1 <= maxSplits) continue;

            Set<String> seen = new HashSet<>();
            int lastCut = 0;
            boolean isValidSplit = true;

            // Iterate over all possible gaps
            for (int i = 0; i < n - 1; i++) {
                // If the i-th bit is a 1, we make a cut AFTER index i
                if ((mask & (1 << i)) != 0) {
                    String sub = s.substring(lastCut, i + 1);
                    if (!seen.add(sub)) {
                        isValidSplit = false;
                        break;
                    }
                    lastCut = i + 1;
                }
            }

            // Don't forget the final piece after the last cut
            if (isValidSplit) {
                String finalSub = s.substring(lastCut, n);
                if (seen.add(finalSub)) {
                    maxSplits = Math.max(maxSplits, seen.size());
                }
            }
        }
        
        return maxSplits;
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        MaxUniqueSubstrings solver = new MaxUniqueSubstrings();

        String[] testCases = {
            "ababccc", // Expected: 5 ("a", "b", "ab", "c", "cc")
            "aba",     // Expected: 2 ("a", "ba" OR "ab", "a")
            "aa"       // Expected: 1 ("aa" - since "a" and "a" would be duplicates)
        };

        for (String test : testCases) {
            System.out.println("--- Test Case: \"" + test + "\" ---");
            System.out.println("Standard DFS:  " + solver.maxUniqueSplitStandard(test));
            System.out.println("Pruned DFS:    " + solver.maxUniqueSplitPruned(test));
            System.out.println("Bitmasking:    " + solver.maxUniqueSplitBitmask(test));
            System.out.println();
        }
    }
}

public class MaximumUniqueSplit {

    private int maxCount = 0;

    public int maxUniqueSplit(String s) {

        /*
         * ============================================================
         * HOW TO THINK ABOUT THIS PROBLEM
         * ============================================================
         *
         * My first instinct might be:
         *
         *      Keep building a substring character by character.
         *
         *      If the substring is NOT already in the set:
         *          take it immediately,
         *          add it to the set,
         *          reset the substring,
         *          and continue.
         *
         * Example:
         *
         *      s = "ababccc"
         *
         *      a  -> unique -> take "a"
         *      b  -> unique -> take "b"
         *      a  -> duplicate, so keep extending
         *      ab -> unique -> take "ab"
         *      ...
         *
         *
         * The problem with this approach is that it is GREEDY.
         *
         * As soon as we find a valid substring, we permanently decide
         * to split there.
         *
         * But just because a substring is valid NOW does not mean
         * splitting there gives the maximum number of unique substrings
         * in the FINAL answer.
         *
         *
         * ------------------------------------------------------------
         * THE IMPORTANT MENTAL SHIFT
         * ------------------------------------------------------------
         *
         * Instead of thinking:
         *
         *      "If this substring is unique, TAKE it."
         *
         * Think:
         *
         *      "If this substring is unique, TRY taking it."
         *
         *
         * Why "TRY"?
         *
         * Because after trying it, we may later discover that another
         * split produces a larger answer.
         *
         * So we must be able to:
         *
         *      1. Choose a substring
         *      2. Explore what happens after that choice
         *      3. Undo the choice
         *      4. Try a different substring
         *
         * That is exactly BACKTRACKING.
         *
         *
         * ============================================================
         * WHAT IS THE DECISION AT EVERY POSITION?
         * ============================================================
         *
         * Suppose we are currently at index 'start'.
         *
         * Example:
         *
         *      s = "abcd"
         *           ^
         *         start = 0
         *
         * From this index, possible first substrings are:
         *
         *      "a"
         *      "ab"
         *      "abc"
         *      "abcd"
         *
         * So instead of greedily selecting "a" because it is unique,
         * we try ALL possibilities.
         *
         *
         * Conceptually:
         *
         *                 ""
         *              /   |    |     \
         *            "a"  "ab" "abc" "abcd"
         *
         *
         * If we choose "a":
         *
         *      remaining string = "bcd"
         *
         * Then from "bcd", we again try:
         *
         *      "b"
         *      "bc"
         *      "bcd"
         *
         *
         * If we choose "ab":
         *
         *      remaining string = "cd"
         *
         * Then we try:
         *
         *      "c"
         *      "cd"
         *
         *
         * Every branch represents a different way of splitting the
         * original string.
         *
         *
         * ============================================================
         * WHY DO WE NEED A SET?
         * ============================================================
         *
         * The problem says every chosen substring must be unique.
         *
         * Therefore we maintain:
         *
         *      Set<String> used
         *
         * The set represents:
         *
         *      "Substrings already chosen in the CURRENT recursion path."
         *
         *
         * Important:
         *
         * The set does NOT represent substrings used across every possible
         * answer.
         *
         * It only represents one current candidate partition.
         *
         *
         * Example:
         *
         *      a | b | ab | ...
         *
         * At this moment:
         *
         *      used = {"a", "b", "ab"}
         *
         *
         * When we backtrack from "ab", we REMOVE "ab":
         *
         *      used = {"a", "b"}
         *
         * and then we can try another substring from the same position.
         *
         *
         * This is why the pattern is:
         *
         *      CHOOSE
         *      EXPLORE
         *      UNCHOOSE
         *
         */

        backtrack(s, 0, new HashSet<>());

        return maxCount;
    }

    private void backtrack(String s, int start, Set<String> used) {

        /*
         * ============================================================
         * MEANING OF THE PARAMETERS
         * ============================================================
         *
         * s
         *      -> original string
         *
         * start
         *      -> index from which we still need to split the string
         *
         * used
         *      -> substrings already selected in the CURRENT path
         *
         *
         * Example:
         *
         *      s = "ababccc"
         *
         * Suppose we already selected:
         *
         *      "a" | "ba"
         *
         * Then:
         *
         *      used = {"a", "ba"}
         *
         * and 'start' points to the first character that has not yet
         * been consumed.
         */

        /*
         * ============================================================
         * PRUNING
         * ============================================================
         *
         * Suppose:
         *
         *      used.size() = 3
         *
         * and only 2 characters remain.
         *
         * The absolute BEST we can possibly do is split those remaining
         * characters individually:
         *
         *      +1 substring per remaining character
         *
         * Therefore:
         *
         *      maximum possible count from this branch
         *
         *          = used.size() + remainingCharacters
         *
         *
         * If even this optimistic maximum cannot beat maxCount,
         * there is no reason to continue exploring this branch.
         *
         *
         * Example:
         *
         *      current best = 6
         *
         *      currently chosen = 3 substrings
         *
         *      remaining characters = 2
         *
         * Best possible:
         *
         *      3 + 2 = 5
         *
         * We can never beat 6.
         *
         * So stop exploring.
         */

        int remainingCharacters = s.length() - start;

        if (used.size() + remainingCharacters <= maxCount) {
            return;
        }

        /*
         * ============================================================
         * BASE CASE
         * ============================================================
         *
         * If start reaches s.length(), it means:
         *
         *      We successfully consumed the ENTIRE string.
         *
         * Therefore the substrings currently inside 'used'
         * form one valid complete split.
         *
         * Example:
         *
         *      s = "ababccc"
         *
         *      used = {"a", "b", "ab", "c", "cc"}
         *
         * Since the whole string has been consumed,
         * this candidate solution has:
         *
         *      used.size() = 5
         *
         * We update our global maximum.
         */

        if (start == s.length()) {
            maxCount = Math.max(maxCount, used.size());
            return;
        }

        /*
         * ============================================================
         * THE MOST IMPORTANT LOOP
         * ============================================================
         *
         * From the current 'start' index, try EVERY possible substring.
         *
         *
         * Example:
         *
         *      s = "abcd"
         *      start = 1
         *
         * Remaining portion:
         *
         *          b c d
         *          ^
         *
         * Possible substrings starting here:
         *
         *      "b"
         *      "bc"
         *      "bcd"
         *
         *
         * This loop is effectively implementing the decision:
         *
         *      "Where should I place the next cut?"
         *
         *
         * end = start + 1
         *      -> smallest possible non-empty substring
         *
         * end <= s.length()
         *      -> allows substring to extend all the way to the end
         *
         *
         * Remember that Java substring(start, end):
         *
         *      includes start
         *      excludes end
         */

        for (int end = start + 1; end <= s.length(); end++) {

            String currentSubstring = s.substring(start, end);

            /*
             * ========================================================
             * UNIQUE CONSTRAINT
             * ========================================================
             *
             * If this substring is already present in our CURRENT split,
             * we cannot choose it again.
             *
             *
             * Example:
             *
             *      used = {"a", "b"}
             *
             *      currentSubstring = "a"
             *
             * Choosing "a" again would produce:
             *
             *      a | b | a
             *
             * which violates the uniqueness requirement.
             *
             *
             * BUT IMPORTANT:
             *
             * We do NOT return here.
             *
             * Why?
             *
             * Because even though "a" is duplicate,
             * a longer substring may still be valid.
             *
             *
             * Example:
             *
             *      "a"  -> already used
             *
             * but:
             *
             *      "ab" -> may not be used
             *
             *
             * Therefore:
             *
             *      skip this substring,
             *      but keep extending 'end'.
             */

            if (used.contains(currentSubstring)) {
                continue;
            }

            /*
             * ========================================================
             * CHOOSE
             * ========================================================
             *
             * We are saying:
             *
             *      "Let's TRY placing a cut here."
             *
             * Add this substring to our current candidate partition.
             */

            used.add(currentSubstring);

            /*
             * ========================================================
             * EXPLORE
             * ========================================================
             *
             * We have consumed characters:
             *
             *      [start ... end - 1]
             *
             * Therefore the next unresolved portion starts at 'end'.
             *
             *
             * Example:
             *
             *      s = "abcdef"
             *
             *      currentSubstring = "abc"
             *
             * After choosing "abc":
             *
             *      abc | def
             *            ^
             *
             * Next recursion begins at index 3.
             */

            backtrack(s, end, used);

            /*
             * ========================================================
             * UNCHOOSE / BACKTRACK
             * ========================================================
             *
             * This is the step that converts our original greedy idea
             * into backtracking.
             *
             *
             * We tried:
             *
             *      "What happens if I choose currentSubstring?"
             *
             * That branch is now completely explored.
             *
             * So remove the substring and restore the state exactly
             * as it was before making the choice.
             *
             *
             * Then the loop continues and tries a LONGER substring.
             *
             *
             * Example:
             *
             * At start = 0:
             *
             *      first try:
             *
             *          choose "a"
             *          explore everything beginning after "a"
             *          remove "a"
             *
             *      then try:
             *
             *          choose "ab"
             *          explore everything beginning after "ab"
             *          remove "ab"
             *
             *      then:
             *
             *          choose "abc"
             *          ...
             *
             *
             * This is why we do NOT permanently commit to the first
             * unique substring that we encounter.
             */

            used.remove(currentSubstring);
        }
    }
}

/*
 * Greedy thinking:
 *
 *      "This substring works, so take it."
 *
 *
 * Backtracking thinking:
 *
 *      "This substring works, so TRY taking it.
 *       Explore the consequences.
 *       Undo it.
 *       Then try another possible substring."
 */

/*
 * At index 'start':
 *
 *      I am NOT deciding whether to take the next CHARACTER.
 *
 *      I am deciding:
 *
 *          "Where should the NEXT CUT be?"
 *
 *
 * Therefore:
 *
 *      for every possible 'end':
 *
 *          substring = s[start ... end)
 *
 *          if substring is valid:
 *              choose it
 *              recursively solve from 'end'
 *              undo it
 */
