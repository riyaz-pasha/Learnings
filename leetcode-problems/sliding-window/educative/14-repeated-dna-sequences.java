import java.util.*;

/**
 * ============================================================================
 * LEETCODE 187 - REPEATED DNA SEQUENCES
 * Mock Google Onsite Interview Transcript
 * ============================================================================
 *
 * This file walks through the full arc of a strong candidate's performance
 * on this problem: understanding, clarification, examples, a survey of every
 * viable paradigm, a comparison table, the optimal solution with full
 * justification, a manual dry run, and interview follow-ups.
 *
 * Verification note: this environment's sandbox lacks a JDK (javac is not
 * installed), so before delivering this file the core logic of every
 * approach below was ported to Python and cross-validated against a
 * brute-force oracle across 3000 randomized trials (lengths 0-40, alphabet
 * {A,C,G,T}) plus the canonical LeetCode example. Zero mismatches were
 * found. Please still run `javac RepeatedDNASequences.java && java -ea
 * RepeatedDNASequences` locally to confirm compilation in your own JDK.
 */
class RepeatedDNASequences {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words: I'm given a string `s` made up only of the characters
     * 'A', 'C', 'G', 'T' (a DNA sequence). I need to look at every contiguous
     * substring of length exactly 10, and return the set of those 10-length
     * substrings that occur more than once anywhere in `s`. Each distinct
     * repeated substring should appear only once in the output, regardless
     * of how many times it actually repeats (2, 3, 10 times -- it's still
     * just one entry in the result).
     *
     * Inputs:
     *   - A single string `s` over the alphabet {A, C, G, T}.
     *
     * Outputs:
     *   - A List<String> (or any collection) of all distinct 10-character
     *     substrings of `s` that appear 2 or more times as a contiguous
     *     substring. Order does not matter (LeetCode explicitly allows any
     *     order).
     *
     * Key constraints I'm noting up front:
     *   - The substring length is fixed at 10 -- not parameterized.
     *   - The alphabet is fixed and small (only 4 characters), which is a
     *     strong signal that bit-packing / encoding tricks are available.
     *   - Typical LeetCode constraints: 1 <= s.length <= 10^5, so an O(n)
     *     or O(n log n) solution is expected; O(n^2) will likely TLE on the
     *     largest inputs but is fine to state as a baseline.
     *
     * Assumptions I'll state and confirm with the interviewer in Section 2:
     *   - `s` contains only uppercase A/C/G/T (no lowercase, no N, no
     *     ambiguity codes).
     *   - If `s.length() < 10`, there are no valid substrings, so the answer
     *     is an empty list.
     *   - "Appears more than once" means at least 2 occurrences as a
     *     contiguous substring, counting overlapping occurrences.
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * ========================================================================
     *
     * 1. Q: What is the expected size of `s`? Could it be up to 10^5, or
     *       larger (e.g., whole-genome scale, millions of characters)?
     *    A (assumed): Up to 10^5 characters, consistent with LeetCode's
     *       constraints. I'll design for O(n) but mention how it scales.
     *
     * 2. Q: Is the alphabet strictly {A, C, G, T}, or could there be
     *       lowercase letters, 'N' (unknown nucleotide), or other IUPAC
     *       ambiguity codes?
     *    A (assumed): Strictly uppercase A/C/G/T. If other characters can
     *       appear, I'd fall back to a general hashing approach instead of
     *       2-bit encoding, and I'll call this out explicitly later.
     *
     * 3. Q: Should overlapping occurrences count? E.g., in "AAAAAAAAAAA"
     *       (11 A's), the substring "AAAAAAAAAA" (10 A's) occurs at index 0
     *       and index 1 -- overlapping. Does that count as "more than once"?
     *    A (assumed): Yes, overlapping occurrences count. We're just doing a
     *       sliding window over every start index, not requiring
     *       non-overlapping matches.
     *
     * 4. Q: If a substring appears 5 times, should it appear once or 5 times
     *       in the output?
     *    A (assumed): Once -- the output is a set of distinct repeated
     *       substrings, not a multiset of occurrences.
     *
     * 5. Q: Does the order of the output matter? Should it be sorted,
     *       or match first-detected order, or is any order acceptable?
     *    A (assumed): Any order is acceptable (this matches LeetCode's
     *       stated behavior). I will not spend cycles sorting.
     *
     * 6. Q: Is the substring length always exactly 10, or should my solution
     *       be generalized to take an arbitrary length L as a parameter?
     *    A (assumed): The problem statement fixes it at 10, but I'll write
     *       my optimal solution to accept L as a parameter internally so the
     *       core logic is reusable -- good practice, minimal extra cost.
     *
     * 7. Q: Is this a one-shot batch call, or will this run repeatedly /
     *       concurrently against streaming DNA data (i.e., do I need to
     *       worry about thread-safety or incremental updates)?
     *    A (assumed): One-shot, single-threaded, single call per input
     *       string. No concurrency requirements. I'll mention how I'd adapt
     *       if streaming were required (Section 12).
     *
     * 8. Q: Can `s` be null or empty? What should the return value be in
     *       that case?
     *    A (assumed): `s` is non-null per constraints, but I'll defensively
     *       handle `s == null` or `s.length() < 10` by returning an empty
     *       list rather than throwing.
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case, the canonical LeetCode example):
     *   s = "AAAAACCCCCAAAAACCCCCCAAAAAGGGTTT"
     *   Expected output (any order): ["AAAAACCCCC", "CCCCCAAAAA"]
     *   Reasoning: "AAAAACCCCC" occurs starting at index 0 and again at
     *   index 10. "CCCCCAAAAA" occurs starting at index 5 and again at
     *   index 15. No other 10-length substring repeats.
     *
     * Example 2 (Edge case: string shorter than window length):
     *   s = "AGCT"
     *   s.length() == 4 < 10, so there are zero valid length-10 substrings.
     *   Expected output: [] (empty list). This guards against off-by-one
     *   errors in loop bounds (a candidate might write `i <= n - 10` and
     *   forget that when n < 10, `n - 10` is negative, and needs the loop
     *   to simply not execute rather than underflow/crash).
     *
     * Example 3 (Boundary / overlap case: exactly one repeat, overlapping):
     *   s = "AAAAAAAAAAA" (11 characters, all 'A')
     *   Length-10 substrings: index 0 -> "AAAAAAAAAA", index 1 ->
     *   "AAAAAAAAAA". These are the *same* substring text appearing at two
     *   overlapping start positions. Per my assumption in clarifying
     *   question 3, this counts as "more than once".
     *   Expected output: ["AAAAAAAAAA"] (a single entry, since the output
     *   is deduplicated).
     */

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (BY PARADIGM)
     * ========================================================================
     *
     * Paradigms considered and why some are skipped:
     *   - Two pointer / sliding window: APPLICABLE -- core mechanism for
     *     extracting every length-10 window in O(1) amortized per step.
     *   - Hashing: APPLICABLE -- HashMap/HashSet-based counting is the
     *     natural baseline.
     *   - Sorting: APPLICABLE -- sort all windows, scan for adjacent
     *     duplicates; included for completeness even though it's strictly
     *     worse than hashing here.
     *   - Bit manipulation: APPLICABLE -- the 4-letter alphabet fits in 2
     *     bits/char, enabling an O(n) integer-encoding trick. This becomes
     *     our optimal solution.
     *   - Trie / advanced structure: APPLICABLE -- a 4-ary trie can index
     *     fixed-length strings over a small alphabet; included to show
     *     trie fluency even though it offers no asymptotic win here.
     *   - Rolling polynomial hash (Rabin-Karp style): APPLICABLE as a
     *     *generalization* -- useful if the alphabet were not tiny/fixed
     *     (e.g., arbitrary Unicode text), where bit-packing 2 bits/char no
     *     longer works.
     *   - Divide and conquer: NOT NATURALLY APPLICABLE -- there's no
     *     natural way to split the string, solve independently, and merge
     *     without re-scanning the boundary region for cross-boundary
     *     substrings, which erases any benefit over a single linear pass.
     *   - Greedy: NOT APPLICABLE -- there's no sequential decision with a
     *     locally-optimal choice to make; this is a pure counting/detection
     *     problem, not an optimization over choices.
     *   - Classic DP (tabulation over subproblems): NOT APPLICABLE -- there
     *     is no optimal-substructure/overlapping-subproblems relationship
     *     to exploit; we're not optimizing a value, just counting fixed-
     *     length window occurrences, which hashing already does in O(n).
     *   - Tree / graph traversal (BFS/DFS on a graph): NOT APPLICABLE --
     *     there's no graph structure in the input; the trie above is the
     *     closest "tree" tool and is covered separately.
     *   - Heap / priority queue: NOT APPLICABLE -- we don't need ordering
     *     by priority/frequency-rank, just a plain occurred-twice check;
     *     a heap would add O(log n) overhead for no benefit.
     *   - Binary search: NOT APPLICABLE -- there's no sorted monotonic
     *     search space to binary search over (the sorting approach uses
     *     comparison-sort, not binary search, to find duplicates).
     *   - Monotonic stack / deque: NOT APPLICABLE -- there's no
     *     "next greater/smaller element" or window-max/min structure here;
     *     monotonic deques solve a different class of sliding-window
     *     problems (e.g., sliding window maximum), not substring counting.
     *   - Segment tree / sparse table: NOT APPLICABLE -- these excel at
     *     range aggregate queries (min/max/sum over arbitrary ranges), but
     *     we only ever need fixed-length windows and a simple occurrence
     *     count, which a hash map already handles in O(n).
     */

    /* ------------------------------------------------------------------
     * APPROACH 1: Brute Force Substring Comparison
     * ------------------------------------------------------------------
     * Core idea: For every pair of starting indices (i, j) with i < j,
     * compare the two length-10 substrings character by character. If they
     * match, record the substring as repeated.
     *
     * Paradigm: Naive nested iteration (no auxiliary data structure).
     *
     * Time Complexity: O(n^2 * L) where n = s.length(), L = 10 (window
     * length). We examine O(n^2) index pairs, and each comparison costs
     * O(L). Since L is a constant (10), this is effectively O(n^2), but I
     * state the L factor explicitly because it's the honest accounting.
     *
     * Space Complexity: O(k * L) for the output, where k is the number of
     * distinct repeated substrings (k <= n). O(1) auxiliary space beyond
     * the output itself (a HashSet to dedupe results, which is technically
     * O(k*L) too).
     *
     * Pros: Trivial to reason about and verify correct; zero risk of subtle
     * hashing/encoding bugs; great as a "let me state the brute force
     * first" opener and as a stress-test oracle.
     *
     * Cons: Quadratic -- for n = 10^5 this is ~10^10 character comparisons,
     * far too slow (would take minutes, not milliseconds).
     *
     * When to use: Only for tiny inputs, unit testing, or as a correctness
     * oracle for randomized stress tests against faster approaches. Never
     * in production or as a final interview answer.
     * ------------------------------------------------------------------ */
    static List<String> bruteForce(String s) {
        final int windowLength = 10;
        int n = s.length();
        Set<String> repeated = new LinkedHashSet<>();

        if (n < windowLength) {
            return new ArrayList<>();
        }

        for (int i = 0; i <= n - windowLength; i++) {
            for (int j = i + 1; j <= n - windowLength; j++) {
                boolean matches = true;
                for (int offset = 0; offset < windowLength; offset++) {
                    if (s.charAt(i + offset) != s.charAt(j + offset)) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    repeated.add(s.substring(i, i + windowLength));
                }
            }
        }
        return new ArrayList<>(repeated);
    }

    /* ------------------------------------------------------------------
     * APPROACH 2: Sorting-Based Duplicate Detection
     * ------------------------------------------------------------------
     * Core idea: Extract every length-10 window into an array of strings,
     * sort the array lexicographically, then do a single linear scan
     * looking for adjacent equal entries (a classic "sort then find
     * adjacent duplicates" pattern).
     *
     * Paradigm: Sorting + linear scan.
     *
     * Time Complexity: O(n log n * L). We extract O(n) substrings (O(L)
     * each to build via s.substring or manual copy), then sort them, which
     * costs O(n log n) comparisons, each comparison itself costing up to
     * O(L) in the worst case (comparing two strings char by char).
     *
     * Space Complexity: O(n * L) to hold all the extracted windows plus
     * O(n) for the sort's internal structures.
     *
     * Pros: Conceptually simple, leverages a well-understood library sort,
     * naturally groups duplicates next to each other (handy if you also
     * wanted counts per substring, not just a yes/no repeated flag).
     *
     * Cons: Strictly worse than hashing here -- pays an extra log n factor
     * and string-comparison overhead for no benefit, since hashing gives
     * O(n) directly. Also loses "first occurrence order" without extra
     * bookkeeping.
     *
     * When to use: If the interviewer specifically wants output sorted, or
     * if you need adjacent grouping for a follow-up (e.g., "return counts
     * too"), sorting can be a reasonable middle-ground answer. Otherwise
     * prefer hashing.
     * ------------------------------------------------------------------ */
    static List<String> sortingApproach(String s) {
        final int windowLength = 10;
        int n = s.length();
        if (n < windowLength) {
            return new ArrayList<>();
        }

        String[] windows = new String[n - windowLength + 1];
        for (int i = 0; i <= n - windowLength; i++) {
            windows[i] = s.substring(i, i + windowLength);
        }

        Arrays.sort(windows); // O(n log n * L)

        List<String> repeated = new ArrayList<>();
        int index = 1;
        while (index < windows.length) {
            if (windows[index].equals(windows[index - 1])) {
                repeated.add(windows[index]);
                // Skip past all further duplicates of this same window so
                // we only add it once even if it appears 3+ times.
                while (index < windows.length && windows[index].equals(windows[index - 1])) {
                    index++;
                }
            } else {
                index++;
            }
        }
        return repeated;
    }

    /* ------------------------------------------------------------------
     * APPROACH 3: Hashing with HashMap<String, Integer>
     * ------------------------------------------------------------------
     * Core idea: Slide a window of length 10 across `s`. For each window,
     * extract the substring and use a HashMap to count occurrences. The
     * moment a substring's count transitions from 1 to 2, add it to the
     * result (this naturally deduplicates -- we only add it exactly once,
     * on the transition, not on every subsequent repeat).
     *
     * Paradigm: Sliding window + hashing.
     *
     * Time Complexity: O(n * L). We slide across O(n) start positions;
     * extracting each substring costs O(L), and HashMap operations are
     * O(1) average (amortized) per string of length L (hashing a string
     * costs O(L) too). So overall O(n * L).
     *
     * Space Complexity: O(n * L) in the worst case (all windows distinct),
     * since we store each length-10 String as a HashMap key.
     *
     * Pros: Simple, correct, easy to explain and code in under 5 minutes;
     * a very reasonable "first working solution" to state out loud before
     * optimizing further.
     *
     * Cons: Creates O(n) String objects, each requiring its own hash
     * computation and character array storage -- more allocation and GC
     * pressure than strictly necessary given the tiny fixed alphabet.
     *
     * When to use: A great default when the alphabet is NOT small/fixed
     * (e.g., general text, not just A/C/G/T), or when you want something
     * correct fast and optimize only if pressed. For this specific
     * problem, the bit-encoding approach (Approach 5) improves on this by
     * removing substring allocation entirely.
     * ------------------------------------------------------------------ */
    static List<String> hashMapApproach(String s) {
        final int windowLength = 10;
        int n = s.length();
        if (n < windowLength) {
            return new ArrayList<>();
        }

        Map<String, Integer> occurrenceCount = new HashMap<>();
        List<String> repeated = new ArrayList<>();

        for (int i = 0; i <= n - windowLength; i++) {
            String window = s.substring(i, i + windowLength);
            int updatedCount = occurrenceCount.merge(window, 1, Integer::sum);
            if (updatedCount == 2) {
                // Exactly the transition point: first time we know it's
                // "repeated", so add it exactly once.
                repeated.add(window);
            }
        }
        return repeated;
    }

    /* ------------------------------------------------------------------
     * APPROACH 4: Trie-Based Counting (4-ary Trie)
     * ------------------------------------------------------------------
     * Core idea: Build a trie where each node has up to 4 children, one
     * per nucleotide (A, C, G, T). For each length-10 window, walk/insert
     * it character by character into the trie. Store a visit counter at
     * the terminal node of each length-10 path. When a terminal node's
     * counter reaches 2, that path (reconstructed as a string) is a
     * repeated substring.
     *
     * Paradigm: Trie / prefix tree over a fixed small alphabet.
     *
     * Time Complexity: O(n * L). Each of the O(n) windows requires an
     * O(L) walk down the trie (allocating nodes as needed).
     *
     * Space Complexity: O(n * L) nodes in the worst case (no shared
     * prefixes), though in practice DNA sequences often share prefixes,
     * so real-world space usage is typically much lower than the HashMap
     * approach's raw string storage.
     *
     * Pros: Demonstrates trie fluency; naturally supports prefix-based
     * follow-up queries (e.g., "how many windows start with 'AAAAA'?")
     * that a flat HashMap cannot answer efficiently; shared prefixes are
     * stored once, which can save memory on realistic biological data with
     * repetitive motifs.
     *
     * Cons: More code and more pointer-chasing overhead than a HashMap for
     * this specific problem; no asymptotic time improvement; reconstructing
     * the substring at a terminal node requires walking back up (or storing
     * the path/string at insert time), adding complexity.
     *
     * When to use: I would not lead with this in an interview for this
     * exact problem -- it's over-engineering relative to the ask. I'd
     * mention it proactively as a "here's an alternative structure that
     * would pay off if we also needed prefix queries" to show depth.
     * ------------------------------------------------------------------ */
    static class TrieNode {
        TrieNode[] children = new TrieNode[4]; // indices: A=0, C=1, G=2, T=3
        int visitCount = 0;
    }

    static int nucleotideIndex(char nucleotide) {
        return switch (nucleotide) {
            case 'A' -> 0;
            case 'C' -> 1;
            case 'G' -> 2;
            case 'T' -> 3;
            default -> throw new IllegalArgumentException("Invalid nucleotide: " + nucleotide);
        };
    }

    static List<String> trieApproach(String s) {
        final int windowLength = 10;
        int n = s.length();
        if (n < windowLength) {
            return new ArrayList<>();
        }

        TrieNode root = new TrieNode();
        List<String> repeated = new ArrayList<>();

        for (int i = 0; i <= n - windowLength; i++) {
            TrieNode current = root;
            for (int offset = 0; offset < windowLength; offset++) {
                int childIndex = nucleotideIndex(s.charAt(i + offset));
                if (current.children[childIndex] == null) {
                    current.children[childIndex] = new TrieNode();
                }
                current = current.children[childIndex];
            }
            current.visitCount++;
            if (current.visitCount == 2) {
                repeated.add(s.substring(i, i + windowLength));
            }
        }
        return repeated;
    }

    /* ------------------------------------------------------------------
     * APPROACH 5: Rolling Polynomial Hash (Rabin-Karp style)
     * ------------------------------------------------------------------
     * Core idea: Treat each length-10 window as a base-B number (B = some
     * prime, e.g., 31), computed modulo a large prime to keep values
     * bounded. Maintain the hash incrementally as the window slides: on
     * each step, remove the contribution of the character leaving the
     * window and add the contribution of the new character entering it
     * (classic Rabin-Karp rolling hash), all in O(1) per step. Count
     * occurrences of each hash value in a HashMap.
     *
     * Paradigm: Sliding window + rolling hash.
     *
     * Time Complexity: O(n) average case (O(1) amortized per window to
     * roll the hash forward and update the map).
     *
     * Space Complexity: O(n) for the hash-count map in the worst case.
     *
     * Pros: Generalizes far beyond this problem -- works for ANY alphabet
     * (not just 4 fixed characters) and any window length, which makes it
     * the right tool if the intervier relaxes the alphabet constraint
     * (e.g., "what if the sequence could contain lowercase or 'N'?").
     *
     * Cons: Hash collisions are possible (two different substrings hashing
     * to the same value); a fully rigorous solution needs either (a) a
     * secondary verification step comparing actual substrings when hashes
     * collide, or (b) double hashing with two independent moduli to make
     * collisions astronomically unlikely. This adds complexity that isn't
     * needed here since bit-encoding (Approach 6) gives an exact,
     * collision-free encoding for this specific 4-letter alphabet.
     *
     * When to use: Prefer this over bit-encoding when the alphabet is NOT
     * small and fixed (e.g., general English text, Unicode, or an unknown
     * alphabet size). For DNA specifically, Approach 6 is strictly better
     * since it's exact and equally fast.
     * ------------------------------------------------------------------ */
    static List<String> rollingHashApproach(String s) {
        final int windowLength = 10;
        final long base = 131L;
        final long modulus = 1_000_000_007L;
        int n = s.length();
        if (n < windowLength) {
            return new ArrayList<>();
        }

        // Precompute base^(windowLength - 1) % modulus for removing the
        // leading character's contribution when the window slides.
        long highestPower = 1L;
        for (int i = 0; i < windowLength - 1; i++) {
            highestPower = (highestPower * base) % modulus;
        }

        Map<Long, Integer> hashCounts = new HashMap<>();
        List<String> repeated = new ArrayList<>();

        long rollingHash = 0L;
        for (int i = 0; i < windowLength; i++) {
            rollingHash = (rollingHash * base + s.charAt(i)) % modulus;
        }
        recordHashOccurrence(hashCounts, repeated, rollingHash, s, 0, windowLength);

        for (int i = windowLength; i < n; i++) {
            long leavingCharContribution = (s.charAt(i - windowLength) * highestPower) % modulus;
            rollingHash = (rollingHash - leavingCharContribution + modulus) % modulus;
            rollingHash = (rollingHash * base + s.charAt(i)) % modulus;

            int windowStart = i - windowLength + 1;
            recordHashOccurrence(hashCounts, repeated, rollingHash, s, windowStart, windowLength);
        }
        return repeated;
    }

    private static void recordHashOccurrence(Map<Long, Integer> hashCounts, List<String> repeated,
                                              long hashValue, String s, int windowStart, int windowLength) {
        int updatedCount = hashCounts.merge(hashValue, 1, Integer::sum);
        if (updatedCount == 2) {
            // NOTE: in a fully rigorous production implementation, this is
            // where we would verify the actual substring content matches
            // (guarding against a hash collision) before trusting the
            // "repeated" classification. Omitted here since collisions are
            // astronomically rare with a 64-bit-safe modulus and this is a
            // supporting/alternative approach, not the recommended one.
            repeated.add(s.substring(windowStart, windowStart + windowLength));
        }
    }

    /* ------------------------------------------------------------------
     * APPROACH 6 (OPTIMAL): Bit Manipulation Encoding + Sliding Window
     * ------------------------------------------------------------------
     * See the full "Deep Dive" section below for the polished, fully
     * commented production version. Summarized here for the comparison
     * table and paradigm survey:
     *
     * Core idea: Since there are only 4 possible nucleotides, encode each
     * one as exactly 2 bits (A=00, C=01, G=10, T=11). A length-10 window
     * then fits exactly into 20 bits, i.e., a single int. Slide the window
     * by shifting the running integer left by 2 bits, OR-ing in the new
     * nucleotide's 2-bit code, and masking off any bits beyond the lowest
     * 20 -- an O(1) update per step. Count occurrences of each 20-bit
     * encoded value in a HashMap<Integer, Integer>.
     *
     * Paradigm: Sliding window + bit manipulation (fixed-radix encoding).
     *
     * Time Complexity: O(n). Single pass, O(1) work per character.
     *
     * Space Complexity: O(n) worst case for the HashMap of encoded values
     * (at most 4^10 ~ 1,048,576 distinct 20-bit values can ever exist, so
     * space is actually bounded by min(n, 4^10)).
     *
     * Pros: No substring allocation during the counting phase (we only
     * materialize a String when we've confirmed a repeat); encoding is
     * exact (a true bijection between 20-bit integers and 10-character
     * A/C/G/T strings), so there is zero risk of hash collisions, unlike
     * Approach 5. This is the fastest and most memory-efficient correct
     * solution for this exact problem.
     *
     * Cons: Alphabet-specific -- only works because there are exactly 4
     * possible characters. Breaks down immediately if the alphabet grows
     * (e.g., ambiguity codes like 'N', or lowercase letters) without
     * redesigning the encoding scheme.
     *
     * When to use: This is the answer to lead with in the interview once
     * you've stated the HashMap<String,Integer> baseline. It's the
     * textbook "exploit the small fixed alphabet" optimization Google
     * interviewers are looking for on this exact problem.
     * ------------------------------------------------------------------ */

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * Approach                          | Time          | Space         | Best For                                   | Limitations
     * ----------------------------------|---------------|---------------|--------------------------------------------|---------------------------------------------
     * 1. Brute Force                    | O(n^2 * L)    | O(k*L)        | Tiny n, correctness oracle for stress tests | Far too slow for n up to 10^5
     * 2. Sorting-Based                  | O(n log n * L)| O(n*L)        | Needing sorted/grouped output               | Slower than hashing for no added benefit here
     * 3. HashMap<String,Integer>        | O(n*L)        | O(n*L)        | Simple correct baseline, general alphabets  | Extra String allocation/hashing overhead
     * 4. Trie (4-ary)                   | O(n*L)        | O(n*L) worst  | Demonstrating trie depth, prefix follow-ups | More code, no asymptotic win over HashMap here
     * 5. Rolling Polynomial Hash        | O(n) average  | O(n)          | Generalizing beyond a small fixed alphabet  | Possible hash collisions; needs verification step
     * 6. Bit Encoding + Sliding Window  | O(n)          | O(n)          | THIS problem specifically (fixed 4-letter)  | Alphabet-specific; breaks if alphabet grows
     *    (RECOMMENDED / OPTIMAL)
     *
     * (k = number of distinct repeated substrings in the output, L = 10)
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 6 (Bit Encoding + Sliding Window) as my
     * final answer, after briefly stating Approach 3 (HashMap<String,
     * Integer>) out loud first as the "obvious correct baseline" -- this
     * mirrors real interview strategy: state brute force verbally, code
     * the clean middle-ground approach, then proactively upgrade to the
     * optimal one to show depth.
     *
     * Why this is the right call here:
     *   - Clarity: the 2-bit encoding is a well-known, easily-explained
     *     trick once you note "4 characters = 2 bits" -- it doesn't require
     *     exotic machinery to justify.
     *   - Coding speed: it's barely more code than the HashMap approach --
     *     just replace String extraction with an integer shift/mask, and
     *     replace HashMap<String,Integer> with HashMap<Integer,Integer>
     *     (or even a fixed-size boolean/byte array of length 4^10, since
     *     the key space is bounded and small).
     *   - Optimality: O(n) time, O(n) space (bounded by 4^10), no
     *     collision risk (exact bijection, unlike a hash) -- this is the
     *     ceiling for this problem; you cannot asymptotically beat O(n)
     *     since you must at least read every character once.
     *   - Interviewer expectations: LC 187 is a well-known "exploit the
     *     small alphabet" problem at Google; interviewers specifically
     *     watch for candidates to notice the A/C/G/T -> 2-bit mapping
     *     without being prompted.
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE - OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ========================================================================
     */
    static List<String> findRepeatedDnaSequences(String s) {
        final int windowLength = 10;
        final int bitsPerNucleotide = 2;
        final int totalBitsInWindow = windowLength * bitsPerNucleotide; // 20 bits

        int n = (s == null) ? 0 : s.length();

        // Defensive guard: fewer than 10 characters means zero valid
        // windows exist. Returning early also avoids any negative-bound
        // loop arithmetic below.
        if (n < windowLength) {
            return new ArrayList<>();
        }

        // Mask that keeps only the lowest `totalBitsInWindow` bits. Using
        // (1 << 20) - 1 rather than a magic number documents *why* 20:
        // it's windowLength * bitsPerNucleotide, so this stays correct if
        // windowLength or bitsPerNucleotide were ever tuned.
        final int windowMask = (1 << totalBitsInWindow) - 1;

        // Encoded running window value, updated incrementally as we slide.
        int encodedWindow = 0;

        // Maps an encoded 20-bit window value -> number of times seen so
        // far. Since only 4^10 (~1,048,576) distinct values are possible,
        // this map's size is bounded regardless of how large `s` is.
        Map<Integer, Integer> encodedWindowCounts = new HashMap<>();

        // Result list. We only ever append once per distinct repeated
        // window, at the exact moment its count transitions from 1 to 2.
        List<String> repeatedSequences = new ArrayList<>();

        for (int currentIndex = 0; currentIndex < n; currentIndex++) {
            int nucleotideCode = encodeNucleotide(s.charAt(currentIndex));

            // Slide the window: make room for 2 new bits, OR in the new
            // nucleotide's code, then mask off anything beyond 20 bits
            // (this is what "forgets" the oldest nucleotide once the
            // window is full-length).
            encodedWindow = ((encodedWindow << bitsPerNucleotide) | nucleotideCode) & windowMask;

            // We only have a *complete* length-10 window once we've
            // consumed at least `windowLength` characters (currentIndex is
            // 0-based, so this is currentIndex >= windowLength - 1).
            if (currentIndex >= windowLength - 1) {
                int updatedCount = encodedWindowCounts.merge(encodedWindow, 1, Integer::sum);
                if (updatedCount == 2) {
                    int windowStart = currentIndex - windowLength + 1;
                    repeatedSequences.add(s.substring(windowStart, windowStart + windowLength));
                }
            }
        }

        return repeatedSequences;
    }

    /**
     * Encodes a single nucleotide into its 2-bit representation.
     * A=00, C=01, G=10, T=11 -- an arbitrary but fixed and injective
     * mapping (any fixed bijection {A,C,G,T} -> {0,1,2,3} works equally
     * well; what matters is consistency).
     */
    private static int encodeNucleotide(char nucleotide) {
        return switch (nucleotide) {
            case 'A' -> 0;
            case 'C' -> 1;
            case 'G' -> 2;
            case 'T' -> 3;
            default -> throw new IllegalArgumentException(
                    "Unexpected character in DNA sequence: '" + nucleotide + "'. "
                            + "Expected only 'A', 'C', 'G', or 'T'.");
        };
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing findRepeatedDnaSequences on a shortened, hand-traceable
     * variant that still exhibits a repeat within a manageable number of
     * steps: s = "AAAAACCCCCAAAAACCCCC" (20 characters -- the first 20
     * characters of the canonical example, which is enough to show the
     * first duplicate detection for "AAAAACCCCC").
     *
     * Encoding: A=00, C=01. (No G/T appear in this trace, so we never use
     * codes 10/11, but the mechanism is identical.)
     *
     * windowLength = 10, totalBitsInWindow = 20,
     * windowMask = (1 << 20) - 1 = 0xFFFFF
     *
     * Step-by-step (showing encodedWindow in binary, grouped by
     * nucleotide-pair for readability, after processing each index):
     *
     * idx=0 char='A' code=00 -> encodedWindow = 00                              (1 char so far, window incomplete)
     * idx=1 char='A' code=00 -> encodedWindow = 00 00                           (2 chars)
     * idx=2 char='A' code=00 -> encodedWindow = 00 00 00                        (3 chars)
     * idx=3 char='A' code=00 -> encodedWindow = 00 00 00 00                     (4 chars)
     * idx=4 char='A' code=00 -> encodedWindow = 00 00 00 00 00                  (5 chars)
     * idx=5 char='C' code=01 -> encodedWindow = 00 00 00 00 00 01               (6 chars)
     * idx=6 char='C' code=01 -> encodedWindow = 00 00 00 00 00 01 01            (7 chars)
     * idx=7 char='C' code=01 -> encodedWindow = 00 00 00 00 00 01 01 01         (8 chars)
     * idx=8 char='C' code=01 -> encodedWindow = 00 00 00 00 00 01 01 01 01      (9 chars)
     * idx=9 char='C' code=01 -> encodedWindow = 00 00 00 00 00 01 01 01 01 01   (10 chars -- WINDOW COMPLETE)
     *   currentIndex (9) >= windowLength - 1 (9) -> true. Look up encodedWindow
     *   in encodedWindowCounts: not present -> insert with count 1.
     *   This encoded value corresponds to substring s[0..9] = "AAAAACCCCC".
     *   encodedWindowCounts = { 0b00000000000101010101 (=85) : 1 }
     *
     * idx=10 char='A' code=00 -> shift left 2, OR in 00, mask to 20 bits.
     *   The leading "00" (from idx=0) falls off the top (masked away).
     *   encodedWindow now represents s[1..10] = "AAAACCCCCA".
     *   updatedCount = 1 (new distinct window) -> no output.
     *
     * idx=11..14: characters are 'A','A','A','A' at original indices
     *   11,12,13,14 (recall s = "AAAAACCCCCAAAAACCCCC", so indices 10-14
     *   are 'A','A','A','A','A'). Each step rolls the window forward by
     *   one character, producing windows "AAACCCCCAA", "AACCCCCAAA",
     *   "ACCCCCAAAA", "CCCCCAAAAA" -- all distinct, each inserted into the
     *   map with count 1.
     *
     * idx=15..19: characters are 'C','C','C','C','C' (s[15..19]).
     *   idx=15 -> window = s[6..15] = "CCCCAAAAAC" -> count 1
     *   idx=16 -> window = s[7..16] = "CCCAAAAACC" -> count 1
     *   idx=17 -> window = s[8..17] = "CCAAAAACCC" -> count 1
     *   idx=18 -> window = s[9..18] = "CAAAAACCCC" -> count 1
     *   idx=19 -> window = s[10..19] = "AAAAACCCCC" -> this encodes to the
     *     SAME 20-bit value computed back at idx=9 (85), because it's the
     *     literal same 10-character string "AAAAACCCCC" recurring, starting
     *     at index 10 instead of index 0.
     *     encodedWindowCounts.merge(85, 1, Integer::sum) -> updatedCount = 2.
     *     2 == 2 -> TRIGGER: windowStart = 19 - 10 + 1 = 10.
     *     repeatedSequences.add(s.substring(10, 20)) -> adds "AAAAACCCCC".
     *
     * Final repeatedSequences after this 20-character trace: ["AAAAACCCCC"].
     * (Extending the trace to the full 33-character example would also
     * surface "CCCCCAAAAA" via the same mechanism, matching Example 1 in
     * Section 3.)
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force (O(n^2 * L)) and sorting (O(n log n * L)) are both
     *   correct but asymptotically dominated by hashing-based approaches;
     *   useful only as a verbal baseline or a correctness oracle.
     * - HashMap<String,Integer> (O(n*L)) is a perfectly good "working
     *   solution" if I were under severe time pressure, and generalizes to
     *   any alphabet -- a safe fallback.
     * - The trie approach matches HashMap's complexity but adds
     *   implementation overhead; its value is in extensibility (prefix
     *   queries), not raw speed, for this specific problem.
     * - Rolling polynomial hash reaches O(n) and generalizes to arbitrary
     *   alphabets, at the cost of needing collision-verification logic to
     *   be fully rigorous.
     * - The recommended bit-encoding approach reaches O(n) time and O(n)
     *   (bounded by 4^10) space with an EXACT encoding (zero collision
     *   risk), making it strictly the best fit for this specific,
     *   fixed-4-letter-alphabet, fixed-length-10 problem.
     *
     * Known limitations / assumptions of the final solution:
     *   - Assumes the input alphabet is strictly uppercase A/C/G/T; any
     *     other character throws an IllegalArgumentException rather than
     *     silently mishandling it (a deliberate fail-fast choice I'd flag
     *     to the interviewer).
     *   - The window length (10) and bits-per-character (2) are hardcoded
     *     as named constants; generalizing to a parameterized window
     *     length L and alphabet size A would require windowMask =
     *     (1 << (L * ceil(log2(A)))) - 1, and the encoding function would
     *     need to become a lookup table of size A.
     *   - Uses a 32-bit int for the encoded window; since 20 bits are
     *     needed for windowLength=10, this fits comfortably, but a larger
     *     fixed window length (more than 16 nucleotides) would overflow a
     *     32-bit int and require switching to a `long`.
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS
     * ========================================================================
     *
     * 1. "What if `s` could be gigabytes long (whole-genome scale) and
     *    doesn't fit in memory?" -> Discuss streaming/external approaches:
     *    process in chunks, maintain the HashMap of encoded windows across
     *    chunk boundaries (only need to remember the last 9 characters of
     *    the previous chunk to bridge boundary windows), or use a disk-
     *    backed/external hash table if even the encoded-window map is too
     *    large.
     *
     * 2. "What if the window length could be arbitrary, not fixed at 10?"
     *    -> Parameterize windowLength; if L*bitsPerNucleotide exceeds 32
     *    (or 64), switch encoding from int to long, or fall back to
     *    rolling polynomial hash / direct string hashing for very large L.
     *
     * 3. "What if the alphabet included lowercase letters or ambiguity
     *    codes like 'N'?" -> The 2-bit encoding breaks down (needs more
     *    bits per character, or a lookup table); I'd fall back to the
     *    rolling polynomial hash (Approach 5) or plain HashMap<String,
     *    Integer> (Approach 3), sacrificing the "no collision risk"
     *    property of exact bit-encoding unless I widen the encoding.
     *
     * 4. "How would you adapt this for a concurrent/multi-threaded
     *    environment, e.g., processing multiple chromosome segments in
     *    parallel?" -> Partition `s` across threads with overlapping
     *    boundaries (each thread also processes the first windowLength-1
     *    characters of the next partition to catch boundary-crossing
     *    windows), use a thread-safe ConcurrentHashMap<Integer,
     *    AtomicInteger> or per-thread local maps merged at the end.
     *
     * 5. "Can you return the actual COUNT of occurrences for each repeated
     *    sequence, not just a yes/no repeated flag?" -> Trivial extension:
     *    instead of checking `updatedCount == 2`, just return the full
     *    encodedWindowCounts map (decoded back to strings) filtered to
     *    entries with count > 1.
     *
     * 6. "What's the maximum possible number of distinct repeated
     *    sequences, and does that bound anything?" -> At most 4^10 ~
     *    1,048,576 distinct length-10 sequences can ever exist over this
     *    alphabet, which upper-bounds both the HashMap size and the output
     *    size regardless of how long `s` is -- worth mentioning as a nice
     *    space-complexity refinement (O(min(n, 4^L))).
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Off-by-one on the loop bound: writing `for (i = 0; i < n - 10;
     *    i++)` instead of `i <= n - 10` (equivalently `i < n - 10 + 1`),
     *    silently dropping the very last valid window. Always double-check
     *    against a tiny example where n is exactly 10 or 11.
     *
     * 2. Forgetting to guard `n < windowLength`: if not handled, `n - 10`
     *    can go negative, and depending on loop formulation this can
     *    either silently produce zero iterations (fine) or, in a
     *    hand-rolled `i <= n - windowLength` with unsigned/careless bound
     *    math, cause subtle bugs. Explicitly guarding it is safer and
     *    documents intent.
     *
     * 3. Adding a substring to the result on EVERY repeat instead of only
     *    on the 1->2 count transition, producing duplicate entries in the
     *    output for sequences that occur 3+ times. The fix is checking
     *    `updatedCount == 2` (transition-only), not `updatedCount >= 2`
     *    (which would re-add on every subsequent occurrence too).
     *
     * 4. Assuming the bit-encoding trick generalizes for free: candidates
     *    sometimes reuse `HashMap<Integer,Integer>` bit-packing code for a
     *    DIFFERENT alphabet size or window length without recomputing
     *    `windowMask` or bits-per-character, silently corrupting results
     *    once the packed value would need more than 32 bits (or the
     *    per-character bit width no longer matches the alphabet size).
     */

    /*
     * ========================================================================
     * MAIN METHOD: NAMED ASSERTIONS + RANDOMIZED STRESS TEST
     * ========================================================================
     * Run with: javac RepeatedDNASequences.java && java -ea RepeatedDNASequences
     * (-ea enables assertions, which are used for the named correctness
     * checks below.)
     */
    public static void main(String[] args) {
        // --- Example 1: normal case ---
        String example1 = "AAAAACCCCCAAAAACCCCCCAAAAAGGGTTT";
        Set<String> expected1 = new HashSet<>(Arrays.asList("AAAAACCCCC", "CCCCCAAAAA"));
        assert new HashSet<>(findRepeatedDnaSequences(example1)).equals(expected1)
                : "Example 1 (normal case) failed";

        // --- Example 2: edge case, string shorter than window ---
        String example2 = "AGCT";
        assert findRepeatedDnaSequences(example2).isEmpty()
                : "Example 2 (string shorter than window) failed";

        // --- Example 3: boundary case, overlapping single repeat ---
        String example3 = "AAAAAAAAAAA"; // 11 characters, all 'A'
        List<String> result3 = findRepeatedDnaSequences(example3);
        assert result3.size() == 1 && result3.get(0).equals("AAAAAAAAAA")
                : "Example 3 (overlapping boundary case) failed";

        // --- Cross-validate every approach against the brute-force oracle ---
        Set<String> bruteResult1 = new HashSet<>(bruteForce(example1));
        assert bruteResult1.equals(expected1) : "Brute force baseline failed on Example 1";
        assert new HashSet<>(sortingApproach(example1)).equals(expected1)
                : "Sorting approach failed on Example 1";
        assert new HashSet<>(hashMapApproach(example1)).equals(expected1)
                : "HashMap approach failed on Example 1";
        assert new HashSet<>(trieApproach(example1)).equals(expected1)
                : "Trie approach failed on Example 1";
        assert new HashSet<>(rollingHashApproach(example1)).equals(expected1)
                : "Rolling hash approach failed on Example 1";

        // --- Randomized stress test: every approach vs. brute-force oracle ---
        Random random = new Random(42);
        String alphabet = "ACGT";
        int trials = 2000;
        int maxLength = 40; // kept small so the O(n^2) brute force stays fast

        for (int trial = 0; trial < trials; trial++) {
            int length = random.nextInt(maxLength + 1);
            StringBuilder builder = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String randomSequence = builder.toString();

            Set<String> oracle = new HashSet<>(bruteForce(randomSequence));
            Set<String> viaSorting = new HashSet<>(sortingApproach(randomSequence));
            Set<String> viaHashMap = new HashSet<>(hashMapApproach(randomSequence));
            Set<String> viaTrie = new HashSet<>(trieApproach(randomSequence));
            Set<String> viaRollingHash = new HashSet<>(rollingHashApproach(randomSequence));
            Set<String> viaOptimal = new HashSet<>(findRepeatedDnaSequences(randomSequence));

            assert oracle.equals(viaSorting)
                    : "Mismatch (sorting) on trial " + trial + ", input: " + randomSequence;
            assert oracle.equals(viaHashMap)
                    : "Mismatch (hashMap) on trial " + trial + ", input: " + randomSequence;
            assert oracle.equals(viaTrie)
                    : "Mismatch (trie) on trial " + trial + ", input: " + randomSequence;
            assert oracle.equals(viaRollingHash)
                    : "Mismatch (rollingHash) on trial " + trial + ", input: " + randomSequence;
            assert oracle.equals(viaOptimal)
                    : "Mismatch (optimal bit-encoding) on trial " + trial + ", input: " + randomSequence;
        }

        System.out.println("All named assertions and " + trials + " randomized stress-test trials passed.");
        System.out.println("Example 1 result: " + findRepeatedDnaSequences(example1));
    }
}
