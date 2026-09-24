/* ============================================================================
 * FILE: MinAppendToMakeSubsequence.java
 * CONTEXT: Mock Google-style Data Structures & Algorithms Interview
 * PROBLEM: Minimum number of characters to append to the END of `source`
 *          so that `target` becomes a subsequence of the resulting string.
 * ============================================================================
 */

import java.util.*;

public class MinAppendToMakeSubsequence {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ------------------------------------------------------------------------
     * In my own words:
     *   I am given two lowercase strings, `source` and `target`. I am only
     *   allowed to APPEND characters to the END of `source` (I cannot insert
     *   in the middle, delete, or reorder anything in `source`). I need to
     *   find the minimum number of characters I must append so that `target`
     *   appears as a SUBSEQUENCE of the new, longer string.
     *
     * Key definitions:
     *   - Subsequence: characters of `target` must appear in `source`
     *     (post-append) in the same relative order, but not necessarily
     *     contiguously.
     *
     * Inputs:
     *   - source: String, lowercase English letters, possibly empty.
     *   - target: String, lowercase English letters, possibly empty.
     *
     * Output:
     *   - An integer: the minimum number of characters to append.
     *
     * Core Insight:
     *   Since we can only ever append to the END of `source`, and we cannot
     *   modify existing characters, the best we can do is greedily match as
     *   much of `target` as possible against the EXISTING characters of
     *   `source` (in order). Whatever suffix of `target` we fail to match
     *   must be appended verbatim at the end. So the answer reduces to:
     *
     *       answer = length(target) - (length of the longest prefix of
     *                 target that can be matched, in order, as a
     *                 subsequence within source)
     *
     * Assumptions (to be confirmed with interviewer in Section 2):
     *   - Only lowercase English letters (a-z).
     *   - We only care about the COUNT of characters to append, not what
     *     those characters actually are (though it's trivially the
     *     remaining unmatched suffix of target).
     * ======================================================================== */


    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ------------------------------------------------------------------------
     * 1. Q: What is the character set — lowercase only, or full ASCII/Unicode?
     *    A: Assume lowercase English letters 'a'-'z' only.
     *
     * 2. Q: What are the expected sizes of `source` and `target`?
     *    A: Assume up to 10^5 or 10^6 characters each — large enough that we
     *       need at least a linear-time solution.
     *
     * 3. Q: Can either string be empty?
     *    A: Yes. If `target` is empty, answer is 0. If `source` is empty and
     *       `target` is non-empty, we must append all of `target`.
     *
     * 4. Q: Can `source` or `target` be null?
     *    A: Assume not null per problem constraints, but I will defensively
     *       guard against null in production code.
     *
     * 5. Q: Are duplicate characters expected/common in either string?
     *    A: Yes, duplicates are expected and must be handled naturally by
     *       the matching logic (no special-casing needed).
     *
     * 6. Q: Do we need to return the actual appended substring, or just the
     *       count?
     *    A: Just the minimum COUNT (integer), per the problem statement.
     *
     * 7. Q: Is this a one-shot computation, or will this be called
     *       repeatedly on a stream of incoming `target` queries against a
     *       fixed `source` (concurrency / amortization concerns)?
     *    A: Assume single one-shot query for this interview; I'll mention
     *       how to extend for repeated queries in follow-ups.
     *
     * 8. Q: Is case sensitivity a factor (e.g., 'A' vs 'a')?
     *    A: Assume case-sensitive exact character matching; input is
     *       guaranteed lowercase so this is moot but worth confirming.
     * ======================================================================== */


    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ------------------------------------------------------------------------
     * Example 1 (Normal case):
     *   source = "coding", target = "codeforce"
     *   Matching walk: c-o-d match source[0..2], then target[3]='e' cannot
     *   be found further in source ("i","n","g" remain, none is 'e').
     *   Matched prefix length = 3 ("cod"). Remaining target suffix =
     *   "eforce" (6 chars).
     *   => Answer = 9 - 3 = 6.
     *
     * Example 2 (Edge case: target already a subsequence):
     *   source = "abcde", target = "ace"
     *   'a' matches index 0, 'c' matches index 2, 'e' matches index 4.
     *   Fully matched (3/3) => Answer = 0. No characters need to be appended.
     *
     * Example 3 (Boundary/tie-breaking case: empty strings & full mismatch):
     *   3a. source = "", target = "xyz"  => nothing to match => Answer = 3.
     *   3b. source = "xyz", target = ""  => empty target trivially a
     *       subsequence of anything => Answer = 0.
     *   3c. source = "aaaa", target = "aaaaa" (one more 'a' than source)
     *       => matches all 4 a's, 1 left over => Answer = 1.
     *       (This tests the "duplicate character" / boundary counting case.)
     * ======================================================================== */


    /* ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS
     * ------------------------------------------------------------------------
     * Paradigms considered and applicability:
     *   - Brute force / naive............ APPLICABLE (Approach 1)
     *   - Two-pointer / greedy scan....... APPLICABLE, and OPTIMAL (Approach 2)
     *   - Dynamic Programming............. APPLICABLE as a generalization/
     *                                       instructive alternative (Approach 3)
     *   - Sorting-based.................... NOT APPLICABLE — order of
     *       characters is semantically meaningful (subsequence depends on
     *       original order); sorting would destroy the very property
     *       we need to preserve.
     *   - Hashing-based..................... NOT APPLICABLE — hashing helps
     *       with existence/frequency queries, but this problem is about
     *       strict positional/order matching, which hashing doesn't capture.
     *   - Divide and conquer................ NOT APPLICABLE — there's no
     *       clean way to split source/target and combine subsequence-match
     *       results independently; matching state (pointer position) must
     *       flow sequentially left-to-right.
     *   - Tree / graph traversal............ NOT APPLICABLE — no natural
     *       tree/graph structure underlies this problem.
     *   - Heap / priority queue.............. NOT APPLICABLE — no ordering-
     *       by-priority requirement; we scan strictly left to right.
     *   - Binary search...................... NOT APPLICABLE — there is no
     *       monotonic predicate over an answer space that binary search
     *       could exploit faster than the already-linear greedy scan.
     *   - Monotonic stack/deque.............. NOT APPLICABLE — no
     *       "next greater/smaller element" structure exists here.
     *   - Trie / segment tree / advanced..... NOT APPLICABLE for this exact
     *       problem — these shine when doing MANY repeated queries against
     *       a fixed source (e.g., precomputing "next occurrence of char c
     *       after index i" tables). Worth mentioning in follow-ups.
     * ======================================================================== */


    /* ------------------------------------------------------------------------
     * APPROACH 1: Brute Force — "Shrinking Prefix Subsequence Check"
     * ------------------------------------------------------------------------
     * Core idea:
     *   Try prefixes of `target` from LONGEST (full target) down to shortest.
     *   For each candidate prefix length L, check (from scratch, via a
     *   standard O(n) subsequence check) whether target[0..L) is a
     *   subsequence of `source`. The first L for which this succeeds gives
     *   us: answer = target.length() - L.
     *
     * Data structure / paradigm:
     *   Simple string scanning, repeated subsequence-check helper. No
     *   auxiliary data structure needed — this is the "naive" restart-every-
     *   time approach.
     *
     * Time Complexity: O(m * (n + m))
     *   - We try up to m+1 prefix lengths (m = target.length()).
     *   - Each subsequence check costs O(n + m) (n = source.length()).
     *   - Total: O(m * (n + m)), which is quadratic-ish and wasteful because
     *     we re-scan `source` from the beginning for every candidate length.
     *
     * Space Complexity: O(1) extra (excluding input storage), since each
     *   check uses only pointers, no extra structures.
     *
     * Pros:
     *   - Very easy to reason about and verify correctness.
     *   - Good as a warm-up / correctness baseline before optimizing.
     *
     * Cons:
     *   - Wasteful repeated work — re-derives matching information that
     *     could be computed once in a single pass.
     *   - Not viable for large inputs (n, m ~ 10^5 or more).
     *
     * When to use / not use:
     *   - Use only to sanity-check logic on tiny inputs or in unit tests.
     *   - Do NOT use in production or for the actual interview solution.
     * ------------------------------------------------------------------------ */
    static int minAppendBruteForce(String source, String target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Inputs must not be null.");
        }
        int targetLength = target.length();

        // Try prefixes from full length down to 0.
        for (int prefixLength = targetLength; prefixLength >= 0; prefixLength--) {
            String candidatePrefix = target.substring(0, prefixLength);
            if (isSubsequence(source, candidatePrefix)) {
                return targetLength - prefixLength;
            }
        }
        // Unreachable: prefixLength = 0 (empty string) is always a subsequence.
        return targetLength;
    }

    // Helper: standard O(n + m) check of whether `pattern` is a subsequence of `text`.
    private static boolean isSubsequence(String text, String pattern) {
        int textIndex = 0;
        int patternIndex = 0;
        while (textIndex < text.length() && patternIndex < pattern.length()) {
            if (text.charAt(textIndex) == pattern.charAt(patternIndex)) {
                patternIndex++;
            }
            textIndex++;
        }
        return patternIndex == pattern.length();
    }


    /* ------------------------------------------------------------------------
     * APPROACH 2 (OPTIMAL): Two-Pointer Greedy Single Pass
     * ------------------------------------------------------------------------
     * Core idea:
     *   Walk through `source` once with a pointer `sourceIndex`, and through
     *   `target` once with a pointer `targetIndex`. Whenever the characters
     *   at both pointers match, advance BOTH pointers (we greedily "claim"
     *   the earliest possible matching character in source — this is
     *   provably optimal because matching as early as possible in `source`
     *   never hurts future matches, and can only help). Otherwise, advance
     *   only `sourceIndex`. At the end, `targetIndex` tells us how many
     *   characters of `target` were matched; the rest must be appended.
     *
     * Data structure / paradigm:
     *   Two-pointer technique (greedy). No auxiliary storage needed.
     *
     * Time Complexity: O(n + m)
     *   - Single linear pass through `source` (length n); `targetIndex`
     *     also advances monotonically and never exceeds m.
     *
     * Space Complexity: O(1)
     *   - Only a couple of integer pointers are used.
     *
     * Pros:
     *   - Optimal time and space.
     *   - Simple to implement, easy to explain, hard to get wrong once the
     *     greedy justification is understood.
     *   - Exactly what an interviewer expects as the "final" solution.
     *
     * Cons:
     *   - Requires a brief but non-obvious greedy-correctness argument
     *     (why matching earliest is optimal) to fully justify to a skeptical
     *     interviewer.
     *
     * When to use:
     *   - Always, for this exact single-query formulation of the problem.
     * ------------------------------------------------------------------------ */
    static int minAppendTwoPointer(String source, String target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Inputs must not be null.");
        }

        int sourceIndex = 0;
        int targetIndex = 0;
        int sourceLength = source.length();
        int targetLength = target.length();

        while (sourceIndex < sourceLength && targetIndex < targetLength) {
            if (source.charAt(sourceIndex) == target.charAt(targetIndex)) {
                targetIndex++; // Claim this character of target as matched.
            }
            sourceIndex++; // Always advance through source.
        }

        // Whatever of target we could NOT match must be appended.
        return targetLength - targetIndex;
    }


    /* ------------------------------------------------------------------------
     * APPROACH 3: Dynamic Programming (Generalized Prefix-Matching Table)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Build a DP table where dp[i][j] = the length of the longest prefix
     *   of target[0..j) that can be matched as a subsequence using
     *   source[0..i). Transition:
     *     if source[i-1] == target[j-1]: dp[i][j] = dp[i-1][j-1] + 1
     *     else:                          dp[i][j] = dp[i-1][j]
     *   The final answer is target.length() - dp[n][m].
     *
     *   This is functionally equivalent to Approach 2 but expressed as a
     *   full DP table. It generalizes naturally if the problem were
     *   extended (e.g., "what's the best matching using ANY prefix of
     *   source of a given length", or combined with edit-distance-style
     *   problems where insertions/substitutions elsewhere are allowed).
     *
     * Data structure / paradigm: Dynamic Programming, 2D table (or rolling
     *   1D array optimization).
     *
     * Time Complexity: O(n * m) — one DP cell per (source, target) index
     *   pair, O(1) transition per cell.
     *
     * Space Complexity: O(n * m) with a full table, reducible to O(m) with
     *   a rolling 1D array since dp[i][*] only depends on dp[i-1][*].
     *
     * Pros:
     *   - Generalizes well to related but harder problem variants.
     *   - Table can answer sub-queries like "longest prefix matchable using
     *     only source[0..i)" for every i, which the two-pointer approach
     *     doesn't directly expose.
     *
     * Cons:
     *   - Strictly worse time and space than Approach 2 for this exact
     *     problem — total overkill.
     *   - More code, more room for off-by-one bugs, harder to explain
     *     quickly in an interview.
     *
     * When to use:
     *   - Only if the interviewer extends the problem to require info this
     *     table provides that the greedy pointer does not (see Follow-Ups).
     *   - Not recommended as the primary solution here.
     * ------------------------------------------------------------------------ */
    static int minAppendDynamicProgramming(String source, String target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Inputs must not be null.");
        }

        int sourceLength = source.length();
        int targetLength = target.length();

        // dp[i][j] = longest prefix of target[0..j) matched using source[0..i)
        int[][] dp = new int[sourceLength + 1][targetLength + 1];

        for (int i = 1; i <= sourceLength; i++) {
            for (int j = 1; j <= targetLength; j++) {
                if (source.charAt(i - 1) == target.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        int longestMatchedPrefix = dp[sourceLength][targetLength];
        return targetLength - longestMatchedPrefix;
    }


    /* ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ------------------------------------------------------------------------
     * | Approach                      | Time         | Space   | Best For                              | Limitations                                  |
     * |--------------------------------|--------------|---------|----------------------------------------|-----------------------------------------------|
     * | 1. Brute Force (shrinking prefix)| O(m*(n+m))  | O(1)    | Sanity-checking correctness on tiny data| Far too slow for real input sizes (n,m large) |
     * | 2. Two-Pointer Greedy (OPTIMAL)  | O(n+m)      | O(1)    | Production use; the interview answer   | Requires greedy-correctness justification     |
     * | 3. Dynamic Programming           | O(n*m)      | O(n*m) or O(m) rolling | Generalizing to richer variants | Overkill in time & space for this exact problem|
     * ======================================================================== */


    /* ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ------------------------------------------------------------------------
     * I would present APPROACH 2 (Two-Pointer Greedy).
     *
     * Why:
     *   - It is optimal in both time O(n+m) and space O(1) — nothing beats
     *     this asymptotically since we must at least read every character
     *     of `source` once in the worst case.
     *   - It is FAST to code correctly under interview time pressure —
     *     roughly 10 lines of core logic, minimal edge-case surface area.
     *   - It demonstrates a clean greedy-correctness argument, which is
     *     exactly what interviewers want to hear articulated out loud:
     *     "matching a target character as early as possible in source
     *     never disadvantages later matches, so greedy earliest-match is
     *     optimal." This shows algorithmic maturity beyond "I know the
     *     trick."
     *   - It requires no auxiliary memory, which is a strong signal for
     *     efficiency-conscious interviewers (e.g., Google-style bar raiser).
     * ======================================================================== */


    /* ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (Production-Quality)
     * ======================================================================== */
    /**
     * Computes the minimum number of characters that must be appended to the
     * end of {@code source} so that {@code target} becomes a subsequence of
     * the resulting string.
     *
     * <p>Algorithm: single-pass two-pointer greedy match. We advance through
     * {@code source} once; every time the current source character equals
     * the next unmatched target character, we "consume" that target
     * character. At the end, any unconsumed suffix of {@code target} must
     * be appended verbatim.
     *
     * @param source the base string; characters may only be appended after it
     * @param target the string that must become a subsequence of the result
     * @return the minimum number of characters to append (never negative)
     * @throws IllegalArgumentException if either argument is null
     */
    static int minAppendToMakeSubsequenceOptimal(String source, String target) {
        // --- Defensive input validation (production hygiene) ---
        if (source == null || target == null) {
            throw new IllegalArgumentException("source and target must not be null");
        }

        // --- Fast exit: nothing to match means nothing to append ---
        if (target.isEmpty()) {
            return 0;
        }

        final int sourceLength = source.length();
        final int targetLength = target.length();

        // targetPointer tracks how many leading characters of `target`
        // have been successfully matched, in order, within `source` so far.
        int targetPointer = 0;

        // Single linear scan over `source`.
        for (int sourcePointer = 0; sourcePointer < sourceLength && targetPointer < targetLength; sourcePointer++) {
            // Only advance targetPointer when we find the character it's
            // currently looking for. This is the greedy "earliest match"
            // rule that guarantees optimality.
            if (source.charAt(sourcePointer) == target.charAt(targetPointer)) {
                targetPointer++;
            }
        }

        // Characters of target from index `targetPointer` onward were never
        // matched, and must be appended verbatim to source.
        int charactersToAppend = targetLength - targetPointer;

        // Sanity guard (should never trigger given the loop invariant above).
        assert charactersToAppend >= 0 : "Appended character count cannot be negative";

        return charactersToAppend;
    }


    /* ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ------------------------------------------------------------------------
     * Using Example 1: source = "coding", target = "codeforce"
     *
     * Initial state: targetPointer = 0
     *
     * sourcePointer=0, source[0]='c', target[targetPointer=0]='c' -> MATCH
     *     targetPointer becomes 1
     * sourcePointer=1, source[1]='o', target[targetPointer=1]='o' -> MATCH
     *     targetPointer becomes 2
     * sourcePointer=2, source[2]='d', target[targetPointer=2]='d' -> MATCH
     *     targetPointer becomes 3
     * sourcePointer=3, source[3]='i', target[targetPointer=3]='e' -> no match
     *     targetPointer stays 3
     * sourcePointer=4, source[4]='n', target[targetPointer=3]='e' -> no match
     *     targetPointer stays 3
     * sourcePointer=5, source[5]='g', target[targetPointer=3]='e' -> no match
     *     targetPointer stays 3
     * Loop ends: sourcePointer reached sourceLength (6).
     *
     * Final targetPointer = 3 (matched "cod")
     * targetLength = 9
     * charactersToAppend = 9 - 3 = 6
     *
     * => Answer: 6  (we'd append "eforce" to "coding" to get
     *    "codingeforce", in which "codeforce" is indeed a subsequence:
     *    c-o-d [from "coding"] -e-f-o-r-c-e [from appended tail] — wait,
     *    let's verify precisely against "codingeforce":
     *      c(0) o(1) d(2) i(3) n(4) g(5) e(6) f(7) o(8) r(9) c(10) e(11)
     *    target "codeforce": c(0) o(1) d(2) e(6) f(7) o(8) r(9) c(10) e(11)
     *    -> indices strictly increasing: 0,1,2,6,7,8,9,10,11 ✓ valid subsequence.
     * ======================================================================== */


    /* ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ------------------------------------------------------------------------
     * - Approach 1 (Brute Force) is correct but O(m*(n+m)) — useful only as
     *   a baseline / correctness oracle for testing.
     * - Approach 2 (Two-Pointer Greedy) is optimal at O(n+m) time, O(1)
     *   space, and is what I'd write in a real interview.
     * - Approach 3 (DP) matches Approach 2's correctness but costs O(n*m)
     *   time/space — valuable conceptually for generalizations, not for
     *   this exact problem.
     *
     * Known assumptions/limitations of the final (Approach 2) solution:
     *   - Assumes lowercase English letters (though the algorithm actually
     *     works for ANY comparable character set / Unicode without
     *     modification — the a-z constraint doesn't affect correctness).
     *   - Assumes a single one-shot query; repeated queries against a fixed
     *     `source` could be optimized further (see Follow-Ups).
     *   - Assumes we only need the COUNT, not the exact appended substring
     *     (trivially target.substring(targetPointer) if needed).
     * ======================================================================== */


    /* ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ------------------------------------------------------------------------
     * 1. "What if we run this same `source` against THOUSANDS of different
     *     `target` queries?" 
     *     -> Precompute a "next occurrence of character c after index i"
     *        table (26 x n) for source, enabling each query to run in
     *        O(m log n) via binary search per character, independent of n.
     *
     * 2. "What if insertions were also allowed at the FRONT or MIDDLE of
     *     source, not just the end?"
     *     -> Then the problem becomes closer to computing the longest
     *        common subsequence (LCS) between source and target, and the
     *        answer would be target.length() - LCS(source, target),
     *        which needs the full O(n*m) DP (Approach 3 becomes essential
     *        here, not overkill).
     *
     * 3. "What if we wanted to know the actual resulting string, not just
     *     the count?"
     *     -> Trivial extension: return source + target.substring(targetPointer).
     *
     * 4. "How would this change if the alphabet were Unicode instead of
     *     just lowercase English letters?"
     *     -> No algorithmic change needed for Approach 2; only the
     *        next-occurrence table (for the repeated-query follow-up)
     *        would need a hash map instead of a fixed 26-size array.
     *
     * 5. "Can you solve this in a single pass with O(1) space AND handle
     *     streaming input (source arrives character by character, target
     *     is fixed upfront)?"
     *     -> Yes: maintain only `targetPointer` as state; process each
     *        incoming character of source exactly as in Approach 2, no
     *        buffering required.
     *
     * 6. "What if source and target could contain wildcard characters
     *     (e.g., '?' matches any character)?"
     *     -> Modify the match condition to
     *        `source.charAt(sourcePointer) == target.charAt(targetPointer)
     *         || target.charAt(targetPointer) == '?'`.
     * ======================================================================== */


    /* ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ------------------------------------------------------------------------
     * 1. Forgetting the empty-string edge cases: an empty `target` should
     *    immediately return 0 (vacuously a subsequence of anything), and an
     *    empty `source` with non-empty `target` should return
     *    target.length() — candidates sometimes special-case one but not
     *    the other.
     *
     * 2. Advancing BOTH pointers unconditionally instead of only advancing
     *    `targetPointer` on a match. This is the single most common bug —
     *    it silently produces wrong (too-small) answers because it skips
     *    target characters without actually verifying a match.
     *
     * 3. Off-by-one on the final subtraction: computing
     *    `targetLength - targetPointer` vs. accidentally using
     *    `sourceLength - targetPointer` or similar — always double check
     *    which length anchors the "remaining characters" subtraction.
     *
     * 4. Assuming greedy "leftmost match" needs proof by exhaustive case
     *    analysis in the interview — it's worth being ready to briefly
     *    justify: if you match target[j] at the earliest possible position
     *    in source, you leave the MAXIMAL remaining suffix of source
     *    available for matching target[j+1..], which can only help (never
     *    hurt) compared to matching target[j] at any later position.
     * ======================================================================== */


    /* ========================================================================
     * MAIN METHOD — Demonstration / Manual Testing Harness
     * ======================================================================== */
    public static void main(String[] args) {
        String[][] testCases = {
            {"coding", "codeforce"},   // Example 1: normal case -> expect 6
            {"abcde", "ace"},          // Example 2: already subsequence -> expect 0
            {"", "xyz"},               // Example 3a: empty source -> expect 3
            {"xyz", ""},               // Example 3b: empty target -> expect 0
            {"aaaa", "aaaaa"}          // Example 3c: duplicate boundary -> expect 1
        };

        for (String[] testCase : testCases) {
            String source = testCase[0];
            String target = testCase[1];

            int bruteForceResult = minAppendBruteForce(source, target);
            int twoPointerResult = minAppendTwoPointer(source, target);
            int dpResult = minAppendDynamicProgramming(source, target);
            int optimalResult = minAppendToMakeSubsequenceOptimal(source, target);

            System.out.printf(
                "source=%-10s target=%-10s => brute=%d, twoPointer=%d, dp=%d, optimal=%d%n",
                "\"" + source + "\"", "\"" + target + "\"",
                bruteForceResult, twoPointerResult, dpResult, optimalResult
            );

            // All four approaches must agree.
            assert bruteForceResult == twoPointerResult
                && twoPointerResult == dpResult
                && dpResult == optimalResult
                : "Mismatch detected across approaches!";
        }
    }
}
