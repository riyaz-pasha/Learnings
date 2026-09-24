import java.util.HashMap;
import java.util.Random;

/*
 * ================================================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: "Substrings With At Least One Character Appearing K Times"
 * ================================================================================================
 *
 * PROBLEM STATEMENT (as given):
 * Given a string `s` and an integer `k`, return the total number of substrings of `s` where
 * at least one character appears at least `k` times.
 *
 * This file walks through the full interview lifecycle: restatement, clarifying questions,
 * examples, an exhaustive sweep of approaches (naive -> optimal), a comparison table, the
 * recommended approach, a production-quality deep dive, a manual dry run, follow-ups, and
 * common candidate mistakes.
 * ================================================================================================
 */
class SubstringsWithCharAtLeastKTimes {

    /*
     * ============================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ============================================================================================
     *
     * In plain English: I'm given a string `s` of length n, and an integer `k`. I need to count
     * how many contiguous substrings s[i..j] (0 <= i <= j < n) have the property that AT LEAST
     * ONE distinct character occurring in that substring appears with frequency >= k within that
     * substring.
     *
     * Key points to confirm:
     *   - "Substring" means contiguous (not subsequence). "edu" is a substring of "educative",
     *     but "eu" is not.
     *   - Substrings are counted by (start, end) INDEX PAIRS, not by distinct string values.
     *     E.g., in "aa", the substring "a" occurs twice (indices [0,0] and [1,1]) and both count
     *     separately unless told otherwise.
     *   - The condition is an OR across characters within the substring: we need >=1 character
     *     whose count within that specific substring reaches k. We do NOT need every character
     *     to reach k (that would be a different, harder problem -- see LeetCode 395 for contrast).
     *   - Output is a single integer count (I'll use `long` -- justified in Clarifying Questions).
     *
     * Inputs: String s, int k.
     * Output: long (or int, per interviewer's return-type convention) -- total qualifying
     *         substrings.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ============================================================================================
     *
     * 1. Q: What is the character set of `s` -- lowercase English letters only, or could it
     *       include uppercase, digits, unicode, etc.?
     *    A (assumed): Lowercase English letters 'a'-'z' only (26 distinct characters). This is
     *       the standard constraint for this problem family; the approaches generalize trivially
     *       to a larger fixed alphabet (e.g. 128 for ASCII) by widening the frequency array.
     *
     * 2. Q: What is the maximum length of `s`? This affects which approaches are viable.
     *    A (assumed): n can be up to ~10^5, so we need at least an O(n log n) solution, and
     *       ideally O(n), to be safe.
     *
     * 3. Q: Can k be 0?
     *    A (assumed): No, 1 <= k <= n. If k == 0 were allowed, every substring (including the
     *       empty substring) would trivially qualify -- not an interesting case, and not part of
     *       standard constraints. I will still defensively handle k <= 1 in code (k == 1 means
     *       every non-empty substring qualifies, since any character in it appears >= 1 time).
     *
     * 4. Q: Can k exceed n (the length of the string)?
     *    A (assumed): Yes, this is possible and simply means the answer is 0 (no substring can
     *       be long enough to have a character repeat k times if k > n). The algorithm should
     *       handle this gracefully without special-casing.
     *
     * 5. Q: Should the output be an int or a long? For n ~ 10^5, the total number of substrings
     *       is n*(n+1)/2, which can approach ~5*10^9 -- this overflows a 32-bit int.
     *    A (assumed): Return type should be `long` to avoid overflow.
     *
     * 6. Q: Is `s` guaranteed non-null and non-empty?
     *    A (assumed): `s` is non-null, but I will defensively handle the empty-string case
     *       (returns 0) and a null input (throws IllegalArgumentException) in the production
     *       version.
     *
     * 7. Q: Do we need to support streaming input / is this a single-threaded, in-memory
     *       computation, or does `s` need to be processed incrementally (e.g., as characters
     *       arrive)?
     *    A (assumed): Single-threaded, `s` fully resident in memory. No concurrency concerns.
     *
     * 8. Q: Are we counting substrings by index-position (multiplicity) or counting DISTINCT
     *       substring VALUES that satisfy the property (deduplicated)?
     *    A (assumed): By index position -- i.e., every (i, j) pair is counted once, even if two
     *       different (i, j) pairs produce identical substring text. This matches the standard
     *       problem definition and the "total number of substrings" phrasing.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ============================================================================================
     *
     * --- Example 1 (normal case): s = "abacb", k = 2 ---
     * Index:      0 1 2 3 4
     * Characters: a b a c b
     *
     * Enumerate all 15 substrings (n(n+1)/2 = 5*6/2 = 15) and check which have a char with
     * frequency >= 2 within that substring:
     *   Length 1: "a","b","a","c","b"           -> none qualify (max freq = 1 each)
     *   Length 2: "ab","ba","ac","cb"            -> none qualify (all distinct chars)
     *   Length 3: "aba"(a:2,b:1) QUALIFIES
     *             "bac"(b:1,a:1,c:1) no
     *             "acb"(a:1,c:1,b:1) no
     *   Length 4: "abac"(a:2,b:1,c:1) QUALIFIES
     *             "bacb"(b:2,a:1,c:1) QUALIFIES
     *   Length 5: "abacb"(a:2,b:2,c:1) QUALIFIES
     *   Total qualifying = 1 + 2 + 1 = 4.
     * Expected answer: 4.
     *
     * --- Example 2 (boundary / tie-breaking case): s = "aaaa", k = 2 ---
     * Every substring of length >= 2 automatically has 'a' appearing >= 2 times (since ALL
     * characters are 'a'). Substrings of length 1 never qualify (freq = 1 < 2).
     *   Total substrings = 4*5/2 = 10.
     *   Length-1 substrings that DON'T qualify = 4.
     *   Answer = 10 - 4 = 6.
     * This is a good boundary case because it stresses "every sufficiently long substring
     * qualifies" -- useful for sanity-checking the complement-counting technique used in the
     * optimal solution.
     *
     * --- Example 3 (edge case): s = "a", k = 2 ---
     * Only one substring exists: "a", with frequency 1 < 2. Answer = 0.
     * This also covers: what happens when k > n entirely (here k=2 > n=1).
     *
     * --- Additional edge cases worth mentioning out loud in the interview ---
     *   - k == 1: every non-empty substring qualifies (any character present appears >= 1 time).
     *     Answer = n*(n+1)/2 directly, no scanning needed.
     *   - Empty string (n == 0): answer is 0 (no substrings exist).
     *   - All distinct characters (e.g., "abcde", k=2): answer = 0, since no character can ever
     *     repeat within any substring.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (naive -> optimal), across applicable paradigms
     * ============================================================================================
     *
     * Paradigms considered and explicitly ruled out (stating WHY, as a good candidate should):
     *   - Dynamic Programming: There's no clean optimal-substructure decomposition for an
     *     "at least one character reaches k" OR-condition over arbitrary contiguous ranges beyond
     *     what the two-pointer technique already captures with O(1) state; a DP table over
     *     (left, right) would just be an O(n^2) re-derivation of the brute force with no
     *     additional insight or savings.
     *   - Greedy: There's no single locally-optimal choice to make -- we need an exact count of
     *     all qualifying ranges, not an optimization of one quantity.
     *   - Heap / Priority Queue: No ordering/selection-by-priority need exists; frequencies
     *     aren't being compared against each other to extract a max/min repeatedly.
     *   - Trie / Segment Tree: A Trie is for prefix relationships across multiple strings/words --
     *     not applicable to counting substrings of ONE string by frequency. A segment tree could
     *     support range-frequency queries with updates, but this problem is static (no mutation),
     *     so it's pure overkill versus the O(n) two-pointer solution.
     *   - Monotonic Stack / Deque: Monotonic stacks solve "next greater/smaller element" style
     *     problems; there's no such relation here. (Note: the optimal solution DOES use a
     *     monotonic *pointer*, which is a different technique -- two-pointer/sliding-window.)
     *   - Tree / Graph traversal: The input is a flat string with no tree/graph structure implied.
     *   - Divide & Conquer: This technique shines on the RELATED-BUT-DIFFERENT problem "every
     *     character in the substring must appear >= k times" (LeetCode 395), where you can split
     *     the string at any character whose total frequency is < k (that character can never be
     *     part of a valid answer, so it's a safe splitting point). For OUR problem (an OR/"at
     *     least one" condition), a substring spanning across such a split point can still
     *     independently satisfy the condition, so the recursive decomposition doesn't cleanly
     *     apply. I mention this to show awareness of the distinction, but don't pursue it further.
     *
     * Paradigms that ARE genuinely applicable, from naive to optimal:
     *   Approach 1: Brute Force (Naive)            -- brute force / hashing
     *   Approach 2: Optimized Brute Force            -- brute force + monotonicity shortcut
     *   Approach 3: Prefix Sums + Binary Search       -- hashing/prefix-sum + binary search
     *   Approach 4: Sliding Window / Two-Pointer      -- two-pointer, complement counting (OPTIMAL)
     * ============================================================================================
     */

    /*
     * --------------------------------------------------------------------------------------------
     * APPROACH 1: Brute Force (Naive) -- "Hashing" dimension
     * --------------------------------------------------------------------------------------------
     * Core idea: Enumerate every (i, j) substring explicitly. For each one, build a frequency
     * map (HashMap<Character,Integer>) from scratch and check whether any value reaches k.
     *
     * Data structures: HashMap for character frequency counting.
     *
     * Time Complexity: O(n^3) worst case -- O(n^2) substrings, and O(n) work to build/scan the
     *   frequency map for each one (map operations add a constant-ish but non-trivial overhead
     *   over an array).
     * Space Complexity: O(n) for the frequency map of the current substring (auxiliary), not
     *   counting output.
     *
     * Pros: Trivial to reason about and verify by hand; great as a correctness oracle for
     *   cross-validating faster approaches.
     * Cons: Far too slow for any n beyond a few hundred; wasteful re-computation (doesn't reuse
     *   work between overlapping substrings).
     * When to use: Only as a first-pass sanity check / correctness oracle in an interview, or for
     *   tiny inputs. Never in production.
     * --------------------------------------------------------------------------------------------
     */
    public static long bruteForceNaive(String s, int k) {
        int n = s.length();
        long qualifyingCount = 0;

        for (int start = 0; start < n; start++) {
            for (int end = start; end < n; end++) {
                HashMap<Character, Integer> frequencyMap = new HashMap<>();
                for (int index = start; index <= end; index++) {
                    frequencyMap.merge(s.charAt(index), 1, Integer::sum);
                }
                boolean hasCharacterReachingK = false;
                for (int frequency : frequencyMap.values()) {
                    if (frequency >= k) {
                        hasCharacterReachingK = true;
                        break;
                    }
                }
                if (hasCharacterReachingK) {
                    qualifyingCount++;
                }
            }
        }
        return qualifyingCount;
    }

    /*
     * --------------------------------------------------------------------------------------------
     * APPROACH 2: Optimized Brute Force -- exploits monotonicity of frequency as window grows
     * --------------------------------------------------------------------------------------------
     * Core idea: For a fixed `start`, as `end` increases, character counts only ever increase
     * (never decrease). So once some character's count reaches k for a given `start`, EVERY
     * larger `end` also qualifies (the substring only grows, so that character's count stays
     * >= k). This means for each `start`, we only need to find the FIRST `end` at which the
     * condition becomes true, then all `(n - end)` substrings from that point onward for this
     * `start` qualify in bulk -- no need to check them individually.
     *
     * Data structures: A single int[26] frequency array reused (reset) per `start`.
     *
     * Time Complexity: O(n^2) worst case (e.g., k very large or never satisfied, so the inner
     *   loop runs to completion for every `start`). In practice often much faster due to early
     *   termination, but worst-case bound is still quadratic.
     * Space Complexity: O(1) -- fixed-size 26-length array, independent of n.
     *
     * Pros: Much simpler to code correctly than the prefix-sum/binary-search approach; avoids
     *   HashMap overhead entirely; easy to explain the "extend, stop at first success, count
     *   the rest in bulk" insight -- a good "safe intermediate" to present before optimizing
     *   further.
     * Cons: Still O(n^2) worst case -- won't pass for n ~ 10^5.
     * When to use: Good stepping-stone solution to present first in an interview (shows the key
     *   monotonicity insight) before pivoting to full O(n). Also fine for n up to a few thousand
     *   in practice.
     * --------------------------------------------------------------------------------------------
     */
    public static long bruteForceOptimized(String s, int k) {
        int n = s.length();
        long qualifyingCount = 0;
        final int ALPHABET_SIZE = 26;

        for (int start = 0; start < n; start++) {
            int[] frequency = new int[ALPHABET_SIZE];
            int firstQualifyingEnd = -1;

            for (int end = start; end < n; end++) {
                int charIndex = s.charAt(end) - 'a';
                frequency[charIndex]++;
                if (frequency[charIndex] >= k) {
                    firstQualifyingEnd = end;
                    break; // Monotonicity: every larger `end` also qualifies from here on.
                }
            }

            if (firstQualifyingEnd != -1) {
                // All substrings [start, firstQualifyingEnd] through [start, n-1] qualify.
                qualifyingCount += (n - firstQualifyingEnd);
            }
        }
        return qualifyingCount;
    }

    /*
     * --------------------------------------------------------------------------------------------
     * APPROACH 3: Prefix Frequency Sums + Binary Search -- "binary search" dimension
     * --------------------------------------------------------------------------------------------
     * Core idea: Precompute, for each of the 26 letters, a prefix-sum array so that the count of
     * character c within s[left..right] is answerable in O(1) via prefix[c][right+1] -
     * prefix[c][left]. Then, for a FIXED `left`, the predicate "does s[left..end] contain a
     * character with frequency >= k?" is monotonic in `end` (false, false, ..., false, true,
     * true, ..., true) -- exactly the shape binary search needs. So for each `left`, binary
     * search over `end` in [left, n-1] for the first index where the predicate flips to true;
     * every substring from that boundary through n-1 (for this `left`) qualifies.
     *
     * Data structures: 26 prefix-sum arrays (or a single 2D int[26][n+1]) + binary search.
     *
     * Time Complexity: O(n log n) with a constant-factor of 26 per predicate check, i.e.
     *   O(26 * n * log n) -- still effectively O(n log n) for fixed alphabet size.
     * Space Complexity: O(26n) for the prefix tables.
     *
     * Pros: Demonstrates the monotonicity insight clearly and is a natural stepping stone toward
     *   realizing that this same monotonicity supports a linear two-pointer scan instead of
     *   log-n binary search per position. Handles large n comfortably.
     * Cons: The O(26n) prefix table costs real extra memory versus the O(1)-ish optimal
     *   solution; the log(n) factor and 26-way scan per binary-search step are unnecessary once
     *   we notice the two-pointer boundary is *itself* monotonically non-decreasing as `left`
     *   increases (which is what Approach 4 exploits to drop the log factor entirely).
     * When to use: A reasonable middle-ground if, for some reason, random-access frequency
     *   queries into arbitrary ranges are needed elsewhere in a larger system (i.e., the prefix
     *   table has value beyond this one query) -- otherwise Approach 4 dominates it.
     * --------------------------------------------------------------------------------------------
     */
    public static long binarySearchApproach(String s, int k) {
        int n = s.length();
        final int ALPHABET_SIZE = 26;
        int[][] prefixFrequency = new int[ALPHABET_SIZE][n + 1];

        for (int i = 0; i < n; i++) {
            for (int c = 0; c < ALPHABET_SIZE; c++) {
                prefixFrequency[c][i + 1] = prefixFrequency[c][i];
            }
            prefixFrequency[s.charAt(i) - 'a'][i + 1]++;
        }

        long qualifyingCount = 0;
        for (int left = 0; left < n; left++) {
            int lo = left, hi = n - 1, firstQualifyingEnd = -1;
            while (lo <= hi) {
                int mid = lo + (hi - lo) / 2;
                boolean satisfied = false;
                for (int c = 0; c < ALPHABET_SIZE; c++) {
                    if (prefixFrequency[c][mid + 1] - prefixFrequency[c][left] >= k) {
                        satisfied = true;
                        break;
                    }
                }
                if (satisfied) {
                    firstQualifyingEnd = mid;
                    hi = mid - 1; // search for an even earlier qualifying boundary
                } else {
                    lo = mid + 1;
                }
            }
            if (firstQualifyingEnd != -1) {
                qualifyingCount += (n - firstQualifyingEnd);
            }
        }
        return qualifyingCount;
    }

    /*
     * --------------------------------------------------------------------------------------------
     * APPROACH 4 (OPTIMAL): Sliding Window / Two-Pointer -- Complement Counting
     * --------------------------------------------------------------------------------------------
     * Core idea (the key insight): It's easier to count the COMPLEMENT -- substrings where NO
     * character reaches k ("bad" substrings) -- and subtract from the total substring count
     * n*(n+1)/2. A "bad" window is characterized purely by frequencies all being < k.
     *
     * As `right` advances one step at a time, maintain the LARGEST possible `left` such that
     * window [left, right] is still "bad" (all frequencies < k). Critically: this boundary
     * `left` only ever moves FORWARD (never backward) as `right` increases -- because once a
     * position is excluded to keep the window bad, growing the window further to the right can
     * only ever require excluding more from the left, never less. This monotonic-pointer
     * property is what allows a single forward pass (each pointer visits each index at most
     * once) to replace the O(log n) binary search entirely -- true O(n).
     *
     * For each `right`: increment freq[s[right]]. If that just pushed freq[s[right]] to >= k,
     * shrink from the left (decrementing freq[s[left]], left++) until it drops back below k.
     * After this adjustment, window [left, right] is the maximal bad window ending at `right`,
     * so exactly (right - left + 1) substrings ending at `right` are bad (all sub-windows
     * [left', right] for left' in [left, right] are bad too, since they're subsets with
     * only-lower-or-equal counts).
     *
     * Data structures: A single int[26] frequency array + two pointers (`left`, `right`).
     *
     * Time Complexity: O(n) -- `right` advances n times; `left` advances at most n times total
     *   across the entire run (amortized), so total pointer movement is O(n), with O(1) work
     *   per step.
     * Space Complexity: O(1) -- fixed 26-length array, independent of n.
     *
     * Pros: Truly linear, minimal extra memory, elegant once the complement-counting insight is
     *   seen, and is the version I'd want in production.
     * Cons: The complement-counting trick ("count what DOESN'T satisfy the condition, then
     *   subtract") is a non-obvious leap -- if I couldn't derive it live, I'd present Approach 2
     *   first as a safe, correct fallback, then pivot to this one.
     * When to use: This is the production-grade choice for any realistic input size. Use
     *   Approach 2 only if you need something quick-and-obviously-correct to write under time
     *   pressure, or as a fallback if the interviewer doesn't want the complement trick.
     * --------------------------------------------------------------------------------------------
     */
    public static long slidingWindowOptimal(String s, int k) {
        int n = s.length();
        if (k <= 1) {
            // Every non-empty substring trivially has some character appearing >= 1 time.
            return (long) n * (n + 1) / 2;
        }

        final int ALPHABET_SIZE = 26;
        int[] frequency = new int[ALPHABET_SIZE];
        long badSubstringCount = 0;
        int left = 0;

        for (int right = 0; right < n; right++) {
            int rightCharIndex = s.charAt(right) - 'a';
            frequency[rightCharIndex]++;

            // Shrink from the left while the window is no longer "bad" (i.e., the character we
            // just added has reached k). This loop runs O(n) total times across the whole
            // method, not per iteration -- hence the amortized O(n) bound.
            while (frequency[rightCharIndex] >= k) {
                frequency[s.charAt(left) - 'a']--;
                left++;
            }

            // Every substring [left', right] for left' in [left, right] is "bad".
            badSubstringCount += (right - left + 1);
        }

        long totalSubstrings = (long) n * (n + 1) / 2;
        return totalSubstrings - badSubstringCount;
    }

    /*
     * ============================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ============================================================================================
     *
     * Approach                          | Time              | Space   | Best For              | Limitations
     * ----------------------------------|-------------------|---------|-----------------------|--------------------------------
     * 1. Brute Force (Naive, HashMap)   | O(n^3)            | O(n)    | Correctness oracle,   | Unusable beyond tiny n
     *                                   |                   |         | tiny n                |
     * 2. Optimized Brute Force          | O(n^2) worst      | O(1)    | Simple safe first cut,| Quadratic; too slow n > ~few
     *    (monotonic early-exit)         |                   |         | small-to-medium n     | thousand
     * 3. Prefix Sums + Binary Search    | O(26 n log n)     | O(26n)  | Demonstrating         | Extra memory + unnecessary
     *                                   |                   |         | monotonicity insight  | log factor once Approach 4
     *                                   |                   |         |                       | is seen
     * 4. Sliding Window / Two-Pointer   | O(n) amortized    | O(1)/   | Production, large n,  | Requires spotting the
     *    (complement counting) OPTIMAL  |                   | O(26)   | interview finale      | complement-counting insight
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ============================================================================================
     *
     * I would present Approach 2 (Optimized Brute Force) FIRST as a safe, clearly-correct
     * baseline -- it's fast to code, easy to narrate ("counts only increase as the window grows,
     * so once satisfied I can count the rest in bulk"), and gives the interviewer confidence I
     * can produce working code under pressure.
     *
     * I would then explicitly call out that O(n^2) may not be sufficient depending on the stated
     * constraints, and pivot to Approach 4 (Sliding Window / Two-Pointer with complement
     * counting) as the optimal solution -- explaining the "count the complement" insight before
     * writing code, then implementing it cleanly. This mirrors exactly how Google interviews are
     * typically scored: a correct, well-reasoned O(n^2) fallback earns real credit, but landing
     * the O(n) solution with a clear explanation of WHY the two-pointer boundary is monotonic is
     * what separates a strong "Hire" signal from a "Lean Hire."
     *
     * I would mention Approach 3 (prefix sums + binary search) verbally as a middle stepping
     * stone (to show I understand WHY the monotonicity enables removing the log factor), but I
     * would not necessarily code it fully unless asked, since it's dominated by Approach 4 in
     * every respect once the two-pointer insight is on the table.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 9: DEEP DIVE -- PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ============================================================================================
     */
    public static final class OptimalSolution {

        /** Size of the fixed lowercase-English-letter alphabet this solution assumes. */
        private static final int ALPHABET_SIZE = 26;

        private OptimalSolution() {
            // Utility class; not instantiable.
        }

        /**
         * Counts the total number of substrings of {@code s} in which at least one character
         * appears at least {@code k} times.
         *
         * <p>Runs in O(n) amortized time using a two-pointer / sliding-window technique that
         * counts the complement (substrings where NO character reaches {@code k}) and subtracts
         * from the total substring count. Uses O(1) auxiliary space (a fixed 26-slot frequency
         * array).</p>
         *
         * @param s the input string, assumed to contain only lowercase English letters ('a'-'z')
         * @param k the minimum frequency threshold a character must reach within a substring
         *          for that substring to qualify
         * @return the total number of qualifying substrings, as a {@code long} to avoid overflow
         *         for large inputs (n*(n+1)/2 can exceed Integer.MAX_VALUE)
         * @throws IllegalArgumentException if {@code s} is null, or if {@code k} is negative
         */
        public static long countSubstringsWithCharAtLeastK(String s, int k) {
            if (s == null) {
                throw new IllegalArgumentException("Input string must not be null.");
            }
            if (k < 0) {
                throw new IllegalArgumentException("k must be non-negative, but was: " + k);
            }

            int stringLength = s.length();
            if (stringLength == 0) {
                return 0L; // No substrings exist in an empty string.
            }

            long totalSubstringCount = (long) stringLength * (stringLength + 1) / 2;

            // k <= 1: every non-empty substring trivially has some character with frequency >= 1.
            if (k <= 1) {
                return totalSubstringCount;
            }

            int[] characterFrequency = new int[ALPHABET_SIZE];
            long badSubstringCount = 0; // Substrings where NO character reaches k.
            int windowStart = 0;        // Left boundary of the maximal "bad" window ending at windowEnd.

            for (int windowEnd = 0; windowEnd < stringLength; windowEnd++) {
                int rightCharIndex = charToIndex(s.charAt(windowEnd));
                characterFrequency[rightCharIndex]++;

                // If the character we just added reached the threshold k, the window
                // [windowStart, windowEnd] is no longer "bad" -- shrink from the left until it is
                // again. This inner loop advances windowStart monotonically forward across the
                // ENTIRE outer loop (never resets), which is what keeps the whole method O(n).
                while (characterFrequency[rightCharIndex] >= k) {
                    int leftCharIndex = charToIndex(s.charAt(windowStart));
                    characterFrequency[leftCharIndex]--;
                    windowStart++;
                }

                // Every substring [x, windowEnd] for x in [windowStart, windowEnd] is bad,
                // because it's a subset of the maximal bad window [windowStart, windowEnd] and
                // subset frequencies can only be <= the containing window's frequencies.
                badSubstringCount += (windowEnd - windowStart + 1);
            }

            return totalSubstringCount - badSubstringCount;
        }

        /**
         * Maps a lowercase English letter to a zero-based index in [0, 25].
         *
         * @param character a lowercase letter 'a'-'z'
         * @return the zero-based alphabet index
         */
        private static int charToIndex(char character) {
            return character - 'a';
        }
    }

    /*
     * ============================================================================================
     * SECTION 10: DRY RUN / TRACE
     * ============================================================================================
     *
     * Tracing OptimalSolution.countSubstringsWithCharAtLeastK("abacb", 2):
     *
     * stringLength = 5, totalSubstringCount = 5*6/2 = 15. k = 2 > 1, so we proceed to the scan.
     * characterFrequency = [0]*26, windowStart = 0, badSubstringCount = 0.
     *
     * windowEnd=0, char='a' (index 0): frequency[a]=1. 1 < 2, no shrink.
     *   window=[0,0] ("a"), size=1. badSubstringCount += 1 -> badSubstringCount = 1.
     *
     * windowEnd=1, char='b' (index 1): frequency[b]=1. 1 < 2, no shrink.
     *   window=[0,1] ("ab"), size=2. badSubstringCount += 2 -> badSubstringCount = 3.
     *
     * windowEnd=2, char='a' (index 0): frequency[a]=2. 2 >= 2 -> SHRINK:
     *     remove s[windowStart=0]='a': frequency[a]=1, windowStart=1.
     *     recheck frequency[a]=1 < 2, stop shrinking.
     *   window=[1,2] ("ba"), size=2. badSubstringCount += 2 -> badSubstringCount = 5.
     *
     * windowEnd=3, char='c' (index 2): frequency[c]=1. 1 < 2, no shrink.
     *   window=[1,3] ("bac"), size=3. badSubstringCount += 3 -> badSubstringCount = 8.
     *
     * windowEnd=4, char='b' (index 1): frequency[b]=2. 2 >= 2 -> SHRINK:
     *     remove s[windowStart=1]='b': frequency[b]=1, windowStart=2.
     *     recheck frequency[b]=1 < 2, stop shrinking.
     *   window=[2,4] ("acb"), size=3. badSubstringCount += 3 -> badSubstringCount = 11.
     *
     * Loop ends. totalSubstringCount (15) - badSubstringCount (11) = 4.
     * Result: 4  -- matches the manually-enumerated answer from Section 3, Example 1. Correct.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 11: CLOSING SUMMARY
     * ============================================================================================
     *
     * - Approach 1 (naive HashMap brute force) is purely for correctness-checking; O(n^3) is
     *   unusable at scale.
     * - Approach 2 (optimized brute force) is a strong, simple O(n^2) fallback that showcases
     *   the core monotonicity insight (counts never decrease as the window grows) without
     *   requiring the harder complement-counting leap.
     * - Approach 3 (prefix sums + binary search) trades O(26n) extra memory and a log(n) factor
     *   for a clean demonstration of "the predicate is monotonic in `end` for fixed `left`" --
     *   useful conceptually, but dominated by Approach 4 in practice.
     * - Approach 4 (sliding window / two-pointer, complement counting) is the recommended final
     *   answer: true O(n) time, O(1) extra space (beyond the fixed-size alphabet array), by
     *   counting substrings where NO character reaches k and subtracting from the total.
     *
     * Known assumptions/limitations of the final solution:
     *   - Assumes a fixed 26-letter lowercase alphabet (widen the array size and charToIndex
     *     mapping trivially if the alphabet is larger, e.g., full ASCII or Unicode).
     *   - Assumes s fits comfortably in memory (no streaming support).
     *   - Returns 0 for an empty string and treats k <= 1 as "everything qualifies," both by
     *     explicit design decisions documented in the Clarifying Questions section.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ============================================================================================
     *
     * 1. "What if the alphabet were the full Unicode character set instead of just lowercase
     *    English letters?" -- Discuss swapping the fixed int[26] array for a HashMap<Character,
     *    Integer>, and how that changes the space bound from O(1) to O(distinct characters), and
     *    may add a small constant-factor overhead per operation (hashing vs. array indexing).
     *
     * 2. "Can you solve the 'every character must appear at least k times' variant instead of
     *    'at least one'?" -- This is LeetCode 395 (Longest Substring with At Least K Repeating
     *    Characters); discuss the divide-and-conquer approach that splits at characters whose
     *    total frequency is < k, since such characters can never be part of ANY valid answer.
     *
     * 3. "What if `s` is extremely large and doesn't fit in memory (streaming)?" -- Discuss
     *    whether an approximate/streaming frequency structure (e.g., Count-Min Sketch) could
     *    help, and the trade-offs versus exact counting, and whether the problem can even be
     *    solved in a single streaming pass given the complement-counting technique still needs
     *    to track a shrinking-left-pointer window.
     *
     * 4. "How would you modify this if `k` changes for every query (many queries against the
     *    same fixed string)?" -- Discuss precomputing prefix-frequency arrays once (Approach 3's
     *    building block) so that each new k can be answered in O(n log n) per query without
     *    rebuilding the underlying prefix tables, versus re-running the O(n) two-pointer scan
     *    per k (also acceptable if the number of queries is modest).
     *
     * 5. "Can you return the actual list of qualifying (start, end) index pairs, not just the
     *    count?" -- Discuss how the two-pointer approach can be adapted: instead of just adding
     *    (windowEnd - windowStart + 1) to a running "bad" count, you'd need to enumerate the
     *    complementary GOOD range [0, windowStart-1] pairs and materialize them, which changes
     *    the space complexity from O(1) to O(number of qualifying substrings) -- potentially
     *    O(n^2) in the worst case, since there can be that many qualifying substrings.
     *
     * 6. "What's the worst-case input that stresses your solution the most?" -- Discuss a string
     *    of a single repeated character with k = n (forces the window to nearly span the whole
     *    string before any shrink) versus a string with all-distinct characters and k = 2
     *    (window never shrinks at all, so the entire string is "bad" -- answer 0).
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ============================================================================================
     *
     * 1. Off-by-one in the "bad substring count" bookkeeping: forgetting that the number of bad
     *    substrings ending at `windowEnd` is (windowEnd - windowStart + 1), NOT
     *    (windowEnd - windowStart), leading to systematically undercounting by one per step.
     *
     * 2. Confusing this "at least ONE character reaches k" problem with the "EVERY character
     *    must reach k" problem (LeetCode 395) -- they look superficially similar but require
     *    fundamentally different techniques (two-pointer/complement-counting here vs.
     *    divide-and-conquer or a sliding-window-with-distinct-count-limit there). Candidates who
     *    don't clarify this up front sometimes code the wrong algorithm entirely.
     *
     * 3. Integer overflow: using `int` instead of `long` for the running count or for
     *    n*(n+1)/2, which silently overflows for n in the tens of thousands and produces a
     *    negative or garbage result rather than a clear error.
     *
     * 4. Assuming the two-pointer `left`/`windowStart` boundary can be RESET to 0 for each new
     *    `windowEnd` (turning the elegant O(n) solution back into an accidental O(n^2)) --
     *    the entire correctness and efficiency of the sliding-window approach hinges on
     *    `windowStart` only ever moving forward, never backward, across the full outer loop.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * TEST HARNESS -- cross-validates all four approaches against each other, plus a randomized
     * stress test against the brute-force oracle, following the established validation pattern
     * of comparing multiple independent implementations before trusting the optimal one.
     * ============================================================================================
     */
    public static void main(String[] args) {
        System.out.println("=== Manual Examples ===");
        runAndCompare("abacb", 2, 4L);
        runAndCompare("aaaa", 2, 6L);
        runAndCompare("a", 2, 0L);
        runAndCompare("abcde", 1, 15L);
        runAndCompare("", 3, 0L);

        System.out.println();
        System.out.println("=== Randomized Stress Test (all four approaches must agree) ===");
        Random random = new Random(2026);
        boolean allMatched = true;
        int trialCount = 3000;

        for (int trial = 0; trial < trialCount; trial++) {
            int length = 1 + random.nextInt(11); // keep small so O(n^3) brute force stays fast
            StringBuilder builder = new StringBuilder();
            int alphabetSize = 1 + random.nextInt(4); // small alphabet forces repeats to occur
            for (int i = 0; i < length; i++) {
                builder.append((char) ('a' + random.nextInt(alphabetSize)));
            }
            String randomString = builder.toString();
            int k = 1 + random.nextInt(length + 1);

            long naiveResult = bruteForceNaive(randomString, k);
            long optimizedBruteResult = bruteForceOptimized(randomString, k);
            long binarySearchResult = binarySearchApproach(randomString, k);
            long slidingWindowResult = slidingWindowOptimal(randomString, k);
            long productionResult = OptimalSolution.countSubstringsWithCharAtLeastK(randomString, k);

            boolean allEqual = naiveResult == optimizedBruteResult
                    && optimizedBruteResult == binarySearchResult
                    && binarySearchResult == slidingWindowResult
                    && slidingWindowResult == productionResult;

            if (!allEqual) {
                allMatched = false;
                System.out.println("MISMATCH on s=\"" + randomString + "\", k=" + k
                        + " -> naive=" + naiveResult
                        + ", optimizedBrute=" + optimizedBruteResult
                        + ", binarySearch=" + binarySearchResult
                        + ", slidingWindow=" + slidingWindowResult
                        + ", production=" + productionResult);
            }
        }

        System.out.println(trialCount + " random trials completed. All approaches agreed: " + allMatched);
    }

    /**
     * Helper for the test harness: runs all approaches on one input, prints results, and
     * asserts they agree with each other and (if provided) with an expected value.
     */
    private static void runAndCompare(String s, int k, Long expected) {
        long naiveResult = bruteForceNaive(s, k);
        long optimizedBruteResult = bruteForceOptimized(s, k);
        long binarySearchResult = binarySearchApproach(s, k);
        long slidingWindowResult = slidingWindowOptimal(s, k);
        long productionResult = OptimalSolution.countSubstringsWithCharAtLeastK(s, k);

        System.out.println("s=\"" + s + "\", k=" + k
                + " -> naive=" + naiveResult
                + ", optimizedBrute=" + optimizedBruteResult
                + ", binarySearch=" + binarySearchResult
                + ", slidingWindow=" + slidingWindowResult
                + ", production=" + productionResult
                + (expected != null ? ", expected=" + expected : ""));

        boolean allEqual = naiveResult == optimizedBruteResult
                && optimizedBruteResult == binarySearchResult
                && binarySearchResult == slidingWindowResult
                && slidingWindowResult == productionResult
                && (expected == null || slidingWindowResult == expected);

        if (!allEqual) {
            throw new AssertionError("Mismatch detected for s=\"" + s + "\", k=" + k);
        }
    }

    class Solution {

        public long numberOfSubstrings(String s, int k) {

            final int n = s.length();

            // Frequency of each lowercase character inside the current window.
            int[] frequency = new int[26];

            // Sliding window boundaries.
            int left = 0;

            // Total valid substrings.
            long answer = 0;

            /*
             * Number of distinct characters whose frequency has reached
             * at least 'k' inside the current window.
             *
             * Window is considered VALID whenever:
             * charactersMeetingRequirement > 0
             *
             * Instead of scanning all 26 characters every time,
             * we maintain this value incrementally in O(1).
             */
            int charactersMeetingRequirement = 0;

            // Expand the window one character at a time.
            for (int right = 0; right < n; right++) {

                int rightChar = s.charAt(right) - 'a';

                // Include the new character.
                frequency[rightChar]++;

                /*
                 * This character has JUST reached frequency 'k'.
                 *
                 * Example:
                 * k = 3
                 * frequency becomes
                 * 1 -> 2 (not enough)
                 * 2 -> 3 (window becomes valid because of this character)
                 * 3 -> 4 (already counted, do nothing)
                 */
                if (frequency[rightChar] == k) {
                    charactersMeetingRequirement++;
                }

                /*
                 * While the current window is valid,
                 * try shrinking it from the left.
                 */
                while (charactersMeetingRequirement > 0) {

                    /*
                     * IMPORTANT OBSERVATION
                     * ---------------------
                     * Current window:
                     *
                     * left ........ right
                     *
                     * is already valid.
                     *
                     * Extending the window further right can never decrease
                     * any character frequency.
                     *
                     * Therefore ALL substrings
                     *
                     * left...right
                     * left...right+1
                     * left...right+2
                     * ...
                     * left...n-1
                     *
                     * are guaranteed to remain valid.
                     *
                     * Number of such substrings = n - right
                     */
                    answer += (n - right);

                    int leftChar = s.charAt(left) - 'a';

                    /*
                     * If this character is currently contributing to
                     * window validity (frequency == k),
                     * removing it will make frequency become k-1,
                     * so the window may become invalid.
                     */
                    if (frequency[leftChar] == k) {
                        charactersMeetingRequirement--;
                    }

                    // Remove the leftmost character.
                    frequency[leftChar]--;

                    // Continue searching for a smaller valid window.
                    left++;
                }
            }

            return answer;
        }
    }

}
