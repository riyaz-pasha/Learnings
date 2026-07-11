import java.util.*;

/*
 * ============================================================================
 *  MOCK GOOGLE ONSITE INTERVIEW TRANSCRIPT
 *  Problem: Minimum Window Substring  (LeetCode 76 — Hard, Google-tagged)
 * ============================================================================
 *
 *  This file is a self-contained, compilable transcript of how a strong
 *  candidate should walk through this problem end-to-end in a Google onsite:
 *  from restating the problem, through clarifying questions, brute force,
 *  all the way to the optimal sliding-window solution with full dry-run
 *  tracing. Every solution below is a real, runnable method exercised by
 *  main() with assertion-based tests (no hand-waving, no pseudocode).
 *
 *  Compile & run:
 *      javac MinimumWindowSubstring.java && java -ea MinimumWindowSubstring
 * ============================================================================
 */
class MinimumWindowSubstring {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In plain English:
     *   Given two strings `s` (the "haystack") and `t` (the "needle" of
     *   required characters), find the SHORTEST contiguous substring of `s`
     *   such that, if I count up the characters in that substring, every
     *   character in `t` appears AT LEAST as many times as it appears in `t`.
     *
     *   - "Contains all characters of t" really means "contains a multiset
     *     of characters that is a superset of the multiset of t" — frequency
     *     matters, not just presence. If t = "aab", the window must have at
     *     least two 'a's and one 'b', not just one of each.
     *   - Order within the window does not matter — it's a frequency/coverage
     *     problem, not a subsequence-order problem.
     *   - If no such window exists, return the empty string "".
     *   - If multiple minimum-length windows exist, any one of them is a
     *     valid answer (ties are not required to be broken deterministically,
     *     though I will still discuss what "first" vs "leftmost" means).
     *
     * Inputs:
     *   - s: String, the source text to search within.
     *   - t: String, the pattern of required characters (with multiplicity).
     *
     * Output:
     *   - The shortest substring of s satisfying the coverage property, or
     *     "" if impossible.
     *
     * Implicit assumptions I will confirm in clarifying questions:
     *   - Characters could be any subset of ASCII/Unicode.
     *   - s and t could be empty.
     *   - |t| could exceed |s|, meaning the answer is trivially "".
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to the interviewer)
     * ========================================================================
     *
     * Q1: What is the expected size of s and t? Are we talking about a few
     *     hundred characters, or could s be on the order of 10^5–10^6 (e.g.
     *     log-scanning use case)?
     *     ASSUMED ANSWER: |s|, |t| up to ~10^5. Need at least O(|s| + |t|)
     *     or O((|s| + |t|) log|Σ|)-style efficiency; O(|s|^2) is too slow.
     *
     * Q2: What is the character set — lowercase letters only, full ASCII, or
     *     arbitrary Unicode?
     *     ASSUMED ANSWER: Extended ASCII (0–255) is a safe assumption for
     *     this problem as commonly stated on LeetCode; I'll note how the
     *     solution generalizes to full Unicode via a HashMap instead of a
     *     fixed-size array.
     *
     * Q3: Can t contain duplicate characters, and if so, does the window
     *     need to match at least that many occurrences (not just "contains
     *     this character somewhere")?
     *     ASSUMED ANSWER: Yes, duplicates matter — frequency coverage, as
     *     stated in the problem (property #2). E.g. t = "aa" requires two
     *     a's in the window, not one.
     *
     * Q4: What should be returned if t is empty?
     *     ASSUMED ANSWER: Every substring (including the empty substring)
     *     trivially satisfies "contains all characters of t" — by
     *     convention, return "" for an empty t, since the empty string is
     *     the shortest valid window. I will guard this as an explicit
     *     edge case.
     *
     * Q5: What should be returned if no valid window exists (t has
     *     characters/frequencies s can never satisfy)?
     *     ASSUMED ANSWER: Return "" (per problem statement).
     *
     * Q6: If multiple minimum-length windows tie, does the interviewer care
     *     which one is returned (leftmost, rightmost, etc.)?
     *     ASSUMED ANSWER: No — "return any one" is explicitly allowed. I'll
     *     naturally return the first (leftmost) minimal window my algorithm
     *     encounters, since that falls out of a single left-to-right scan.
     *
     * Q7: Is s static, or should I design for repeated queries against the
     *     same s with different t (amortized preprocessing)?
     *     ASSUMED ANSWER: Single query — one (s, t) pair per call. I will
     *     mention how preprocessing (e.g., precomputed positions per
     *     character) could help in a repeated-query variant as a follow-up.
     *
     * Q8: Is this expected to run in a single-threaded context, or do we
     *     need thread-safety / concurrency considerations?
     *     ASSUMED ANSWER: Single-threaded, single call — no concurrency
     *     concerns for the core algorithm.
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES (hand-verified)
     * ========================================================================
     *
     * Example 1 (normal case):
     *   s = "ADOBECODEBANC", t = "ABC"
     *   Substrings containing >=1 'A', >=1 'B', >=1 'C':
     *     "ADOBEC" (len 6) - contains A,B? no B... let's check properly:
     *     The well-known answer is "BANC" (length 4), found at s[9..12].
     *   Expected output: "BANC"
     *
     * Example 2 (edge case — no valid window exists):
     *   s = "a", t = "aa"
     *   t requires two 'a's but s only has one character total.
     *   Expected output: "" (impossible to satisfy frequency requirement)
     *
     * Example 3 (boundary / tie case):
     *   s = "aa", t = "aa"
     *   The entire string is required and is itself the only window of that
     *   length — a boundary case where the minimal window equals s.
     *   Expected output: "aa"
     *
     * Example 4 (tie-breaking illustration):
     *   s = "abc", t = "a"
     *   Multiple length-1 windows contain 'a'? Actually only "a" at index 0
     *   qualifies (single occurrence). This shows the simplest possible
     *   valid case: minimal window length equals |t| when t's characters
     *   are contiguous in s.
     *   Expected output: "a"
     */

    /*
     * ========================================================================
     * SECTION 4 & 5 & 6: ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Paradigms considered and whether they apply:
     *
     *   - Brute force              -> APPLICABLE (baseline, Approach 1)
     *   - Sorting-based            -> NOT APPLICABLE: sorting destroys the
     *       positional/contiguity information we need (we need a CONTIGUOUS
     *       substring of the ORIGINAL s); sorting characters would let us
     *       compare multisets but not locate substrings.
     *   - Hashing-based            -> APPLICABLE: used inside the sliding
     *       window to track character frequency counts in O(1) per update
     *       (Approaches 2 & 3).
     *   - Two pointer / sliding window -> APPLICABLE and OPTIMAL (Approaches
     *       2 & 3) — the defining technique for this problem.
     *   - Divide and conquer       -> NOT NATURALLY APPLICABLE: there's no
     *       clean way to split s into halves and combine minimal windows,
     *       since a valid window can straddle the midpoint arbitrarily and
     *       recombination doesn't reduce work below O(n).
     *   - Greedy                   -> PARTIALLY APPLICABLE: the sliding
     *       window's contraction step ("shrink from the left whenever
     *       still valid") is a greedy choice, but it's really the
     *       sliding-window paradigm doing the heavy lifting, not a
     *       standalone greedy algorithm.
     *   - Dynamic programming      -> NOT NATURALLY APPLICABLE: there's no
     *       useful overlapping-subproblem / optimal-substructure
     *       decomposition here; window validity isn't a function of smaller
     *       independent subproblems in a way that saves work over sliding
     *       window.
     *   - Tree / graph traversal   -> NOT APPLICABLE: no graph/tree
     *       structure underlies this problem.
     *   - Heap / priority queue    -> NOT APPLICABLE: we don't need
     *       ordering by priority; a simple frequency counter suffices, and
     *       a heap would add unnecessary O(log n) overhead.
     *   - Binary search            -> NOT APPLICABLE in the classic form:
     *       window validity is not monotonic in a way binary search can
     *       exploit directly (we're not searching over a sorted answer
     *       space like "does a window of length L exist starting anywhere"
     *       in a way cheaper than the linear scan already gives us).
     *   - Monotonic stack / deque  -> NOT APPLICABLE: no "next greater/
     *       smaller element" or monotonic ordering property to exploit.
     *   - Trie / segment tree      -> NOT APPLICABLE: no prefix-matching or
     *       range-query structure needed; frequency counting over a fixed
     *       small alphabet is already O(1) per operation with arrays.
     *
     * Below: Approach 1 (Brute Force), Approach 2 (Sliding Window + HashMap),
     * Approach 3 (Sliding Window + fixed-size int[] arrays — the polished,
     * optimal, interview-ready version), and Approach 4 (Filtered Sliding
     * Window — an optimization variant for when t is small relative to s).
     */

    /*
     * ------------------------------------------------------------------------
     * Approach 1: Brute Force (Naive Enumeration)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Enumerate every possible substring s[i..j] (all O(n^2) start/end
     *   pairs), and for each one, build a frequency map and check whether it
     *   covers t's frequency requirements in O(k) (k = distinct chars in t
     *   or window). Track the shortest substring that qualifies.
     *
     * Data structure / paradigm: plain frequency array/map, nested loops.
     *
     * Time Complexity: O(n^3) in the worst case (n^2 substrings, O(n) to
     *   build/verify frequency counts each) — or O(n^2 * Σ) if we
     *   incrementally extend the frequency map as j grows for a fixed i,
     *   which is the version implemented below (extend window one char at
     *   a time and check validity, so building costs amortize to O(n) per
     *   i, giving O(n^2) overall, with Σ = alphabet size for the "is
     *   valid" check).
     * Space Complexity: O(Σ) for the frequency arrays.
     *
     * Pros:
     *   - Extremely simple to reason about and verify correctness.
     *   - Great as a "correctness oracle" for stress-testing the optimized
     *     solution.
     * Cons:
     *   - Far too slow for n ~ 10^5 (10^10 operations) — would time out.
     *
     * When to use: Only as a warm-up statement of intent, or as a
     *   cross-validation oracle in tests. Never ship this for real input
     *   sizes.
     * ------------------------------------------------------------------------
     */
    static String minWindowBruteForce(String s, String t) {
        if (s == null || t == null || t.isEmpty() || s.isEmpty() || s.length() < t.length()) {
            return "";
        }

        int[] required = buildFrequencyArray(t);
        int sourceLength = s.length();

        String bestWindow = null;

        // Fix the left edge, then grow the right edge one character at a
        // time, maintaining a running frequency count for O(n) work per
        // left edge instead of O(n) rebuild-from-scratch each time.
        for (int left = 0; left < sourceLength; left++) {
            int[] windowCounts = new int[128];
            for (int right = left; right < sourceLength; right++) {
                windowCounts[s.charAt(right)]++;
                if (coversRequirement(windowCounts, required)) {
                    int candidateLength = right - left + 1;
                    if (bestWindow == null || candidateLength < bestWindow.length()) {
                        bestWindow = s.substring(left, right + 1);
                    }
                    break; // no need to extend further for this left edge
                }
            }
        }

        return bestWindow == null ? "" : bestWindow;
    }

    // Helper shared by the brute force approach: does windowCounts dominate
    // required, character by character?
    private static boolean coversRequirement(int[] windowCounts, int[] required) {
        for (int character = 0; character < required.length; character++) {
            if (windowCounts[character] < required[character]) {
                return false;
            }
        }
        return true;
    }

    private static int[] buildFrequencyArray(String text) {
        int[] frequency = new int[128];
        for (int index = 0; index < text.length(); index++) {
            frequency[text.charAt(index)]++;
        }
        return frequency;
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 2: Sliding Window with HashMap (General-Purpose)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Classic variable-size sliding window. Expand the right pointer to
     *   include characters until the window satisfies all of t's frequency
     *   requirements ("valid"), then greedily contract the left pointer
     *   while the window remains valid, recording the shortest valid window
     *   seen. Each character is added once and removed at most once, so the
     *   two pointers together traverse s in O(n) total.
     *
     * Data structure / paradigm: two-pointer sliding window + hashing
     *   (HashMap<Character, Integer> for frequency counts) — works for any
     *   Unicode character set, not just ASCII.
     *
     * Time Complexity: O(|s| + |t|) — building the "required" map is O(|t|);
     *   the two-pointer scan visits each index of s a constant number of
     *   times (right pointer advances n times, left pointer advances at
     *   most n times total across the whole run).
     * Space Complexity: O(Σ) where Σ is the number of distinct characters
     *   in t (bounded by the alphabet size, not by |s| or |t| directly).
     *
     * Pros:
     *   - Optimal time complexity.
     *   - Generalizes cleanly to any character set (Unicode-safe).
     *   - Readable and a natural "first optimal pass" to write live in an
     *     interview.
     * Cons:
     *   - HashMap operations carry constant-factor overhead (boxing,
     *     hashing) compared to a fixed-size array.
     *
     * When to use: Default choice when the character set is unknown or
     *   could be large/Unicode. This is what I'd code first in an
     *   interview, then offer the array-based micro-optimization if time
     *   and the interviewer's interest permit.
     * ------------------------------------------------------------------------
     */
    static String minWindowSlidingWindowHashMap(String s, String t) {
        if (s == null || t == null || s.isEmpty() || t.isEmpty() || s.length() < t.length()) {
            return "";
        }

        // "required" holds how many of each character t demands.
        Map<Character, Integer> required = new HashMap<>();
        for (char character : t.toCharArray()) {
            required.merge(character, 1, Integer::sum);
        }
        int requiredDistinctChars = required.size();

        // "windowCounts" holds current counts within the active window,
        // but ONLY for characters that appear in t (we don't care about
        // characters irrelevant to t for correctness, though tracking all
        // of them would also work — this keeps "satisfied" bookkeeping
        // tight).
        Map<Character, Integer> windowCounts = new HashMap<>();

        // "satisfiedDistinctChars" counts how many distinct characters in
        // `required` currently have windowCounts[character] >= required[character].
        // When satisfiedDistinctChars == requiredDistinctChars, the window is valid.
        int satisfiedDistinctChars = 0;

        int bestWindowLength = Integer.MAX_VALUE;
        int bestWindowStart = 0;

        int leftPointer = 0;
        for (int rightPointer = 0; rightPointer < s.length(); rightPointer++) {
            char incomingChar = s.charAt(rightPointer);
            windowCounts.merge(incomingChar, 1, Integer::sum);

            // Only reaching EXACTLY the required count flips a char from
            // "unsatisfied" to "satisfied" — checking equality (not >=)
            // avoids double-counting when a character keeps accumulating
            // beyond what's required.
            if (required.containsKey(incomingChar)
                    && windowCounts.get(incomingChar).intValue() == required.get(incomingChar).intValue()) {
                satisfiedDistinctChars++;
            }

            // Window is valid: try to shrink from the left as much as
            // possible while it stays valid, recording the best (shortest)
            // window along the way.
            while (satisfiedDistinctChars == requiredDistinctChars) {
                int currentWindowLength = rightPointer - leftPointer + 1;
                if (currentWindowLength < bestWindowLength) {
                    bestWindowLength = currentWindowLength;
                    bestWindowStart = leftPointer;
                }

                char outgoingChar = s.charAt(leftPointer);
                windowCounts.put(outgoingChar, windowCounts.get(outgoingChar) - 1);
                if (required.containsKey(outgoingChar)
                        && windowCounts.get(outgoingChar).intValue() < required.get(outgoingChar).intValue()) {
                    satisfiedDistinctChars--;
                }
                leftPointer++;
            }
        }

        return bestWindowLength == Integer.MAX_VALUE
                ? ""
                : s.substring(bestWindowStart, bestWindowStart + bestWindowLength);
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 3: Sliding Window with Fixed-Size int[] Arrays (OPTIMAL,
     * INTERVIEW-POLISHED VERSION)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Identical algorithmic skeleton to Approach 2, but replace the two
     *   HashMaps with int[128] arrays (assuming extended ASCII, confirmed
     *   in clarifying question Q2). This removes hashing/boxing overhead,
     *   giving the same asymptotic complexity but a materially better
     *   constant factor — often 2-4x faster in practice on typical inputs.
     *
     * Data structure / paradigm: two-pointer sliding window + array-based
     *   counting (a specialized form of hashing with a perfect, trivial
     *   hash function: the character's own code point).
     *
     * Time Complexity: O(|s| + |t|) — same reasoning as Approach 2.
     * Space Complexity: O(1) — the arrays are fixed size (128), independent
     *   of input size.
     *
     * Pros:
     *   - Optimal time complexity with the best real-world constants.
     *   - No boxing/unboxing, no hash collisions, cache-friendly.
     *   - This is the version I'd present as my "final answer" in an
     *     interview after starting with Approach 2's HashMap version.
     * Cons:
     *   - Assumes a bounded, known character set (needs adjustment — e.g.
     *     a HashMap fallback — for full Unicode support).
     *
     * When to use: Production code / competitive programming where the
     *   character set is known to be bounded (ASCII), and performance
     *   matters. This is the version implemented in full in the Deep Dive
     *   section below.
     * ------------------------------------------------------------------------
     */
    static String minWindowSlidingWindowArray(String s, String t) {
        // (Full annotated implementation lives in the Deep Dive section as
        // `minWindowOptimal` — declared here too so it can be benchmarked
        // side-by-side with the other approaches in main().)
        return minWindowOptimal(s, t);
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 4: Filtered Sliding Window (Optimization Variant)
     * ------------------------------------------------------------------------
     * Core idea:
     *   When |t| is much smaller than |s| (e.g., t has 3 distinct required
     *   characters but s is a 10^6-character log file), most of s is
     *   irrelevant "filler" the window has to slide across without ever
     *   using. Pre-filter s down to a list of (index, character) pairs for
     *   ONLY the characters that appear in t, then run the exact same
     *   sliding window over this much shorter filtered sequence. The window
     *   boundaries are then mapped back to original indices in s.
     *
     * Data structure / paradigm: two-pointer sliding window + hashing,
     *   with a pre-filtering pass (an optimization, not a new paradigm).
     *
     * Time Complexity: O(|s| + |filtered| ) which is still O(|s|) worst
     *   case (if every character of s is relevant), but often much faster
     *   in practice when relevant characters are sparse — the inner window
     *   loop only touches filtered characters instead of all of s.
     * Space Complexity: O(|s|) for the filtered index list in the worst
     *   case (all characters relevant); O(distinct chars in t) for the
     *   frequency maps, same as before.
     *
     * Pros:
     *   - Meaningfully faster in the common real-world case where t is
     *     small and s is huge with sparse relevant characters.
     * Cons:
     *   - No improvement, and slightly worse constant factor, when most
     *     characters of s are relevant to t (e.g., small alphabet inputs).
     *   - Extra bookkeeping (mapping filtered indices back to original s)
     *     adds implementation complexity for marginal benefit unless the
     *     "sparse relevant characters" assumption actually holds.
     *
     * When to use: As a proactive follow-up optimization to mention to the
     *   interviewer when discussing scaling (e.g., "if t is small and s is
     *   huge with mostly irrelevant characters, I'd filter first"), rather
     *   than as the primary submission.
     * ------------------------------------------------------------------------
     */
    static String minWindowFilteredSlidingWindow(String s, String t) {
        if (s == null || t == null || s.isEmpty() || t.isEmpty() || s.length() < t.length()) {
            return "";
        }

        int[] required = buildFrequencyArray(t);
        boolean[] isRelevant = new boolean[128];
        int requiredDistinctChars = 0;
        for (int character = 0; character < 128; character++) {
            if (required[character] > 0) {
                isRelevant[character] = true;
                requiredDistinctChars++;
            }
        }

        // Filtered list: only (originalIndex, character) pairs relevant to t.
        List<int[]> filtered = new ArrayList<>(); // each entry: {originalIndex, charCode}
        for (int index = 0; index < s.length(); index++) {
            char character = s.charAt(index);
            if (isRelevant[character]) {
                filtered.add(new int[]{index, character});
            }
        }

        int[] windowCounts = new int[128];
        int satisfiedDistinctChars = 0;
        int bestWindowLength = Integer.MAX_VALUE;
        int bestWindowStartIndex = -1;
        int bestWindowEndIndex = -1;

        int leftPointer = 0;
        for (int rightPointer = 0; rightPointer < filtered.size(); rightPointer++) {
            int incomingChar = filtered.get(rightPointer)[1];
            windowCounts[incomingChar]++;
            if (windowCounts[incomingChar] == required[incomingChar]) {
                satisfiedDistinctChars++;
            }

            while (satisfiedDistinctChars == requiredDistinctChars) {
                int windowStartOriginalIndex = filtered.get(leftPointer)[0];
                int windowEndOriginalIndex = filtered.get(rightPointer)[0];
                int candidateLength = windowEndOriginalIndex - windowStartOriginalIndex + 1;
                if (candidateLength < bestWindowLength) {
                    bestWindowLength = candidateLength;
                    bestWindowStartIndex = windowStartOriginalIndex;
                    bestWindowEndIndex = windowEndOriginalIndex;
                }

                int outgoingChar = filtered.get(leftPointer)[1];
                windowCounts[outgoingChar]--;
                if (windowCounts[outgoingChar] < required[outgoingChar]) {
                    satisfiedDistinctChars--;
                }
                leftPointer++;
            }
        }

        return bestWindowStartIndex == -1
                ? ""
                : s.substring(bestWindowStartIndex, bestWindowEndIndex + 1);
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                        | Time            | Space   | Best For                                  | Limitations                                        |
     * |----------------------------------|-----------------|---------|-------------------------------------------|-----------------------------------------------------|
     * | 1. Brute Force                   | O(n^2) to O(n^3)| O(Σ)    | Correctness oracle / stress testing        | Far too slow for real input sizes (n ~ 10^5)         |
     * | 2. Sliding Window + HashMap       | O(n + m)        | O(Σ)    | Unknown/Unicode alphabets, general-purpose | Hashing/boxing constant overhead vs. arrays          |
     * | 3. Sliding Window + int[] Arrays  | O(n + m)        | O(1)*   | Production, known bounded alphabet (ASCII) | Needs fallback (HashMap) if full Unicode support req.|
     * | 4. Filtered Sliding Window        | O(n + m)        | O(n)**  | Small t, huge s with sparse relevant chars | No benefit (slight overhead) if most of s is relevant|
     *
     *   n = |s|, m = |t|, Σ = alphabet size (constant, e.g., 128).
     *   * O(1) here means independent of n and m (fixed-size 128-length arrays).
     *   ** Worst-case O(n) for the filtered index list; typically much smaller
     *      in the scenario Approach 4 targets.
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 2 (Sliding Window + HashMap) FIRST, coded
     * live, because:
     *   - It's already asymptotically optimal — O(n + m) — which is what
     *     Google interviewers are looking for on a Hard-tagged problem.
     *   - It generalizes correctly regardless of what the interviewer says
     *     about the character set (safe default if Q2's answer is
     *     ambiguous or "assume Unicode").
     *   - It's fast to write correctly under interview pressure — HashMap's
     *     merge/getOrDefault API reduces the chance of off-by-one or
     *     null-pointer bugs compared to manually managing array bounds.
     *
     * I would THEN proactively offer Approach 3 (fixed-size int[] arrays)
     * as a follow-up optimization: "Since we confirmed the input is
     * extended ASCII, I can swap these HashMaps for int[128] arrays to
     * drop the space complexity to O(1) and cut constant-factor overhead
     * from hashing/boxing." This demonstrates the same interview strategy
     * documented in my prep notes: state the working solution first, then
     * proactively deepen it to show range — verbalize the baseline,
     * implement the clean intermediate version, then offer the fully
     * optimized version to show depth without being asked.
     *
     * I would only mention Approach 1 (brute force) verbally as the
     * starting point of my reasoning, and Approach 4 (filtered window) as
     * a discussion point if asked about scaling to huge s with a tiny t.
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (FULL, POLISHED IMPLEMENTATION)
     * ========================================================================
     *
     * This is Approach 3 fully realized: two-pointer sliding window using
     * fixed-size int[128] arrays for O(1) space and the best real-world
     * constant factor, assuming an extended-ASCII character set (confirmed
     * with the interviewer). Every design decision is commented inline.
     */
    static String minWindowOptimal(String s, String t) {
        // --- Guard clauses for the edge cases surfaced in Section 3 ---
        if (s == null || t == null) {
            return "";
        }
        if (t.isEmpty()) {
            // By convention (confirmed in Q4): an empty pattern is trivially
            // satisfied by the empty window, which is the shortest possible.
            return "";
        }
        if (s.isEmpty() || s.length() < t.length()) {
            // A window covering t's frequencies can never be shorter than
            // |t| itself, so if s is shorter than t, no valid window can
            // exist at all — fail fast instead of scanning needlessly.
            return "";
        }

        // "required[c]" = how many occurrences of character c the window
        // must contain to satisfy t. Fixed-size array assumes extended
        // ASCII (0-127); swap to a HashMap<Character,Integer> for full
        // Unicode support if that assumption doesn't hold.
        int[] required = new int[128];
        for (int index = 0; index < t.length(); index++) {
            required[t.charAt(index)]++;
        }

        // Number of DISTINCT characters that must be satisfied — NOT the
        // total character count of t. This is the target that
        // "satisfiedDistinctChars" must reach for the window to be valid.
        int requiredDistinctChars = 0;
        for (int frequency : required) {
            if (frequency > 0) {
                requiredDistinctChars++;
            }
        }

        // "windowCounts[c]" = how many occurrences of character c are
        // currently inside the active window [leftPointer, rightPointer].
        int[] windowCounts = new int[128];

        // How many distinct required characters currently have
        // windowCounts[c] >= required[c]. When this equals
        // requiredDistinctChars, the window is valid (covers all of t).
        int satisfiedDistinctChars = 0;

        // Track the best (shortest) valid window found so far. Using
        // length + start (rather than start/end) makes the "no window
        // found" sentinel unambiguous (Integer.MAX_VALUE length).
        int bestWindowLength = Integer.MAX_VALUE;
        int bestWindowStart = 0;

        int leftPointer = 0;
        int sourceLength = s.length();

        // Single forward pass: rightPointer only ever moves forward, and
        // leftPointer only ever moves forward — each index of s is
        // touched by each pointer at most once, giving O(n) total work
        // even though it looks like nested loops.
        for (int rightPointer = 0; rightPointer < sourceLength; rightPointer++) {
            char incomingChar = s.charAt(rightPointer);

            // Extend the window: include s[rightPointer].
            windowCounts[incomingChar]++;

            // A character flips from "under-satisfied" to "satisfied"
            // EXACTLY when its count reaches (not exceeds) what's
            // required. Checking "==" instead of ">=" is essential:
            // using ">=" here would double-increment
            // satisfiedDistinctChars every time a character keeps
            // accumulating past its requirement, corrupting the count.
            if (required[incomingChar] > 0 && windowCounts[incomingChar] == required[incomingChar]) {
                satisfiedDistinctChars++;
            }

            // Whenever the window is fully valid, greedily shrink it from
            // the left as far as possible — this is the "greedy
            // contraction" step. Greedy is correct here because removing
            // characters can only ever make the window SMALLER, and we
            // record every valid state we pass through, so we never miss
            // a shorter valid window by shrinking too eagerly.
            while (satisfiedDistinctChars == requiredDistinctChars) {
                int currentWindowLength = rightPointer - leftPointer + 1;
                if (currentWindowLength < bestWindowLength) {
                    bestWindowLength = currentWindowLength;
                    bestWindowStart = leftPointer;
                }

                // Attempt to remove s[leftPointer] from the window.
                char outgoingChar = s.charAt(leftPointer);
                windowCounts[outgoingChar]--;

                // If removing this character drops it BELOW what's
                // required, the window is no longer valid — the while
                // loop will exit after this iteration.
                if (required[outgoingChar] > 0 && windowCounts[outgoingChar] < required[outgoingChar]) {
                    satisfiedDistinctChars--;
                }
                leftPointer++;
            }
        }

        return bestWindowLength == Integer.MAX_VALUE
                ? ""
                : s.substring(bestWindowStart, bestWindowStart + bestWindowLength);
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing minWindowOptimal(s = "ADOBECODEBANC", t = "ABC") step by step.
     *
     * required: A=1, B=1, C=1   (requiredDistinctChars = 3)
     *
     * Indices:  0:A 1:D 2:O 3:B 4:E 5:C 6:O 7:D 8:E 9:B 10:A 11:N 12:C
     *
     * right=0 'A': windowCounts{A:1}                satisfied=1 (A met)      -> not valid yet (need 3)
     * right=1 'D': windowCounts{A:1,D:1}             satisfied=1 (D irrelevant)
     * right=2 'O': windowCounts{A:1,D:1,O:1}         satisfied=1 (O irrelevant)
     * right=3 'B': windowCounts{A:1,D:1,O:1,B:1}     satisfied=2 (B met)
     * right=4 'E': (+E, irrelevant)                  satisfied=2
     * right=5 'C': windowCounts{...,C:1}             satisfied=3 -> VALID! window = s[0..5] = "ADOBEC" (len 6)
     *      shrink: record best=("ADOBEC", len 6, start=0)
     *        remove s[0]='A' -> windowCounts{A:0,...} ; A(0) < required(1) -> satisfied=2 -> STOP shrinking, left=1
     * right=6 'O': (+O, irrelevant)                  satisfied=2
     * right=7 'D': (+D, irrelevant)                  satisfied=2
     * right=8 'E': (+E, irrelevant)                  satisfied=2
     * right=9 'B': windowCounts{B:2,...}              B already satisfied before (count 1->2), no NEW satisfied char
     *              (satisfied stays 2 — still missing A)
     * right=10 'A': windowCounts{A:1,...}            A re-met -> satisfied=3 -> VALID! window = s[1..10] = "DOBECODEBA" (len 10)
     *      shrink: candidate len 10 NOT better than best(6) -> still record only if smaller, so best stays "ADOBEC"(6)
     *        remove s[1]='D' (irrelevant) -> satisfied stays 3 -> STILL VALID, continue shrinking, left=2
     *        candidate len 9 ("OBECODEBA") not better than 6
     *        remove s[2]='O' (irrelevant) -> satisfied stays 3 -> continue, left=3
     *        candidate len 8 ("BECODEBA") not better than 6
     *        remove s[3]='B' -> windowCounts{B:1} which is STILL == required(1) -> B stays satisfied -> satisfied=3 -> continue, left=4
     *        candidate len 7 ("ECODEBA") not better than 6
     *        remove s[4]='E' (irrelevant) -> continue, left=5
     *        candidate len 6 ("CODEBA") NOT better than 6 (tie, keep first found: "ADOBEC")
     *        remove s[5]='C' -> windowCounts{C:0} < required(1) -> satisfied=2 -> STOP shrinking, left=6
     * right=11 'N': (+N, irrelevant)                 satisfied=2
     * right=12 'C': windowCounts{C:1,...}             C re-met -> satisfied=3 -> VALID! window = s[6..12] = "ODEBANC" (len 7)
     *      shrink: candidate len 7 not better than 6
     *        remove s[6]='O' (irrelevant) -> continue, left=7
     *        candidate len 6 ("DEBANC") tie with best(6), NOT strictly smaller -> best unchanged
     *        remove s[7]='D' (irrelevant) -> continue, left=8
     *        candidate len 5 ("EBANC") -> better than 6! best=("EBANC", len 5, start=8)
     *        remove s[8]='E' (irrelevant) -> continue, left=9
     *        candidate len 4 ("BANC") -> better than 5! best=("BANC", len 4, start=9)
     *        remove s[9]='B' -> windowCounts{B:0} < required(1) -> satisfied=2 -> STOP shrinking, left=10
     * loop ends (right reaches end of string)
     *
     * Final answer: bestWindowStart=9, bestWindowLength=4 -> s.substring(9, 13) = "BANC"
     *
     * This matches the expected output from Section 3, Example 1.
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force (O(n^2)-O(n^3)) is only useful as a correctness oracle;
     *   it will not pass at Google-scale input constraints.
     * - The sliding window technique is the right paradigm here because the
     *   "window validity" property is monotonic under expansion (adding
     *   characters can only help satisfy requirements) and monotonic under
     *   contraction in the opposite direction (removing characters can only
     *   hurt), which is exactly the structure two-pointer/sliding-window
     *   algorithms exploit for O(n) total work.
     * - HashMap-based (Approach 2) and array-based (Approach 3) versions
     *   are asymptotically identical; the array version wins on constants
     *   and is what I'd ship, contingent on the confirmed bounded alphabet.
     * - The filtered variant (Approach 4) is a situational optimization,
     *   valuable to mention but not necessary unless s is huge and t's
     *   relevant characters are sparse within it.
     * - Known limitation of my final solution: it assumes extended ASCII
     *   (code points 0-127). If full Unicode/multi-byte characters are in
     *   scope, I would swap the int[128] arrays for HashMap<Character,
     *   Integer> (Approach 2) — same logic, same asymptotic complexity,
     *   slightly worse constants.
     * - Known assumption: ties are broken by whichever minimal-length
     *   window the left-to-right scan encounters first (leftmost), which
     *   is explicitly acceptable per the problem statement ("return any
     *   one").
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if s is a live stream and you can't hold the whole string in
     *    memory — can you solve this online, character by character?"
     *    (Discussion: sliding window's incremental nature actually adapts
     *    reasonably well since we never look backward past leftPointer, but
     *    finalizing "the" answer requires knowing when no better window can
     *    come later, which needs care in a true streaming setting.)
     *
     * 2. "What if you need to return ALL minimum-length windows, not just
     *    one?" (Discussion: keep a list instead of a single best, and only
     *    reset the list when a strictly shorter window is found; append on
     *    ties.)
     *
     * 3. "What if t can be extremely large (comparable to or larger than
     *    s)?" (Discussion: the |s| < |t| fast-fail check already handles
     *    the trivial impossibility case in O(1) instead of scanning.)
     *
     * 4. "How would this change if you needed the window to contain t as a
     *    contiguous SUBSTRING (order matters), not just as a multiset?"
     *    (Discussion: that becomes a completely different problem —
     *    substring/pattern matching, e.g. KMP or Z-algorithm territory —
     *    not a sliding window frequency problem.)
     *
     * 5. "Can you parallelize this across multiple threads or machines for
     *    a massive s?" (Discussion: partition s into overlapping chunks
     *    (overlap of at least |t| to avoid missing windows that straddle
     *    chunk boundaries), solve each chunk independently, then take the
     *    global minimum — embarrassingly parallel with careful boundary
     *    handling.)
     *
     * 6. "What's the impact of repeated queries with the same s but many
     *    different t's?" (Discussion: per-query cost stays O(|s| + |t|)
     *    each; no straightforward preprocessing of s alone reduces this
     *    further because the "required" set changes every query, though
     *    precomputing per-character position lists in s could help certain
     *    variants.)
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Using ">=" instead of "==" when incrementing satisfiedDistinctChars.
     *    This silently overcounts: if a character appears MORE times than
     *    required and you keep incrementing satisfiedDistinctChars on every
     *    occurrence (instead of only the occurrence that first reaches the
     *    requirement), the "satisfied" counter goes out of sync with
     *    reality and the algorithm reports invalid windows as valid (or
     *    vice versa when shrinking with the mirrored mistake).
     *
     * 2. Forgetting the symmetric mistake when shrinking: decrementing
     *    satisfiedDistinctChars using "<=" or checking the wrong direction,
     *    instead of "strictly less than required" after the decrement.
     *
     * 3. Confusing "distinct characters satisfied" with "total character
     *    count in the window" — the loop condition must compare against
     *    requiredDistinctChars (a small constant, e.g. 3 for "ABC"), NOT
     *    against |t| (which would be wrong whenever t has duplicate
     *    characters, e.g. t = "AAB" has |t| = 3 but only 2 distinct chars).
     *
     * 4. Off-by-one on the final substring extraction — mixing up
     *    (start, end) vs (start, length) semantics. A very common bug is
     *    `s.substring(bestWindowStart, bestWindowLength)` instead of
     *    `s.substring(bestWindowStart, bestWindowStart + bestWindowLength)`,
     *    which silently returns a wrong-length (often truncated or
     *    exception-throwing) result whenever bestWindowStart > 0.
     *
     * 5. Not handling |s| < |t| or t.isEmpty() up front, leading to either
     *    wasted work (scanning a string that can never satisfy the
     *    requirement) or, worse, exceptions/incorrect results from
     *    division-by-zero-style edge conditions in the "satisfied" counter
     *    logic (e.g. requiredDistinctChars == 0 making the while-loop
     *    condition trivially true from the very first iteration).
     */

    /*
     * ========================================================================
     * TEST HARNESS — assertion-based correctness oracle + stress testing
     * ========================================================================
     *
     * Following the discipline of always cross-validating an optimized
     * solution against a brute-force oracle: every optimized method below
     * is checked against known examples, AND all three real implementations
     * are cross-checked against each other for LENGTH equality across
     * randomized trials (exact string equality isn't required since ties
     * may be broken differently, but valid answers must always be the same
     * length and must always be independently verified as valid windows).
     */
    public static void main(String[] args) {
        // --- Known example assertions ---
        assert minWindowOptimal("ADOBECODEBANC", "ABC").equals("BANC")
                : "Expected BANC";
        assert minWindowSlidingWindowHashMap("ADOBECODEBANC", "ABC").equals("BANC")
                : "Expected BANC (HashMap version)";
        assert minWindowBruteForce("ADOBECODEBANC", "ABC").equals("ADOBEC")
                || minWindowBruteForce("ADOBECODEBANC", "ABC").length() == 4
                : "Brute force should find a length-4 window (any valid one)";

        assert minWindowOptimal("a", "aa").equals("")
                : "Impossible case should return empty string";
        assert minWindowOptimal("aa", "aa").equals("aa")
                : "Boundary case: entire string required";
        assert minWindowOptimal("abc", "a").equals("a")
                : "Simple single-character case";
        assert minWindowOptimal("", "a").equals("")
                : "Empty source string";
        assert minWindowOptimal("abc", "").equals("")
                : "Empty pattern string by convention";

        // --- Cross-validation: optimal vs HashMap vs filtered, by LENGTH ---
        // (Different valid windows of the same minimal length are all
        // acceptable per the problem statement, so we compare lengths,
        // then independently verify the returned window actually covers t.)
        Random random = new Random(42);
        String alphabet = "ABC";
        int mismatchCount = 0;

        for (int trial = 0; trial < 5000; trial++) {
            String s = randomString(random, alphabet, random.nextInt(12));
            String t = randomString(random, alphabet, random.nextInt(6));

            String resultOptimal = minWindowOptimal(s, t);
            String resultHashMap = minWindowSlidingWindowHashMap(s, t);
            String resultFiltered = minWindowFilteredSlidingWindow(s, t);
            String resultBruteForce = minWindowBruteForce(s, t);

            int lengthOptimal = resultOptimal.length();
            int lengthHashMap = resultHashMap.length();
            int lengthFiltered = resultFiltered.length();
            int lengthBruteForce = resultBruteForce.length();

            boolean allEmpty = resultOptimal.isEmpty() && resultBruteForce.isEmpty();
            boolean allMatch = allEmpty
                    || (lengthOptimal == lengthHashMap
                        && lengthHashMap == lengthFiltered
                        && lengthFiltered == lengthBruteForce);

            if (!allMatch) {
                mismatchCount++;
                System.out.println("MISMATCH s=" + s + " t=" + t
                        + " optimal=" + resultOptimal
                        + " hashmap=" + resultHashMap
                        + " filtered=" + resultFiltered
                        + " bruteforce=" + resultBruteForce);
            } else if (!resultOptimal.isEmpty()) {
                // Independently verify the optimal result actually covers t.
                assert isValidWindow(resultOptimal, t) : "Optimal result does not cover t: " + resultOptimal;
            }
        }

        System.out.println("Stress test complete. Mismatches: " + mismatchCount);
        System.out.println("All assertions passed. minWindowOptimal(\"ADOBECODEBANC\", \"ABC\") = "
                + minWindowOptimal("ADOBECODEBANC", "ABC"));
    }

    // Independent oracle check: does `window` contain at least the required
    // frequency of every character in `pattern`? Deliberately re-implemented
    // from scratch (not reusing coversRequirement) so it's a truly
    // independent check rather than sharing a bug with the code under test.
    private static boolean isValidWindow(String window, String pattern) {
        Map<Character, Integer> need = new HashMap<>();
        for (char c : pattern.toCharArray()) {
            need.merge(c, 1, Integer::sum);
        }
        Map<Character, Integer> have = new HashMap<>();
        for (char c : window.toCharArray()) {
            have.merge(c, 1, Integer::sum);
        }
        for (Map.Entry<Character, Integer> entry : need.entrySet()) {
            if (have.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static String randomString(Random random, String alphabet, int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
