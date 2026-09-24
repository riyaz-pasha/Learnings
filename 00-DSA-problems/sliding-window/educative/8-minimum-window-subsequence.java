import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: "Shortest Substring Containing S2 as a Subsequence"
 * (This is the classic problem known on LeetCode as #727 "Minimum Window Subsequence")
 * ============================================================================
 *
 * This single file walks through the full interview lifecycle: restating the
 * problem, asking clarifying questions, working examples, sweeping every
 * applicable paradigm, comparing approaches, and landing on a polished,
 * production-quality optimal solution with a full dry run and test harness.
 */
class MinimumWindowSubsequence {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In plain English:
     *   We are given two strings, s1 and s2. We need to find the SHORTEST
     *   CONTIGUOUS substring of s1 such that s2 appears inside it as a
     *   SUBSEQUENCE (characters in order, not necessarily adjacent).
     *
     *   - "Substring" of s1  -> must be contiguous (a window [start, end] in s1).
     *   - "Subsequence" of s2 -> the characters of s2 must appear in that window
     *      in the same relative order, but other characters may sit in between.
     *
     * Inputs:
     *   - s1: the "haystack" string we search within.
     *   - s2: the "pattern" string whose characters must appear, in order,
     *         inside some window of s1.
     *
     * Output:
     *   - The shortest window of s1 (as a String) that contains s2 as a
     *     subsequence.
     *   - If multiple windows tie for shortest length, return the one whose
     *     starting index in s1 is smallest (leftmost).
     *   - If no such window exists, return "".
     *
     * Implicit assumptions to confirm with the interviewer (see Section 2):
     *   - Character set (ASCII? Unicode?), case sensitivity, empty-string
     *     handling, and whether we need just one answer or all minimal windows.
     */

    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * 1. Q: What is the expected size of s1 and s2? Are we talking about
     *       small strings (<100 chars) or large ones (up to 10^5 - 10^6)?
     *    A (assumed): s1.length() can be up to ~2 * 10^4, s2.length() up to
     *       ~100, per typical LeetCode-style constraints. This matters
     *       because it tells us whether an O(n*m) or even O(n^2) solution
     *       is acceptable, or whether we truly need sub-quadratic behavior.
     *
     * 2. Q: Is the character set restricted (e.g., lowercase English
     *       letters) or can it include arbitrary Unicode?
     *    A (assumed): Lowercase English letters ('a'-'z'), but the solution
     *       should not hard-code this assumption -- it should work for any
     *       Comparable character set since we're doing direct char equality.
     *
     * 3. Q: Should the match be case-sensitive?
     *    A (assumed): Yes, case-sensitive exact character matching.
     *
     * 4. Q: What should we return if s2 is empty?
     *    A (assumed): An empty s2 is trivially a subsequence of the empty
     *       substring, so we return "" (the empty window at the very start
     *       is the shortest possible match, length 0).
     *
     * 5. Q: What should we return if s1 is empty, or if s2 is longer than
     *       s1 (making a match impossible outright)?
     *    A (assumed): Return "" immediately -- no valid window can exist.
     *
     * 6. Q: If there are multiple windows of the same minimal length, which
     *       one should we return?
     *    A (assumed): The leftmost one (smallest starting index in s1), as
     *       explicitly stated in the problem.
     *
     * 7. Q: Do we need to return just one shortest window, or all of them?
     *    A (assumed): Just one -- the leftmost shortest window.
     *
     * 8. Q: Is this a one-shot query, or will the same s1 be queried
     *       repeatedly against many different s2 patterns (which would
     *       change our preprocessing strategy, e.g., precomputing
     *       next-occurrence tables)?
     *    A (assumed): Single query for this interview; I'll mention the
     *       repeated-query optimization as a follow-up extension.
     */

    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case):
     *   s1 = "abcdebdde"
     *   s2 = "bde"
     *   Walk-through: s2's characters b, d, e must appear in order somewhere
     *   in a contiguous window. Two candidate windows of length 4 exist:
     *     - "bcde" (indices 1..4): b(1) ... d(3) ... e(4)  -> valid, length 4
     *     - "bdde" (indices 5..8): b(5) ... d(6 or 7) ... e(8) -> valid, length 4
     *   Both are length 4, so we return the LEFTMOST: "bcde".
     *
     * Example 2 (Edge case -- no valid window exists):
     *   s1 = "abcabc"
     *   s2 = "cab" reversed order check: does "cab" appear as a subsequence
     *   of any window such that order c -> a -> b holds?
     *   Actually, let's pick a cleaner true-negative: s1 = "abc", s2 = "acb".
     *   No window of "abc" contains 'a' then 'c' then 'b' in that order
     *   (the only 'c' comes after both 'a' and 'b', and 'b' comes before
     *   'c', so "a", "c", "b" in that exact order never occurs).
     *   Expected output: "" (empty string).
     *
     * Example 3 (Boundary / tie-breaking case):
     *   s1 = "abab"
     *   s2 = "ab"
     *   Two minimal windows of length 2 exist: s1[0..1] = "ab" and
     *   s1[2..3] = "ab". Both are valid and both have length 2. Per the
     *   tie-breaking rule, we return the leftmost: "ab" starting at index 0.
     *
     * Additional edge cases to keep in mind while coding:
     *   - s2 is empty            -> return "".
     *   - s1 is empty            -> return "".
     *   - s2 longer than s1      -> return "" (impossible).
     *   - s2 has repeated characters (e.g., s2 = "aa") -- must correctly
     *     require two distinct positions of 'a' in s1, not the same index twice.
     *   - s1 == s2 exactly       -> the whole string is the (only) answer.
     */

    /* ========================================================================
     * SECTION 4 & 5 & 6: ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Paradigms swept and why they are/aren't applicable:
     *
     *   - Brute force            -> APPLICABLE (baseline correctness oracle).
     *   - Sorting-based          -> NOT APPLICABLE. Order of characters in s1
     *                               is semantically meaningful (we need
     *                               subsequence order preserved); sorting
     *                               destroys that structure entirely.
     *   - Hashing-based          -> NOT DIRECTLY APPLICABLE. Hashing helps us
     *                               test set membership or exact substring
     *                               equality (e.g., Rabin-Karp), but subsequence
     *                               containment is an ORDERED, positional
     *                               relationship that hashing does not capture.
     *   - Two pointer / sliding window -> APPLICABLE and OPTIMAL for a single
     *                               query. This is the classic expand/contract
     *                               greedy scan.
     *   - Divide and conquer     -> NOT NATURALLY APPLICABLE. There's no clean
     *                               way to split s1 in half and combine
     *                               subsequence-containment results, because a
     *                               valid window can straddle the midpoint.
     *   - Greedy                 -> APPLICABLE. The two-pointer approach IS a
     *                               greedy algorithm: greedily match forward,
     *                               then greedily contract backward.
     *   - Dynamic programming    -> APPLICABLE. A 2D DP table tracking "best
     *                               start index for matching a prefix of s2
     *                               ending at position i in s1" gives a
     *                               reliable, non-degenerate O(n*m) solution.
     *   - Tree / graph traversal -> NOT APPLICABLE. No natural graph structure
     *                               models this problem.
     *   - Heap / priority queue  -> NOT APPLICABLE. There's no "top-k" or
     *                               ordering-by-priority aspect; window
     *                               validity is a positional yes/no check.
     *   - Binary search          -> NOT APPLICABLE in the classic sense. Window
     *                               length isn't monotonic in a way that lets
     *                               us binary-search "does a window of length L
     *                               exist" independent of position -- we'd
     *                               still need an O(n) check per candidate
     *                               length, and it wouldn't beat the two-pointer
     *                               approach's overall complexity.
     *   - Monotonic stack/deque  -> NOT APPLICABLE. There's no "next greater/
     *                               smaller element" structure or window-max
     *                               structure at play here.
     *   - Trie / segment tree    -> NOT NATURALLY APPLICABLE for a single
     *                               query; a Trie helps with prefix matching
     *                               across MANY patterns, and a segment tree
     *                               helps with range aggregate queries --
     *                               neither models "ordered character
     *                               containment in a sliding window" directly.
     *
     * So the three approaches worth presenting, in increasing order of
     * refinement, are: Brute Force, Two-Pointer Greedy Expand/Contract
     * (the interview-optimal answer), and Dynamic Programming (a robust
     * alternative with the same asymptotic complexity but no pathological
     * re-scanning behavior).
     */

    /* ------------------------------------------------------------------------
     * Approach 1: Brute Force (Generate All Substrings + Subsequence Check)
     * ------------------------------------------------------------------------
     * Core idea:
     *   For every possible starting index in s1, extend the window rightward
     *   one character at a time. As soon as the current window contains s2
     *   as a subsequence, record it (it's the shortest window for THIS start)
     *   and move to the next starting index.
     *
     * Data structure / paradigm:
     *   None beyond raw string scanning -- this is exhaustive enumeration.
     *
     * Time complexity:
     *   O(n^2 * m) in the worst case: O(n) choices of start, O(n) choices of
     *   end, and each subsequence check costs O(window length) <= O(n),
     *   which itself is bounded by needing to scan through up to m matches.
     *   Concretely: O(n) starts * O(n) end-extensions * O(m) per-character
     *   comparison work in the inner check.
     *
     * Space complexity:
     *   O(1) extra space (excluding the output string).
     *
     * Pros:
     *   - Trivial to reason about and verify correctness.
     *   - Zero risk of subtle off-by-one bugs -- a great "safety net" to
     *     cross-validate optimized solutions against.
     *
     * Cons:
     *   - Far too slow for realistic input sizes (n up to 20,000+).
     *   - Re-does a lot of redundant subsequence-matching work across
     *     overlapping windows.
     *
     * When to use in practice:
     *   - Never in production. Only as a correctness oracle in tests, or as
     *     an opening "let me start with something clearly correct" answer
     *     in an interview before optimizing.
     */
    public static String bruteForce(String s1, String s2) {
        int n = s1.length();
        int m = s2.length();

        // An empty pattern is trivially satisfied by the empty window.
        if (m == 0) {
            return "";
        }
        if (n == 0 || m > n) {
            return "";
        }

        String bestWindow = "";

        for (int start = 0; start < n; start++) {
            for (int end = start; end < n; end++) {
                if (isSubsequenceInRange(s1, start, end, s2)) {
                    int candidateLength = end - start + 1;
                    if (bestWindow.isEmpty() || candidateLength < bestWindow.length()) {
                        bestWindow = s1.substring(start, end + 1);
                    }
                    // Once we find the first (shortest, for this start) valid
                    // end, no need to keep extending -- longer windows from
                    // the same start can't beat this one.
                    break;
                }
            }
        }
        return bestWindow;
    }

    // Helper: checks whether s2 is a subsequence of s1[start..end] (inclusive).
    private static boolean isSubsequenceInRange(String s1, int start, int end, String s2) {
        int patternIndex = 0;
        for (int i = start; i <= end && patternIndex < s2.length(); i++) {
            if (s1.charAt(i) == s2.charAt(patternIndex)) {
                patternIndex++;
            }
        }
        return patternIndex == s2.length();
    }

    /* ------------------------------------------------------------------------
     * Approach 2: Two-Pointer Greedy Expand & Contract (INTERVIEW-OPTIMAL)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Scan s1 left to right with a pointer i, greedily matching characters
     *   of s2 with a pointer j. The moment we've matched all of s2 (j reaches
     *   m), we know we've found SOME valid window ending at i. But it may not
     *   be minimal on the LEFT side, so we walk backward from i, greedily
     *   re-matching s2 in reverse, to find the tightest possible start for
     *   this particular ending position. We record that window if it beats
     *   our current best, then resume the forward scan just after the
     *   window's start (not from scratch), to find the next candidate.
     *
     * Data structure / paradigm:
     *   Two pointers / greedy expand-then-contract scan. No auxiliary data
     *   structure needed.
     *
     * Time complexity:
     *   O(n * m) worst case (e.g., s1 = "aaaa...a", s2 = "aaaa...a" forces a
     *   full O(m) backward contraction at nearly every position). In
     *   practice this is much faster than the brute force because each
     *   forward scan naturally skips non-matching characters in O(1) and
     *   we never re-examine a prefix of s1 we've already ruled out.
     *
     * Space complexity:
     *   O(1) extra space (a few integer pointers), excluding the output.
     *
     * Pros:
     *   - Optimal (or matching-optimal) time complexity for a single query.
     *   - O(1) extra space -- no auxiliary table needed.
     *   - Conceptually elegant: "match forward, then snap-tight backward."
     *
     * Cons:
     *   - Trickier to get exactly right under interview pressure (backward
     *     contraction indices are a common source of off-by-one bugs).
     *   - Worst-case degrades to O(n*m) on adversarial/repetitive inputs,
     *     same as the DP approach -- so it isn't asymptotically better than
     *     DP, just more space-efficient.
     *
     * When to use in practice:
     *   - This is the approach to present and code in an interview: it's
     *     optimal in space, simple to explain at a high level, and doesn't
     *     require justifying an O(n*m) table allocation.
     */
    public static String twoPointerOptimal(String s1, String s2) {
        int n = s1.length();
        int m = s2.length();

        if (m == 0) {
            return "";
        }
        if (n == 0 || m > n) {
            return "";
        }

        int i = 0;                       // pointer scanning s1
        int j = 0;                       // pointer scanning s2
        int minWindowLength = Integer.MAX_VALUE;
        int resultStart = -1;

        while (i < n) {
            if (s1.charAt(i) == s2.charAt(j)) {
                j++;
                if (j == m) {
                    // Full match found ending at index i (inclusive).
                    int windowEndInclusive = i;

                    // Contract backward: re-match s2 in reverse starting
                    // from windowEndInclusive to find the tightest start.
                    int patternIndex = m - 1;
                    int p = i;
                    while (patternIndex >= 0) {
                        if (s1.charAt(p) == s2.charAt(patternIndex)) {
                            patternIndex--;
                        }
                        p--;
                    }
                    p++; // p now sits exactly on the tightest valid start index.

                    int candidateLength = windowEndInclusive - p + 1;
                    if (candidateLength < minWindowLength) {
                        minWindowLength = candidateLength;
                        resultStart = p;
                    }

                    // Resume scanning just after this window's start --
                    // any shorter window must start later than p, since we
                    // already know p is the tightest start for THIS ending.
                    i = p;
                    j = 0;
                }
            }
            i++;
        }

        return resultStart == -1 ? "" : s1.substring(resultStart, resultStart + minWindowLength);
    }

    /* ------------------------------------------------------------------------
     * Approach 3: Dynamic Programming (2D Table of Best Start Indices)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Define dp[i][j] = the starting index (0-indexed, in s1) of the
     *   tightest window within s1[0..i-1] such that s2[0..j-1] is fully
     *   matched as a subsequence with its LAST character matched exactly at
     *   s1[i-1]. Use -1 to mean "no such match is possible."
     *
     *   Recurrence:
     *     dp[i][0] = i               (matching 0 chars of s2 is trivially
     *                                  satisfied by the empty window "ending"
     *                                  right before position i)
     *     dp[0][j] = -1 for j >= 1   (can't match a non-empty pattern with
     *                                  zero characters of s1 consumed)
     *     dp[i][j] = dp[i-1][j-1]    if s1[i-1] == s2[j-1] (extend the match)
     *              = dp[i-1][j]      otherwise (carry forward the best start
     *                                  found so far without consuming s1[i-1])
     *
     *   After filling the table, scan dp[i][m] for all i from 1..n; whichever
     *   i gives the smallest (i - dp[i][m]) with dp[i][m] != -1 is our answer.
     *   Because we scan i in increasing order and use a strict "<" comparison,
     *   ties naturally resolve to the leftmost-starting window.
     *
     * Data structure / paradigm:
     *   Bottom-up dynamic programming over a 2D table indexed by
     *   (position in s1, position in s2).
     *
     * Time complexity:
     *   O(n * m) to fill the table, plus O(n) to scan the last column --
     *   overall O(n * m).
     *
     * Space complexity:
     *   O(n * m) for the full table (can be reduced to O(m) with a rolling
     *   array of two rows, since dp[i][*] only depends on dp[i-1][*]).
     *
     * Pros:
     *   - No worst-case re-scanning surprises -- every cell is computed once,
     *     so the complexity is a hard O(n*m), never worse.
     *   - Conceptually systematic -- easy to prove correct via the
     *     recurrence, and easy to extend (e.g., to count all minimal windows,
     *     or answer repeated queries against a fixed s1 more cheaply after
     *     one O(n*m) preprocessing pass).
     *
     * Cons:
     *   - O(n*m) space is a real cost for large n (e.g., n = 20,000,
     *     m = 100 -> 2,000,000 ints -- fine, but if m were also large this
     *     grows fast).
     *   - More setup/boilerplate to write correctly under time pressure
     *     compared to the two-pointer approach.
     *
     * When to use in practice:
     *   - Great as a "let me also show you a more systematic alternative"
     *     follow-up, especially if the interviewer pushes on worst-case
     *     guarantees or wants to extend the problem to multiple queries
     *     against the same s1 (the DP table's structure generalizes more
     *     easily to such variants).
     */
    public static String dpApproach(String s1, String s2) {
        int n = s1.length();
        int m = s2.length();

        if (m == 0) {
            return "";
        }
        if (n == 0 || m > n) {
            return "";
        }

        // dp[i][j]: tightest start index for matching s2[0..j-1] with the
        // last character landing exactly at s1[i-1]. -1 means impossible.
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }
        for (int j = 1; j <= m; j++) {
            dp[0][j] = -1;
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        int minWindowLength = Integer.MAX_VALUE;
        int resultStart = -1;
        for (int i = 1; i <= n; i++) {
            int candidateStart = dp[i][m];
            if (candidateStart != -1) {
                int candidateLength = i - candidateStart;
                if (candidateLength < minWindowLength) {
                    minWindowLength = candidateLength;
                    resultStart = candidateStart;
                }
            }
        }

        return resultStart == -1 ? "" : s1.substring(resultStart, resultStart + minWindowLength);
    }

    /* ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * Approach              | Time      | Space    | Best For                          | Limitations
     * ----------------------|-----------|----------|-----------------------------------|-------------------------------------------
     * Brute Force           | O(n^2*m)  | O(1)     | Correctness oracle / sanity check | Far too slow for real inputs
     * Two-Pointer (optimal) | O(n*m)*   | O(1)     | Interview presentation, single query | Worst case still O(n*m); trickier indices
     * Dynamic Programming   | O(n*m)    | O(n*m)   | Robust guarantee, repeated-query extension | O(n*m) memory cost
     *
     * * Two-pointer's O(n*m) is a worst-case bound on adversarial/repetitive
     *   inputs; in practice it's typically much closer to O(n) on inputs
     *   without heavy character repetition.
     */

    /* ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present the TWO-POINTER GREEDY EXPAND & CONTRACT approach
     * (Approach 2) as my primary solution. Reasoning:
     *
     *   - It matches the DP approach's time complexity but uses O(1) extra
     *     space, which is strictly better and easier to defend when asked
     *     "can we do this with less memory?"
     *   - It's fast to code once the "forward match, then backward snap-tight"
     *     insight is explained -- roughly 15-20 lines of core logic.
     *   - It demonstrates a genuinely clever, non-obvious greedy insight
     *     (the backward contraction step), which tends to impress
     *     interviewers more than a mechanical DP table fill.
     *   - I'd mention the DP approach as a natural follow-up: "if you wanted
     *     a hard worst-case guarantee independent of input patterns, or
     *     needed to answer many queries against the same s1 efficiently,
     *     I'd reach for the DP table instead."
     *
     * I would open with the brute force verbally (10 seconds) to confirm
     * problem understanding, then move directly to coding the two-pointer
     * solution.
     */

    /* ========================================================================
     * SECTION 9: DEEP DIVE -- PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ========================================================================
     */

    /** Sentinel used to signal "no valid start found yet" during contraction. */
    private static final int NO_MATCH = -1;

    /**
     * Finds the shortest contiguous substring of {@code text} in which
     * {@code pattern} appears as a subsequence (characters in order, not
     * necessarily contiguous). If multiple substrings of the minimal length
     * qualify, the leftmost one (smallest starting index) is returned.
     *
     * <p>Algorithm: a greedy two-pointer "expand then contract" scan.
     * <ol>
     *   <li>Expand: walk {@code text} left to right, greedily matching
     *       characters of {@code pattern} in order.</li>
     *   <li>Once {@code pattern} is fully matched (some window ending at the
     *       current position is valid), contract backward from that ending
     *       position to find the tightest possible starting index for this
     *       particular end -- this guarantees minimality for this end point.</li>
     *   <li>Record the window if it beats the current best (strict
     *       improvement only, which preserves the leftmost tie-breaker),
     *       then resume the forward scan immediately after the discovered
     *       start, since any subsequent shorter window must begin later.</li>
     * </ol>
     *
     * @param text    the string to search within (previously called s1)
     * @param pattern the subsequence that must appear, in order (previously s2)
     * @return the shortest, leftmost substring of {@code text} containing
     *         {@code pattern} as a subsequence, or {@code ""} if none exists
     *         or if {@code pattern} is empty
     * @throws NullPointerException if either argument is null
     */
    public static String findShortestSubsequenceWindow(String text, String pattern) {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(pattern, "pattern must not be null");

        final int textLength = text.length();
        final int patternLength = pattern.length();

        // Defensive short-circuits for degenerate inputs.
        if (patternLength == 0) {
            return ""; // Empty pattern is trivially satisfied by an empty window.
        }
        if (textLength == 0 || patternLength > textLength) {
            return ""; // No valid window can possibly exist.
        }

        int textPointer = 0;      // walks forward through `text`
        int patternPointer = 0;   // walks forward through `pattern`
        int bestWindowLength = Integer.MAX_VALUE;
        int bestWindowStart = NO_MATCH;

        while (textPointer < textLength) {
            if (text.charAt(textPointer) == pattern.charAt(patternPointer)) {
                patternPointer++;

                if (patternPointer == patternLength) {
                    // Full match completed; `textPointer` is the inclusive
                    // end of a valid (but not necessarily tightest) window.
                    final int windowEndInclusive = textPointer;

                    // Contract backward to find the tightest matching start
                    // for this specific ending position.
                    int reverseMatchIndex = patternLength - 1;
                    int contractingPointer = textPointer;
                    while (reverseMatchIndex >= 0) {
                        if (text.charAt(contractingPointer) == pattern.charAt(reverseMatchIndex)) {
                            reverseMatchIndex--;
                        }
                        contractingPointer--;
                    }
                    final int windowStart = contractingPointer + 1;

                    final int windowLength = windowEndInclusive - windowStart + 1;
                    // Strict "<" preserves the leftmost window on ties,
                    // because we always scan text left to right.
                    if (windowLength < bestWindowLength) {
                        bestWindowLength = windowLength;
                        bestWindowStart = windowStart;
                    }

                    // Resume scanning right after this window's start: any
                    // future window shorter than this one must begin later.
                    textPointer = windowStart;
                    patternPointer = 0;
                }
            }
            textPointer++;
        }

        return bestWindowStart == NO_MATCH
                ? ""
                : text.substring(bestWindowStart, bestWindowStart + bestWindowLength);
    }

    /* ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing findShortestSubsequenceWindow("abcdebdde", "bde"):
     *
     *   text:    a  b  c  d  e  b  d  d  e
     *   index:   0  1  2  3  4  5  6  7  8
     *   pattern: b(0) d(1) e(2)
     *
     * Forward expand #1:
     *   textPointer=0 'a' vs pattern[0]='b' -> no match, textPointer=1
     *   textPointer=1 'b' vs pattern[0]='b' -> MATCH, patternPointer=1, textPointer=2
     *   textPointer=2 'c' vs pattern[1]='d' -> no match, textPointer=3
     *   textPointer=3 'd' vs pattern[1]='d' -> MATCH, patternPointer=2, textPointer=4
     *   textPointer=4 'e' vs pattern[2]='e' -> MATCH, patternPointer=3 == patternLength -> STOP
     *   windowEndInclusive = 4
     *
     * Backward contract #1 (reverseMatchIndex starts at 2, contractingPointer at 4):
     *   idx=4 'e' vs pattern[2]='e' -> match, reverseMatchIndex=1, contractingPointer=3
     *   idx=3 'd' vs pattern[1]='d' -> match, reverseMatchIndex=0, contractingPointer=2
     *   idx=2 'c' vs pattern[0]='b' -> no match, contractingPointer=1
     *   idx=1 'b' vs pattern[0]='b' -> match, reverseMatchIndex=-1, contractingPointer=0
     *   loop stops (reverseMatchIndex < 0); windowStart = contractingPointer + 1 = 1
     *   windowLength = 4 - 1 + 1 = 4  -> bestWindowStart=1, bestWindowLength=4
     *   Resume: textPointer = 1, patternPointer = 0, then textPointer++ -> 2
     *
     * Forward expand #2 (textPointer starts at 2):
     *   textPointer=2 'c' vs 'b' -> no, textPointer=3
     *   textPointer=3 'd' vs 'b' -> no, textPointer=4
     *   textPointer=4 'e' vs 'b' -> no, textPointer=5
     *   textPointer=5 'b' vs 'b' -> MATCH, patternPointer=1, textPointer=6
     *   textPointer=6 'd' vs 'd' -> MATCH, patternPointer=2, textPointer=7
     *   textPointer=7 'd' vs 'e' -> no match, textPointer=8
     *   textPointer=8 'e' vs 'e' -> MATCH, patternPointer=3 == patternLength -> STOP
     *   windowEndInclusive = 8
     *
     * Backward contract #2 (reverseMatchIndex=2, contractingPointer=8):
     *   idx=8 'e' vs 'e' -> match, reverseMatchIndex=1, contractingPointer=7
     *   idx=7 'd' vs 'd' -> match, reverseMatchIndex=0, contractingPointer=6
     *   idx=6 'd' vs 'b' -> no match, contractingPointer=5
     *   idx=5 'b' vs 'b' -> match, reverseMatchIndex=-1, contractingPointer=4
     *   loop stops; windowStart = 4 + 1 = 5
     *   windowLength = 8 - 5 + 1 = 4
     *   Compare: 4 < bestWindowLength(4)? NO (strict inequality fails) ->
     *   bestWindowStart stays at 1. This is exactly how the leftmost tie
     *   ("bcde" over "bdde") is preserved.
     *   Resume: textPointer = 5, patternPointer = 0, then textPointer++ -> 6
     *
     * Forward expand #3 (textPointer starts at 6): no further 'b' found
     *   before textLength=9 is reached, so the scan ends.
     *
     * Final result: bestWindowStart=1, bestWindowLength=4 ->
     *   text.substring(1, 5) = "bcde"   <-- matches expected output.
     */

    /* ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force gives us an easy-to-trust O(n^2*m) baseline, useful only
     *   for validating faster solutions on small inputs.
     * - The two-pointer greedy expand/contract scan achieves O(n*m)
     *   worst-case time with O(1) extra space, and is the approach I'd
     *   actually write in an interview: it's a genuine algorithmic insight
     *   (forward-match-then-backward-snap) rather than mechanical table
     *   filling.
     * - The DP table approach matches the two-pointer's time complexity
     *   with a harder worst-case guarantee (no repeated backward re-scans),
     *   at the cost of O(n*m) space -- a solid answer if asked to remove
     *   the two-pointer approach's data-dependent slowdown, or to support
     *   repeated queries against a fixed text.
     * - Known limitation of the final solution: worst-case time is O(n*m),
     *   which degrades on highly repetitive inputs (e.g., text and pattern
     *   both consisting of a single repeated character). This is a fully
     *   understood and acceptable trade-off for this problem class, and I'd
     *   flag it proactively rather than waiting to be asked.
     * - Assumption baked into the implementation: case-sensitive, direct
     *   char-by-char equality with no locale-specific collation.
     */

    /* ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "Can you reduce the DP approach's space from O(n*m) to O(m)?"
     *    (Yes -- since dp[i][*] only depends on dp[i-1][*], keep two rolling
     *    rows instead of the full 2D table.)
     *
     * 2. "What if we need to answer this query for the SAME text against
     *    MANY different patterns? How would you optimize for that?"
     *    (Precompute, for each position and each character, the next
     *    occurrence of that character at or after that position -- a
     *    "next occurrence" table -- enabling O(m log n) or O(m) window
     *    discovery per query after O(n * alphabet size) preprocessing.)
     *
     * 3. "What if the alphabet is very large (e.g., full Unicode) --
     *    does that change your approach?"
     *    (Char equality checks are O(1) regardless of alphabet size for our
     *    two-pointer/DP approaches, so no asymptotic change; only the
     *    "next occurrence" precomputation table from Q2 would need a hash
     *    map instead of a fixed-size array per position.)
     *
     * 4. "Can you return ALL minimal-length windows, not just the leftmost?"
     *    (Track bestWindowLength as before, then do a second pass collecting
     *    every start index whose window length equals the minimum.)
     *
     * 5. "How would this change if we wanted s2 to appear as a CONTIGUOUS
     *    substring rather than a subsequence?"
     *    (That reduces to classic substring search -- KMP, Z-algorithm, or
     *    Rabin-Karp -- which is a fundamentally different, easier problem.)
     *
     * 6. "What's the largest input size your solution comfortably handles,
     *    and where would you expect it to start struggling?"
     *    (For n ~ 20,000 and m ~ 100, O(n*m) is ~2,000,000 operations --
     *    comfortably fast. If n grows to 10^6 with non-trivial m, the
     *    "next occurrence" precomputation from Q2 becomes necessary for
     *    a single query too.)
     */

    /* ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Using ">=" or non-strict comparisons when updating the best window,
     *    which silently breaks the "leftmost on tie" requirement by letting
     *    a later, equally-short window overwrite an earlier one.
     *
     * 2. Forgetting to reset patternPointer to 0 after a full match is found
     *    and processed -- without this reset, the next forward scan starts
     *    from a stale, already-matched pattern position and misses valid
     *    windows entirely.
     *
     * 3. Off-by-one errors in the backward contraction loop -- especially
     *    confusing "the index where the loop naturally stops" with "the
     *    actual start of the window," which is one position AFTER where the
     *    reverse match pointer goes negative.
     *
     * 4. Not handling degenerate inputs up front (empty pattern, empty text,
     *    or pattern longer than text) -- these can cause array index
     *    exceptions or incorrect infinite-window results if not special-cased
     *    before the main scan begins.
     */

    /* ========================================================================
     * TEST HARNESS: cross-validate all three approaches against each other
     * on the examples above (and a few extra edge cases).
     * ========================================================================
     */
    public static void main(String[] args) {
        record TestCase(String text, String pattern, String expected, String label) {}

        var testCases = List.of(
                new TestCase("abcdebdde", "bde", "bcde", "Normal case (from Section 3, Example 1)"),
                new TestCase("abc", "acb", "", "Edge case: impossible order (Example 2)"),
                new TestCase("abab", "ab", "ab", "Tie-breaking / boundary case (Example 3)"),
                new TestCase("", "a", "", "Empty text"),
                new TestCase("abc", "", "", "Empty pattern"),
                new TestCase("ab", "abc", "", "Pattern longer than text"),
                new TestCase("aaaaaaaaaa", "aaa", "aaa", "Highly repetitive (worst-case timing) input"),
                new TestCase("xyz", "xyz", "xyz", "Text equals pattern exactly")
        );

        int passCount = 0;
        for (TestCase testCase : testCases) {
            String bruteResult = bruteForce(testCase.text(), testCase.pattern());
            String twoPointerResult = twoPointerOptimal(testCase.text(), testCase.pattern());
            String dpResult = dpApproach(testCase.text(), testCase.pattern());
            String productionResult = findShortestSubsequenceWindow(testCase.text(), testCase.pattern());

            boolean allAgree = bruteResult.equals(twoPointerResult)
                    && twoPointerResult.equals(dpResult)
                    && dpResult.equals(productionResult);
            boolean matchesExpected = productionResult.equals(testCase.expected());

            String status = (allAgree && matchesExpected) ? "PASS" : "FAIL";
            if (status.equals("PASS")) {
                passCount++;
            }

            System.out.printf(
                    "[%s] %s%n    text=\"%s\" pattern=\"%s\"%n    brute=\"%s\" twoPointer=\"%s\" dp=\"%s\" production=\"%s\" expected=\"%s\"%n",
                    status, testCase.label(), testCase.text(), testCase.pattern(),
                    bruteResult, twoPointerResult, dpResult, productionResult, testCase.expected()
            );
        }

        System.out.printf("%n%d/%d test cases passed across all four implementations.%n",
                passCount, testCases.size());
    }

    public static String minWindow(String s1, String s2) {

        int n = s1.length();
        int m = s2.length();

        int minLen = Integer.MAX_VALUE;
        int startIndex = -1;

        int i = 0;

        while (i < n) {

            if (s1.charAt(i) == s2.charAt(0)) {

                int s1Index = i;
                int s2Index = 0;

                // Forward scan
                while (s1Index < n && s2Index < m) {
                    if (s1.charAt(s1Index) == s2.charAt(s2Index)) {
                        s2Index++;
                    }
                    s1Index++;
                }

                if (s2Index == m) {

                    int end = s1Index - 1;
                    s1Index = end;
                    s2Index = m - 1;

                    // Backward shrink
                    while (s2Index >= 0) {
                        if (s1.charAt(s1Index) == s2.charAt(s2Index)) {
                            s2Index--;
                        }
                        s1Index--;
                    }

                    int start = s1Index + 1;

                    if (end - start + 1 < minLen) {
                        minLen = end - start + 1;
                        startIndex = start;
                    }

                    // Resume search from the start of this window
                    i = start;
                }

            }

            i++;
        }

        return startIndex == -1 ? "" : s1.substring(startIndex, startIndex + minLen);
    }
}
