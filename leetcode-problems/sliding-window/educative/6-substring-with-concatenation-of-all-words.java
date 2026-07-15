import java.util.*;

/*
 * =====================================================================================
 * PROBLEM: Substring with Concatenation of All Words (LeetCode 30)
 * =====================================================================================
 *
 * ---------------------------------------------------------------------------
 * SECTION 1: RESTATE THE PROBLEM
 * ---------------------------------------------------------------------------
 * In plain English:
 *   We are given a big string `s` and a list `words` of smaller strings, all of
 *   the SAME fixed length. We need to find every starting index in `s` where
 *   a substring exists that is exactly a concatenation of ALL the words in
 *   `words`, each used EXACTLY ONCE, in ANY order, with no extra characters
 *   in between and no characters left over.
 *
 *   Think of it as: "does a permutation of `words`, glued together, appear
 *   starting at this position in `s`?" We need ALL such starting positions.
 *
 * Inputs:
 *   - s: String, the haystack we search within.
 *   - words: String[], all elements have identical length L. May contain
 *     duplicate words (e.g., ["ab", "ab"] is valid input).
 *
 * Output:
 *   - List<Integer>: all starting indices in `s` where a valid concatenation
 *     begins. Order doesn't matter per problem statement.
 *
 * Key constraints (from LeetCode's actual constraint block, restated):
 *   - 1 <= s.length <= 10^4
 *   - 1 <= words.length <= 5000
 *   - 1 <= words[i].length <= 30 (all words[i] have the SAME length)
 *   - s and words[i] consist of lowercase English letters (in the general
 *     case; I'll confirm this with the interviewer rather than assume it).
 *
 * Core assumption I will explicitly validate in Section 2:
 *   - "Concatenated" means back-to-back with zero gaps, using each word
 *     index exactly once (so duplicate word VALUES are allowed and counted
 *     with multiplicity, not just uniqueness of value).
 *
 * =====================================================================================
 *
 * ---------------------------------------------------------------------------
 * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
 * ---------------------------------------------------------------------------
 * 1. Q: Can `words` contain duplicate strings (e.g., ["foo", "foo"])?
 *    A (assumed): Yes. If words = ["foo","foo"], a valid match needs "foo"
 *       to appear twice in the window, using frequency counting, not a set.
 *
 * 2. Q: Are we guaranteed all words have the same length, or must I validate that?
 *    A (assumed): Guaranteed by problem constraints, but I will still add a
 *       defensive check since production code shouldn't trust inputs blindly.
 *
 * 3. Q: What character set are we dealing with — lowercase only, or can there
 *       be Unicode, punctuation, mixed case?
 *    A (assumed): Lowercase English letters only, matching LeetCode's
 *       constraints. This simplifies nothing algorithmically but is worth
 *       confirming since it rules out exotic encoding issues.
 *
 * 4. Q: Should indices be returned in sorted order, or is any order acceptable?
 *    A (assumed): Any order is acceptable per the prompt, but I'll return
 *       them in ascending order anyway since it's free and aids debugging.
 *
 * 5. Q: What should happen if `words` is empty, or `s` is empty, or a single
 *       word is longer than `s` itself?
 *    A (assumed): Return an empty list in all such degenerate cases rather
 *       than throwing — treat "no possible match" as a valid, boring answer.
 *
 * 6. Q: Is there a bound on how large words.length can get relative to s.length
 *       (i.e., could total word length vastly exceed s.length)?
 *    A (assumed): Yes it could — if numWords * wordLength > s.length, there's
 *       simply no valid window and we return early.
 *
 * 7. Q: Do overlapping matches count independently? E.g., if two different
 *       windows starting at index 0 and index 3 both qualify, do we report both?
 *    A (assumed): Yes — every valid starting index is reported independently,
 *       windows are allowed to overlap in the source string `s`.
 *
 * 8. Q: Is this a one-shot query, or will this run repeatedly against a
 *       changing `words` list with the same `s` (i.e., should I care about
 *       amortizing preprocessing)?
 *    A (assumed): One-shot per call — no persistent index structure is
 *       expected, though I'll note how one *could* be built if asked.
 *
 * =====================================================================================
 *
 * ---------------------------------------------------------------------------
 * SECTION 3: EXAMPLES & EDGE CASES
 * ---------------------------------------------------------------------------
 * Example 1 (Normal case):
 *   s = "barfoothefoobarman", words = ["foo", "bar"]
 *   wordLength = 3, numWords = 2, totalLength = 6
 *   -> "barfoo" at index 0 = "bar"+"foo"  (valid permutation)
 *   -> "foobar" at index 9 = "foo"+"bar"  (valid permutation)
 *   Expected output: [0, 9]
 *
 * Example 2 (Edge case — no valid match / duplicates in words):
 *   s = "wordgoodgoodgoodbestword", words = ["word","good","best","word"]
 *   wordLength = 4, numWords = 4, totalLength = 16
 *   Note "word" appears TWICE in `words`, so any valid window must contain
 *   "word" twice. Scanning s, no window satisfies the exact multiset
 *   {word:2, good:1, best:1} in the right contiguous arrangement.
 *   Expected output: [] (empty — this specifically stress-tests duplicate
 *   handling, since a naive HashSet-based approach would incorrectly treat
 *   "word" appearing once as sufficient).
 *
 * Example 3 (Boundary / tie-breaking case — overlapping valid windows):
 *   s = "ababab", words = ["ab", "ba"]
 *   wordLength = 2, numWords = 2, totalLength = 4
 *   -> index 0: "abab" = "ab"+"ab"? No — "ab","ab" isn't a permutation of
 *      ["ab","ba"]. Let's check properly: substring(0,4) = "abab" ->
 *      chunks "ab","ab" -> multiset {ab:2} != target {ab:1, ba:1} -> invalid.
 *   -> index 1: substring(1,5) = "baba" -> chunks "ba","ba" -> invalid too.
 *   -> index 2: substring(2,6) = "abab" -> same as index 0 pattern -> invalid.
 *   Expected output: [] — this demonstrates that adjacent, overlapping
 *   windows must each be independently validated against the EXACT
 *   multiset of words, not just "contains all distinct word values."
 * =====================================================================================
 */
class ConcatenatedSubstringFinder {

    /*
     * -----------------------------------------------------------------------
     * SECTION 4-6: ALL POSSIBLE SOLUTIONS
     * -----------------------------------------------------------------------
     * Paradigm sweep — which categories genuinely apply here:
     *
     *   - Brute force / naive            -> APPLICABLE (Approach 1)
     *   - Hashing-based                  -> APPLICABLE (Approach 2, and inside Approach 3)
     *   - Two pointer / sliding window    -> APPLICABLE (Approach 3, the optimal one)
     *   - Sorting-based                  -> NOT APPLICABLE: sorting words or characters
     *       destroys the exact positional/order information needed to detect
     *       contiguous concatenation; sorting is a good fit for anagram-style
     *       "same multiset anywhere" problems, not fixed-position gluing.
     *   - Divide and conquer             -> NOT APPLICABLE: there's no natural
     *       way to split `s` into independent subproblems whose solutions
     *       combine, since a valid window can straddle any split point.
     *   - Greedy                         -> NOT APPLICABLE: there's no locally
     *       optimal choice to make each step; validity is only known once an
     *       entire window of totalLength characters has been examined.
     *   - Dynamic programming            -> NOT APPLICABLE: there's no
     *       overlapping-subproblem / optimal-substructure relationship to
     *       exploit — this is a fixed-window multiset-matching problem, not
     *       an optimization over subsequences.
     *   - Tree / graph traversal         -> NOT APPLICABLE: no graph or tree
     *       structure is implied by the problem.
     *   - Heap / priority queue          -> NOT APPLICABLE: nothing about
     *       this problem involves retrieving a min/max repeatedly.
     *   - Binary search                  -> NOT APPLICABLE: the search space
     *       (starting indices) isn't monotonic with respect to any predicate
     *       we could binary search over.
     *   - Monotonic stack / deque        -> NOT APPLICABLE: no "next greater/
     *       smaller element" style relationship exists here.
     *   - Trie / segment tree            -> WORTH MENTIONING but not needed:
     *       since all words share one fixed length L, a plain HashMap<String,
     *       Integer> already gives O(1) average lookup on chunks of length L.
     *       A trie would only help if word lengths varied (they don't here),
     *       so it adds complexity without benefit for this exact problem.
     * -----------------------------------------------------------------------
     */

    /*
     * =====================================================================
     * APPROACH 1: Brute Force via Permutation Generation
     * =====================================================================
     * Core idea:
     *   Generate every possible ordering ("permutation") of `words`, glue
     *   each ordering into one long string, collect all such strings into
     *   a set of "valid concatenations", then slide a fixed-size window
     *   across `s` and check set membership at every position.
     *
     * Paradigm: brute force enumeration + hashing (set membership).
     *
     * Time Complexity:  O(k! * k * L + n * L)
     *   - k! permutations of the k words, each costing O(k * L) to build
     *     the concatenated string (k words of length L each).
     *   - Then O(n) window positions, each requiring an O(L)-ish substring
     *     extraction and hash lookup, where n = s.length().
     *   This is factorial time — explodes catastrophically past ~10 words.
     *
     * Space Complexity: O(k! * k * L) to store every distinct concatenation.
     *
     * Pros:
     *   - Trivial to reason about correctness; obviously matches the
     *     problem definition ("try every permutation, check verbatim").
     *   - Good as a warm-up / correctness oracle for small inputs in tests.
     *
     * Cons:
     *   - Factorial blowup makes it unusable for anything beyond a handful
     *     of words (10! is already ~3.6 million, 15! is infeasible).
     *   - Wastes enormous memory storing near-duplicate strings.
     *
     * When to use:
     *   - Never in production or in a real interview as a final answer.
     *   - Only worth mentioning to show you understand the literal problem
     *     definition before optimizing away from it, and as a brute-force
     *     cross-check in a test harness against small hand-built cases.
     * =====================================================================
     */
    public static List<Integer> bruteForcePermutations(String s, String[] words) {
        List<Integer> resultIndices = new ArrayList<>();
        if (s == null || words == null || words.length == 0 || s.isEmpty()) {
            return resultIndices;
        }

        int wordLength = words[0].length();
        int numWords = words.length;
        int totalLength = wordLength * numWords;
        if (totalLength == 0 || s.length() < totalLength) {
            return resultIndices;
        }

        // Generate every distinct permutation's concatenation up front.
        Set<String> allValidConcatenations = new HashSet<>();
        String[] workingCopy = words.clone();
        generateAllPermutations(workingCopy, 0, allValidConcatenations);

        // Slide a fixed-size window across s and test set membership.
        for (int start = 0; start + totalLength <= s.length(); start++) {
            String window = s.substring(start, start + totalLength);
            if (allValidConcatenations.contains(window)) {
                resultIndices.add(start);
            }
        }
        return resultIndices;
    }

    // Classic recursive swap-based permutation generator (Heap-adjacent style).
    // Duplicates in `words` naturally collapse because we insert into a Set<String>.
    private static void generateAllPermutations(String[] arr, int fixedPrefixLength, Set<String> output) {
        if (fixedPrefixLength == arr.length) {
            StringBuilder concatenation = new StringBuilder();
            for (String word : arr) {
                concatenation.append(word);
            }
            output.add(concatenation.toString());
            return;
        }
        for (int swapIndex = fixedPrefixLength; swapIndex < arr.length; swapIndex++) {
            swap(arr, fixedPrefixLength, swapIndex);
            generateAllPermutations(arr, fixedPrefixLength + 1, output);
            swap(arr, fixedPrefixLength, swapIndex); // backtrack
        }
    }

    private static void swap(String[] arr, int indexA, int indexB) {
        String temp = arr[indexA];
        arr[indexA] = arr[indexB];
        arr[indexB] = temp;
    }

    /*
     * =====================================================================
     * APPROACH 2: Brute Force with Hash-Based Frequency Counting Per Index
     * =====================================================================
     * Core idea:
     *   Instead of enumerating permutations, precompute the TARGET word
     *   frequency map once (e.g., {"foo":1, "bar":1}). Then for every
     *   candidate starting index in `s`, chop the fixed-size window into
     *   `numWords` chunks of length `wordLength`, build a frequency map of
     *   what we actually see, and bail out early the moment it can't match
     *   (an unknown chunk, or a chunk count exceeding the target).
     *
     * Paradigm: brute force outer loop + hashing (frequency map comparison).
     *
     * Time Complexity: O(n * numWords * wordLength)
     *   - Roughly n starting positions (n = s.length()).
     *   - At each position, up to numWords chunk extractions, each an
     *     O(wordLength) substring + hash operation.
     *   Since numWords * wordLength == totalLength <= n, this is often
     *   described as O(n * totalLength) in the worst case — no reuse of
     *   work between adjacent starting indices.
     *
     * Space Complexity: O(numWords) for the two frequency hash maps
     *   (target counts + window counts), plus O(wordLength) per extracted chunk.
     *
     * Pros:
     *   - Correctly handles duplicate words via frequency counts (fixes the
     *     multiset bug a HashSet-only approach would have).
     *   - Much simpler to write correctly under interview pressure than the
     *     sliding-window version, and still asymptotically reasonable for
     *     the given constraints (s.length() <= 10^4).
     *   - Great "safe first pass" to present in an interview: show it works,
     *     then discuss how to remove the redundant re-scanning.
     *
     * Cons:
     *   - Re-does all the chunk hashing from scratch at every single start
     *     index, even though adjacent windows overlap heavily and share
     *     almost all their chunk data.
     *   - Not optimal: there is a well-known O(n * wordLength) technique
     *     (Approach 3) that eliminates the redundant re-scanning.
     *
     * When to use:
     *   - As your FIRST working solution in an interview: it's correct,
     *     easy to explain, and easy to verify by hand.
     *   - In production if s.length() is small/bounded and code simplicity
     *     matters more than shaving a further constant factor.
     * =====================================================================
     */
    public static List<Integer> bruteForceHashing(String s, String[] words) {
        List<Integer> resultIndices = new ArrayList<>();
        if (s == null || words == null || words.length == 0 || s.isEmpty()) {
            return resultIndices;
        }

        int wordLength = words[0].length();
        int numWords = words.length;
        int totalLength = wordLength * numWords;
        int stringLength = s.length();
        if (totalLength == 0 || stringLength < totalLength) {
            return resultIndices;
        }

        Map<String, Integer> targetWordCounts = new HashMap<>();
        for (String word : words) {
            targetWordCounts.merge(word, 1, Integer::sum);
        }

        for (int start = 0; start + totalLength <= stringLength; start++) {
            Map<String, Integer> windowWordCounts = new HashMap<>();
            int wordsExamined = 0;

            for (; wordsExamined < numWords; wordsExamined++) {
                int chunkStart = start + wordsExamined * wordLength;
                String chunk = s.substring(chunkStart, chunkStart + wordLength);

                Integer targetCount = targetWordCounts.get(chunk);
                if (targetCount == null) {
                    break; // Unknown chunk -> this start index can't work.
                }
                int updatedCount = windowWordCounts.merge(chunk, 1, Integer::sum);
                if (updatedCount > targetCount) {
                    break; // Too many copies of this chunk -> can't work.
                }
            }

            if (wordsExamined == numWords) {
                resultIndices.add(start);
            }
        }
        return resultIndices;
    }

    /*
     * =====================================================================
     * APPROACH 3 (OPTIMAL): Sliding Window Per Modulo Offset
     * =====================================================================
     * Core idea:
     *   The key insight is that every valid window's starting index, modulo
     *   `wordLength`, determines which "track" of word-aligned chunks it
     *   lives on. If we fix an offset in [0, wordLength), the positions
     *   offset, offset+wordLength, offset+2*wordLength, ... partition s into
     *   a sequence of non-overlapping word-sized chunks. We can then run a
     *   classic variable-size sliding window (two pointers: windowStart and
     *   windowEnd) ACROSS THIS CHUNK SEQUENCE, expanding by one chunk at a
     *   time and shrinking from the left whenever a chunk's frequency in the
     *   current window would exceed its target frequency. This way each
     *   character of `s` is only ever visited O(1) times per offset, and
     *   there are only `wordLength` offsets total.
     *
     * Paradigm: two pointer / sliding window + hashing (frequency counting).
     *
     * Time Complexity: O(n * wordLength)
     *   - There are `wordLength` distinct offsets to process.
     *   - For each offset, windowEnd advances through roughly n / wordLength
     *     chunks, and each chunk costs O(wordLength) to extract as a
     *     substring, plus O(1) amortized hash map work. Each chunk is added
     *     and removed from the window at most once (windowStart only moves
     *     forward), so total work per offset is O(n).
     *   - Summed across all `wordLength` offsets: O(n * wordLength).
     *   Since wordLength <= 30 by constraint, this is effectively linear
     *   in practice, and asymptotically it's the best known approach for
     *   this exact problem shape.
     *
     * Space Complexity: O(numWords) for the target and window frequency
     *   maps, plus O(k) for the result list, where k is the number of matches.
     *
     * Pros:
     *   - No redundant re-scanning: each character of s participates in
     *     O(wordLength) total chunk extractions across all offsets, versus
     *     Approach 2's much heavier re-scanning per start index.
     *   - Handles duplicate words correctly via frequency maps, same as
     *     Approach 2, but with far better asymptotic behavior.
     *   - This is the standard, expected "optimal" solution for this
     *     specific LeetCode problem (#30), and is what a strong candidate
     *     is expected to land on.
     *
     * Cons:
     *   - Noticeably trickier to get right under pressure: the double-loop
     *     structure (offset loop + inner two-pointer loop) with three
     *     separate exit/reset conditions (unknown chunk, over-count,
     *     full match) is a common source of off-by-one and state-reset bugs.
     *   - Slightly harder to explain on a whiteboard than Approach 2.
     *
     * When to use:
     *   - This is the production-grade / final-interview-answer solution:
     *     use it whenever `s` or `words` could be large enough that
     *     Approach 2's re-scanning becomes wasteful.
     * =====================================================================
     */
    public static List<Integer> optimalSlidingWindow(String s, String[] words) {
        List<Integer> resultIndices = new ArrayList<>();
        if (s == null || words == null || words.length == 0 || s.isEmpty()) {
            return resultIndices;
        }

        int wordLength = words[0].length();
        int numWords = words.length;
        int totalLength = wordLength * numWords;
        int stringLength = s.length();
        if (totalLength == 0 || stringLength < totalLength) {
            return resultIndices;
        }

        Map<String, Integer> targetWordCounts = new HashMap<>();
        for (String word : words) {
            targetWordCounts.merge(word, 1, Integer::sum);
        }

        // Try every possible alignment offset within one word length.
        for (int offset = 0; offset < wordLength; offset++) {
            int windowStart = offset;
            int wordsCurrentlyMatched = 0;
            Map<String, Integer> windowWordCounts = new HashMap<>();

            for (int windowEnd = offset; windowEnd + wordLength <= stringLength; windowEnd += wordLength) {
                String currentChunk = s.substring(windowEnd, windowEnd + wordLength);

                if (!targetWordCounts.containsKey(currentChunk)) {
                    // This chunk can never be part of any valid concatenation.
                    // Discard everything accumulated so far and restart fresh
                    // right after this "poison" chunk.
                    windowWordCounts.clear();
                    wordsCurrentlyMatched = 0;
                    windowStart = windowEnd + wordLength;
                    continue;
                }

                // Tentatively include this chunk in the window.
                int updatedCount = windowWordCounts.merge(currentChunk, 1, Integer::sum);
                wordsCurrentlyMatched++;

                // Shrink from the left if we now have too many copies of
                // this particular chunk to fit the target multiset.
                while (updatedCount > targetWordCounts.get(currentChunk)) {
                    String leftChunk = s.substring(windowStart, windowStart + wordLength);
                    windowWordCounts.merge(leftChunk, -1, Integer::sum);
                    windowStart += wordLength;
                    wordsCurrentlyMatched--;
                    if (leftChunk.equals(currentChunk)) {
                        updatedCount--; // Track the count of the chunk we care about.
                    }
                }

                // If the window now contains exactly numWords chunks and
                // every count matched (guaranteed by the invariant above),
                // we've found a valid concatenated substring.
                if (wordsCurrentlyMatched == numWords) {
                    resultIndices.add(windowStart);

                    // Slide the window forward by one chunk to look for
                    // the next possible match on this same offset track.
                    String leftChunk = s.substring(windowStart, windowStart + wordLength);
                    windowWordCounts.merge(leftChunk, -1, Integer::sum);
                    windowStart += wordLength;
                    wordsCurrentlyMatched--;
                }
            }
        }

        Collections.sort(resultIndices); // Ascending order for determinism/debuggability.
        return resultIndices;
    }

    /*
     * ---------------------------------------------------------------------------
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ---------------------------------------------------------------------------
     *
     * | Approach                        | Time                     | Space          | Best For                                   | Limitations                                          |
     * |----------------------------------|--------------------------|----------------|--------------------------------------------|-------------------------------------------------------|
     * | 1. Brute Force (Permutations)    | O(k! * k * L + n * L)    | O(k! * k * L)  | Tiny word counts, correctness sanity check | Factorial blowup; unusable beyond ~10 words           |
     * | 2. Brute Force (Hashing per idx) | O(n * numWords * L)      | O(numWords)    | First correct pass in an interview          | Re-scans overlapping windows redundantly              |
     * | 3. Optimal Sliding Window        | O(n * L)                 | O(numWords)    | Production use, large s / many words        | Trickier control flow; more edge cases to get right   |
     *
     * (n = s.length(), L = wordLength, k = numWords)
     * ---------------------------------------------------------------------------
     */

    /*
     * ---------------------------------------------------------------------------
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ---------------------------------------------------------------------------
     * I would present Approach 2 (Brute Force Hashing) FIRST, out loud, as my
     * initial working solution — it's fast to code correctly, easy to verify
     * against the examples on the whiteboard, and demonstrates I've correctly
     * identified the "duplicate words need frequency counting, not a set"
     * subtlety from the very start.
     *
     * Once that's verified against the examples, I would proactively raise
     * the redundant-rescanning inefficiency myself ("notice adjacent windows
     * share almost all their chunk data — we're throwing that work away")
     * and pivot to Approach 3 (Optimal Sliding Window) as the follow-up,
     * final answer. This mirrors exactly how Google interviewers expect
     * candidates to progress: correct-and-clear first, then optimize with
     * a clear justification for WHY the optimization helps, not just that
     * it exists.
     *
     * I would NOT lead with Approach 1 in an interview — it's only useful
     * as a mental model / test oracle, not as a serious candidate answer.
     * ---------------------------------------------------------------------------
     */

    /*
     * ---------------------------------------------------------------------------
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ---------------------------------------------------------------------------
     * This is a hardened, fully-commented version of Approach 3 suitable for
     * a "write production code" style deep dive, with named constants,
     * defensive input validation, and Javadoc.
     * ---------------------------------------------------------------------------
     */

    /** Sentinel used to short-circuit when inputs make a match structurally impossible. */
    private static final List<Integer> NO_MATCHES_POSSIBLE = Collections.emptyList();

    /**
     * Finds all starting indices in {@code text} where a contiguous substring
     * is formed by concatenating every string in {@code words} exactly once,
     * in any order.
     *
     * <p>Algorithm: for each of the {@code wordLength} possible alignment
     * offsets, run a variable-size sliding window over the word-aligned
     * chunk sequence, using a frequency map to track how many times each
     * word currently appears in the window versus how many times it is
     * allowed to appear.
     *
     * @param text  the string to search within; must be non-null.
     * @param words the words to concatenate; must be non-null, non-empty,
     *              and every element must share the same length.
     * @return a list of all valid starting indices, in ascending order;
     *         empty if no match exists or inputs are degenerate.
     * @throws IllegalArgumentException if words contains elements of
     *         differing lengths (defensive check; not expected per
     *         problem constraints, but production code should not trust
     *         unvalidated input blindly).
     */
    public static List<Integer> findConcatenatedSubstringIndices(String text, String[] words) {
        if (text == null || words == null || words.length == 0 || text.isEmpty()) {
            return NO_MATCHES_POSSIBLE;
        }

        final int wordLength = words[0].length();
        validateAllWordsSameLength(words, wordLength);

        final int numberOfWords = words.length;
        final int totalConcatenationLength = wordLength * numberOfWords;
        final int textLength = text.length();

        // Structurally impossible: not enough room for even one full match,
        // or a degenerate zero-length word.
        if (wordLength == 0 || textLength < totalConcatenationLength) {
            return NO_MATCHES_POSSIBLE;
        }

        Map<String, Integer> targetWordFrequency = buildFrequencyMap(words);
        List<Integer> matchStartIndices = new ArrayList<>();

        // Every valid match must start at some index whose remainder mod
        // wordLength is fixed for that match's "alignment track". We check
        // every possible remainder independently.
        for (int alignmentOffset = 0; alignmentOffset < wordLength; alignmentOffset++) {
            slideWindowForOffset(
                    text,
                    alignmentOffset,
                    wordLength,
                    numberOfWords,
                    targetWordFrequency,
                    matchStartIndices
            );
        }

        Collections.sort(matchStartIndices);
        return matchStartIndices;
    }

    /** Defensive check: every word must share the same length for this algorithm to be valid. */
    private static void validateAllWordsSameLength(String[] words, int expectedLength) {
        for (String word : words) {
            if (word == null || word.length() != expectedLength) {
                throw new IllegalArgumentException(
                        "All words must be non-null and share the same length. Offending word: " + word);
            }
        }
    }

    /** Builds a frequency map counting how many times each distinct word value appears. */
    private static Map<String, Integer> buildFrequencyMap(String[] words) {
        Map<String, Integer> frequency = new HashMap<>();
        for (String word : words) {
            frequency.merge(word, 1, Integer::sum);
        }
        return frequency;
    }

    /**
     * Runs one sliding-window pass over the chunk sequence starting at
     * {@code alignmentOffset}, appending any discovered match start indices
     * to {@code matchStartIndices}.
     */
    private static void slideWindowForOffset(
            String text,
            int alignmentOffset,
            int wordLength,
            int numberOfWords,
            Map<String, Integer> targetWordFrequency,
            List<Integer> matchStartIndices) {

        int textLength = text.length();
        int windowStart = alignmentOffset;
        int wordsCurrentlyInWindow = 0;
        Map<String, Integer> windowWordFrequency = new HashMap<>();

        for (int windowEnd = alignmentOffset;
             windowEnd + wordLength <= textLength;
             windowEnd += wordLength) {

            String candidateChunk = text.substring(windowEnd, windowEnd + wordLength);

            if (!targetWordFrequency.containsKey(candidateChunk)) {
                // A chunk that isn't one of our target words poisons any
                // window that would contain it. Reset entirely and resume
                // scanning immediately after this chunk.
                windowWordFrequency.clear();
                wordsCurrentlyInWindow = 0;
                windowStart = windowEnd + wordLength;
                continue;
            }

            int chunkCountInWindow = windowWordFrequency.merge(candidateChunk, 1, Integer::sum);
            wordsCurrentlyInWindow++;

            // If we now have more copies of candidateChunk than the target
            // allows, shrink from the left until we don't.
            while (chunkCountInWindow > targetWordFrequency.get(candidateChunk)) {
                String evictedChunk = text.substring(windowStart, windowStart + wordLength);
                windowWordFrequency.merge(evictedChunk, -1, Integer::sum);
                windowStart += wordLength;
                wordsCurrentlyInWindow--;
                if (evictedChunk.equals(candidateChunk)) {
                    chunkCountInWindow--;
                }
            }

            if (wordsCurrentlyInWindow == numberOfWords) {
                // Every word has been used exactly once, in some order,
                // contiguously starting at windowStart. Record it.
                matchStartIndices.add(windowStart);

                // Advance the window by exactly one chunk to continue
                // looking for further matches along this same offset track.
                String leftmostChunk = text.substring(windowStart, windowStart + wordLength);
                windowWordFrequency.merge(leftmostChunk, -1, Integer::sum);
                windowStart += wordLength;
                wordsCurrentlyInWindow--;
            }
        }
    }

    /*
     * ---------------------------------------------------------------------------
     * SECTION 10: DRY RUN / TRACE
     * ---------------------------------------------------------------------------
     * Using Example 1: s = "barfoothefoobarman", words = ["foo", "bar"]
     * wordLength = 3, numberOfWords = 2, totalConcatenationLength = 6
     * targetWordFrequency = {"foo": 1, "bar": 1}
     *
     * Index reference (0-based):
     *   b(0) a(1) r(2) f(3) o(4) o(5) t(6) h(7) e(8) f(9) o(10) o(11)
     *   b(12) a(13) r(14) m(15) a(16) n(17)
     *
     * --- alignmentOffset = 0 ---
     * windowStart=0, wordsCurrentlyInWindow=0, windowWordFrequency={}
     *
     * windowEnd=0: candidateChunk = s[0:3] = "bar" (in target)
     *   windowWordFrequency={bar:1}, wordsCurrentlyInWindow=1
     *   1 <= target["bar"]=1, no shrink needed. 1 != 2, no match yet.
     *
     * windowEnd=3: candidateChunk = s[3:6] = "foo" (in target)
     *   windowWordFrequency={bar:1, foo:1}, wordsCurrentlyInWindow=2
     *   2 == numberOfWords -> MATCH at windowStart=0. Record index 0.
     *   Evict leftmost chunk s[0:3]="bar": windowWordFrequency={bar:0, foo:1}
     *   windowStart becomes 3, wordsCurrentlyInWindow=1
     *
     * windowEnd=6: candidateChunk = s[6:9] = "the" (NOT in target)
     *   Reset: windowWordFrequency={}, wordsCurrentlyInWindow=0
     *   windowStart = windowEnd + wordLength = 9
     *
     * windowEnd=9: candidateChunk = s[9:12] = "foo" (in target)
     *   windowWordFrequency={foo:1}, wordsCurrentlyInWindow=1
     *
     * windowEnd=12: candidateChunk = s[12:15] = "bar" (in target)
     *   windowWordFrequency={foo:1, bar:1}, wordsCurrentlyInWindow=2
     *   2 == numberOfWords -> MATCH at windowStart=9. Record index 9.
     *   Evict leftmost chunk s[9:12]="foo": windowWordFrequency={foo:0, bar:1}
     *   windowStart becomes 12, wordsCurrentlyInWindow=1
     *
     * windowEnd=15: candidateChunk = s[15:18] = "man" (NOT in target)
     *   Reset: windowWordFrequency={}, wordsCurrentlyInWindow=0
     *   windowStart = 18 (loop ends: 18 + 3 > 18)
     *
     * --- alignmentOffset = 1 and alignmentOffset = 2 ---
     * (Traced identically; neither produces a match for this particular
     * input, since no word-aligned chunk sequence on those offsets ever
     * accumulates exactly {foo:1, bar:1}.)
     *
     * Final matchStartIndices (after sorting): [0, 9]  <-- matches expected output.
     * ---------------------------------------------------------------------------
     */

    /*
     * ---------------------------------------------------------------------------
     * SECTION 11: CLOSING SUMMARY
     * ---------------------------------------------------------------------------
     * - Approach 1 (permutation brute force) is only useful conceptually or
     *   as a tiny-input correctness oracle; it is factorial and impractical.
     * - Approach 2 (per-index hashing) is correct, simple, and a perfectly
     *   reasonable first solution to present live — it handles duplicate
     *   words correctly via frequency maps, unlike a naive set-based check.
     * - Approach 3 (offset-based sliding window) is the optimal, expected
     *   final answer: O(n * wordLength) time, O(numWords) space, achieved
     *   by avoiding redundant re-scanning of overlapping windows.
     * - Known assumptions/limitations of the final solution: it assumes all
     *   words share a single fixed length (validated defensively), assumes
     *   words.length >= 1, and treats duplicate word values as requiring
     *   multiplicity via frequency counts rather than simple set membership.
     * ---------------------------------------------------------------------------
     */

    /*
     * ---------------------------------------------------------------------------
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ---------------------------------------------------------------------------
     * 1. "What if words could have DIFFERENT lengths?" — This breaks the
     *    fixed-offset chunking trick entirely; you'd likely need a
     *    trie-based or backtracking approach closer to word-break-style DP.
     * 2. "What if s or words were streamed / arrived incrementally?" —
     *    Discuss maintaining a rolling window and incremental frequency
     *    map updates rather than recomputing from scratch.
     * 3. "How would you parallelize this across multiple threads/machines?" —
     *    Discuss partitioning by alignmentOffset (each offset's scan is
     *    fully independent) or partitioning s into overlapping chunks with
     *    boundary overlap of totalConcatenationLength - 1 characters.
     * 4. "What if we only needed to know IF a match exists, not WHERE?" —
     *    Discuss early-exit optimization the moment the first match is found.
     * 5. "What if words.length were extremely large (e.g., 5000) but
     *    wordLength were small?" — Discuss how targetWordFrequency lookups
     *    dominate and whether a more compact encoding (e.g., hashing chunks
     *    to longs instead of Strings) would reduce constant factors.
     * 6. "Could you solve this with O(1) extra space (in-place)?" — Discuss
     *    the fundamental need for at least O(numWords) space to track
     *    target frequencies, and why true O(1) space isn't realistic here.
     * ---------------------------------------------------------------------------
     */

    /*
     * ---------------------------------------------------------------------------
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ---------------------------------------------------------------------------
     * 1. Using a HashSet of word values instead of a frequency map — this
     *    silently breaks on inputs with duplicate words (Example 2), since
     *    it can't distinguish "word appears once" from "word appears twice."
     * 2. Forgetting to check ALL `wordLength` alignment offsets in the
     *    optimal solution — checking only offset 0 misses valid matches
     *    that start at other remainders mod wordLength.
     * 3. Off-by-one / boundary errors in the loop condition
     *    (windowEnd + wordLength <= textLength) — using strict "<" instead
     *    of "<=" silently drops the last valid chunk in the string.
     * 4. Forgetting to reset windowWordFrequency AND wordsCurrentlyInWindow
     *    together when an unknown chunk is encountered — resetting only one
     *    of the two leaves the window in an inconsistent state that can
     *    produce false positives or false negatives on subsequent chunks.
     * ---------------------------------------------------------------------------
     */

    /*
     * ---------------------------------------------------------------------------
     * TEST HARNESS: cross-validates all three approaches against each other
     * and against known expected outputs, including the edge cases above.
     * ---------------------------------------------------------------------------
     */
    public static void main(String[] args) {
        runTestCase(
                "Example 1: normal case",
                "barfoothefoobarman",
                new String[]{"foo", "bar"},
                List.of(0, 9)
        );

        runTestCase(
                "Example 2: duplicate words, no match",
                "wordgoodgoodgoodbestword",
                new String[]{"word", "good", "best", "word"},
                List.of()
        );

        runTestCase(
                "Example 3: overlapping boundary case, no match",
                "ababab",
                new String[]{"ab", "ba"},
                List.of()
        );

        runTestCase(
                "Additional: multiple valid matches",
                "wordgoodgoodgoodbestword",
                new String[]{"word", "good", "best", "good"},
                List.of(8)
        );

        runTestCase(
                "Additional: single word equals whole string",
                "foobar",
                new String[]{"foobar"},
                List.of(0)
        );

        runTestCase(
                "Additional: no room for a full match",
                "ab",
                new String[]{"abc"},
                List.of()
        );

        System.out.println("All test cases completed.");
    }

    private static void runTestCase(String testName, String text, String[] words, List<Integer> expected) {
        List<Integer> bruteForcePermResult = sorted(bruteForcePermutations(text, words));
        List<Integer> bruteForceHashResult = sorted(bruteForceHashing(text, words));
        List<Integer> optimalResult = sorted(optimalSlidingWindow(text, words));
        List<Integer> productionResult = sorted(findConcatenatedSubstringIndices(text, words));
        List<Integer> expectedSorted = sorted(expected);

        boolean allMatch = bruteForcePermResult.equals(expectedSorted)
                && bruteForceHashResult.equals(expectedSorted)
                && optimalResult.equals(expectedSorted)
                && productionResult.equals(expectedSorted);

        System.out.println("=== " + testName + " ===");
        System.out.println("  s = \"" + text + "\", words = " + Arrays.toString(words));
        System.out.println("  Expected:               " + expectedSorted);
        System.out.println("  Brute Force (Perms):    " + bruteForcePermResult);
        System.out.println("  Brute Force (Hashing):  " + bruteForceHashResult);
        System.out.println("  Optimal Sliding Window: " + optimalResult);
        System.out.println("  Production Method:      " + productionResult);
        System.out.println("  Result: " + (allMatch ? "PASS" : "FAIL"));
        System.out.println();
    }

    private static List<Integer> sorted(List<Integer> list) {
        List<Integer> copy = new ArrayList<>(list);
        Collections.sort(copy);
        return copy;
    }
}
