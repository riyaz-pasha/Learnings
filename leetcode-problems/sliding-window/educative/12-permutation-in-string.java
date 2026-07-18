import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: "Permutation in String" (LeetCode 567 family)
 * ============================================================================
 *
 * Problem statement as given:
 *
 *   Given two strings s1 and s2, determine whether any permutation of s1
 *   appears as a contiguous substring within s2. Return TRUE if s2 contains
 *   a substring that uses the same characters as s1, with the same
 *   frequencies, but possibly in a different order. Otherwise, FALSE.
 *
 * This file is organized as a single Java source file, structured exactly as
 * I would walk through it in a real onsite/phone-screen interview, section by
 * section, using block comments as section headers.
 */
class PermutationInString {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words: I'm given a "pattern" string s1 and a "haystack"
     * string s2. I need to check whether s2 contains ANY contiguous window
     * whose multiset of characters (i.e., character frequency count) exactly
     * matches the multiset of characters in s1. The order of characters
     * inside that window does not matter -- only that it is some rearrangement
     * (permutation) of s1's characters. If such a window exists anywhere in
     * s2, I return true; otherwise false.
     *
     * Key constraints / inputs / outputs / assumptions (to confirm with
     * interviewer, formalized further in Section 2):
     *   - Input: two strings s1, s2.
     *   - Output: a boolean.
     *   - A "permutation" of s1 is any rearrangement of ALL of its
     *     characters -- so the matching window in s2 must have length
     *     exactly s1.length(), and identical character frequencies.
     *   - This is fundamentally a "fixed-size sliding window + frequency
     *     matching" problem, not a general subsequence or edit-distance
     *     problem.
     */


    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * 1. Q: What is the character set? Lowercase English letters only, or full
     *       Unicode / ASCII?
     *    A (assumed): Lowercase English letters 'a'-'z' only, as is standard
     *       for this classic problem. I will mention that the solution
     *       generalizes to a HashMap-based frequency count if the alphabet is
     *       larger or unknown (e.g., full Unicode).
     *
     * 2. Q: What are the size constraints on s1 and s2 (length)?
     *    A (assumed): 1 <= s1.length() <= s2.length() <= 10^4 or so (LeetCode-
     *       style constraints). This confirms an O(s2.length()) or
     *       O(s2.length() * alphabet size) solution is expected, not
     *       something exponential.
     *
     * 3. Q: Can s1 or s2 be empty strings?
     *    A (assumed): s1 is guaranteed non-empty (length >= 1). s2 could be
     *       empty, in which case the answer is trivially false unless s1 is
     *       also empty (an edge case I will guard defensively regardless).
     *
     * 4. Q: Does s1 contain duplicate characters, and if so, do their
     *       frequencies matter exactly, or just "at least this many"?
     *    A (assumed): Duplicates are allowed and frequencies must match
     *       EXACTLY -- e.g., if s1 = "aab", the matching window in s2 must
     *       have exactly two 'a's and one 'b', not more, not fewer.
     *
     * 5. Q: Is the match case-sensitive?
     *    A (assumed): Yes, case-sensitive. 'A' and 'a' are different
     *       characters unless told otherwise.
     *
     * 6. Q: Do I need to return WHERE the match occurs (index), or just
     *       whether one exists?
     *    A (assumed): Just a boolean per the problem statement. I'll note
     *       that my optimal solution can trivially be extended to return the
     *       starting index with almost no extra cost.
     *
     * 7. Q: Should I handle multiple calls efficiently (e.g., is this called
     *       repeatedly on the same s2 with different s1), implying
     *       preprocessing of s2?
     *    A (assumed): No, treat this as a single one-off call. I will
     *       mention that if this were called many times with a fixed s2,
     *       a suffix-automaton or precomputed rolling structure could help,
     *       but that's out of scope here.
     *
     * 8. Q: Is thread-safety / concurrency a concern?
     *    A (assumed): No, this runs on a single thread with immutable inputs.
     */


    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case):
     *   s1 = "ab", s2 = "eidbaooo"
     *   Windows of length 2 in s2: "ei","id","db","ba","ao","oo","oo"
     *   "ba" is a permutation of "ab" -> return TRUE.
     *
     * Example 2 (Edge case -- no match exists):
     *   s1 = "ab", s2 = "eidboaoo"
     *   No length-2 window has exactly one 'a' and one 'b' together
     *   -> return FALSE.
     *
     * Example 3 (Boundary / tie-breaking case -- duplicates & s1 longer
     * than remaining suffix of s2):
     *   s1 = "aab", s2 = "aacba"
     *   Windows of length 3: "aac","acb","cba"
     *   "acb" has frequencies {a:1,c:1,b:1} -- not equal to {a:2,b:1} -> no.
     *   "cba" similarly not equal.
     *   None match -> return FALSE.
     *   This exercises: duplicate character frequency counting (not just
     *   "does it contain the letters"), and the fact that a naive "contains
     *   all characters" check would incorrectly return TRUE here (since 'a',
     *   'b' are both present in "acb"/"cba") -- frequency EXACTNESS matters.
     *
     * Additional edge cases considered:
     *   - s1.length() > s2.length(): immediately FALSE (no valid window fits).
     *   - s1 == s2 exactly: TRUE (a string is trivially a permutation of
     *     itself).
     *   - s2 contains the match at the very start or very end (off-by-one
     *     risk in loop bounds).
     *   - All characters identical, e.g., s1 = "aaa", s2 = "aaaaa" -- multiple
     *     valid windows, only need to find one.
     */


    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (across relevant paradigms)
     * ========================================================================
     *
     * Paradigm sweep -- which categories apply and which don't:
     *   - Brute force / naive               -> APPLICABLE (baseline).
     *   - Sorting-based                     -> APPLICABLE (per-window sort).
     *   - Hashing-based (frequency arrays)   -> APPLICABLE (core insight).
     *   - Two pointer / sliding window       -> APPLICABLE (optimal solution).
     *   - Divide and conquer                 -> NOT APPLICABLE: there's no way
     *        to combine "does a permutation-substring exist" results from two
     *        independently-solved halves of s2, because a valid window can
     *        straddle the midpoint boundary; D&C offers no asymptotic benefit
     *        here.
     *   - Greedy                             -> NOT APPLICABLE: there is no
     *        notion of making locally optimal choices that build toward a
     *        global solution; this is a fixed-window matching problem, not an
     *        optimization/selection problem.
     *   - Dynamic programming                -> NOT APPLICABLE: there's no
     *        overlapping-subproblem/optimal-substructure relationship to
     *        exploit -- each window's frequency signature is independent and
     *        already computable incrementally via sliding window in O(1)
     *        amortized, so DP would add complexity without benefit.
     *   - Tree / graph traversal             -> NOT APPLICABLE: no inherent
     *        tree/graph structure in the input.
     *   - Heap / priority queue              -> NOT APPLICABLE: no ordering /
     *        top-K / priority extraction requirement.
     *   - Binary search                      -> NOT APPLICABLE: the search
     *        space (window start positions) isn't monotonic with respect to
     *        any predicate we could binary search on.
     *   - Monotonic stack / deque            -> NOT APPLICABLE: no "next
     *        greater/smaller element" or monotonic ordering structure here.
     *   - Trie / segment tree / advanced structures -> NOT APPLICABLE (over-
     *        kill): a trie helps with prefix matching across many patterns;
     *        we have exactly one fixed-length pattern to match by frequency,
     *        which fixed-size arrays handle in O(1) space already.
     *
     * The four genuinely meaningful approaches below are presented from most
     * naive to most optimal.
     */

    /**
     * -----------------------------------------------------------------------
     * Approach 1: Brute Force -- Generate All Permutations of s1
     * -----------------------------------------------------------------------
     * Core idea: Generate every distinct permutation of s1 as a String, then
     * check via String.contains() whether s2 contains any of them.
     *
     * Data structure / paradigm: Backtracking / permutation generation +
     * a HashSet to dedupe permutations of repeated characters.
     *
     * Time Complexity: O(n! * n) to generate permutations of length n =
     * s1.length() (each of the n! permutations costs O(n) to build), plus
     * O(n! * m) for the contains() checks against s2 of length m in the
     * worst case. This is factorial and utterly impractical beyond n ~ 8-10.
     *
     * Space Complexity: O(n! * n) to store all permutations (before
     * dedup up to n!/ (multiplicities!) distinct strings).
     *
     * Pros:
     *   - Conceptually the most straightforward "brute force" -- directly
     *     mirrors the problem statement ("any permutation of s1").
     *   - Easy to reason about correctness (it's exhaustive).
     *
     * Cons:
     *   - Factorial blow-up; unusable for n > ~10.
     *   - Wasteful: regenerates full permutations instead of reasoning about
     *     character frequency, which is the real invariant that matters.
     *
     * When to use: Only as a "let me start with the obvious brute force to
     * confirm the problem" opener in an interview, never in production, and
     * only worth actually running for tiny n in testing/validation.
     */
    public static boolean permutationInStringBruteForcePermutations(String s1, String s2) {
        if (s1.length() > s2.length()) {
            return false;
        }
        Set<String> allPermutationsOfS1 = new HashSet<>();
        generatePermutations(s1.toCharArray(), 0, allPermutationsOfS1);

        for (String permutation : allPermutationsOfS1) {
            if (s2.contains(permutation)) {
                return true;
            }
        }
        return false;
    }

    // Standard recursive backtracking permutation generator (Heap's-algorithm-
    // style via swapping), collecting distinct permutations into a Set.
    private static void generatePermutations(char[] characters, int startIndex, Set<String> resultCollector) {
        if (startIndex == characters.length) {
            resultCollector.add(new String(characters));
            return;
        }
        for (int swapIndex = startIndex; swapIndex < characters.length; swapIndex++) {
            swapCharacters(characters, startIndex, swapIndex);
            generatePermutations(characters, startIndex + 1, resultCollector);
            swapCharacters(characters, startIndex, swapIndex); // backtrack
        }
    }

    private static void swapCharacters(char[] characters, int indexA, int indexB) {
        char temporary = characters[indexA];
        characters[indexA] = characters[indexB];
        characters[indexB] = temporary;
    }


    /**
     * -----------------------------------------------------------------------
     * Approach 2: Sorting-Based -- Sort Each Fixed-Size Window and Compare
     * -----------------------------------------------------------------------
     * Core idea: A permutation check is equivalent to "sorted forms are
     * equal." Precompute sortedS1 = sort(s1). Then, for every contiguous
     * window of s2 with length == s1.length(), extract the substring, sort
     * it, and compare to sortedS1.
     *
     * Data structure / paradigm: Sorting + fixed-size sliding window
     * (re-extracted each iteration, not incrementally maintained).
     *
     * Time Complexity: O(n log n) to sort s1 once, then O((m - n + 1) * n log n)
     * to sort each of the (m - n + 1) windows of length n, where m =
     * s2.length(). Overall O(m * n log n) in the worst case.
     *
     * Space Complexity: O(n) per window for the extracted substring/char
     * array being sorted (plus O(n) for sortedS1).
     *
     * Pros:
     *   - Conceptually simple and easy to explain: "compare canonical sorted
     *     forms."
     *   - Correct and reasonably easy to verify by hand.
     *
     * Cons:
     *   - Re-sorts from scratch every window -- doesn't exploit the fact that
     *     consecutive windows differ by only one character entering and one
     *     leaving (wasted work).
     *   - Strictly dominated by the hashing/frequency-array approach below in
     *     both time and space.
     *
     * When to use: As an intermediate stepping stone in an interview to show
     * you understand the "permutation == same sorted string" equivalence,
     * before pivoting to the frequency-count approach for better complexity.
     * Not recommended for production use given a strictly better alternative
     * exists.
     */
    public static boolean permutationInStringSortingWindow(String s1, String s2) {
        int windowLength = s1.length();
        if (windowLength > s2.length()) {
            return false;
        }

        char[] sortedS1Characters = s1.toCharArray();
        Arrays.sort(sortedS1Characters);
        String sortedS1 = new String(sortedS1Characters);

        int lastValidStartIndex = s2.length() - windowLength;
        for (int windowStart = 0; windowStart <= lastValidStartIndex; windowStart++) {
            String currentWindow = s2.substring(windowStart, windowStart + windowLength);
            char[] sortedWindowCharacters = currentWindow.toCharArray();
            Arrays.sort(sortedWindowCharacters);
            if (sortedS1.equals(new String(sortedWindowCharacters))) {
                return true;
            }
        }
        return false;
    }


    /**
     * -----------------------------------------------------------------------
     * Approach 3: Hashing-Based -- Fixed-Size Frequency Array Per Window
     * -----------------------------------------------------------------------
     * Core idea: Instead of sorting, directly compare 26-length character
     * frequency count arrays (since the alphabet is lowercase English
     * letters). For each fixed-size window of s2, build its frequency array
     * from scratch and compare it to s1's frequency array using
     * Arrays.equals().
     *
     * Data structure / paradigm: Hashing via fixed-size frequency arrays
     * (a specialized/perfect hash over a small, known alphabet) + fixed-size
     * sliding window.
     *
     * Time Complexity: O(m * n) where m = s2.length(), n = s1.length() --
     * building each window's frequency array from scratch costs O(n), done
     * for O(m) windows. This is a clear improvement over sorting's O(m * n log n).
     *
     * Space Complexity: O(1) -- two fixed 26-length integer arrays,
     * independent of input size (since alphabet size is a constant).
     *
     * Pros:
     *   - Avoids sorting entirely -- direct O(26) array comparison.
     *   - Simple, robust, and easy to get right in an interview under time
     *     pressure.
     *   - Already asymptotically close to optimal for many practical input
     *     sizes.
     *
     * Cons:
     *   - Still redundantly rebuilds the entire frequency array for every
     *     new window position instead of incrementally updating it -- this
     *     is the key inefficiency the optimal sliding-window approach fixes.
     *
     * When to use: A very reasonable "safe, correct, and clearly explained"
     * solution to present first if time is tight, with the explicit
     * follow-up: "I can make the per-window comparison O(1) amortized instead
     * of O(n) by sliding the window incrementally -- would you like me to
     * optimize further?"
     */
    public static boolean permutationInStringFrequencyArrayPerWindow(String s1, String s2) {
        int windowLength = s1.length();
        int s2Length = s2.length();
        if (windowLength > s2Length) {
            return false;
        }

        final int ALPHABET_SIZE = 26;
        int[] s1FrequencyCounts = new int[ALPHABET_SIZE];
        for (int index = 0; index < windowLength; index++) {
            s1FrequencyCounts[s1.charAt(index) - 'a']++;
        }

        int lastValidStartIndex = s2Length - windowLength;
        for (int windowStart = 0; windowStart <= lastValidStartIndex; windowStart++) {
            int[] windowFrequencyCounts = new int[ALPHABET_SIZE];
            for (int offset = 0; offset < windowLength; offset++) {
                windowFrequencyCounts[s2.charAt(windowStart + offset) - 'a']++;
            }
            if (Arrays.equals(s1FrequencyCounts, windowFrequencyCounts)) {
                return true;
            }
        }
        return false;
    }


    /**
     * -----------------------------------------------------------------------
     * Approach 4 (OPTIMAL): Sliding Window With Incremental Frequency Diff
     * -----------------------------------------------------------------------
     * Core idea: Maintain a single frequency-difference array of size 26,
     * initialized to (count of each letter in s1) minus (count of each
     * letter in the FIRST window of s2). Also maintain a "matches" counter
     * tracking how many of the 26 letters currently have a difference of
     * exactly 0. As the window slides one character at a time (two-pointer
     * style: one new character enters on the right, one old character leaves
     * on the left), update only the O(1) affected counts and the matches
     * counter incrementally -- never re-scanning the whole window. The
     * window is a permutation match exactly when matches == 26 (i.e., every
     * letter's count difference is zero).
     *
     * Data structure / paradigm: Two-pointer fixed-size sliding window +
     * hashing via frequency array, with an O(1)-maintained "matches" counter
     * to avoid ever re-comparing the full 26-length arrays.
     *
     * Time Complexity: O(m + n) -- O(n) to build the initial frequency diff
     * from s1 and the first window, then O(m - n) O(1)-amortized slides
     * across the rest of s2. This is optimal: we must at minimum read every
     * character of s1 and s2 once.
     *
     * Space Complexity: O(1) -- one fixed 26-length integer array (constant
     * alphabet size), regardless of input length.
     *
     * Pros:
     *   - Optimal time complexity, O(1) extra space.
     *   - Single pass, no re-scanning -- ideal for an interview "final
     *     answer."
     *   - Trivially extended to return match indices instead of a boolean.
     *
     * Cons:
     *   - Slightly more intricate to implement correctly under pressure --
     *     easy to introduce off-by-one errors in the slide step or forget to
     *     update the matches counter symmetrically for both the entering and
     *     leaving character.
     *   - Assumes a small, fixed alphabet (or requires swapping the int[26]
     *     for a HashMap<Character,Integer> if the alphabet is large/unknown,
     *     which slightly increases constant factors but keeps the same
     *     asymptotic shape).
     *
     * When to use: This is the production-quality answer -- always prefer
     * this once you've demonstrated you understand the simpler approaches
     * and their trade-offs.
     */
    public static boolean permutationInStringOptimalSlidingWindow(String s1, String s2) {
        int patternLength = s1.length();
        int textLength = s2.length();
        if (patternLength > textLength) {
            return false;
        }

        final int ALPHABET_SIZE = 26;
        // frequencyDifference[c] = (count of letter c needed per s1)
        //                          - (count of letter c currently in window)
        int[] frequencyDifference = new int[ALPHABET_SIZE];
        for (int index = 0; index < patternLength; index++) {
            frequencyDifference[s1.charAt(index) - 'a']++;
        }

        // matchedLetterCount tracks how many of the 26 letters currently have
        // frequencyDifference == 0 (i.e., "satisfied"). A full match occurs
        // when matchedLetterCount == ALPHABET_SIZE.
        int matchedLetterCount = 0;
        for (int letterIndex = 0; letterIndex < ALPHABET_SIZE; letterIndex++) {
            if (frequencyDifference[letterIndex] == 0) {
                matchedLetterCount++;
            }
        }

        // Slide a window of size patternLength across s2, character by
        // character. windowStart and windowEnd together define the current
        // window [windowStart, windowEnd].
        for (int windowEnd = 0; windowEnd < textLength; windowEnd++) {

            // --- Step A: absorb the new character entering the window ---
            int enteringCharIndex = s2.charAt(windowEnd) - 'a';
            matchedLetterCount = adjustAfterCharacterEntersWindow(
                    frequencyDifference, enteringCharIndex, matchedLetterCount);

            // --- Step B: if the window has grown beyond patternLength,
            //             evict the character leaving on the left ---
            int windowStart = windowEnd - patternLength;
            if (windowStart >= 0) {
                int leavingCharIndex = s2.charAt(windowStart) - 'a';
                matchedLetterCount = adjustAfterCharacterLeavesWindow(
                        frequencyDifference, leavingCharIndex, matchedLetterCount);
            }

            // --- Step C: check for a full match ---
            if (matchedLetterCount == ALPHABET_SIZE) {
                return true;
            }
        }
        return false;
    }

    // A character entering the window means we need ONE FEWER of it, so we
    // decrement its frequencyDifference. We must update matchedLetterCount
    // by checking the count BEFORE and AFTER the change (it can only move
    // into or out of "satisfied" state by exactly one unit at a time).
    private static int adjustAfterCharacterEntersWindow(int[] frequencyDifference, int charIndex, int matchedLetterCount) {
        frequencyDifference[charIndex]--;
        if (frequencyDifference[charIndex] == 0) {
            matchedLetterCount++;       // this letter just became satisfied
        } else if (frequencyDifference[charIndex] == -1) {
            matchedLetterCount--;       // this letter just became over-represented
        }
        return matchedLetterCount;
    }

    // A character leaving the window means we need ONE MORE of it again, so
    // we increment its frequencyDifference, with the mirrored logic.
    private static int adjustAfterCharacterLeavesWindow(int[] frequencyDifference, int charIndex, int matchedLetterCount) {
        frequencyDifference[charIndex]++;
        if (frequencyDifference[charIndex] == 0) {
            matchedLetterCount++;       // this letter just became satisfied
        } else if (frequencyDifference[charIndex] == 1) {
            matchedLetterCount--;       // this letter just became under-represented
        }
        return matchedLetterCount;
    }


    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                              | Time            | Space | Best For                          | Limitations                                  |
     * |----------------------------------------|-----------------|-------|-----------------------------------|-----------------------------------------------|
     * | 1. Brute Force (all permutations)      | O(n! * n)       | O(n!) | Confirming problem understanding  | Factorial blow-up; unusable beyond tiny n     |
     * | 2. Sorting-Based (per window sort)     | O(m*n log n)    | O(n)  | Showing sorted-form equivalence   | Re-sorts every window; dominated by Approach 3|
     * | 3. Hashing (freq array per window)     | O(m*n)          | O(1)  | Safe correct first pass           | Rebuilds frequency array every window          |
     * | 4. Optimal Sliding Window (this file)  | O(m + n)        | O(1)  | Production / final interview ans. | Slightly trickier increment/decrement logic    |
     *
     *  (n = s1.length(), m = s2.length())
     */


    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 4 (Optimal Sliding Window with Incremental
     * Frequency Diff) as my final answer, but I would get there by BRIEFLY
     * narrating Approaches 2 and 3 first as reasoning checkpoints:
     *
     *   1. State the brute force (Approach 1) exists but is factorial --
     *      dismiss it in one sentence to show awareness without wasting time.
     *   2. Note the key insight: "permutation match" == "same character
     *      frequency multiset," which immediately suggests either sorting
     *      (Approach 2) or a frequency array (Approach 3).
     *   3. Prefer frequency arrays over sorting since comparing counts is
     *      O(26) flat, versus O(n log n) per window for sorting -- this is a
     *      clear, defensible complexity argument interviewers want to hear.
     *   4. Recognize that consecutive windows overlap almost entirely (all
     *      but 2 characters are shared), which is the classic signal for a
     *      sliding window optimization -- update counts incrementally rather
     *      than rebuilding from scratch, dropping the per-window cost from
     *      O(n) to O(1) amortized.
     *
     * This progression (brute force -> insight -> better data structure ->
     * exploit incremental structure) is exactly what Google interviewers look
     * for: not just landing on the optimal answer, but demonstrating the
     * reasoning chain that gets there, plus clean, defensible complexity
     * analysis at each step.
     */


    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE -- PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ========================================================================
     * See PermutationChecker class below for the polished, documented,
     * defensively-checked production version of Approach 4.
     */
    static final class PermutationChecker {

        private static final int LOWERCASE_ALPHABET_SIZE = 26;

        /**
         * Determines whether any permutation of {@code pattern} occurs as a
         * contiguous substring of {@code text}.
         *
         * <p>Implementation notes: uses a single fixed-size sliding window of
         * length {@code pattern.length()} over {@code text}, maintaining an
         * incremental character-frequency difference array so that each
         * slide step costs O(1) amortized time and O(1) extra space (a
         * constant-size 26-length array), for an overall O(text.length() +
         * pattern.length()) time complexity.
         *
         * @param pattern the string whose permutations we are searching for;
         *                must be non-null.
         * @param text    the string to search within; must be non-null.
         * @return {@code true} if some contiguous substring of {@code text}
         *         is a character-for-character permutation of {@code pattern};
         *         {@code false} otherwise, including when {@code pattern} is
         *         longer than {@code text}.
         * @throws NullPointerException if either argument is null.
         * @throws IllegalArgumentException if either argument contains a
         *         character outside the lowercase English alphabet ('a'-'z').
         */
        public static boolean containsPermutationOf(String pattern, String text) {
            Objects.requireNonNull(pattern, "pattern must not be null");
            Objects.requireNonNull(text, "text must not be null");
            validateLowercaseAlphabet(pattern);
            validateLowercaseAlphabet(text);

            final int patternLength = pattern.length();
            final int textLength = text.length();

            // Defensive/edge-case short-circuit: a window of pattern's exact
            // length cannot possibly fit inside a shorter text.
            if (patternLength == 0) {
                // An empty string is trivially "a permutation" of itself and
                // (by convention) a substring of any string, including an
                // empty one. Confirmed with interviewer in Section 2 that
                // s1 is guaranteed non-empty, but we guard defensively anyway.
                return true;
            }
            if (patternLength > textLength) {
                return false;
            }

            int[] requiredMinusPresentCount = buildInitialFrequencyDifference(pattern);
            int satisfiedLetterCount = countInitiallySatisfiedLetters(requiredMinusPresentCount);

            for (int windowEndInclusive = 0; windowEndInclusive < textLength; windowEndInclusive++) {
                satisfiedLetterCount = admitCharacterIntoWindow(
                        requiredMinusPresentCount,
                        text.charAt(windowEndInclusive),
                        satisfiedLetterCount);

                int windowStartExclusiveBoundary = windowEndInclusive - patternLength;
                if (windowStartExclusiveBoundary >= 0) {
                    satisfiedLetterCount = evictCharacterFromWindow(
                            requiredMinusPresentCount,
                            text.charAt(windowStartExclusiveBoundary),
                            satisfiedLetterCount);
                }

                if (satisfiedLetterCount == LOWERCASE_ALPHABET_SIZE) {
                    return true; // every letter's required count is exactly met
                }
            }
            return false;
        }

        private static void validateLowercaseAlphabet(String value) {
            for (int index = 0; index < value.length(); index++) {
                char character = value.charAt(index);
                if (character < 'a' || character > 'z') {
                    throw new IllegalArgumentException(
                            "Expected only lowercase 'a'-'z' characters, found: '" + character + "'");
                }
            }
        }

        private static int[] buildInitialFrequencyDifference(String pattern) {
            int[] frequencyDifference = new int[LOWERCASE_ALPHABET_SIZE];
            for (int index = 0; index < pattern.length(); index++) {
                frequencyDifference[pattern.charAt(index) - 'a']++;
            }
            return frequencyDifference;
        }

        private static int countInitiallySatisfiedLetters(int[] frequencyDifference) {
            int satisfiedCount = 0;
            for (int count : frequencyDifference) {
                if (count == 0) {
                    satisfiedCount++;
                }
            }
            return satisfiedCount;
        }

        // A character entering the window is now "present" one more time, so
        // the required-minus-present difference goes DOWN by one.
        private static int admitCharacterIntoWindow(int[] frequencyDifference, char enteringCharacter, int satisfiedLetterCount) {
            int letterIndex = enteringCharacter - 'a';
            frequencyDifference[letterIndex]--;
            if (frequencyDifference[letterIndex] == 0) {
                satisfiedLetterCount++;
            } else if (frequencyDifference[letterIndex] == -1) {
                satisfiedLetterCount--;
            }
            return satisfiedLetterCount;
        }

        // A character leaving the window is now "present" one fewer time, so
        // the required-minus-present difference goes UP by one.
        private static int evictCharacterFromWindow(int[] frequencyDifference, char leavingCharacter, int satisfiedLetterCount) {
            int letterIndex = leavingCharacter - 'a';
            frequencyDifference[letterIndex]++;
            if (frequencyDifference[letterIndex] == 0) {
                satisfiedLetterCount++;
            } else if (frequencyDifference[letterIndex] == 1) {
                satisfiedLetterCount--;
            }
            return satisfiedLetterCount;
        }

        private PermutationChecker() {
            // Utility class; no instances.
        }
    }


    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE (using the optimal solution)
     * ========================================================================
     *
     * Tracing s1 = "ab", s2 = "eidbaooo" through PermutationChecker:
     *
     * Initial frequencyDifference for "ab": index 'a'=0 -> +1, 'b'=1 -> +1.
     *   frequencyDifference = [a:1, b:1, others:0]
     * Initially satisfiedLetterCount = 24 (all letters except 'a' and 'b'
     *   already have difference 0).
     *
     * windowEndInclusive=0, char='e': admit 'e' -> diff['e'] goes 0 -> -1,
     *   satisfiedLetterCount: 'e' leaves satisfied set -> 23.
     *   windowStart = 0-2 = -2 (< 0, no eviction yet).
     *   satisfiedLetterCount=23 != 26 -> continue.
     *
     * windowEndInclusive=1, char='i': admit 'i' -> diff['i']: 0 -> -1,
     *   satisfiedLetterCount -> 22.
     *   windowStart = 1-2 = -1 (< 0, no eviction).
     *   22 != 26 -> continue.
     *
     * windowEndInclusive=2, char='d': admit 'd' -> diff['d']: 0 -> -1,
     *   satisfiedLetterCount -> 21.
     *   windowStart = 2-2 = 0 (>= 0!): evict s2.charAt(0)='e'.
     *     diff['e']: -1 -> 0, satisfiedLetterCount -> 22.
     *   22 != 26 -> continue.
     *   [window is now s2[1..2] = "id"]
     *
     * windowEndInclusive=3, char='b': admit 'b' -> diff['b']: 1 -> 0,
     *   satisfiedLetterCount -> 23.
     *   windowStart = 3-2 = 1 (>= 0): evict s2.charAt(1)='i'.
     *     diff['i']: -1 -> 0, satisfiedLetterCount -> 24.
     *   24 != 26 -> continue.
     *   [window is now s2[2..3] = "db"]
     *
     * windowEndInclusive=4, char='a': admit 'a' -> diff['a']: 1 -> 0,
     *   satisfiedLetterCount -> 25.
     *   windowStart = 4-2 = 2 (>= 0): evict s2.charAt(2)='d'.
     *     diff['d']: -1 -> 0, satisfiedLetterCount -> 26.
     *   satisfiedLetterCount == 26 -> RETURN TRUE.
     *   [window is now s2[3..4] = "ba", which is indeed a permutation of "ab"]
     *
     * Final result: TRUE, matching the expected answer from Section 3,
     * Example 1. Every increment/decrement of satisfiedLetterCount is O(1),
     * so the whole trace ran in O(s2.length()) total work.
     */


    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - The problem reduces to: "does any fixed-length window of s2 share an
     *   identical character-frequency signature with s1?"
     * - Brute-force permutation generation (Approach 1) is correct but
     *   factorial and impractical.
     * - Sorting-based comparison (Approach 2) is a reasonable intermediate
     *   step, O(m * n log n), but is asymptotically dominated once we realize
     *   frequency counting avoids sorting entirely.
     * - Frequency-array-per-window (Approach 3), O(m * n), is a safe,
     *   correct, easy-to-explain solution -- a good "first working answer."
     * - The optimal sliding-window approach (Approach 4), O(m + n) time and
     *   O(1) space, exploits the fact that consecutive windows overlap in all
     *   but two characters, so we maintain frequency differences and a
     *   "satisfied letter count" incrementally rather than recomputing from
     *   scratch.
     * - Known assumptions/limitations of the final solution: assumes a fixed
     *   lowercase English alphabet (26 letters) for O(1) space; for a
     *   larger/unknown alphabet (e.g., full Unicode), the fixed array would
     *   be replaced by a HashMap<Character,Integer>, which preserves the same
     *   O(m + n) time shape but with higher constant factors and O(distinct
     *   characters) space instead of true O(1).
     */


    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "Can you return ALL starting indices of matching windows, not just
     *     a boolean?" -> Trivial extension: instead of returning true at
     *     first match, collect windowStart into a List<Integer> and continue
     *     scanning to the end.
     * 2. "What if the alphabet is full Unicode instead of just lowercase
     *     English letters?" -> Swap the int[26] arrays for
     *     HashMap<Character,Integer>, and track "distinct characters
     *     required" instead of a fixed 26 for the matched-count target.
     * 3. "What if s1 can be very large (e.g., 10^6) and this function is
     *     called repeatedly with different s2 values but the SAME s1?" ->
     *     Precompute s1's frequency array once and reuse it across calls;
     *     the amortized per-call cost stays O(s2.length()).
     * 4. "Can you solve the analogous problem for finding all anagrams
     *     (LeetCode 438), returning every start index instead of a boolean?"
     *     -> Same sliding-window technique, just collect matches instead of
     *     early-returning.
     * 5. "What if we only need to match a SUBSET of s1's characters (i.e., a
     *     'contains at least these counts' rather than 'exactly these
     *     counts') check?" -> Change the matching condition to only track
     *     "count(window) >= count(pattern)" per letter rather than requiring
     *     exact equality (a supersequence checkrather than exact
     *     permutation check).
     * 6. "How would you parallelize this across a very large s2 (e.g.,
     *     streaming or distributed across machines)?" -> Partition s2 into
     *     overlapping chunks (each overlap of size patternLength - 1 to avoid
     *     missing boundary-straddling windows), run the sliding window
     *     independently per chunk, and OR the boolean results together.
     */


    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Forgetting the eviction step entirely, or evicting BEFORE admitting
     *    the new character -- this shifts the window boundary by one and
     *    produces subtly wrong results only on some inputs (easy to miss in
     *    quick manual testing).
     * 2. Off-by-one in the eviction index: the character leaving the window
     *    is at index (windowEnd - patternLength), NOT (windowEnd -
     *    patternLength + 1) or (windowEnd - patternLength - 1); candidates
     *    frequently mis-derive this under pressure.
     * 3. Checking "does the window contain all the same characters" instead
     *    of "does it contain the same character FREQUENCIES" -- e.g.,
     *    incorrectly treating "acb" as a match for "aab" just because both
     *    contain 'a' and 'b' (see Section 3, Example 3) -- frequency
     *    exactness, including duplicates, is essential.
     * 4. Not short-circuiting when s1.length() > s2.length() -- without this
     *    guard, the sliding window logic can throw an ArrayIndexOutOfBounds/
     *    StringIndexOutOfBounds exception or silently loop zero times and
     *    return an incorrect false-negative/positive depending on
     *    implementation details, rather than cleanly returning false.
     */


    /*
     * ========================================================================
     * TEST HARNESS -- cross-validates all four approaches against each other
     * and against expected results for the examples from Section 3.
     * ========================================================================
     */
    public static void main(String[] args) {
        record TestCase(String s1, String s2, boolean expected, String description) {}

        List<TestCase> testCases = List.of(
                new TestCase("ab", "eidbaooo", true, "Example 1: normal case, match exists ('ba')"),
                new TestCase("ab", "eidboaoo", false, "Example 2: edge case, no match exists"),
                new TestCase("aab", "aacba", false, "Example 3: boundary case, duplicates, no match"),
                new TestCase("abc", "abc", true, "s1 equals s2 exactly"),
                new TestCase("abcd", "abc", false, "s1 longer than s2"),
                new TestCase("aaa", "aaaaa", true, "all identical characters, multiple valid windows"),
                new TestCase("a", "a", true, "single-character exact match"),
                new TestCase("a", "b", false, "single-character no match"),
                new TestCase("adc", "dcda", true, "match at the very end of s2")
        );

        int totalTests = testCases.size();
        int passedTests = 0;

        for (TestCase testCase : testCases) {
            boolean resultBruteForce = permutationInStringBruteForcePermutations(testCase.s1(), testCase.s2());
            boolean resultSorting = permutationInStringSortingWindow(testCase.s1(), testCase.s2());
            boolean resultFrequencyArray = permutationInStringFrequencyArrayPerWindow(testCase.s1(), testCase.s2());
            boolean resultOptimal = permutationInStringOptimalSlidingWindow(testCase.s1(), testCase.s2());
            boolean resultProduction = PermutationChecker.containsPermutationOf(testCase.s1(), testCase.s2());

            boolean allApproachesAgree =
                    resultBruteForce == testCase.expected() &&
                    resultSorting == testCase.expected() &&
                    resultFrequencyArray == testCase.expected() &&
                    resultOptimal == testCase.expected() &&
                    resultProduction == testCase.expected();

            if (allApproachesAgree) {
                passedTests++;
                System.out.printf("[PASS] %s -> %b%n", testCase.description(), testCase.expected());
            } else {
                System.out.printf(
                        "[FAIL] %s | expected=%b brute=%b sorting=%b freqArray=%b optimal=%b production=%b%n",
                        testCase.description(), testCase.expected(), resultBruteForce,
                        resultSorting, resultFrequencyArray, resultOptimal, resultProduction);
            }
        }

        System.out.printf("%n%d / %d test cases passed across all five implementations.%n", passedTests, totalTests);
    }

    class PermutationInString2 {

        /**
         * Returns true if any permutation of s1 exists as a substring of s2.
         *
         * Assumption:
         * - Strings contain only lowercase English letters.
         */
        public static boolean checkInclusion(String s1, String s2) {

            // If s1 is longer, it is impossible.
            if (s1.length() > s2.length()) {
                return false;
            }

            int[] need = new int[26];
            int[] window = new int[26];

            // Build frequency of s1.
            for (char c : s1.toCharArray()) {
                need[c - 'a']++;
            }

            int windowSize = s1.length();

            for (int right = 0; right < s2.length(); right++) {

                // Include current character into the window.
                window[s2.charAt(right) - 'a']++;

                // Keep window size fixed.
                if (right >= windowSize) {
                    window[s2.charAt(right - windowSize) - 'a']--;
                }

                // Compare frequencies.
                if (matches(need, window)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Returns true if both frequency arrays are identical.
         */
        private static boolean matches(int[] a, int[] b) {
            for (int i = 0; i < 26; i++) {
                if (a[i] != b[i]) {
                    return false;
                }
            }
            return true;
        }

        public static void main(String[] args) {

            System.out.println(checkInclusion("ab", "eidbaooo")); // true
            System.out.println(checkInclusion("ab", "eidboaoo")); // false
            System.out.println(checkInclusion("adc", "dcda")); // true
        }
    }

}
