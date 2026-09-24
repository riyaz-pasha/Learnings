import java.util.*;

/*
================================================================================
 GOOGLE ONSITE MOCK INTERVIEW TRANSCRIPT
 Problem: Longest Repeating Character Replacement  (LeetCode 424, Medium)
 Tags: Sliding Window, Hashing/Counting, String
================================================================================
*/

/*
 * ============================================================================
 * SECTION 1: RESTATE THE PROBLEM
 * ============================================================================
 * In my own words:
 *   We are given a string `s` made up of uppercase English letters, and an
 *   integer `k`. We are allowed to change AT MOST `k` characters in `s` to
 *   any other uppercase letter we like. After making these changes, we want
 *   the longest possible contiguous run (substring) of a single repeated
 *   character. We must return the LENGTH of that longest achievable run --
 *   not the substring itself, and not the modified string.
 *
 * Key constraints / inputs / outputs:
 *   - Input: String s (uppercase 'A'-'Z' only), integer k (0 <= k <= s.length())
 *   - Output: An integer -- the length of the longest substring of identical
 *             characters obtainable after at most k replacements.
 *   - The replacements are "virtual" -- we never actually need to construct
 *     the resulting string, only determine the best achievable window length.
 *   - Any window [left, right] is achievable if and only if
 *         (windowLength - countOfMostFrequentCharInWindow) <= k
 *     i.e., the number of characters that are NOT the majority character in
 *     that window is at most k (those are the ones we'd flip).
 *
 * Assumptions I'll state out loud to the interviewer:
 *   - Alphabet is fixed size 26 (uppercase English letters only).
 *   - s is non-null; length could be 0.
 *   - k is non-negative and never larger than necessary to worry about
 *     overflow (bounded by s.length()).
 */

/*
 * ============================================================================
 * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
 * ============================================================================
 * 1. Q: What is the expected size of `s`? Do we need to worry about huge
 *       inputs (10^6+) that demand a linear-time solution?
 *    A: Assume up to 10^5 characters, so O(n) or O(n log n) is expected;
 *       O(n^2) would likely time out but is fine to mention as a stepping
 *       stone.
 *
 * 2. Q: Is the alphabet strictly uppercase A-Z, or could it include lowercase,
 *       digits, or unicode?
 *    A: Strictly uppercase English letters (26 possible characters), per the
 *       problem statement.
 *
 * 3. Q: Can k be 0? What should happen then?
 *    A: Yes. If k == 0, the answer is simply the longest run of an
 *       already-repeated character with zero replacements.
 *
 * 4. Q: Can k exceed s.length()? Should we clamp it?
 *    A: Assume 0 <= k <= s.length(); no clamping needed, but the algorithm
 *       naturally handles k >= s.length() by returning s.length().
 *
 * 5. Q: Should the replacement characters be constrained (e.g., must differ
 *       from the original), or can we "replace" a character with itself?
 *    A: Doesn't matter for correctness -- if a character already equals the
 *       target, no actual replacement occurs, but conceptually it doesn't
 *       change the math (windowLength - maxFreq counts only actual mismatches).
 *
 * 6. Q: Do we need to return WHICH substring achieves the optimum, or just
 *       its length?
 *    A: Just the length, per the problem statement.
 *
 * 7. Q: Is s ever empty or null?
 *    A: Assume s can be empty (length 0) but not null; we'll guard for
 *       null defensively and return 0.
 *
 * 8. Q: Is this a single-threaded, single-call problem, or do we need to
 *       support concurrent/repeated queries on the same string efficiently?
 *    A: Single call, single-threaded -- no concurrency concerns for this
 *       problem in its classic form.
 */

/*
 * ============================================================================
 * SECTION 3: EXAMPLES & EDGE CASES
 * ============================================================================
 * Example 1 (normal case):
 *   s = "AABABBA", k = 1
 *   One good choice: flip the 'A' at index 3 to 'B' -> "AAB[B]BBA" is NOT
 *   optimal; the better-known optimal window is s[0..3] = "AABA" -> flip the
 *   single 'B' at index 2 to 'A', giving "AAAA" (length 4), using 1 replacement.
 *   Expected output: 4
 *
 * Example 2 (edge case -- k = 0):
 *   s = "ABAB", k = 0
 *   No replacements allowed, so the answer is the longest existing run of a
 *   single character, which is 1 (no two adjacent characters are equal).
 *   Expected output: 1
 *
 * Example 3 (boundary / tie-breaking case -- k covers everything):
 *   s = "ABCD", k = 3
 *   We can replace 3 of the 4 characters to match the 4th, turning the whole
 *   string into one repeated character.
 *   Expected output: 4
 *
 * Additional edge cases worth mentioning out loud:
 *   - Empty string: s = "", k = 5 -> answer 0.
 *   - Single character: s = "Z", k = 0 -> answer 1.
 *   - All identical characters: s = "AAAA", k = 2 -> answer 4 (no
 *     replacements even needed).
 *   - k larger than needed: s = "AAAB", k = 5 -> answer 4 (capped at
 *     s.length(), extra k is simply unused).
 */

class LongestRepeatingCharacterReplacement {

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS
     * ========================================================================
     */

    /*
     * ------------------------------------------------------------------
     * Approach 1: Naive Triple Loop (True Brute Force)
     * ------------------------------------------------------------------
     * Core idea:
     *   For every possible substring s[i..j], count the frequency of each
     *   of the 26 letters within it from scratch, find the max frequency
     *   character, and check if (length - maxFreq) <= k. Track the best
     *   valid length seen.
     *
     * Paradigm: Brute force enumeration.
     *
     * Time Complexity: O(n^3) -- O(n^2) substrings, each requiring O(n) to
     *   recompute frequencies from scratch.
     * Space Complexity: O(26) = O(1) for the frequency array per substring.
     *
     * Pros:
     *   - Extremely easy to reason about and verify correctness against.
     *   - Zero risk of subtle bugs; great "oracle" for stress testing.
     * Cons:
     *   - Far too slow for n > ~500; would TLE on real constraints.
     * When to use:
     *   - Only as a correctness oracle during development/testing, never
     *     as a submitted production solution.
     * ------------------------------------------------------------------
     */
    static int longestRepeatingBruteForceCubic(String s, int k) {
        if (s == null || s.isEmpty()) return 0;
        int n = s.length();
        int bestLength = 0;
        for (int start = 0; start < n; start++) {
            for (int end = start; end < n; end++) {
                int[] frequency = new int[26];
                for (int index = start; index <= end; index++) {
                    frequency[s.charAt(index) - 'A']++;
                }
                int maxFrequency = 0;
                for (int count : frequency) maxFrequency = Math.max(maxFrequency, count);
                int windowLength = end - start + 1;
                if (windowLength - maxFrequency <= k) {
                    bestLength = Math.max(bestLength, windowLength);
                }
            }
        }
        return bestLength;
    }

    /*
     * ------------------------------------------------------------------
     * Approach 2: Improved Brute Force (Incremental Counting / Hashing)
     * ------------------------------------------------------------------
     * Core idea:
     *   Same nested-loop structure, but instead of recomputing frequencies
     *   from scratch for every (start, end) pair, we fix `start` and slide
     *   `end` forward, incrementally updating a frequency array (a direct-
     *   address hash map over the fixed 26-letter alphabet) and a running
     *   maxFrequency. This removes the innermost O(n) recount.
     *
     * Paradigm: Hashing / counting (direct-address table), brute force outer
     *   loop structure retained.
     *
     * Time Complexity: O(n^2) -- for each of the n starting points, we scan
     *   forward once, O(1) work per step.
     * Space Complexity: O(26) = O(1).
     *
     * Pros:
     *   - Much faster than the cubic version, still simple to write and
     *     explain.
     *   - Good "middle ground" to narrate briefly before jumping to the
     *     optimal window solution.
     * Cons:
     *   - Still quadratic; would not pass for n ~ 10^5.
     * When to use:
     *   - Reasonable fallback under extreme time pressure, or as a secondary
     *     oracle. Not the final answer in an interview focused on optimality.
     * ------------------------------------------------------------------
     */
    static int longestRepeatingBruteForceQuadratic(String s, int k) {
        if (s == null || s.isEmpty()) return 0;
        int n = s.length();
        int bestLength = 0;
        for (int start = 0; start < n; start++) {
            int[] frequency = new int[26];
            int maxFrequencyInWindow = 0;
            for (int end = start; end < n; end++) {
                int letterIndex = s.charAt(end) - 'A';
                frequency[letterIndex]++;
                maxFrequencyInWindow = Math.max(maxFrequencyInWindow, frequency[letterIndex]);
                int windowLength = end - start + 1;
                if (windowLength - maxFrequencyInWindow <= k) {
                    bestLength = Math.max(bestLength, windowLength);
                }
            }
        }
        return bestLength;
    }

    /*
     * ------------------------------------------------------------------
     * Approach 3: Sliding Window -- Shrink-on-Violation (Correct & Clear)
     * ------------------------------------------------------------------
     * Core idea:
     *   Maintain a window [left, right] with a frequency array of the 26
     *   letters inside it and the max frequency of any single letter in the
     *   current window. Expand `right` one step at a time. If the window
     *   ever becomes invalid -- i.e. (windowLength - maxFrequency) > k --
     *   shrink from the left (removing s[left] from the frequency count and
     *   advancing left) until it is valid again. Track the best valid window
     *   length seen at every step.
     *
     * Paradigm: Classic variable-size sliding window + counting.
     *
     * Time Complexity: O(n) -- each index enters and leaves the window at
     *   most once, so left and right each advance at most n times total.
     * Space Complexity: O(26) = O(1).
     *
     * Pros:
     *   - Linear time, easy to justify correctness: the window is *always*
     *     valid at the end of each iteration, so maxFrequency is always
     *     accurate for the current window.
     *   - Very intuitive to explain: "grow greedily, shrink when broken."
     * Cons:
     *   - Slightly more code than Approach 4 because of the explicit shrink
     *     loop (technically the shrink is at most one step per expansion
     *     here, but that's a subtlety worth calling out).
     * When to use:
     *   - This is a perfectly good, fully correct O(n) solution to present
     *     if you want the "obviously correct" version first, with Approach 4
     *     offered as a polish/optimization afterward.
     * ------------------------------------------------------------------
     */
    static int longestRepeatingSlidingWindowShrinking(String s, int k) {
        if (s == null || s.isEmpty()) return 0;
        int n = s.length();
        int[] frequency = new int[26];
        int left = 0;
        int maxFrequencyInWindow = 0;
        int bestLength = 0;

        for (int right = 0; right < n; right++) {
            int rightLetterIndex = s.charAt(right) - 'A';
            frequency[rightLetterIndex]++;
            maxFrequencyInWindow = Math.max(maxFrequencyInWindow, frequency[rightLetterIndex]);

            // Shrink from the left while the window requires more than k replacements.
            while ((right - left + 1) - maxFrequencyInWindow > k) {
                int leftLetterIndex = s.charAt(left) - 'A';
                frequency[leftLetterIndex]--;
                left++;
                // Note: maxFrequencyInWindow is intentionally NOT recomputed here.
                // Recomputing it would cost O(26) per shrink step; since we only
                // ever need to know whether the window is currently valid (and
                // the window length itself only grows when we find a new best),
                // an over-estimated maxFrequencyInWindow can only ever make the
                // shrink condition MORE conservative, never break correctness
                // of the reported bestLength. See Approach 4 for the full
                // "greedy, never shrink" argument, which relies on this same fact.
                maxFrequencyInWindow = recomputeMaxFrequency(frequency);
            }

            bestLength = Math.max(bestLength, right - left + 1);
        }
        return bestLength;
    }

    // Helper for Approach 3: O(26) recompute, used only to keep Approach 3
    // fully "textbook correct" at every step (see Approach 4 for the version
    // that skips this and is still provably correct).
    private static int recomputeMaxFrequency(int[] frequency) {
        int max = 0;
        for (int count : frequency) max = Math.max(max, count);
        return max;
    }

    /*
     * ------------------------------------------------------------------
     * Approach 4: Sliding Window -- Greedy Non-Shrinking (OPTIMAL, Classic)
     * ------------------------------------------------------------------
     * Core idea:
     *   Same window and frequency array as Approach 3, but instead of
     *   shrinking the window back down when it becomes invalid, we simply
     *   slide it: increment `right`, and if the window is invalid, move
     *   `left` forward by exactly one step (net window LENGTH stays the
     *   same or grows -- it never shrinks below its previous best size).
     *   We never decrease `maxFrequencyInWindow` even when characters leave
     *   the window on the left.
     *
     *   Why this is still correct: `maxFrequencyInWindow` may become STALE
     *   (an overestimate of the true max frequency in the current window)
     *   once we start dropping left-side characters without recomputing it.
     *   But that's fine, because:
     *     1. The window length we report as the answer is monotonically
     *        non-decreasing -- we only ever grow or hold the window size.
     *     2. A stale (too-high) maxFrequencyInWindow can only make us think
     *        a window of the CURRENT size is valid when it might not be --
     *        but we already proved a window of that exact size WAS valid
     *        earlier (that's how maxFrequencyInWindow got that high in the
     *        first place). So the reported length is never wrong, even
     *        though the window's exact contents may drift.
     *   In short: we are not tracking "the best window right now"; we are
     *   tracking "the best window length found so far," and using the
     *   window purely as a mechanism to discover strictly longer windows.
     *
     * Paradigm: Sliding window + greedy (never shrink) + counting.
     *
     * Time Complexity: O(n) -- both left and right pointers only move
     *   forward, each at most n times total. No inner while-loop.
     * Space Complexity: O(26) = O(1).
     *
     * Pros:
     *   - Optimal time complexity with the tightest constant factor of all
     *     approaches (no shrink loop, no recompute loop).
     *   - Elegant once the "stale max frequency is OK" argument is
     *     understood -- a great chance to demonstrate deep insight in an
     *     interview.
     * Cons:
     *   - The correctness argument is non-obvious; must be explained
     *     clearly or it looks like a bug to the interviewer.
     * When to use:
     *   - This is the production-quality answer for this problem; use it
     *     as the final, polished solution.
     * ------------------------------------------------------------------
     */
    static int longestRepeatingSlidingWindowOptimal(String s, int k) {
        if (s == null || s.isEmpty()) return 0;
        int n = s.length();
        int[] frequency = new int[26];
        int left = 0;
        int maxFrequencyInWindow = 0; // Highest frequency of any single letter ever seen in-window.

        for (int right = 0; right < n; right++) {
            int rightLetterIndex = s.charAt(right) - 'A';
            frequency[rightLetterIndex]++;
            maxFrequencyInWindow = Math.max(maxFrequencyInWindow, frequency[rightLetterIndex]);

            int windowLength = right - left + 1;
            if (windowLength - maxFrequencyInWindow > k) {
                // Window is invalid: slide (not shrink) by moving left forward once.
                int leftLetterIndex = s.charAt(left) - 'A';
                frequency[leftLetterIndex]--;
                left++;
                // windowLength stays exactly the same after this: right advanced by 1
                // next iteration, left advanced by 1 this iteration -- net window size
                // is preserved, never allowed to shrink below its previous peak.
            }
        }
        // Final window size (right - left + 1) at loop end equals the best length found,
        // since the window length is non-decreasing throughout.
        return n - left;
    }

    /*
     * ------------------------------------------------------------------
     * Approach 5: Binary Search on Answer Length + Window Feasibility Check
     * ------------------------------------------------------------------
     * Core idea:
     *   Binary search over candidate answer lengths L in [0, n]. For a
     *   given L, check feasibility with a fixed-size sliding window of
     *   width L: slide it across s, and see if ANY position has
     *   (L - maxFrequencyInThatWindow) <= k. If some window of size L is
     *   feasible, try larger L; otherwise try smaller L.
     *
     * Paradigm: Binary search + sliding window (fixed-size) feasibility
     *   check.
     *
     * Time Complexity: O(n log n) -- O(log n) binary search iterations,
     *   each doing an O(n) fixed-window feasibility scan.
     * Space Complexity: O(26) = O(1).
     *
     * Pros:
     *   - Demonstrates the "binary search on the answer" pattern, a
     *     transferable technique for many optimization problems.
     *   - Conceptually decouples "is length L achievable?" from "what is
     *     the max achievable length?", which can be a useful reframing.
     * Cons:
     *   - Strictly worse than Approach 4 (O(n log n) vs O(n)) with no
     *     offsetting benefit for this specific problem.
     *   - The feasibility function is monotonic (if L works, so does any
     *     L' < L), which is what justifies binary search, but that
     *     monotonicity argument itself takes explanation time better spent
     *     elsewhere in an interview.
     * When to use:
     *   - Worth mentioning as an alternative if the interviewer asks "can
     *     you think of another approach?", but Approach 4 should be your
     *     primary submission.
     * ------------------------------------------------------------------
     */
    static int longestRepeatingBinarySearch(String s, int k) {
        if (s == null || s.isEmpty()) return 0;
        int n = s.length();
        int lo = 0, hi = n;
        int best = 0;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (mid == 0 || isLengthFeasible(s, k, mid)) {
                best = Math.max(best, mid);
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
    }

    // Fixed-size window feasibility check used by Approach 5: is there a
    // window of exactly `length` where (length - maxFrequency) <= k?
    private static boolean isLengthFeasible(String s, int k, int length) {
        int[] frequency = new int[26];
        int maxFrequencyInWindow = 0;
        for (int index = 0; index < s.length(); index++) {
            int letterIndex = s.charAt(index) - 'A';
            frequency[letterIndex]++;
            maxFrequencyInWindow = Math.max(maxFrequencyInWindow, frequency[letterIndex]);

            if (index >= length) {
                int outgoingLetterIndex = s.charAt(index - length) - 'A';
                frequency[outgoingLetterIndex]--;
                // Recompute is needed here since window is fixed-size and we need
                // an accurate feasibility check for THIS exact length each time.
                maxFrequencyInWindow = recomputeMaxFrequency(frequency);
            }
            if (index >= length - 1 && length - maxFrequencyInWindow <= k) {
                return true;
            }
        }
        return length == 0;
    }

    /*
     * ------------------------------------------------------------------
     * Paradigms considered and explicitly ruled out (with reasons):
     * ------------------------------------------------------------------
     *   - Sorting: Order and adjacency of characters is exactly what
     *     defines a "substring" here; sorting destroys positional/contiguity
     *     information, so it cannot help.
     *   - Divide and Conquer: There's no clean way to combine answers from
     *     the left half and right half of the string, because the optimal
     *     window can straddle the midpoint arbitrarily and depends on
     *     character composition, not on independent subproblems. Doesn't
     *     decompose naturally.
     *   - Dynamic Programming: One could imagine a DP over (position,
     *     replacements used so far), but the state would need to track
     *     "which character are we currently trying to extend," effectively
     *     re-deriving the sliding-window frequency logic with far more
     *     bookkeeping and no better complexity. Not a natural or idiomatic
     *     fit.
     *   - Tree / Graph traversal: No graph or hierarchical structure exists
     *     in the problem; not applicable.
     *   - Heap / Priority Queue: We only ever need the SINGLE maximum
     *     frequency among 26 fixed buckets, which a heap would over-engineer;
     *     a simple O(26) scan or running max is strictly simpler and just
     *     as fast.
     *   - Trie / Segment Tree: These shine when queries are over arbitrary
     *     ranges repeatedly or over dynamic/large alphabets; here the
     *     alphabet is a fixed 26 letters and we only need one linear pass,
     *     so these advanced structures add complexity without benefit.
     *   - Greedy (standalone): Greedy is really an ingredient of the
     *     sliding window approaches (Approach 4 in particular is greedy in
     *     spirit) rather than a separate standalone approach here.
     */

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                                   | Time       | Space | Best For                                   | Limitations                                    |
     * |---------------------------------------------|------------|-------|--------------------------------------------|-------------------------------------------------|
     * | 1. Naive Triple Loop                         | O(n^3)     | O(1)  | Correctness oracle, tiny n (<= ~200)         | Unusably slow for real constraints              |
     * | 2. Improved Brute Force (incremental count)  | O(n^2)     | O(1)  | Secondary oracle, quick fallback under panic | Still TLEs around n ~ 10^4-10^5                  |
     * | 3. Sliding Window (shrink-on-violation)       | O(n)       | O(1)  | Clear, obviously-correct linear solution     | Slightly more code than Approach 4 (shrink loop) |
     * | 4. Sliding Window (greedy, non-shrinking)     | O(n)       | O(1)  | Production/interview-optimal answer          | Correctness argument is subtle, must explain it  |
     * | 5. Binary Search on Answer + Window Check     | O(n log n) | O(1)  | Showcasing binary-search-on-answer pattern   | Strictly dominated by Approach 4 here            |
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ========================================================================
     * I would present Approach 4 (Sliding Window, Greedy Non-Shrinking) as my
     * final solution, but I would get there by talking through the process
     * out loud in this order:
     *   1. State the brute-force triple loop (Approach 1) in one sentence
     *      to show I understand the problem's baseline, without coding it.
     *   2. Note the key reframe: "for any window, the number of characters
     *      to replace equals windowLength - (count of most frequent char
     *      in that window)." This reframe is the crux of every faster
     *      approach and is worth stating explicitly.
     *   3. Code Approach 3 (shrink-on-violation) first if I want to be
     *      safe and obviously correct -- it's O(n) and easy to verify live.
     *   4. Proactively upgrade to Approach 4, explaining the "stale max
     *      frequency is fine because window length never decreases" insight.
     *      This demonstrates depth beyond "I found A solution" -- it shows I
     *      understand WHY the simplification is valid, not just that it
     *      passes.
     * Why Approach 4 specifically:
     *   - It is asymptotically optimal (O(n) time, O(1) extra space).
     *   - It has the tightest constant factor (no inner while-loop, no
     *     recompute-max calls).
     *   - It is a well-known "signature" pattern (also seen in Max
     *     Consecutive Ones III, Longest Substring with At Most K Distinct
     *     Characters), so recognizing and articulating it signals strong
     *     pattern recognition to the interviewer.
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE -- OPTIMAL SOLUTION (production-quality)
     * ========================================================================
     * This is a clean, final version of Approach 4 intended as the actual
     * submission, with defensive null/empty handling and full comments.
     */
    public static int characterReplacement(String s, int k) {
        // Defensive guard: treat null as empty input.
        if (s == null || s.isEmpty()) {
            return 0;
        }

        final int ALPHABET_SIZE = 26;
        int[] letterFrequency = new int[ALPHABET_SIZE]; // counts of each uppercase letter in the current window
        int windowStart = 0;
        int maxSingleLetterFrequency = 0; // highest frequency of any one letter EVER seen while right-expanding

        for (int windowEnd = 0; windowEnd < s.length(); windowEnd++) {
            // 1) Absorb the new right-hand character into the window.
            int incomingLetter = s.charAt(windowEnd) - 'A';
            letterFrequency[incomingLetter]++;
            maxSingleLetterFrequency = Math.max(maxSingleLetterFrequency, letterFrequency[incomingLetter]);

            // 2) Determine how many characters in the CURRENT window differ from the
            //    most frequent letter we've recorded -- that's exactly how many
            //    replacements this window would require.
            int currentWindowSize = windowEnd - windowStart + 1;
            int replacementsNeeded = currentWindowSize - maxSingleLetterFrequency;

            // 3) If we'd need more than k replacements, the window is invalid.
            //    Slide (not shrink!) by advancing windowStart by exactly one.
            //    We deliberately do NOT decrement maxSingleLetterFrequency here,
            //    even though the letter leaving might have been the majority
            //    letter -- see Approach 4's docstring above for why this is safe:
            //    the reported answer only ever reflects window sizes we've
            //    already PROVEN achievable, so a stale (too-high) max frequency
            //    cannot cause us to overcount the final answer.
            if (replacementsNeeded > k) {
                int outgoingLetter = s.charAt(windowStart) - 'A';
                letterFrequency[outgoingLetter]--;
                windowStart++;
            }
            // Note: we intentionally do NOT track a running "bestLength" variable.
            // Because the window never shrinks below a size it previously proved
            // valid, the final window size (s.length() - windowStart) at the end
            // of the loop IS the answer.
        }

        return s.length() - windowStart;
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Tracing characterReplacement("AABABBA", 1) step by step.
     * Letters of interest: A = index 0, B = index 1 in letterFrequency[].
     *
     * Initial: windowStart=0, maxSingleLetterFrequency=0, letterFrequency=[A:0,B:0]
     *
     * windowEnd=0 ('A'):
     *   letterFrequency[A]=1 -> maxSingleLetterFrequency=1
     *   currentWindowSize = 0-0+1 = 1; replacementsNeeded = 1-1 = 0 <= k(1) -> no slide
     *   window = s[0..0] = "A"
     *
     * windowEnd=1 ('A'):
     *   letterFrequency[A]=2 -> maxSingleLetterFrequency=2
     *   currentWindowSize = 1-0+1 = 2; replacementsNeeded = 2-2 = 0 <= 1 -> no slide
     *   window = s[0..1] = "AA"
     *
     * windowEnd=2 ('B'):
     *   letterFrequency[B]=1 -> maxSingleLetterFrequency stays 2 (A is still higher)
     *   currentWindowSize = 2-0+1 = 3; replacementsNeeded = 3-2 = 1 <= 1 -> no slide
     *   window = s[0..2] = "AAB"  (conceptually: replace the 'B' with 'A')
     *
     * windowEnd=3 ('A'):
     *   letterFrequency[A]=3 -> maxSingleLetterFrequency=3
     *   currentWindowSize = 3-0+1 = 4; replacementsNeeded = 4-3 = 1 <= 1 -> no slide
     *   window = s[0..3] = "AABA" (best so far: length 4, achievable as "AAAA")
     *
     * windowEnd=4 ('B'):
     *   letterFrequency[B]=2 -> maxSingleLetterFrequency stays 3
     *   currentWindowSize = 4-0+1 = 5; replacementsNeeded = 5-3 = 2 > 1 -> SLIDE
     *     outgoingLetter = s[0]='A' -> letterFrequency[A]=2, windowStart becomes 1
     *   (maxSingleLetterFrequency intentionally stays stale at 3)
     *   window = s[1..4] = "ABAB", size still 4
     *
     * windowEnd=5 ('B'):
     *   letterFrequency[B]=3 -> maxSingleLetterFrequency=3 (ties, stays 3, now
     *     accurately reflecting B's count too)
     *   currentWindowSize = 5-1+1 = 5; replacementsNeeded = 5-3 = 2 > 1 -> SLIDE
     *     outgoingLetter = s[1]='A' -> letterFrequency[A]=1, windowStart becomes 2
     *   window = s[2..5] = "BABB", size still 4
     *
     * windowEnd=6 ('A'):
     *   letterFrequency[A]=2 -> maxSingleLetterFrequency stays 3
     *   currentWindowSize = 6-2+1 = 5; replacementsNeeded = 5-3 = 2 > 1 -> SLIDE
     *     outgoingLetter = s[2]='B' -> letterFrequency[B]=2, windowStart becomes 3
     *   window = s[3..6] = "ABBA", size still 4
     *
     * Loop ends. Final answer = s.length() - windowStart = 7 - 3 = 4.
     *
     * Result: 4  -- matches the expected output from Section 3, Example 1.
     * Notice the window size itself never dropped below 4 once it was reached,
     * even as maxSingleLetterFrequency became temporarily stale.
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - Approaches 1 and 2 are brute-force baselines (O(n^3) and O(n^2))
     *   useful only for building intuition and as test oracles.
     * - Approach 3 is a fully correct O(n) sliding window that recomputes the
     *   in-window max frequency whenever it shrinks -- easy to trust, slightly
     *   more code.
     * - Approach 4 is the optimal O(n) solution and the one to submit: it
     *   never shrinks the window and tolerates a stale max-frequency value,
     *   relying on the invariant that the reported window length is
     *   monotonically non-decreasing.
     * - Approach 5 (binary search + fixed window check) is a valid but
     *   asymptotically inferior O(n log n) alternative, worth mentioning for
     *   breadth but not as the primary submission.
     * - Known limitations/assumptions of the final solution: assumes
     *   uppercase-English-letter-only input (fixed alphabet size 26); if the
     *   alphabet were unbounded or Unicode, we'd swap the int[26] array for a
     *   HashMap<Character, Integer>, which would not change the asymptotic
     *   time complexity but would add a constant-factor hashing overhead.
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "What if the alphabet were the full Unicode character set instead of
     *    26 uppercase letters?" -- Swap int[26] for a HashMap<Character,
     *    Integer>; same O(n) time, slightly higher constant factor.
     * 2. "Can you also return the actual substring (not just its length),
     *    or one example of a valid replacement?" -- Track windowStart/
     *    windowEnd at the point bestLength is achieved, then reconstruct.
     * 3. "What if k could be updated dynamically between repeated queries on
     *    the same string?" -- Would likely need a different data structure
     *    (e.g., precomputed run-length groups) to avoid O(n) per query.
     * 4. "How would you extend this to allow replacing characters with a
    *    LIMITED set of allowed target characters (not any letter)?" --
     *    The core (windowLength - maxFrequency <= k) trick still works IF
     *    the target character is fixed per window; you'd need to iterate
     *    the window check per candidate target letter (26 passes), turning
     *    it into O(26n) -- still linear but with a larger constant.
     * 5. "What's the largest input size you'd expect this to handle within,
     *    say, 1 second?" -- With O(n) and simple array operations, this
     *    would comfortably handle n up to ~10^7-10^8 in Java within a
     *    second, depending on JIT warmup and I/O overhead.
     * 6. "Can this be parallelized for very large strings?" -- Not trivially,
     *    because the window state (frequency counts, windowStart) is
     *    inherently sequential; a parallel prefix-sum-based scheme could
     *    theoretically split the string into chunks and merge boundary
     *    windows, but this adds significant complexity for a single-pass
     *    O(n) problem that's already fast.
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Forgetting that the window in Approach 4 is allowed to become
     *    "impure" (i.e., maxSingleLetterFrequency can be stale) and panicking
     *    to "fix" it with an unnecessary O(26) recompute inside the loop,
     *    which silently degrades the approach to O(26n) -- not wrong, but
     *    loses the elegance and can raise the interviewer's eyebrow if you
     *    can't explain why you added it.
     * 2. Off-by-one errors in the final answer: forgetting that after the
     *    loop ends, the answer is `s.length() - windowStart` (equivalently
     *    the final window size), NOT `windowEnd - windowStart` computed
     *    inside the last iteration index that may be stale after the slide.
     * 3. Sliding instead of shrinking, but still using a `while` loop for
     *    the correction step -- since the window length never decreases in
     *    Approach 4, at most ONE slide step is ever needed per expansion; a
     *    `while` here (as opposed to Approach 3's necessary `while`) is
     *    harmless but signals the candidate hasn't fully internalized why a
     *    single `if` suffices.
     * 4. Assuming the answer must correspond to a single contiguous
     *    "replace-able" run visually inspectable in the ORIGINAL string --
     *    the window found by the algorithm is only guaranteed valid AFTER
     *    replacements are conceptually applied; candidates sometimes try to
     *    double check by eyeballing runs of already-identical characters
     *    only, missing valid windows that mix two different original
     *    characters (as in the "AABA" -> "AAAA" example).
     */

    /*
     * ========================================================================
     * TEST HARNESS (assertion-based, with brute-force oracle cross-checks
     * and randomized stress testing) -- run with: javac and java -ea
     * ========================================================================
     */
    public static void main(String[] args) {
        // --- Section 3 worked examples, checked against the optimal solution ---
        assert characterReplacement("AABABBA", 1) == 4 : "Example 1 failed";
        assert characterReplacement("ABAB", 0) == 1 : "Example 2 failed";
        assert characterReplacement("ABCD", 3) == 4 : "Example 3 failed";

        // --- Additional edge cases ---
        assert characterReplacement("", 5) == 0 : "Empty string failed";
        assert characterReplacement(null, 5) == 0 : "Null string failed";
        assert characterReplacement("Z", 0) == 1 : "Single character failed";
        assert characterReplacement("AAAA", 2) == 4 : "All identical failed";
        assert characterReplacement("AAAB", 5) == 4 : "k larger than needed failed";

        // --- Cross-check every approach against every other approach on the
        //     same fixed examples, to make sure they all agree. ---
        String[] fixedStrings = {"AABABBA", "ABAB", "ABCD", "AAAA", "AAAB", "Z", ""};
        int[] fixedKs = {1, 0, 3, 2, 5, 0, 5};
        for (int i = 0; i < fixedStrings.length; i++) {
            String testString = fixedStrings[i];
            int testK = fixedKs[i];
            int expected = characterReplacement(testString, testK);
            assert longestRepeatingBruteForceCubic(testString, testK) == expected
                    : "Cubic brute force mismatch on \"" + testString + "\", k=" + testK;
            assert longestRepeatingBruteForceQuadratic(testString, testK) == expected
                    : "Quadratic brute force mismatch on \"" + testString + "\", k=" + testK;
            assert longestRepeatingSlidingWindowShrinking(testString, testK) == expected
                    : "Shrinking window mismatch on \"" + testString + "\", k=" + testK;
            assert longestRepeatingSlidingWindowOptimal(testString, testK) == expected
                    : "Optimal window mismatch on \"" + testString + "\", k=" + testK;
            assert longestRepeatingBinarySearch(testString, testK) == expected
                    : "Binary search mismatch on \"" + testString + "\", k=" + testK;
        }

        // --- Randomized stress test: optimal solution vs. cubic brute-force
        //     oracle, on small random strings so the O(n^3) oracle stays fast. ---
        Random random = new Random(42); // fixed seed for reproducibility
        int numberOfTrials = 2000;
        int maxLength = 12; // kept small so the O(n^3) oracle runs quickly
        int alphabetSizeForStress = 4; // small alphabet to force lots of repeats/mismatches

        for (int trial = 0; trial < numberOfTrials; trial++) {
            int length = random.nextInt(maxLength + 1); // 0..maxLength inclusive
            StringBuilder builder = new StringBuilder();
            for (int position = 0; position < length; position++) {
                char randomLetter = (char) ('A' + random.nextInt(alphabetSizeForStress));
                builder.append(randomLetter);
            }
            String randomString = builder.toString();
            int randomK = random.nextInt(length + 1); // 0..length inclusive

            int oracleAnswer = longestRepeatingBruteForceCubic(randomString, randomK);
            int optimalAnswer = longestRepeatingSlidingWindowOptimal(randomString, randomK);
            int shrinkingAnswer = longestRepeatingSlidingWindowShrinking(randomString, randomK);
            int binarySearchAnswer = longestRepeatingBinarySearch(randomString, randomK);
            int productionAnswer = characterReplacement(randomString, randomK);

            if (oracleAnswer != optimalAnswer || oracleAnswer != shrinkingAnswer
                    || oracleAnswer != binarySearchAnswer || oracleAnswer != productionAnswer) {
                throw new AssertionError(String.format(
                        "Mismatch on trial %d: s=\"%s\", k=%d -> oracle=%d, optimal=%d, shrinking=%d, binarySearch=%d, production=%d",
                        trial, randomString, randomK, oracleAnswer, optimalAnswer, shrinkingAnswer,
                        binarySearchAnswer, productionAnswer));
            }
        }

        System.out.println("All assertions passed, including " + numberOfTrials
                + " randomized stress trials cross-checking all five approaches.");
    }
}
