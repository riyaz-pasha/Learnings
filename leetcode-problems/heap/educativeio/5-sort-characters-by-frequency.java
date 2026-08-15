import java.util.*;

/*
 * ============================================================================
 * LEETCODE 451 — SORT CHARACTERS BY FREQUENCY (Google-tagged, Medium)
 * ============================================================================
 * Mock Google onsite interview transcript.
 * Single-file, self-contained, compiles with `javac` and runs with
 * `java -ea SortCharactersByFrequency` (assertions enabled).
 * Sandbox fallback: `java -ea SortCharactersByFrequency.java` (Java 21+
 * single-file source launcher) when javac is unavailable.
 * ============================================================================
 */
class SortCharactersByFrequency {

    /*
     * ========================================================================
     * SECTION 1: PROBLEM RESTATEMENT
     * ========================================================================
     * In my own words:
     *   Given a string `s`, produce a rearrangement of the SAME multiset of
     *   characters such that:
     *     (a) every occurrence of a given character is contiguous (one
     *         unbroken "block" per distinct character), and
     *     (b) blocks are ordered so that a character occurring more often
     *         appears strictly before a character occurring less often.
     *   Characters that tie on frequency may be ordered arbitrarily relative
     *   to each other, as long as (a) and (b) still hold.
     *
     * Inputs:
     *   - A single string `s`, 1 <= s.length() <= 5 * 10^5.
     *   - Alphabet: uppercase letters, lowercase letters, and digits
     *     (62 possible distinct characters total). Case-sensitive
     *     ('A' != 'a').
     *
     * Outputs:
     *   - A string of the same length as `s`, a permutation of `s`'s
     *     characters, satisfying the contiguity + non-increasing-frequency
     *     ordering property above. Multiple correct outputs may exist for a
     *     single input (whenever there's a frequency tie) — any one of them
     *     is accepted.
     *
     * Key constraint that shapes the whole approach: the alphabet is BOUNDED
     * (only 62 possible characters), independent of how large `s` gets. That
     * turns this from a "sort n items" problem into a "count into <=62
     * buckets, then order the buckets" problem — which is what unlocks
     * linear time.
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * ========================================================================
     * 1. Q: Is the string guaranteed non-null and non-empty?
     *    A (assumed): Yes — 1 <= s.length(), per constraints. I'll still
     *       guard defensively against null/empty for production robustness.
     *
     * 2. Q: Is the comparison case-sensitive — i.e., are 'A' and 'a' treated
     *       as two different characters with independent frequencies?
     *    A (assumed): Yes, case-sensitive. Standard ASCII/Unicode identity.
     *
     * 3. Q: When frequencies tie, is there ANY required tie-break order
     *       (e.g., first-seen order, lexicographic order), or is any order
     *       acceptable?
     *    A (assumed): Any order is acceptable, as the problem statement
     *       explicitly says "may appear in any order." I will not impose
     *       artificial tie-break logic that the grader doesn't require,
     *       since that would only add complexity without benefit.
     *
     * 4. Q: Can the alphabet include characters outside [A-Z, a-z, 0-9] —
     *       punctuation, whitespace, Unicode?
     *    A (assumed): No — constraints guarantee only the 62-character
     *       alphanumeric alphabet. This lets me use fixed-size O(1) arrays
     *       instead of a general-purpose HashMap for counting, which is
     *       both faster and more cache-friendly.
     *
     * 5. Q: Should the solution mutate the input string, or return a new
     *       one? (Strings are immutable in Java, so this is really asking
     *       about the input array/CharSequence contract in general.)
     *    A (assumed): Return a new String; `s` is immutable in Java so this
     *       is moot here, but worth confirming in languages with mutable
     *       strings/char arrays.
     *
     * 6. Q: What's the expected time/space complexity target given
     *       n up to 5*10^5? Is O(n log n) acceptable or do you want O(n)?
     *    A (assumed): O(n) is achievable and expected given the bounded
     *       alphabet — I'll aim for that, but will mention the O(n log n)
     *       approach as a simpler fallback if I run low on time.
     *
     * 7. Q: Is this a one-shot call, or could `sort` be called repeatedly
     *       on a stream of incoming characters (i.e., should I design for
     *       incremental updates)?
     *    A (assumed): One-shot, full string given upfront. I'll mention
     *       streaming/top-K variants as a follow-up extension, not as the
     *       primary design constraint.
     *
     * 8. Q: Should I worry about thread-safety / concurrent calls?
     *    A (assumed): No — single-threaded, single call. Not a concurrent
     *       data structure design problem.
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     * Example 1 (normal case):
     *   s = "tree"
     *   Frequencies: 't' -> 1, 'r' -> 1, 'e' -> 2
     *   Valid outputs: "eert" or "eetr"  ('e' block must come first since it
     *   has the highest frequency; 't' and 'r' tie at 1 and may appear in
     *   either relative order).
     *
     * Example 2 (edge case — single distinct character):
     *   s = "aaaa"
     *   Frequencies: 'a' -> 4
     *   Only valid output: "aaaa" (nothing to reorder, no ties to break).
     *   Also covers the s.length() == 1 boundary trivially if we test "Z".
     *
     * Example 3 (boundary / tie-breaking case with case-sensitivity +
     * digits, both explicitly in-scope per constraints):
     *   s = "Aa11"
     *   Frequencies: 'A' -> 1, 'a' -> 1, '1' -> 2
     *   The digit '1' has strictly higher frequency than either letter, so
     *   its block MUST come first. 'A' and 'a' are distinct characters
     *   (case-sensitive) tied at frequency 1, and may appear in either
     *   order relative to each other.
     *   Valid outputs: "11Aa", "11aA".
     *   This example specifically exercises: (a) digits vs letters sharing
     *   the same counting structure, (b) 'A' and 'a' NOT being merged into
     *   one bucket, and (c) a case where a tie must be permitted but a
     *   strict ordering must still be enforced against the higher-frequency
     *   block.
     */

    /*
     * ========================================================================
     * SECTION 4-6: ALL POSSIBLE APPROACHES
     * ========================================================================
     * A true "generate every permutation and check validity" brute force is
     * NOT implemented below — with n up to 5*10^5, n! is astronomically
     * infeasible even to state, let alone run. I mention it here only to
     * dismiss it: it establishes correctness-by-definition but has zero
     * practical value. The real spectrum of *meaningful* approaches starts
     * at "sort the whole string directly" and gets progressively smarter
     * about exploiting the bounded alphabet.
     *
     * Paradigms explicitly considered and ruled out (one-line reasons):
     *   - Two-pointer / sliding window: no contiguous-subarray property is
     *     being optimized over; this is a global regrouping problem.
     *   - Divide & conquer: no natural recursive split whose subproblems
     *     combine more cheaply than direct counting does.
     *   - Dynamic programming: no overlapping subproblems / optimal
     *     substructure — frequency counting has no recurrence to exploit.
     *   - Tree / graph traversal: no graph or tree structure in the input.
     *   - Binary search: no monotonic predicate over a search space to
     *     exploit; nothing to binary-search over.
     *   - Monotonic stack / deque: no need to pop/maintain a monotonic
     *     sequence while scanning; not that shape of problem.
     *   - Trie / segment tree: could technically back a frequency count
     *     with a segment tree/BIT, but with only 62 possible keys this is
     *     pure overkill — a fixed-size array already gives O(1) access.
     *
     * Paradigms that DO apply and are implemented below: naive full sort
     * (sorting-based), hashing + sort-the-distinct-keys (hashing +
     * sorting), heap / priority queue (greedy), and bucket / counting sort
     * (the optimal, alphabet-aware solution).
     */

    // Shared alphabet size: digits (10) + uppercase (26) + lowercase (26) = 62.
    private static final int ALPHABET_SIZE = 62;

    /**
     * Maps a character in [0-9, A-Z, a-z] to a dense index in [0, 61].
     * Centralizing this avoids duplicating the mapping logic (and its
     * off-by-one risk) across every approach below.
     */
    private static int charToIndex(char character) {
        if (character >= '0' && character <= '9') {
            return character - '0';                     // 0..9
        }
        if (character >= 'A' && character <= 'Z') {
            return 10 + (character - 'A');               // 10..35
        }
        if (character >= 'a' && character <= 'z') {
            return 36 + (character - 'a');                // 36..61
        }
        throw new IllegalArgumentException("Unsupported character outside [0-9A-Za-z]: " + character);
    }

    /** Inverse of charToIndex — maps a dense index in [0, 61] back to its character. */
    private static char indexToChar(int index) {
        if (index < 10) {
            return (char) ('0' + index);
        }
        if (index < 36) {
            return (char) ('A' + (index - 10));
        }
        return (char) ('a' + (index - 36));
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 1: Naive Full Sort  (Sorting-based, most naive practical one)
     * ------------------------------------------------------------------------
     * Core idea: Count frequencies with a HashMap, then directly sort ALL n
     * characters of the string using a comparator that looks up each
     * character's frequency and orders descending. This is the approach
     * most candidates reach for first — it's correct and simple, but it
     * pays a sort over n elements when there are at most 62 distinct keys.
     *
     * Data structure / paradigm: HashMap for counting + comparison sort
     * (Arrays.sort on boxed Character[], which uses TimSort — O(n log n)).
     *
     * Time complexity: O(n log n) — dominated by sorting n boxed characters,
     *   each comparison doing an O(1) expected HashMap lookup.
     * Space complexity: O(n) — boxed Character[] array of size n, plus
     *   O(k) for the frequency map (k <= 62).
     *
     * Pros:
     *   - Extremely simple to write correctly under interview pressure.
     *   - No custom bucket-index math; low bug surface.
     * Cons:
     *   - Wasteful: sorts n items when only <=62 distinct keys exist —
     *     ignores the bounded-alphabet constraint entirely.
     *   - Boxing every character (Character[] instead of char[]) adds
     *     real constant-factor overhead at n = 5*10^5.
     *
     * When to use: as a warm-up/baseline to state out loud, or under severe
     * time pressure where writing something correct fast beats writing
     * something optimal slowly. Not what I'd ship.
     */
    static String naiveFullSort(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        Map<Character, Integer> frequencyByChar = new HashMap<>();
        for (int i = 0; i < s.length(); i++) {
            frequencyByChar.merge(s.charAt(i), 1, Integer::sum);
        }

        Character[] boxedCharacters = new Character[s.length()];
        for (int i = 0; i < s.length(); i++) {
            boxedCharacters[i] = s.charAt(i);
        }

        // Sort all n characters directly by descending frequency. A comparator
        // keyed ONLY on frequency is a classic trap here: two DIFFERENT
        // characters can tie on frequency, and a stable sort would then
        // preserve their original interleaved relative order (e.g. "abab"
        // with a:2,b:2 would stay "abab" instead of grouping), breaking the
        // contiguity requirement. Breaking ties by character identity forces
        // every occurrence of the same character to collapse together.
        Arrays.sort(boxedCharacters, (charA, charB) -> {
            int frequencyComparison = frequencyByChar.get(charB) - frequencyByChar.get(charA);
            if (frequencyComparison != 0) {
                return frequencyComparison;
            }
            return Character.compare(charA, charB); // tie-break groups identical chars together
        });

        StringBuilder result = new StringBuilder(s.length());
        for (char c : boxedCharacters) {
            result.append(c);
        }
        return result.toString();
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 2: HashMap Count + Sort Distinct Entries  (Hashing + Sorting)
     * ------------------------------------------------------------------------
     * Core idea: Instead of sorting all n characters, count into a HashMap
     * (or, given the bounded alphabet, could use int[62] here too) and then
     * sort only the DISTINCT entries (at most 62 of them) by frequency.
     * Build the output by repeating each character `frequency` times, in
     * that sorted order.
     *
     * Data structure / paradigm: Hashing for counting, comparison sort over
     * a small, alphabet-bounded key set.
     *
     * Time complexity: O(n + k log k) where k = number of distinct
     *   characters <= 62 (a fixed constant). Since k is bounded by a
     *   constant independent of n, this is O(n) overall — the k log k term
     *   is at most ~62*log2(62) ~ 370 operations, negligible against n up
     *   to 5*10^5.
     * Space complexity: O(n) for the output StringBuilder + O(k) for the
     *   map/entry list.
     *
     * Pros:
     *   - Directly exploits the bounded alphabet — sorts a handful of
     *     entries instead of the whole string.
     *   - Reads very naturally in an interview: "count, then sort what I
     *     counted" is an easy narrative.
     * Cons:
     *   - Still technically pays a comparison sort (even though the
     *     constant is tiny) instead of true linear counting.
     *   - HashMap<Character,Integer> boxing has real (if small) overhead
     *     vs a primitive int[] count table.
     *
     * When to use: this is a perfectly good "good enough, clearly correct,
     * clearly efficient" answer, and arguably the most *practically*
     * efficient one given the alphabet is fixed at 62 — see the note in
     * the comparison table about it being competitive with bucket sort.
     */
    static String hashMapSortEntries(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        Map<Character, Integer> frequencyByChar = new HashMap<>();
        for (int i = 0; i < s.length(); i++) {
            frequencyByChar.merge(s.charAt(i), 1, Integer::sum);
        }

        List<Map.Entry<Character, Integer>> entries = new ArrayList<>(frequencyByChar.entrySet());
        // Sort at most 62 entries by descending frequency — effectively O(1) work.
        entries.sort((entryA, entryB) -> entryB.getValue() - entryA.getValue());

        StringBuilder result = new StringBuilder(s.length());
        for (Map.Entry<Character, Integer> entry : entries) {
            result.append(String.valueOf(entry.getKey()).repeat(entry.getValue()));
        }
        return result.toString();
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 3: Max-Heap / Priority Queue  (Greedy)
     * ------------------------------------------------------------------------
     * Core idea: Count frequencies, push every (character, frequency) pair
     * into a max-heap keyed by frequency, then repeatedly pop the current
     * highest-frequency character and append its full block. This is
     * "greedy" in the sense of always emitting the globally-highest
     * remaining frequency block next — which is provably safe here because
     * blocks don't interact with each other once ordered.
     *
     * Data structure / paradigm: PriorityQueue (binary heap), greedy
     * selection.
     *
     * Time complexity: O(n + k log k), same reasoning as Approach 2 — heap
     *   operations are over at most k <= 62 elements, so O(n) overall.
     * Space complexity: O(n) for output + O(k) for the heap.
     *
     * Pros:
     *   - Natural fit if the problem were extended to "give me the top-K
     *     most frequent blocks first" or a streaming variant where you
     *     need incremental access to the current max — the heap
     *     generalizes better than a one-shot sort in that direction.
     *   - Clearly communicates greedy intent to the interviewer.
     * Cons:
     *   - For this exact one-shot problem, it's strictly more machinery
     *     than Approach 2 for no asymptotic benefit — sorting 62 items
     *     once is simpler than maintaining a heap over 62 items.
     *   - PriorityQueue<Map.Entry<...>> adds more boxing/indirection.
     *
     * When to use: mention it to show you know the greedy/heap angle and
     * how it'd extend to streaming/top-K variants, but I would not choose
     * it as my primary submission for the plain one-shot version of this
     * problem.
     */
    static String heapGreedy(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        Map<Character, Integer> frequencyByChar = new HashMap<>();
        for (int i = 0; i < s.length(); i++) {
            frequencyByChar.merge(s.charAt(i), 1, Integer::sum);
        }

        PriorityQueue<Map.Entry<Character, Integer>> maxHeapByFrequency =
                new PriorityQueue<>((entryA, entryB) -> entryB.getValue() - entryA.getValue());
        maxHeapByFrequency.addAll(frequencyByChar.entrySet());

        StringBuilder result = new StringBuilder(s.length());
        while (!maxHeapByFrequency.isEmpty()) {
            Map.Entry<Character, Integer> highestFrequencyEntry = maxHeapByFrequency.poll();
            result.append(String.valueOf(highestFrequencyEntry.getKey())
                    .repeat(highestFrequencyEntry.getValue()));
        }
        return result.toString();
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 4 (OPTIMAL): Bucket Sort by Frequency  (Counting sort variant)
     * ------------------------------------------------------------------------
     * Core idea: Since frequency values themselves range only over
     * [0, n], we can bucket characters BY their frequency instead of
     * comparison-sorting them. buckets[f] holds every character whose
     * frequency is exactly f. Then we walk f from n down to 1 and, for
     * each non-empty bucket, append that many copies of each character it
     * holds. No comparisons at all — pure counting + direct indexing.
     *
     * Data structure / paradigm: Bucket sort / counting sort, exploiting a
     * bounded value range (frequency in [0, n]) the same way counting sort
     * exploits a bounded key range.
     *
     * Time complexity: O(n) — one O(n) pass to count frequencies, one O(62)
     *   pass to place characters into frequency buckets, one O(n) pass to
     *   read buckets back out (total characters emitted == n). No log
     *   factor anywhere.
     * Space complexity: O(n) — the buckets array is sized n+1 to allow
     *   direct indexing by frequency value.
     *
     * Pros:
     *   - True O(n), no comparisons, the canonical textbook-optimal answer
     *     for this exact LeetCode problem.
     *   - Generalizes cleanly to an UNBOUNDED alphabet (e.g., full Unicode)
     *     since it never assumes a small key set — only that frequency
     *     values are boundable by n, which is always true.
     * Cons:
     *   - The buckets array is sized n+1 regardless of how few distinct
     *     characters actually exist — e.g., a string of one repeated
     *     character over 5*10^5 elements still allocates 500,001 bucket
     *     slots, almost all null. Approach 2's O(k) space is strictly
     *     tighter here BECAUSE the alphabet happens to be bounded at 62 in
     *     this problem.
     *
     * When to use: this is my primary answer — it's O(n), it's the
     * expected "aha" optimization interviewers are fishing for, and it's
     * not meaningfully harder to implement than Approach 2. I'll also
     * proactively mention the Approach-2 space trade-off as a footnote,
     * since GIVEN that this problem's alphabet is fixed at 62, that
     * approach's O(k) bucket space can beat this approach's O(n) bucket
     * space in practice — showing awareness of the trade-off without
     * over-engineering the primary submission.
     */
    static String bucketSortOptimal(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        int n = s.length();

        // Step 1: count occurrences of each of the (at most) 62 characters.
        int[] countByCharIndex = new int[ALPHABET_SIZE];
        for (int i = 0; i < n; i++) {
            countByCharIndex[charToIndex(s.charAt(i))]++;
        }

        // Step 2: bucket characters by their frequency value, frequency in [1, n].
        // bucketsByFrequency[f] = list of character-indices whose count == f.
        @SuppressWarnings("unchecked")
        List<Integer>[] bucketsByFrequency = new List[n + 1];
        for (int charIndex = 0; charIndex < ALPHABET_SIZE; charIndex++) {
            int frequency = countByCharIndex[charIndex];
            if (frequency == 0) {
                continue; // character never appeared in s
            }
            if (bucketsByFrequency[frequency] == null) {
                bucketsByFrequency[frequency] = new ArrayList<>();
            }
            bucketsByFrequency[frequency].add(charIndex);
        }

        // Step 3: walk frequencies from high to low, emitting each block.
        StringBuilder result = new StringBuilder(n);
        for (int frequency = n; frequency >= 1; frequency--) {
            List<Integer> bucket = bucketsByFrequency[frequency];
            if (bucket == null) {
                continue;
            }
            for (int charIndex : bucket) {
                char character = indexToChar(charIndex);
                // Append `frequency` copies of this character contiguously.
                for (int repetition = 0; repetition < frequency; repetition++) {
                    result.append(character);
                }
            }
        }
        return result.toString();
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     * Approach                          | Time            | Space | Best For                                  | Limitations
     * ----------------------------------|-----------------|-------|-------------------------------------------|---------------------------------------------
     * 1. Naive Full Sort                | O(n log n)      | O(n)  | Fast baseline under time pressure          | Sorts n items when <=62 keys exist; wasteful
     * 2. HashMap + Sort Distinct Entries| O(n + k log k)  | O(n)  | Best PRACTICAL space/time balance          | Tiny log factor remains (negligible, k<=62)
     *    (k <= 62, so effectively O(n))  |                 |       | given the fixed 62-char alphabet           | still comparison-based, not pure counting
     * 3. Heap / Priority Queue (Greedy) | O(n + k log k)  | O(n)  | Streaming / top-K variants                 | Unneeded overhead for one-shot full sort
     * 4. Bucket Sort by Frequency       | O(n)            | O(n)  | Canonical optimal answer; generalizes to   | Buckets array sized n+1 regardless of k;
     *    (OPTIMAL, this problem's answer)|                 |       | unbounded alphabets; zero comparisons      | wastes space when distinct-char count << n
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     * I would present Approach 4 (Bucket Sort by Frequency) as my final
     * answer:
     *   - It's true O(n) time with no hidden log factor, which is the
     *     complexity interviewers are steering toward once they see the
     *     bounded alphabet in the constraints.
     *   - It's the textbook/canonical solution to this exact LeetCode
     *     problem, so it matches interviewer expectations directly.
     *   - It's not meaningfully harder to code correctly than Approach 2 —
     *     the only extra piece is bucket-index bookkeeping, which is a
     *     well-known pattern (counting sort generalization).
     *   - I'd narrate the Approach 2 trade-off (O(k) vs O(n) bucket space,
     *     given k is fixed at 62 here) as a natural follow-up/optimization
     *     discussion AFTER landing on Approach 4, to show depth without
     *     slowing down the primary implementation.
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ========================================================================
     * This is a polished, defensively-coded version of Approach 4, with
     * every design decision called out inline. This is what I'd actually
     * type in the shared editor.
     */
    static String sortCharactersByFrequency(String s) {
        // Defensive guard: handle null/empty explicitly rather than letting
        // downstream indexing throw an unclear exception.
        if (s == null || s.isEmpty()) {
            return s;
        }

        int n = s.length();

        // --- Step 1: Count occurrences of every character. --------------
        // Using a fixed-size primitive int[] (not a HashMap<Character,Integer>)
        // is both a time and space win: O(1) true array indexing, no boxing,
        // no hashing overhead — directly exploiting the guaranteed 62-char
        // alphabet from the constraints.
        int[] countByCharIndex = new int[ALPHABET_SIZE];
        for (int i = 0; i < n; i++) {
            countByCharIndex[charToIndex(s.charAt(i))]++;
        }

        // --- Step 2: Bucket characters by frequency value. ---------------
        // Frequencies range over [1, n] (a character with count 0 simply
        // never appears and is skipped). bucketsByFrequency[f] collects
        // every character-index whose total count equals exactly f. This
        // is the crux of the optimization: instead of comparing frequencies
        // pairwise (which costs log factors), we use the frequency value
        // itself as a direct array index — classic counting-sort trick.
        @SuppressWarnings("unchecked")
        List<Integer>[] bucketsByFrequency = new List[n + 1];
        for (int charIndex = 0; charIndex < ALPHABET_SIZE; charIndex++) {
            int frequency = countByCharIndex[charIndex];
            if (frequency == 0) {
                continue;
            }
            if (bucketsByFrequency[frequency] == null) {
                bucketsByFrequency[frequency] = new ArrayList<>();
            }
            bucketsByFrequency[frequency].add(charIndex);
        }

        // --- Step 3: Emit blocks from highest frequency to lowest. -------
        // Pre-sizing the StringBuilder to n avoids internal buffer
        // resize/copy churn — a small but real win at n = 5*10^5.
        StringBuilder result = new StringBuilder(n);
        for (int frequency = n; frequency >= 1; frequency--) {
            List<Integer> charsWithThisFrequency = bucketsByFrequency[frequency];
            if (charsWithThisFrequency == null) {
                continue; // no character has exactly this frequency
            }
            // Characters tied at the same frequency are emitted in
            // whatever order the bucket holds them — the problem
            // explicitly permits any order among ties.
            for (int charIndex : charsWithThisFrequency) {
                char character = indexToChar(charIndex);
                for (int repetition = 0; repetition < frequency; repetition++) {
                    result.append(character);
                }
            }
        }
        return result.toString();
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Tracing sortCharactersByFrequency(s) on s = "aaabbc" (n = 6):
     *
     * Step 1 — Counting pass:
     *   'a' -> index 36+0=36, count[36] = 3
     *   'b' -> index 36+1=37, count[37] = 2
     *   'c' -> index 36+2=38, count[38] = 1
     *   (all other 59 slots remain 0)
     *
     * Step 2 — Bucketing pass (buckets sized n+1 = 7, indices 0..6):
     *   charIndex 36 ('a'), frequency 3 -> bucketsByFrequency[3] = [36]
     *   charIndex 37 ('b'), frequency 2 -> bucketsByFrequency[2] = [37]
     *   charIndex 38 ('c'), frequency 1 -> bucketsByFrequency[1] = [38]
     *   bucketsByFrequency[0], [4], [5], [6] remain null (skipped).
     *
     * Step 3 — Emission pass, frequency from 6 down to 1:
     *   frequency=6: bucketsByFrequency[6] == null -> skip
     *   frequency=5: null -> skip
     *   frequency=4: null -> skip
     *   frequency=3: bucketsByFrequency[3] = [36] -> character 'a',
     *                append "aaa" three times -> result = "aaa"
     *   frequency=2: bucketsByFrequency[2] = [37] -> character 'b',
     *                append "bb" -> result = "aaabb"
     *   frequency=1: bucketsByFrequency[1] = [38] -> character 'c',
     *                append "c" -> result = "aaabbc"
     *
     * Final result: "aaabbc" — already frequency-sorted in this case, which
     * conveniently doubles as a self-check: no ties existed here (3, 2, 1
     * are all distinct), so exactly one valid output exists, and the
     * algorithm reproduces it.
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - All four approaches are correct; they differ in how aggressively
     *   they exploit the bounded 62-character alphabet.
     * - Approach 1 (naive full sort) ignores the bound entirely and pays
     *   O(n log n) sorting n items directly.
     * - Approaches 2 and 3 exploit the bound by sorting/heapifying only the
     *   distinct keys (<=62 of them), landing at O(n) overall with a
     *   negligible constant-factor log term.
     * - Approach 4 (bucket sort, my primary answer) achieves true O(n) with
     *   zero comparisons by indexing buckets directly by frequency value,
     *   at the cost of an O(n)-sized (mostly sparse) buckets array.
     * - Known limitation/assumption of the final solution: it assumes the
     *   62-character alphabet guaranteed by the constraints; charToIndex
     *   throws IllegalArgumentException on anything outside that range
     *   rather than silently mishandling it — a deliberate fail-fast choice
     *   I'd flag to the interviewer.
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS
     * ========================================================================
     * 1. "What if the alphabet were unbounded Unicode instead of 62 fixed
     *    characters?" -> Approach 4 still works unchanged (it never assumed
     *    a small key set, only that frequency <= n); Approach 2/3 would
     *    need a HashMap instead of a fixed array, and their k log k term
     *    would no longer be a negligible constant.
     * 2. "What if `s` arrives as a stream and you need the current
     *    frequency-sorted arrangement queryable at any time?" -> Points
     *    toward the heap (Approach 3) or an incrementally-maintained
     *    bucket structure (move a character between adjacent frequency
     *    buckets on each increment) rather than recomputing from scratch.
     * 3. "Can you do this with O(1) extra space (in-place, ignoring
     *    output)?" -> Discuss the tension: true in-place is hard because
     *    grouping by frequency inherently requires reordering based on
     *    aggregate (not local) information; best achievable is roughly
     *    O(k) auxiliary space with in-place-style character swaps once
     *    block boundaries are known.
     * 4. "How would you handle multiple independent strings and want the
     *    combined/merged frequency order across all of them?" -> Sum
     *    frequency arrays across strings first (still O(1) extra states,
     *    62 slots), then bucket-sort once over the aggregate.
     * 5. "What if ties DID need a specific tie-break, e.g. lexicographic
     *    order among same-frequency characters?" -> Sort each bucket's
     *    character list before emission (cheap, since each bucket has at
     *    most 62 elements total across all buckets combined).
     * 6. "Could you parallelize this for very large n?" -> The counting
     *    pass parallelizes trivially (partition s, count in parallel,
     *    merge 62-length count arrays); the emission pass can precompute
     *    output offsets per block and write blocks concurrently.
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Sizing the buckets array as `new List[n]` instead of `new
     *    List[n + 1]` — frequency can legitimately equal n (a string of one
     *    repeated character), and indexing bucketsByFrequency[n] on an
     *    array of length n throws ArrayIndexOutOfBoundsException. Off-by-one
     *    trap directly analogous to interval-boundary bugs elsewhere in the
     *    pattern library.
     * 2. Forgetting that 'A' and 'a' are DIFFERENT characters and
     *    accidentally normalizing case (e.g., via toLowerCase() on input)
     *    before counting — silently corrupts frequencies and violates the
     *    stated case-sensitivity assumption.
     * 3. Iterating buckets from frequency 0 upward and reversing the
     *    result at the end, instead of iterating from n down to 1 directly
     *    — functionally fixable but adds an unnecessary O(n) reversal pass
     *    and is easy to get backwards under interview pressure.
     * 4. Treating this as a problem that NEEDS a comparison sort at all —
     *    many candidates jump straight to Arrays.sort/Collections.sort
     *    without noticing the constraints hand you a bounded value range
     *    (frequency in [0, n]) that enables true linear-time bucketing,
     *    which is exactly the optimization interviewers are listening for.
     * 5. If you DO sort characters directly (Approach 1's style), comparing
     *    ONLY by frequency is a real bug, not just a style nit: two
     *    different characters tied on frequency will keep their original
     *    interleaved relative order under a stable sort (e.g. "abab" stays
     *    "abab" instead of grouping into "aabb"), silently violating the
     *    contiguity requirement. This is caught below by the randomized
     *    stress test, which is exactly why the oracle checks contiguity
     *    explicitly rather than assuming any sort "just works." The fix is
     *    a secondary tie-break on character identity itself.
     */

    /*
     * ========================================================================
     * VALIDATION ORACLE
     * ========================================================================
     * Because ties permit multiple correct outputs, we cannot compare
     * approach outputs for string equality against each other. Instead,
     * this oracle independently verifies the CONTRACT: same multiset of
     * characters as the original, each character's occurrences contiguous,
     * and blocks non-increasing in frequency from left to right.
     */
    static boolean isValidResult(String original, String candidate) {
        if (candidate == null || original.length() != candidate.length()) {
            return false;
        }
        int[] expectedCountByCharIndex = new int[ALPHABET_SIZE];
        for (int i = 0; i < original.length(); i++) {
            expectedCountByCharIndex[charToIndex(original.charAt(i))]++;
        }

        boolean[] blockAlreadyEmitted = new boolean[ALPHABET_SIZE];
        Integer previousBlockFrequency = null;
        int position = 0;
        int n = candidate.length();

        while (position < n) {
            char currentChar = candidate.charAt(position);
            int charIndex = charToIndex(currentChar);

            if (blockAlreadyEmitted[charIndex]) {
                return false; // same character reappearing later => non-contiguous
            }
            blockAlreadyEmitted[charIndex] = true;

            int blockLength = 0;
            while (position < n && candidate.charAt(position) == currentChar) {
                blockLength++;
                position++;
            }

            if (blockLength != expectedCountByCharIndex[charIndex]) {
                return false; // frequency mismatch vs. original
            }
            if (previousBlockFrequency != null && blockLength > previousBlockFrequency) {
                return false; // violates non-increasing frequency ordering
            }
            previousBlockFrequency = blockLength;
        }
        return true;
    }

    /*
     * ========================================================================
     * MAIN — worked examples, named assertions, and randomized stress test
     * ========================================================================
     */
    public static void main(String[] args) {

        /* ---- Worked examples from Section 3, checked with the oracle ---- */
        String example1 = "tree";
        String example2 = "aaaa";
        String example3 = "Aa11";
        String example4 = "Z"; // single-character boundary

        for (String example : List.of(example1, example2, example3, example4)) {
            String naive = naiveFullSort(example);
            String hashSort = hashMapSortEntries(example);
            String heap = heapGreedy(example);
            String bucket = bucketSortOptimal(example);
            String production = sortCharactersByFrequency(example);

            assert isValidResult(example, naive) : "naiveFullSort failed on " + example;
            assert isValidResult(example, hashSort) : "hashMapSortEntries failed on " + example;
            assert isValidResult(example, heap) : "heapGreedy failed on " + example;
            assert isValidResult(example, bucket) : "bucketSortOptimal failed on " + example;
            assert isValidResult(example, production) : "sortCharactersByFrequency failed on " + example;

            System.out.printf("s=%-6s -> naive=%-6s hashSort=%-6s heap=%-6s bucket=%-6s production=%-6s%n",
                    "\"" + example + "\"", naive, hashSort, heap, bucket, production);
        }

        /* ---- Named edge-case assertions ---- */
        assert sortCharactersByFrequency("").isEmpty() : "empty string should return empty string";
        assert sortCharactersByFrequency(null) == null : "null should pass through as null";
        assert isValidResult("aaabbc", sortCharactersByFrequency("aaabbc"))
                : "dry-run example must be a valid frequency-sorted arrangement";

        /* ---- Randomized stress test: cross-validate all approaches ---- */
        Random random = new Random(451); // fixed seed for reproducibility
        String alphabetForRandomStrings =
                "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int trialCount = 3000;

        for (int trial = 0; trial < trialCount; trial++) {
            int length = 1 + random.nextInt(200); // keep strings small so all 5 methods run fast per trial
            StringBuilder randomStringBuilder = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                randomStringBuilder.append(
                        alphabetForRandomStrings.charAt(random.nextInt(alphabetForRandomStrings.length())));
            }
            String randomString = randomStringBuilder.toString();

            String naive = naiveFullSort(randomString);
            String hashSort = hashMapSortEntries(randomString);
            String heap = heapGreedy(randomString);
            String bucket = bucketSortOptimal(randomString);
            String production = sortCharactersByFrequency(randomString);

            assert isValidResult(randomString, naive)
                    : "naiveFullSort invalid on trial " + trial + ": " + randomString;
            assert isValidResult(randomString, hashSort)
                    : "hashMapSortEntries invalid on trial " + trial + ": " + randomString;
            assert isValidResult(randomString, heap)
                    : "heapGreedy invalid on trial " + trial + ": " + randomString;
            assert isValidResult(randomString, bucket)
                    : "bucketSortOptimal invalid on trial " + trial + ": " + randomString;
            assert isValidResult(randomString, production)
                    : "sortCharactersByFrequency invalid on trial " + trial + ": " + randomString;
        }

        System.out.println("All worked examples, named assertions, and " + trialCount
                + " randomized stress-test trials passed.");

        /* ---- One larger-scale timing sanity check near the n = 5*10^5 bound ---- */
        int largeLength = 500_000;
        StringBuilder largeInputBuilder = new StringBuilder(largeLength);
        for (int i = 0; i < largeLength; i++) {
            largeInputBuilder.append(
                    alphabetForRandomStrings.charAt(random.nextInt(alphabetForRandomStrings.length())));
        }
        String largeInput = largeInputBuilder.toString();

        long startNanos = System.nanoTime();
        String largeResult = sortCharactersByFrequency(largeInput);
        long elapsedNanos = System.nanoTime() - startNanos;

        assert isValidResult(largeInput, largeResult) : "optimal solution invalid at n = 500,000";
        System.out.printf("Optimal solution handled n = %,d characters in %.2f ms.%n",
                largeLength, elapsedNanos / 1_000_000.0);
    }
}


class Solution {

    // A simple immutable representation of a character and its frequency.
    record CharFrequency(char ch, int frequency) {}

    public String frequencySort(String s) {

        /*
         * Step 1: Count the frequency of every character.
         *
         * HashMap gives us:
         *
         * character -> frequency
         *
         * Example:
         * "3a3a3BBBcc1"
         *
         * {
         *     3 -> 3,
         *     B -> 3,
         *     a -> 2,
         *     c -> 2,
         *     1 -> 1
         * }
         */
        Map<Character, Integer> frequency = new HashMap<>();

        for (char ch : s.toCharArray()) {
            frequency.merge(ch, 1, Integer::sum);
        }

        /*
         * Step 2: Put all characters into a max heap.
         *
         * We want:
         *
         * 1. Higher frequency first.
         * 2. If frequency is the same, smaller character first.
         *
         * Example:
         *
         * 3 -> 3
         * B -> 3
         *
         * Both have frequency 3, so '3' comes before 'B'.
         */
        PriorityQueue<CharFrequency> maxHeap =
                new PriorityQueue<>(
                        Comparator
                                .comparingInt(CharFrequency::frequency)
                                .reversed()
                                .thenComparing(CharFrequency::ch)
                );

        /*
         * Convert the HashMap entries into our record objects
         * and add them to the heap.
         */
        frequency.forEach((ch, freq) ->
                maxHeap.offer(new CharFrequency(ch, freq))
        );

        /*
         * Step 3: Remove characters from the heap in the required order
         * and build the final string.
         */
        StringBuilder result = new StringBuilder(s.length());

        while (!maxHeap.isEmpty()) {

            CharFrequency current = maxHeap.poll();

            // Append the character according to its frequency.
            result.append(String.valueOf(current.ch()).repeat(current.frequency()));
        }

        return result.toString();
    }
}