import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * ================================================================================================
 * GOOGLE ONSITE MOCK INTERVIEW TRANSCRIPT
 * LeetCode 1358 — Number of Substrings Containing All Three Characters
 * Difficulty: Medium (frequently appears in Google phone screens / early onsite rounds)
 * ================================================================================================
 */
class NumberOfSubstringsAllThree {

    /*
     * ============================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ============================================================================================
     * We are given a string `s` consisting only of the characters 'a', 'b', and 'c'.
     * We must return the total COUNT of substrings of `s` that contain AT LEAST ONE occurrence
     * of each of the three characters ('a', 'b', and 'c' must all be present somewhere in the
     * substring; extra repeats of any character are fine).
     *
     * Key points to confirm out loud with the interviewer:
     *   - "Substring" means a CONTIGUOUS slice of `s` (not a subsequence). Two substrings are
     *     considered different if they start/end at different indices, even if their contents
     *     are identical characters — i.e., we are counting index-pairs (i, j), not distinct
     *     string values.
     *   - Input: a single String s.
     *   - Output: a single integer (or long, given n can be up to 5×10^4 → up to ~1.25 billion
     *     substrings in the worst case, which still fits in a 32-bit int since n*(n+1)/2 for
     *     n=50,000 is ~1.25 * 10^9, just under Integer.MAX_VALUE ~2.147*10^9 — but I will use
     *     `long` internally for safety and to avoid any off-by-one overflow risk).
     *   - Constraints: 3 <= s.length() <= 5 * 10^4, alphabet is strictly {'a','b','c'}.
     *   - Assumption: s is non-null and already validated to contain only 'a'/'b'/'c' — I will
     *     not add defensive validation for out-of-alphabet characters unless asked.
     */


    /*
     * ============================================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to interviewer, with assumed answers)
     * ============================================================================================
     * Q1: Are we counting substrings by (start, end) index pairs, or by distinct string content?
     *     A1 (assumed): By index pairs. "abcabc" has many valid substrings even though some of
     *         them are textually identical (e.g., "abc" appearing twice counts as 2).
     *
     * Q2: Can the string contain characters other than 'a', 'b', 'c'?
     *     A2 (assumed): No — constraints guarantee only 'a', 'b', 'c'. No input sanitization
     *         needed.
     *
     * Q3: What's the maximum input size, and does that rule out O(n^2) or O(n log n) solutions?
     *     A3 (assumed): n <= 5*10^4. O(n^2) is ~2.5*10^9 operations worst case — too slow for a
     *         production/interview-optimal answer (roughly multiple seconds in Java), so the
     *         interviewer expects an O(n) or O(n log n) solution as the "final" answer, though
     *         starting from brute force is fine to build up the intuition.
     *
     * Q4: Is the returned count expected to fit in a 32-bit int, or should I return a long?
     *     A4 (assumed): It fits in int (max ~1.25*10^9 < Integer.MAX_VALUE), but I'll compute
     *         with long internally to be safe and only narrow at the very end, since interview
     *         correctness is prioritized over micro-optimizing the return type.
     *
     * Q5: Is the string ever empty, and what's the minimum guaranteed length?
     *     A5 (assumed): Constraints guarantee length >= 3, so I don't need to special-case
     *         empty or length-1/2 strings, though my solution will handle them correctly anyway
     *         since a window shorter than 3 simply can never satisfy "all three present."
     *
     * Q6: Do we need to support Unicode or multi-byte characters?
     *     A6 (assumed): No — only 'a', 'b', 'c', so `char` indexing via `s.charAt(i) - 'a'` is
     *         always safe and lands in {0, 1, 2}.
     *
     * Q7: Is this a single call, or will this function be invoked repeatedly on overlapping
     *     substrings / streaming input (concurrency concerns)?
     *     A7 (assumed): Single call, single-threaded, one-shot batch computation. No streaming
     *         or thread-safety requirements.
     *
     * Q8: Should I optimize for time complexity, space complexity, or code readability first?
     *     A8 (assumed): Time complexity first (this is a counting problem over up to 50,000
     *         characters), but O(1) extra space is achievable here too, so I'll aim for both.
     */


    /*
     * ============================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ============================================================================================
     *
     * Example 1 (Normal case): s = "abcabc"
     *   Valid substrings containing all of a, b, c at least once:
     *     "abc" (0..2), "abca" (0..3), "abcab" (0..4), "abcabc" (0..5),
     *     "bca" (1..3), "bcab" (1..4), "bcabc" (1..5),
     *     "cab" (2..4), "cabc" (2..5),
     *     "abc" (3..5)
     *   Total = 10. This matches the known LeetCode example output.
     *
     * Example 2 (Edge case — minimum length, exactly one of each): s = "abc"
     *   Only one substring exists that is length >= 3: the whole string "abc" itself.
     *   Shorter substrings (length 1 or 2) can never contain all three distinct characters.
     *   Expected output: 1.
     *
     * Example 3 (Boundary / tie-breaking-like case — heavy repetition, single missing char
     * for a long stretch): s = "aaabbbccc"
     *   Intuition check: the earliest index at which all three characters have appeared is
     *   index 6 (the first 'c', at position 6, since 'a's occupy 0-2 and 'b's occupy 3-5).
     *   Once all three have appeared, every substring starting at index 0..? up to that point
     *   and ending at or after index 6 is valid. This example stresses "how do repeated runs
     *   of a single character affect the count," which is the crux of why the O(n) "last seen
     *   index" trick works: repeats of a character that's already been seen don't invalidate
     *   anything — they just extend how far right the window can go while remaining valid.
     *   (Exact count is verified programmatically in the stress test below rather than by
     *   hand, to avoid arithmetic slips — see Section 13 "brute force as correctness oracle.")
     */


    /*
     * ============================================================================================
     * SECTION 4/5/6: ALL POSSIBLE APPROACHES
     * ============================================================================================
     * Paradigms considered and explicitly RULED OUT (one-line justification each):
     *   - Sorting-based: Irrelevant — order of characters is semantically meaningful (we need
     *     contiguous substrings), so sorting the string would destroy the very structure we're
     *     counting over.
     *   - Divide and conquer: No natural split point exists that avoids re-counting substrings
     *     straddling the midpoint without extra bookkeeping that just reduces to the sliding
     *     window idea anyway — not a natural fit here.
     *   - Dynamic programming: Possible in principle (e.g., dp[i] = number of valid substrings
     *     ending at i), but it collapses into exactly the same recurrence as the "last occurrence
     *     index" technique below — I'll present it as a DP-flavored restatement of Approach 4
     *     rather than a separate paradigm, since it doesn't add anything new.
     *   - Tree / graph traversal: Not applicable — there's no tree/graph structure in a flat
     *     string counting problem like this.
     *   - Heap / priority queue: Not applicable — nothing about this problem requires ordering
     *     by priority; we only care about presence/absence of 3 fixed characters.
     *   - Binary search: Not applicable — there's no sorted monotonic search space to binary
     *     search over (the "smallest valid window ending at i" is found via two pointers in
     *     amortized O(1) per step, not O(log n) binary search).
     *   - Monotonic stack/deque: Not applicable — there's no "next greater/smaller element"
     *     structure here; the fixed 3-character alphabet makes deque-based windowing overkill.
     *   - Trie / segment tree: Not applicable — no prefix/substring-matching-against-a-dictionary
     *     structure, and no range-query-with-updates requirement that would justify a segment
     *     tree.
     *
     * Paradigms that ARE genuinely applicable, presented from naive to optimal:
     */

    /*
     * --------------------------------------------------------------------------------------
     * Approach 1: Brute Force — Enumerate All Substrings (Naive)
     * --------------------------------------------------------------------------------------
     * Core idea: For every possible (start, end) pair, materialize/scan the substring and
     * check whether it contains all three characters, by scanning it character by character.
     * Paradigm: Brute force enumeration.
     * Time Complexity: O(n^3) — O(n^2) substrings, each taking up to O(n) to scan for the
     *   presence of 'a', 'b', 'c'.
     * Space Complexity: O(1) extra (excluding the O(n) substring materialization, which we
     *   avoid entirely by scanning indices directly instead of calling s.substring()).
     * Pros: Trivial to reason about correctness; a great "warm-up" to state out loud first to
     *   show you understand the problem before optimizing.
     * Cons: Far too slow for n = 5*10^4 (would be ~10^14 operations) — not viable even as a
     *   fallback; only useful as a correctness oracle for stress testing on tiny inputs.
     * When to use: Never in production; only as a brute-force oracle for randomized testing
     *   against faster approaches during development.
     */
    static long countBruteForceCubic(String s) {
        int n = s.length();
        long count = 0;
        for (int start = 0; start < n; start++) {
            for (int end = start; end < n; end++) {
                boolean hasA = false, hasB = false, hasC = false;
                for (int k = start; k <= end; k++) {
                    char ch = s.charAt(k);
                    if (ch == 'a') hasA = true;
                    else if (ch == 'b') hasB = true;
                    else hasC = true;
                }
                if (hasA && hasB && hasC) count++;
            }
        }
        return count;
    }

    /*
     * --------------------------------------------------------------------------------------
     * Approach 2: Brute Force with Incremental Character Counting (Quadratic)
     * --------------------------------------------------------------------------------------
     * Core idea: Same double loop over (start, end), but instead of re-scanning the whole
     * substring for every `end`, maintain a running frequency count as `end` extends to the
     * right. This removes the innermost O(n) scan, dropping total work to O(n^2).
     * Paradigm: Brute force enumeration + incremental hashing/frequency counting (fixed-size
     *   alphabet array acts as a tiny "hash map").
     * Time Complexity: O(n^2) — for each of the n starting points, we do a single O(n) sweep
     *   to the right while incrementally updating counts.
     * Space Complexity: O(1) extra (a fixed-size int[3] frequency array per outer iteration).
     * Pros: Meaningfully faster than Approach 1, easy to explain, still very simple to code
     *   correctly under interview pressure — a reasonable "middle" answer if asked to improve
     *   on brute force before jumping straight to the optimal linear solution.
     * Cons: Still O(n^2) ≈ 2.5 * 10^9 operations for n = 5*10^4 — too slow to be the final
     *   answer, though it would pass on smaller inputs.
     * When to use: Acceptable as an intermediate checkpoint in an interview, or in production
     *   only if n is guaranteed small (say, n <= few thousand).
     */
    static long countBruteForceQuadratic(String s) {
        int n = s.length();
        long count = 0;
        for (int start = 0; start < n; start++) {
            int[] freq = new int[3];
            int distinctSeen = 0;
            for (int end = start; end < n; end++) {
                int idx = s.charAt(end) - 'a';
                if (freq[idx] == 0) distinctSeen++;
                freq[idx]++;
                if (distinctSeen == 3) count++;
            }
        }
        return count;
    }

    /*
     * --------------------------------------------------------------------------------------
     * Approach 3: Sliding Window with Shrinking Left Pointer (Two-Pointer)
     * --------------------------------------------------------------------------------------
     * Core idea: Use two pointers `left` and `right`. Expand `right` one step at a time,
     * updating a frequency array. Whenever the window [left, right] contains at least one of
     * each character, greedily shrink from the left until the window is the SMALLEST window
     * ending at `right` that still contains all three (i.e., shrinking one more step would
     * break the "all three present" invariant). At that point, every start index in
     * [0, left - 1] — combined with this same `right` — produces a valid substring (since
     * moving the start further left than `left - 1` only adds more characters, which can never
     * remove a character that's already present). So we add `left` to the running total.
     * Paradigm: Sliding window / two-pointer, with a fixed-size frequency array standing in
     *   for a hash map (since our alphabet is fixed at 3 symbols).
     * Time Complexity: O(n) amortized — `left` only ever moves forward and is bounded by n
     *   across the entire run, so the total work across all iterations of the inner while
     *   loop is O(n), giving O(n) overall alongside the O(n) outer loop.
     * Space Complexity: O(1) — a fixed 3-element frequency array regardless of input size.
     * Pros: Linear time, intuitive "two pointer" mental model that generalizes to many
     *   sliding-window counting problems (e.g., "at most K distinct characters" family);
     *   doesn't require any clever insight about "last occurrence," just the standard
     *   shrink-while-valid pattern.
     * Cons: Slightly more bookkeeping than Approach 4 (needs a frequency array with
     *   increment/decrement instead of a simple "last seen index" overwrite); marginally more
     *   opportunities for off-by-one bugs in the shrink condition.
     * When to use: A great default choice in an interview since the two-pointer pattern is
     *   universally recognized and easy to explain on a whiteboard; production-ready as-is.
     */
    static long countSlidingWindowShrinking(String s) {
        int n = s.length();
        int[] freq = new int[3];
        int left = 0;
        long count = 0;
        for (int right = 0; right < n; right++) {
            freq[s.charAt(right) - 'a']++;
            // Shrink while the window still contains all three characters; stop the moment
            // removing s[left] would drop one of the counts to zero.
            while (freq[0] > 0 && freq[1] > 0 && freq[2] > 0) {
                freq[s.charAt(left) - 'a']--;
                left++;
            }
            // At this point, `left` is exactly one past the last position we could shrink to
            // while still being valid. Every start in [0, left - 1] paired with this `right`
            // forms a valid substring, so there are exactly `left` valid starts.
            count += left;
        }
        return count;
    }

    /*
     * --------------------------------------------------------------------------------------
     * Approach 4: Last Occurrence Index Technique (Optimal — Recommended)
     * --------------------------------------------------------------------------------------
     * Core idea: For each `right` endpoint, track the most recent (rightmost-so-far) index at
     * which 'a', 'b', and 'c' each last appeared. If all three have appeared at least once by
     * position `right`, then EVERY start index from 0 up through
     * min(lastA, lastB, lastC) is a valid left boundary for a substring ending at `right` that
     * contains all three characters — because the substring [start, right] is guaranteed to
     * still include the most recent occurrence of every character as long as
     * start <= min(lastA, lastB, lastC). That gives exactly (min(lastA, lastB, lastC) + 1)
     * valid substrings ending at this `right`. Summing this over all `right` gives the answer.
     * Paradigm: Greedy / prefix-tracking (can also be framed as DP: dp[right] = number of
     *   valid substrings ending at `right`, computed via O(1) transitions using the three
     *   "last seen" pointers instead of a shrinking window).
     * Time Complexity: O(n) — a single left-to-right pass, O(1) work per character.
     * Space Complexity: O(1) — three integers to track last-seen positions.
     * Pros: The cleanest and most direct O(n)/O(1) solution; no inner while loop at all, so
     *   there's no need to reason about amortized analysis — the loop body is a strict O(1)
     *   per iteration, which is easier to defend under interviewer scrutiny; fewer moving
     *   parts than the two-pointer shrinking approach, so fewer places to introduce bugs.
     * Cons: The correctness argument ("min of last-seen indices + 1") is slightly less
     *   immediately intuitive than the shrinking window story, so it benefits from a clear
     *   verbal explanation and a worked example (see Section 10 dry run) before coding it.
     * When to use: This is the version I would ultimately present and defend as final in a
     *   Google interview — it's optimal in both time and space, and once explained, it is the
     *   fewest lines of code with the fewest edge cases.
     */
    static long countLastOccurrenceIndex(String s) {
        int n = s.length();
        // lastSeen[0] = last index of 'a', lastSeen[1] = last index of 'b',
        // lastSeen[2] = last index of 'c'. -1 means "not seen yet."
        int[] lastSeen = {-1, -1, -1};
        long totalCount = 0;

        for (int right = 0; right < n; right++) {
            lastSeen[s.charAt(right) - 'a'] = right;

            // Only once all three characters have appeared at least once can this window
            // (or any window ending at `right`) possibly be valid.
            if (lastSeen[0] != -1 && lastSeen[1] != -1 && lastSeen[2] != -1) {
                int earliestOfTheThreeMostRecent =
                        Math.min(lastSeen[0], Math.min(lastSeen[1], lastSeen[2]));
                // Every start index from 0 to earliestOfTheThreeMostRecent (inclusive) yields
                // a valid substring [start, right], because that range of starts is guaranteed
                // not to exclude any of the three most-recent occurrences.
                totalCount += earliestOfTheThreeMostRecent + 1;
            }
        }
        return totalCount;
    }


    /*
     * ============================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ============================================================================================
     *
     * | Approach                              | Time     | Space | Best For                         | Limitations                                   |
     * |----------------------------------------|----------|-------|-----------------------------------|------------------------------------------------|
     * | 1. Brute Force (Cubic)                 | O(n^3)   | O(1)  | Warm-up / correctness oracle       | Unusably slow beyond tiny n (~few hundred)     |
     * | 2. Brute Force + Running Counts (Quad.) | O(n^2)   | O(1)  | Intermediate checkpoint, small n   | Too slow for n = 5*10^4 (~2.5*10^9 ops)        |
     * | 3. Sliding Window (Shrinking, 2-ptr)    | O(n)     | O(1)  | General interview answer, intuitive| Slightly more bookkeeping than Approach 4      |
     * | 4. Last Occurrence Index (Optimal)     | O(n)     | O(1)  | Final production/interview answer  | Correctness argument needs clear explanation   |
     */


    /*
     * ============================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ============================================================================================
     * I would present Approach 4 (Last Occurrence Index Technique) as my final answer, but only
     * after briefly stating Approach 1 or 2 out loud to demonstrate that I understand the problem
     * and can identify the naive baseline before optimizing — this signals process to the
     * interviewer, not just a memorized final answer.
     *
     * Why Approach 4 specifically, over Approach 3 (also O(n)/O(1)):
     *   - Coding speed: it has no inner while loop, so there's exactly one loop and one branch
     *     to get right — faster to type correctly under time pressure, and easier to unit-test.
     *   - Clarity: once I've explained "min of last-seen indices + 1," the code is almost a
     *     direct transliteration of the explanation, which makes it easy for the interviewer to
     *     follow along as I type.
     *   - Interviewer expectations: for a "count substrings satisfying property X" problem with
     *     n up to 5*10^4, interviewers expect an O(n) or O(n log n) solution — this hits O(n)
     *     with the simplest possible loop structure.
     *   - I would still mention Approach 3 exists as an equally valid O(n) alternative, since
     *     showing awareness of multiple correct linear-time strategies (and being able to
     *     articulate the trade-off) is itself a positive signal.
     */


    /*
     * ============================================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ============================================================================================
     */
    static long numberOfSubstrings(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Input string must not be null.");
        }
        int n = s.length();

        // lastSeenIndex[c] tracks the most recent index at which character ('a' + c) appeared.
        // -1 signals "this character has not appeared yet in the prefix scanned so far."
        int[] lastSeenIndex = {-1, -1, -1};

        long validSubstringCount = 0;

        for (int right = 0; right < n; right++) {
            char currentChar = s.charAt(right);
            int charIndex = currentChar - 'a'; // maps 'a'->0, 'b'->1, 'c'->2

            // Defensive check: problem guarantees alphabet is {'a','b','c'}, but we guard
            // against malformed input rather than silently corrupting results.
            if (charIndex < 0 || charIndex > 2) {
                throw new IllegalArgumentException(
                        "Input string must contain only 'a', 'b', or 'c'. Found: " + currentChar);
            }

            lastSeenIndex[charIndex] = right;

            // A substring ending at `right` can only be valid once every character has been
            // seen at least once somewhere in s[0..right].
            boolean allThreeSeen =
                    lastSeenIndex[0] != -1 && lastSeenIndex[1] != -1 && lastSeenIndex[2] != -1;

            if (allThreeSeen) {
                // The tightest (rightmost) constraint on how far left our substring's start can
                // go is set by whichever of the three characters was seen LEAST recently.
                int limitingEarliestLastSeen =
                        Math.min(lastSeenIndex[0], Math.min(lastSeenIndex[1], lastSeenIndex[2]));

                // Every start index in [0, limitingEarliestLastSeen] produces a valid substring
                // [start, right], since none of those starts can exclude the most recent
                // occurrence of any of the three characters. That's (limitingEarliestLastSeen+1)
                // valid substrings contributed by this particular `right`.
                validSubstringCount += limitingEarliestLastSeen + 1;
            }
        }

        return validSubstringCount;
    }


    /*
     * ============================================================================================
     * SECTION 10: DRY RUN / TRACE — s = "abcabc"
     * ============================================================================================
     * Initial state: lastSeenIndex = [-1, -1, -1] ('a','b','c' none seen yet), validSubstringCount = 0
     *
     * right=0, char='a' (index 0):
     *   lastSeenIndex -> [0, -1, -1]
     *   allThreeSeen? No ('b' and 'c' still -1). No contribution.
     *
     * right=1, char='b' (index 1):
     *   lastSeenIndex -> [0, 1, -1]
     *   allThreeSeen? No ('c' still -1). No contribution.
     *
     * right=2, char='c' (index 2):
     *   lastSeenIndex -> [0, 1, 2]
     *   allThreeSeen? Yes. limitingEarliestLastSeen = min(0, 1, 2) = 0.
     *   Contribution: 0 + 1 = 1.  validSubstringCount = 0 + 1 = 1.
     *   (This corresponds to substring "abc", start=0..0 only.)
     *
     * right=3, char='a' (index 0):
     *   lastSeenIndex -> [3, 1, 2]
     *   allThreeSeen? Yes. limitingEarliestLastSeen = min(3, 1, 2) = 1.
     *   Contribution: 1 + 1 = 2.  validSubstringCount = 1 + 2 = 3.
     *   (Corresponds to substrings [0,3]="abca" and [1,3]="bca" — 2 new valid substrings.)
     *
     * right=4, char='b' (index 1):
     *   lastSeenIndex -> [3, 4, 2]
     *   allThreeSeen? Yes. limitingEarliestLastSeen = min(3, 4, 2) = 2.
     *   Contribution: 2 + 1 = 3.  validSubstringCount = 3 + 3 = 6.
     *   (Corresponds to starts 0,1,2 ending at 4: "abcab","bcab","cab" — 3 new valid substrings.)
     *
     * right=5, char='c' (index 2):
     *   lastSeenIndex -> [3, 4, 5]
     *   allThreeSeen? Yes. limitingEarliestLastSeen = min(3, 4, 5) = 3.
     *   Contribution: 3 + 1 = 4.  validSubstringCount = 6 + 4 = 10.
     *   (Corresponds to starts 0,1,2,3 ending at 5: "abcabc","bcabc","cabc","abc" — 4 new.)
     *
     * Final answer: validSubstringCount = 10, matching the hand-verified count from Section 3,
     * Example 1.
     */


    /*
     * ============================================================================================
     * SECTION 11: CLOSING SUMMARY
     * ============================================================================================
     * - Brute force cubic and quadratic approaches are correct but too slow for n = 5*10^4;
     *   they exist to build intuition and to serve as a correctness oracle during testing.
     * - Both O(n) approaches (shrinking sliding window, and last-occurrence-index) achieve the
     *   optimal asymptotic complexity; the last-occurrence-index technique is preferred for its
     *   simplicity (no inner loop) and directness of implementation.
     * - Known assumptions of the final solution: input string is non-null and, per the problem's
     *   constraints, consists only of 'a', 'b', 'c' with length in [3, 5*10^4]. A defensive
     *   IllegalArgumentException guards against malformed characters, though the constraints
     *   guarantee this won't occur in the grading environment.
     * - Known limitation: this problem-family technique (last-seen-index / exactly-k-distinct
     *   via prefix tracking) generalizes cleanly to a FIXED, SMALL alphabet. It would need to be
     *   adapted (e.g., using a HashMap<Character,Integer> instead of a fixed int[3] array) for
     *   an arbitrary or large alphabet, at the cost of a small constant-factor slowdown from
     *   hashing overhead, though the asymptotic complexity would remain O(n).
     */


    /*
     * ============================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ============================================================================================
     * 1. "What if the alphabet were arbitrary (any lowercase letter), and we needed 'contains
     *    all K distinct characters that appear anywhere in s'?" — Discuss generalizing the
     *    fixed-size array to a HashMap<Character, Integer> for last-seen tracking, and how the
     *    K-distinct-alphabet generalization connects to the "at most K distinct" sliding window
     *    family (LC 340 / LC 992).
     * 2. "Can you solve it with O(1) auxiliary space but without using extra arrays at all —
     *    e.g., using only a few primitive variables?" — Yes; since the alphabet is fixed at
     *    exactly 3 characters, replace the int[3] array with three named int variables
     *    (lastA, lastB, lastC) — purely a style/readability trade-off, no complexity change.
     * 3. "How would you handle streaming input, where characters arrive one at a time and you
     *    need the running total after each new character?" — The last-occurrence-index approach
     *    is naturally online/streaming-friendly: maintain lastSeen[] and a running total as
     *    persistent state across calls, updating in O(1) per new character.
     *  4. "What if we wanted the actual substrings themselves, not just the count?" — Discuss
     *    that this requires switching from an O(1)-space counting formula to actually extracting
     *    substrings using s.substring(start, right+1) for each valid start, which reintroduces
     *    O(n) or more per substring materialized, making total output size the true bottleneck.
     * 5. "What if the requirement changed to 'at least one of at least 2 out of 3 characters'
     *    (an OR instead of an AND)?" — This changes the problem to a complement-counting
     *    argument or a different sliding-window invariant; discuss how the invariant in the
     *    while-loop condition (or the "all three seen" check) would need to change.
     * 6. "Can this be parallelized for very large strings (e.g., n in the billions) split across
     *    multiple machines/threads?" — Discuss a MapReduce-style split with overlap: process
     *    disjoint chunks independently for their "fully contained" substrings, then handle
     *    boundary-crossing substrings (those spanning a chunk boundary) as a small O(boundary
     *    count) correction pass.
     */


    /*
     * ============================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ============================================================================================
     * 1. Off-by-one in the "+1": forgetting that when limitingEarliestLastSeen = 0, there is
     *    still exactly 1 valid start (start = 0), not 0. Candidates sometimes write
     *    `count += limitingEarliestLastSeen` instead of `+ 1`, undercounting every single time.
     * 2. Confusing "first occurrence" with "last occurrence": the algorithm depends on tracking
     *    the MOST RECENT index of each character, not the first. Using first-seen index instead
     *    would silently produce wrong (too-small) answers on inputs with repeated characters,
     *    such as "aaabbbccc".
     * 3. In the shrinking sliding-window version (Approach 3), forgetting that after the while
     *    loop exits, `left` itself (not `left - 1`) is the count of valid starts for the current
     *    `right` — this is a classic off-by-one that's easy to get backwards under pressure.
     * 4. Assuming int is unsafe and reflexively switching everything to long/BigInteger without
     *    checking the actual bound — worth explicitly stating the max output value
     *    (n*(n+1)/2 for n=50,000 is comfortably within int range) to show quantitative rigor,
     *    even though using `long` defensively is also a perfectly acceptable engineering choice.
     */


    /*
     * ============================================================================================
     * MAIN METHOD — NAMED ASSERTIONS + RANDOMIZED STRESS TEST AGAINST BRUTE-FORCE ORACLE
     * ============================================================================================
     * Run with: java -ea NumberOfSubstringsAllThree.java
     */
    public static void main(String[] args) {
        // --- Named assertions on the worked examples from Section 3 ---
        assert numberOfSubstrings("abcabc") == 10
                : "Example 1 failed: expected 10 for \"abcabc\"";
        assert numberOfSubstrings("abc") == 1
                : "Example 2 failed: expected 1 for \"abc\"";
        assert countBruteForceCubic("aaabbbccc") == numberOfSubstrings("aaabbbccc")
                : "Example 3 failed: brute force and optimal disagree on \"aaabbbccc\"";

        // --- Cross-check all four implementations agree on the worked examples ---
        String[] handPickedExamples = {"abcabc", "abc", "aaabbbccc", "aabbcc", "cba", "aaa"};
        for (String example : handPickedExamples) {
            long bruteCubic = countBruteForceCubic(example);
            long bruteQuadratic = countBruteForceQuadratic(example);
            long slidingWindow = countSlidingWindowShrinking(example);
            long lastOccurrence = countLastOccurrenceIndex(example);
            long production = numberOfSubstrings(example);

            assert bruteCubic == bruteQuadratic
                    : "Mismatch (cubic vs quadratic) on: " + example;
            assert bruteCubic == slidingWindow
                    : "Mismatch (cubic vs sliding window) on: " + example;
            assert bruteCubic == lastOccurrence
                    : "Mismatch (cubic vs last-occurrence) on: " + example;
            assert bruteCubic == production
                    : "Mismatch (cubic vs production) on: " + example;

            System.out.println("s=\"" + example + "\" -> count=" + bruteCubic
                    + " (all 5 implementations agree)");
        }

        // --- Randomized stress test: brute force oracle vs. all faster approaches ---
        Random random = new Random(42);
        char[] alphabet = {'a', 'b', 'c'};
        int trials = 2000;
        for (int trial = 0; trial < trials; trial++) {
            int length = 3 + random.nextInt(18); // keep small so cubic brute force stays fast
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < length; i++) {
                builder.append(alphabet[random.nextInt(3)]);
            }
            String randomString = builder.toString();

            long expected = countBruteForceCubic(randomString);
            long actualQuadratic = countBruteForceQuadratic(randomString);
            long actualSlidingWindow = countSlidingWindowShrinking(randomString);
            long actualLastOccurrence = countLastOccurrenceIndex(randomString);
            long actualProduction = numberOfSubstrings(randomString);

            assert expected == actualQuadratic
                    : "Stress test failure (quadratic) on: " + randomString;
            assert expected == actualSlidingWindow
                    : "Stress test failure (sliding window) on: " + randomString;
            assert expected == actualLastOccurrence
                    : "Stress test failure (last occurrence) on: " + randomString;
            assert expected == actualProduction
                    : "Stress test failure (production) on: " + randomString;
        }
        System.out.println("Randomized stress test passed: " + trials
                + " trials, all five implementations agree.");

        // --- Performance sanity check at the upper constraint bound (n = 50,000) ---
        StringBuilder largeInput = new StringBuilder();
        Random largeRandom = new Random(7);
        int largeN = 50_000;
        for (int i = 0; i < largeN; i++) {
            largeInput.append(alphabet[largeRandom.nextInt(3)]);
        }
        long startTimeNanos = System.nanoTime();
        long largeResult = numberOfSubstrings(largeInput.toString());
        long elapsedMillis = (System.nanoTime() - startTimeNanos) / 1_000_000;
        System.out.println("Performance check: n=" + largeN + ", result=" + largeResult
                + ", elapsed=" + elapsedMillis + "ms (well within int range, "
                + "max possible = " + ((long) largeN * (largeN + 1) / 2) + ")");

        System.out.println("ALL TESTS PASSED.");
    }


    public static int numberOfSubstrings2(String s) {

        // Replace this placeholder return statement with your code
        int left = 0;
        int count = 0;

        Map<Character, Integer> freq = new HashMap<>();

        int n = s.length();
        for (int right = 0; right < n; right++) {
            char r = s.charAt(right);
            freq.put(r, freq.getOrDefault(r, 0) + 1);

            while (freq.size() == 3) {
                count += (n - right);
                char l = s.charAt(left);
                freq.put(l, freq.get(l) - 1);
                if (freq.get(l) == 0) {
                    freq.remove(l);
                }
                left++;
            }
        }
        return count;
    }

}
