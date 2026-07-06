import java.util.*;

/*
====================================================================================================
 GOOGLE-STYLE MOCK INTERVIEW WALKTHROUGH
 Problem: Find the Lexicographically Largest String From the Box
 (LeetCode-style: "word" split into "numFriends" non-empty substrings across all unique rounds;
  collect every substring produced in every round into a box; return the lexicographically
  largest string that ever lands in the box.)
====================================================================================================
*/

public class LargestStringFromBox {

    /*
    ================================================================================================
    SECTION 1: RESTATE THE PROBLEM
    ================================================================================================
    In my own words:

    - We're given a string `word` of length n, and an integer `numFriends`.
    - A "round" = one way of cutting `word` into exactly `numFriends` non-empty, contiguous
      substrings (i.e., picking numFriends-1 distinct cut points among the n-1 gaps between
      characters). Two rounds are considered different if the *set of cut positions* differs,
      even if by coincidence the resulting substrings are textually identical.
    - Alice plays EVERY possible unique round exactly once (this is really a combinatorics
      statement: there are C(n-1, numFriends-1) total distinct splits, and all of them happen).
    - Every substring produced by every split, across all rounds, is dropped into one big "box"
      (effectively a multiset of substrings).
    - Goal: return the single lexicographically largest string that appears anywhere in that box.

    Key constraints / inputs / outputs:
    - Input: String word (1 <= word.length()), int numFriends (1 <= numFriends <= word.length()).
    - Output: a single String — the lexicographically largest substring across all splits.
    - Lexicographic ordering as defined: compare character by character; first difference decides;
      if one is a prefix of the other, the LONGER one is considered larger (standard Java
      String.compareTo semantics — this matters a lot for the solution).

    Implicit assumption to verify with interviewer: numFriends can equal word.length() (every
    friend gets exactly 1 character), and numFriends can equal 1 (only one split: the whole word
    itself, box contains just `word`).
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 2: CLARIFYING QUESTIONS (asked to interviewer) + assumed answers
    ================================================================================================
    1) Q: What is the maximum length of `word`? Do we need to handle up to 10^5, or is this closer
          to a 5,000-character constraint?
       A (assumed): 1 <= word.length() <= 5000. (I'll also discuss how the approach generalizes to
          10^5 with hashing / suffix structures.)

    2) Q: What is the range of numFriends relative to word.length()?
       A (assumed): 1 <= numFriends <= word.length(). numFriends == word.length() means every
          substring is a single character.

    3) Q: Is `word` guaranteed to contain only lowercase English letters, or could it include
          uppercase, digits, or unicode?
       A (assumed): lowercase English letters only ('a'-'z'), simplifying comparisons (no locale
          issues, natural char ordering = alphabetical ordering).

    4) Q: Does "unique split" mean unique by cut-position-sequence, or unique by the resulting
          *sequence of substrings*? (E.g., word="aaa", numFriends=2 — cutting after index 0 vs
          after index 1 both produce different substring pairs here, but could two DIFFERENT cut
          points ever yield an IDENTICAL sequence of substrings, and if so, does that round still
          count once or twice?)
       A (assumed): uniqueness is defined by the set of cut positions (combinatorial splits), which
          matches the standard formulation of this problem — the box may thus contain duplicate
          textual substrings from different rounds, but we care only about the max value, so
          duplicates don't change the answer.

    5) Q: Do we need to return which round produced the answer, or count of occurrences — or just
          the string value itself?
       A (assumed): Just the string value.

    6) Q: Should comparison be case-sensitive / ASCII-order, and is a longer string that starts
          identically to a shorter one always considered larger (prefix rule)?
       A (assumed): Yes — standard Java String.compareTo semantics apply exactly as stated in the
          problem.

    7) Q: Can `numFriends` be 0, or can `word` be empty?
       A (assumed): No — numFriends >= 1 and word.length() >= 1 always (guaranteed valid input).

    8) Q: Is this a single query, or do we need to answer this for many (word, numFriends) pairs
          efficiently (batch queries)?
       A (assumed): Single query per call; no batching requirement, but I'll note how
          precomputation (suffix arrays) would help if batching were introduced.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 3: EXAMPLES & EDGE CASES
    ================================================================================================
    Example 1 (normal case):
        word = "dbca", numFriends = 2
        n = 4, maxPossibleSubstringLength = n - numFriends + 1 = 3
        Every substring of length <= 3 that fits is achievable in some split:
          start=0: "dbc" (len 3), "db" (len2), "d" (len1)
          start=1: "bca" (len3), "bc" (len2), "b" (len1)
          start=2: "ca" (len2), "c" (len1)
          start=3: "a" (len1)
        Because a longer string beats its own prefix, only the longest-per-start candidates matter:
          "dbc", "bca", "ca", "a"
        Lexicographically largest = "dbc"  (since 'd' > 'b' > 'c' > 'a' as first characters)
        --> Answer: "dbc"

    Example 2 (edge case — numFriends == word.length(), all singleton substrings):
        word = "gggg", numFriends = 4
        maxPossibleSubstringLength = 4 - 4 + 1 = 1 --> every substring is a single character.
        Box contains only "g" repeated. Largest = "g".
        --> Answer: "g"

    Example 3 (boundary / tie-breaking case — repeated characters causing duplicate candidates):
        word = "aaaa", numFriends = 2
        maxPossibleSubstringLength = 4 - 2 + 1 = 3
        Candidates (longest per start): start0 -> "aaa", start1 -> "aaa", start2 -> "aa", start3 -> "a"
        Two DIFFERENT starting positions (0 and 1) tie with the identical string "aaa" — this is
        fine, since we only need the *value*, not which round/position produced it. compareTo
        treats them as equal, so either can be kept as the running "best".
        --> Answer: "aaa"

    Additional edge case worth stating out loud in the interview:
        numFriends == 1  -->  maxPossibleSubstringLength == word.length(), so the only candidate is
        `word` itself (the single split is "no split at all"). Answer = word. Good sanity check
        that should fall out of the general formula without a special case, but I will special-case
        it anyway for clarity and a tiny speed win.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 4 & 5: ALL POSSIBLE APPROACHES
    ================================================================================================

    ---------------------------------------------------------------------
    Approach 1: Brute Force — Enumerate Every Split, Collect Every Substring
    ---------------------------------------------------------------------
    Core idea: Literally simulate the problem statement. Choose every combination of
    (numFriends - 1) distinct cut points from the (n - 1) available gaps between characters.
    For each combination, slice `word` into numFriends substrings, throw them all into a
    collection ("box"), and after generating every combination, scan the box for the max.

    Paradigm: Combinatorial enumeration / backtracking (generating combinations).

    Time Complexity: There are C(n-1, numFriends-1) total splits, and each split does O(n) work
    to build substrings and compare them, so this is O(n * C(n-1, numFriends-1)) — exponential
    in the worst case (e.g., numFriends ~ n/2 maximizes the binomial coefficient). Completely
    infeasible beyond tiny n (n > ~20-25).

    Space Complexity: O(n) per split held temporarily, or O(n * C(n-1,numFriends-1)) if we
    materialize the whole box at once (I avoid that by tracking only a running max).

    Pros: Directly mirrors the problem statement — trivially "obviously correct," useful as a
          correctness oracle / for writing brute-force unit tests against faster solutions.
    Cons: Exponential blow-up; unusable for realistic n (5,000 or more).
    When to use: Never in production; only as a reference implementation for testing smaller
                 inputs against the optimized solutions.
    ---------------------------------------------------------------------
    */

    /** Approach 1: Brute force — generate all splits explicitly, track max substring seen. */
    public static String bruteForce(String word, int numFriends) {
        int n = word.length();
        int cuts = numFriends - 1;               // number of internal cut points needed
        int[] gapPositions = new int[cuts];       // combination buffer of chosen gap indices (1..n-1)
        String[] best = new String[]{""};         // boxed so recursion can mutate it
        generateCombinations(word, gapPositions, 0, 1, n - 1, best);
        return best[0];
    }

    // Recursively choose `cuts` increasing gap indices from [startGap, n-1], then evaluate the split.
    private static void generateCombinations(String word, int[] chosenGaps, int filled,
                                              int startGap, int lastGap, String[] best) {
        if (filled == chosenGaps.length) {
            evaluateSplit(word, chosenGaps, best);
            return;
        }
        for (int gap = startGap; gap <= lastGap - (chosenGaps.length - filled - 1); gap++) {
            chosenGaps[filled] = gap;
            generateCombinations(word, chosenGaps, filled + 1, gap + 1, lastGap, best);
        }
    }

    // Slice word at the chosen gap indices, push every resulting substring into the running max.
    private static void evaluateSplit(String word, int[] cutGaps, String[] best) {
        int previousCut = 0;
        for (int gapIndex : cutGaps) {
            String piece = word.substring(previousCut, gapIndex);
            if (piece.compareTo(best[0]) > 0) best[0] = piece;
            previousCut = gapIndex;
        }
        String lastPiece = word.substring(previousCut);
        if (lastPiece.compareTo(best[0]) > 0) best[0] = lastPiece;
    }

    /*
    ---------------------------------------------------------------------
    Approach 2: Key-Insight Reduction + Quadratic Direct Comparison
    ---------------------------------------------------------------------
    Core idea (the crucial observation that unlocks this problem):
      - In ANY split, one friend's substring can be as long as (n - numFriends + 1) at most,
        because the other (numFriends - 1) friends must each receive >= 1 character.
      - Conversely, ANY substring of `word` starting at index i with length L, where
        1 <= L <= (n - numFriends + 1) and i + L <= n, IS achievable: just cut off that piece,
        and distribute the remaining (n - L) characters among the other (numFriends - 1) friends
        arbitrarily (always feasible since n - L >= numFriends - 1).
      - So the box's content, ignoring duplicates, is EXACTLY: "every substring of `word` with
        length between 1 and maxLen = n - numFriends + 1 (inclusive)."
      - Because a string is always lexicographically smaller than any longer string that has it
        as a prefix, for a FIXED starting index i, the best possible candidate is always the
        LONGEST allowed substring starting at i (i.e., length = min(maxLen, n - i)). Shorter
        truncations from the same start can never beat it.
      - This collapses an exponential search space down to exactly n candidate strings — one per
        starting index — and we just need the max of those n strings.

    Paradigm: Greedy reduction (prove only n candidates matter) + straightforward comparison.
    This is effectively "greedy" in the sense that we greedily always take the maximum allowed
    length per starting index, which is provably never suboptimal.

    Time Complexity: n candidates; building/comparing each substring costs O(maxLen) in the worst
    case (String.compareTo scans until a mismatch or exhaustion) => O(n * maxLen) worst case,
    which is O(n^2) when maxLen is proportional to n (e.g., numFriends is small/constant).
    For n = 5000 that's ~25 million basic character comparisons — comfortably fast in practice.

    Space Complexity: O(maxLen) per candidate substring materialized (Java's substring makes a
    copy) — O(n) auxiliary at any instant if we don't retain all of them, only the current best.

    Pros: Very simple to explain and code under interview pressure; easy to prove correct on a
          whiteboard; fast enough for typical interview-stated constraints (n up to ~5,000).
    Cons: Degrades to true O(n^2) when maxLen ~ n (e.g., numFriends is small, like 2), which would
          not scale to n = 10^5 or beyond.
    When to use: This is my primary interview-recommended solution for typical stated constraints
                 (see Section 8) — simple, robust, and fast enough.
    ---------------------------------------------------------------------
    */

    /** Approach 2: Reduce to n candidates (longest-per-start), compare directly with compareTo. */
    public static String optimizedQuadratic(String word, int numFriends) {
        int n = word.length();
        if (numFriends == 1) return word; // only one whole-string "split" exists

        int maxLen = n - numFriends + 1;  // longest any single substring can ever be
        String bestSoFar = "";

        for (int start = 0; start < n; start++) {
            int allowedLen = Math.min(maxLen, n - start); // cap by remaining characters too
            String candidate = word.substring(start, start + allowedLen);
            if (candidate.compareTo(bestSoFar) > 0) {
                bestSoFar = candidate;
            }
        }
        return bestSoFar;
    }

    /*
    ---------------------------------------------------------------------
    Approach 3 (Optimal): Rolling-Hash + Binary Search LCP, Single Linear Scan
    ---------------------------------------------------------------------
    Core idea: Same n-candidate reduction as Approach 2, but instead of comparing two candidate
    substrings character-by-character (O(maxLen) per comparison), we compare them in O(log n)
    using:
      1) Precomputed polynomial rolling hashes (double-hashed with two independent mod/base pairs
         to make accidental collisions astronomically unlikely) over the whole string, giving
         O(1) hash-of-any-substring queries.
      2) Binary search over the "longest common prefix length" between two candidate starting
         positions — using the O(1) hash query as the binary-search predicate ("are these two
         substrings of length L equal?") — giving O(log n) to find the LCP length.
      3) Once we know the LCP length, the very next character (or the prefix rule, if one
         candidate is exhausted) tells us which candidate is larger, in O(1).
      4) Do a single linear scan (tournament-style: keep a running "best index," and challenge it
         against every other index once) => n-1 comparisons total, each O(log n).

    Paradigm: Hashing (Rabin–Karp style) + Binary Search. (This is the standard trick used for the
    "10^5-scale" version of this exact LeetCode problem, where the O(n^2) approach is too slow.)

    Time Complexity: O(n log n) — O(n) to build prefix hashes/powers, O(n log n) for the linear
    scan of n-1 pairwise comparisons each costing O(log n) via binary search + O(1) hash checks.

    Space Complexity: O(n) for prefix hash arrays (x2 for double hashing) and power arrays.

    Pros: Scales to n up to 10^5 or beyond; asymptotically optimal for this reduction; still not
          too complex to implement carefully (compared to, say, a full suffix array).
    Cons: More intricate to get exactly right under interview time pressure (mod arithmetic,
          double hashing to avoid adversarial collisions, off-by-one care in binary search);
          theoretical (not practical) risk of hash collision, mitigated via double hashing.
    When to use: When the interviewer tightens constraints (e.g., n up to 10^5), or explicitly
                 asks "can you do better than O(n^2)?" This is the answer to bring out.
    ---------------------------------------------------------------------

    ---------------------------------------------------------------------
    Paradigms considered and why they don't (meaningfully) apply here:
    ---------------------------------------------------------------------
    - Sorting-based: Sorting all n candidate strings would give the max trivially (the last
      element), but full sorting costs O(n log n * maxLen) for comparisons unless combined with
      the hashing trick above — at which point it's really "Approach 3 dressed differently." A
      pure library sort without hashing is dominated by Approach 3's linear scan (no need to fully
      order all candidates, we only need the single maximum).
    - Two pointer / sliding window: There's no "window" whose size we grow/shrink based on a
      running aggregate (like sum/count) — every distinct starting index is an independent
      candidate to compare in full, so classic two-pointer/sliding-window doesn't directly reduce
      the problem's complexity.
    - Divide and conquer: One could split the candidate list in half, recursively find each half's
      max, and merge (compare two strings) — this is a valid O(n log n) *shape* but the expensive
      part is still the string comparison itself, so it doesn't help unless paired with hashing
      (again collapsing to Approach 3's comparator).
    - Dynamic Programming: There's no overlapping-subproblem / optimal-substructure recurrence to
      exploit here — the "value" of a split isn't built up from smaller split values in a way DP
      memoizes; it's fundamentally a "generate candidates, take max" structure, not a DP one.
    - Tree / graph traversal: No graph or tree structure is implied by the problem (no
      adjacency/dependency between splits) — not applicable.
    - Heap / priority queue: A heap could maintain a running top-K of candidates, but we only ever
      need the single max (K=1), so a heap adds overhead (O(log n) per insert) with no benefit
      over a simple running-max comparison; only useful if the problem asked for top-K distinct
      strings in the box.
    - Binary search: DOES apply — used inside Approach 3 to find the longest common prefix length
      between two candidate substrings quickly.
    - Monotonic stack / deque: No "next greater/smaller element" structure or window-min/max
      pattern here — not applicable.
    - Trie / suffix structures: A suffix array (with LCP array + sparse table for O(1) range-min
      queries) is a valid alternative optimal solution, effectively precomputing what Approach 3
      computes online via hashing. It achieves the same O(n log n) (or O(n) with linear-time
      suffix array construction) but is materially more complex to implement correctly under
      interview time pressure; I'd mention it as "the more classical CS way to do this" but
      recommend the hashing approach as more practical to actually write live.
    ================================================================================================
    */

    /** Approach 3: Optimal — double rolling-hash + binary-search LCP + single linear scan. */
    public static String optimalHashingApproach(String word, int numFriends) {
        int n = word.length();
        if (numFriends == 1) return word;

        int maxLen = n - numFriends + 1;
        DoubleHash hasher = new DoubleHash(word);

        int bestIndex = 0; // running champion starting index
        for (int challengerIndex = 1; challengerIndex < n; challengerIndex++) {
            int lenBest = Math.min(maxLen, n - bestIndex);
            int lenChallenger = Math.min(maxLen, n - challengerIndex);
            if (compareCandidates(word, hasher, bestIndex, lenBest, challengerIndex, lenChallenger) < 0) {
                bestIndex = challengerIndex; // challenger wins, becomes new champion
            }
        }
        int finalLen = Math.min(maxLen, n - bestIndex);
        return word.substring(bestIndex, bestIndex + finalLen);
    }

    // Compares the candidate substring starting at indexA (length lenA) against the candidate
    // starting at indexB (length lenB), using binary search over the hash-verified common prefix.
    // Returns negative if A < B, positive if A > B, zero if equal.
    private static int compareCandidates(String word, DoubleHash hasher,
                                          int indexA, int lenA, int indexB, int lenB) {
        int shorter = Math.min(lenA, lenB);

        // Binary search for the largest prefix length L (0 <= L <= shorter) such that the two
        // substrings word[indexA..indexA+L) and word[indexB..indexB+L) are identical.
        int lo = 0, hi = shorter;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (hasher.rangeEquals(indexA, indexB, mid)) {
                lo = mid; // mid-length prefixes match; try a longer common prefix
            } else {
                hi = mid - 1; // mismatch within mid; shrink search space
            }
        }
        int lcpLength = lo;

        if (lcpLength == shorter) {
            // One is a prefix of the other (or they're equal length here) -> longer string wins.
            return Integer.compare(lenA, lenB);
        }
        // First real divergence is right after the common prefix.
        char charA = word.charAt(indexA + lcpLength);
        char charB = word.charAt(indexB + lcpLength);
        return Character.compare(charA, charB);
    }

    /** Double polynomial rolling hash over a fixed string, supporting O(1) substring-equality checks. */
    static final class DoubleHash {
        private final long[] prefixHash1, prefixHash2, power1, power2;
        private static final long MOD1 = 1_000_000_007L, BASE1 = 131L;
        private static final long MOD2 = 998_244_353L, BASE2 = 137L;

        DoubleHash(String s) {
            int n = s.length();
            prefixHash1 = new long[n + 1];
            prefixHash2 = new long[n + 1];
            power1 = new long[n + 1];
            power2 = new long[n + 1];
            power1[0] = 1;
            power2[0] = 1;
            for (int i = 0; i < n; i++) {
                int code = s.charAt(i) - 'a' + 1; // keep values >= 1 to avoid leading-zero collisions
                prefixHash1[i + 1] = (prefixHash1[i] * BASE1 + code) % MOD1;
                prefixHash2[i + 1] = (prefixHash2[i] * BASE2 + code) % MOD2;
                power1[i + 1] = (power1[i] * BASE1) % MOD1;
                power2[i + 1] = (power2[i] * BASE2) % MOD2;
            }
        }

        private long hash1(int start, int len) {
            long raw = (prefixHash1[start + len] - prefixHash1[start] * power1[len]) % MOD1;
            return raw < 0 ? raw + MOD1 : raw;
        }

        private long hash2(int start, int len) {
            long raw = (prefixHash2[start + len] - prefixHash2[start] * power2[len]) % MOD2;
            return raw < 0 ? raw + MOD2 : raw;
        }

        /** True iff word[startA..startA+len) equals word[startB..startB+len) (checked both hashes). */
        boolean rangeEquals(int startA, int startB, int len) {
            if (len == 0) return true;
            return hash1(startA, len) == hash1(startB, len) && hash2(startA, len) == hash2(startB, len);
        }
    }

    /*
    ================================================================================================
    SECTION 7: APPROACHES COMPARISON TABLE
    ================================================================================================

    Approach                       | Time             | Space  | Best For                       | Limitations
    --------------------------------|------------------|--------|---------------------------------|--------------------------------------------
    1. Brute Force (enumerate all   | O(n * C(n-1,     | O(n)   | Correctness oracle / testing on  | Exponential; unusable beyond n ~ 20-25.
       splits explicitly)           |   numFriends-1)) |        | tiny inputs                      |
    2. Key-Insight + Quadratic      | O(n * maxLen)    | O(n)   | Typical interview constraints    | Degrades to O(n^2) when maxLen ~ n
       direct comparison            | ~ O(n^2) worst   |        | (n up to a few thousand)          | (small numFriends); won't scale to 1e5+.
    3. Rolling-Hash + Binary Search | O(n log n)       | O(n)   | Large n (up to 1e5+), or when     | More complex to implement correctly;
       LCP + linear scan            |                  |        | asked "can you do better?"        | tiny theoretical collision risk (mitigated
                                    |                  |        |                                   | via double hashing).
    (Alt) Suffix Array + LCP +      | O(n log n) or    | O(n)   | Same as Approach 3, more          | Significantly more implementation
       sparse table for RMQ         | O(n) w/ SA-IS    |        | "classical" CS answer             | complexity/time to write live; overkill
                                    |                  |        |                                   | unless batched queries are needed.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
    ================================================================================================
    I would present Approach 2 (Key-Insight Reduction + Quadratic Direct Comparison) as my primary
    solution, for these reasons:

    - Clarity: The key insight (max substring length = n - numFriends + 1, and "always take the
      longest allowed length per start") is the crux of the whole problem. Articulating and
      proving this insight is exactly what interviewers are listening for — it demonstrates real
      understanding, not just pattern matching to a known trick.
    - Coding speed: It's ~10 lines of code, easy to get exactly right under time pressure, with
      minimal risk of off-by-one bugs compared to the hashing/binary-search approach.
    - Interviewer expectations: For a first pass, interviewers want to see the reduction insight
      and a correct, reasonably efficient solution. O(n^2) (with small constant factor, since
      String.compareTo short-circuits at first mismatch — most real comparisons are fast) is
      normally accepted as a strong first solution for n in the thousands.
    - Optimality follow-up: I would proactively mention Approach 3 as "if we needed to handle
      n up to 10^5, here's how I'd push this to O(n log n) using rolling hashes + binary search to
      compare candidates faster" — showing depth without over-engineering the first pass.

    I would only lead with Approach 3 if the interviewer stated large constraints (n >= 10^4-10^5)
    up front.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 9: DEEP DIVE — POLISHED PRODUCTION-QUALITY VERSION OF THE RECOMMENDED (OPTIMAL) SOLUTION
    ================================================================================================
    Below is a production-quality version of Approach 2, the one I'd actually write on the
    whiteboard/IDE, with full inline reasoning. (Approach 3's fully-commented implementation is
    already given above in its own method, since it's equally "production-ready" — I present
    Approach 2 here since it's the one I'd lead with live.)
    ================================================================================================
    */

    /**
     * Returns the lexicographically largest substring that can ever appear in Alice's "box"
     * across all unique ways of splitting {@code word} into exactly {@code numFriends}
     * non-empty contiguous substrings.
     *
     * <p>Core insight: any substring of length L (1 <= L <= n - numFriends + 1) starting at any
     * valid index is achievable in some split, because the remaining (n - L) characters can
     * always be distributed among the other (numFriends - 1) friends (each needs only >= 1
     * character, and n - L >= numFriends - 1 is guaranteed by the bound on L). Since a string is
     * always smaller than any longer string it prefixes, for each starting index only the
     * *maximum allowed length* substring can possibly be the global answer.
     *
     * @param word        the source string, containing only lowercase English letters
     * @param numFriends  number of non-empty pieces each split must produce (1 <= numFriends <= word.length())
     * @return the lexicographically largest substring achievable across all valid splits
     */
    public static String findLargestStringInBox(String word, int numFriends) {
        final int n = word.length();

        // Special case: a single "friend" means the only valid split is "no split at all."
        if (numFriends == 1) {
            return word;
        }

        // The longest any individual piece can ever be: total length minus at least 1 char
        // reserved for each of the other (numFriends - 1) friends.
        final int maxCandidateLength = n - numFriends + 1;

        String bestCandidate = ""; // empty string is the correct identity element for compareTo max
        for (int startIndex = 0; startIndex < n; startIndex++) {
            // Cap by both the theoretical max length AND how many characters remain in `word`.
            int candidateLength = Math.min(maxCandidateLength, n - startIndex);
            String candidate = word.substring(startIndex, startIndex + candidateLength);

            // String.compareTo already implements exactly the ordering rule described in the
            // problem: first differing character decides; if one is a prefix of the other, the
            // longer one wins.
            if (candidate.compareTo(bestCandidate) > 0) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    /*
    ================================================================================================
    SECTION 10: DRY RUN / TRACE
    ================================================================================================
    Tracing findLargestStringInBox("dbca", 2):

        n = 4, numFriends = 2 (not 1, so we proceed past the special case)
        maxCandidateLength = 4 - 2 + 1 = 3
        bestCandidate = ""  (initial state)

        startIndex = 0:
            candidateLength = min(3, 4-0) = 3
            candidate = word.substring(0, 3) = "dbc"
            "dbc".compareTo("") > 0 (non-empty beats empty)  -> bestCandidate = "dbc"

        startIndex = 1:
            candidateLength = min(3, 4-1) = 3
            candidate = word.substring(1, 4) = "bca"
            "bca".compareTo("dbc") -> first char 'b' (98) < 'd' (100) -> negative, NOT better
            bestCandidate remains "dbc"

        startIndex = 2:
            candidateLength = min(3, 4-2) = 2
            candidate = word.substring(2, 4) = "ca"
            "ca".compareTo("dbc") -> 'c' (99) < 'd' (100) -> negative, NOT better
            bestCandidate remains "dbc"

        startIndex = 3:
            candidateLength = min(3, 4-3) = 1
            candidate = word.substring(3, 4) = "a"
            "a".compareTo("dbc") -> 'a' (97) < 'd' (100) -> negative, NOT better
            bestCandidate remains "dbc"

        Loop ends. Return "dbc".  <-- matches Example 1's expected answer.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 11: CLOSING SUMMARY
    ================================================================================================
    - The brute-force simulation (Approach 1) is exponential and only useful as a correctness
      oracle for tiny test cases.
    - The real unlock is the observation that the box's distinct content is exactly "every
      substring of length 1..(n - numFriends + 1)," which collapses the search space from
      exponential to linear-in-n candidates (Approach 2), and for each starting index only the
      longest allowed candidate can ever be optimal (prefix domination).
    - Approach 2 (O(n^2) worst case, O(n) candidates each up to O(maxLen) to build/compare) is my
      recommended interview solution: simple, provably correct, and fast enough for constraints on
      the order of a few thousand characters.
    - Approach 3 (rolling hash + binary search, O(n log n)) is the natural "can you do better?"
      follow-up, generalizing to much larger n at the cost of implementation complexity.
    - Known limitations/assumptions of my final solution: assumes lowercase-English-only input
      (any consistent char domain works identically); assumes standard Java String.compareTo
      ordering exactly matches the problem's stated ordering rule; assumes numFriends and
      word.length() are always valid per constraints (no defensive input validation added, though
      trivial to bolt on).
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ================================================================================================
    1. "Can you push this to O(n log n) or better if word.length() can be up to 10^5?"
       -> Present Approach 3 (rolling hash + binary search), or suffix array + LCP + sparse table.

    2. "What if we needed the K-th largest distinct string in the box, not just the largest?"
       -> Would need to actually enumerate/dedupe the O(n) candidate set (a Set<String> or sorting
          candidates), since a simple running max no longer suffices; discuss trade-offs of sorting
          vs. a heap of size K.

    3. "What if numFriends could be 0, or word could be empty — how would you defensively handle
        invalid input?"
       -> Add explicit validation throwing IllegalArgumentException for numFriends < 1,
          numFriends > word.length(), or empty word, before the main logic runs.

    4. "How would this change if we wanted the lexicographically SMALLEST string in the box
        instead?"
       -> The "always take the max length per start" trick breaks (a shorter prefix could now be
          smaller than its longer superstring extension isn't the concern — rather, the smallest
          substring could be as short as length 1 in general, since a single character can be
          smaller than any longer string sharing its prefix only if... actually the analogous
          insight is: the shortest strings (length 1) are always the best candidates for the
          minimum, since any length-1 prefix is <= its longer extension). Worth discussing on the
          fly — a good interviewer follow-up to test adaptability, not just memorized tricks.

    5. "If this function were called millions of times with different (word, numFriends) pairs on
        the SAME word, how would you optimize for repeated queries?"
       -> Precompute a suffix array + LCP + sparse table (or persistent hash structure) once for
          `word`, then answer each query by restricting the "effective suffix length" to that
          query's maxLen using O(log n) range-comparisons — amortizing preprocessing across calls.

    6. "Can two different starting indices ever tie for the global best, and does that matter?"
       -> Yes (see Example 3, "aaaa"). It doesn't matter for this problem since we only return the
          string value, not the position/round — but worth calling out proactively to show
          awareness of the tie case.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ================================================================================================
    1. Assuming you must literally simulate all splits (Approach 1) instead of spotting that the
       box's *distinct content* is fully characterized by "all substrings up to length
       n - numFriends + 1" — missing this collapses the problem from O(n) candidates to an
       exponential brute force, and candidates often time out trying to optimize the wrong thing.

    2. Off-by-one errors in maxLen: writing `n - numFriends` instead of `n - numFriends + 1`
       (forgetting that the "budget" formula is total length minus (numFriends - 1) reserved
       characters, not minus numFriends). This silently truncates the best candidate by one
       character and produces a wrong answer that's easy to miss on hand-crafted small tests.

    3. Forgetting to cap candidate length by remaining string length near the end of `word`
       (i.e., using `maxLen` directly instead of `Math.min(maxLen, n - startIndex)`), causing an
       ArrayIndexOutOfBoundsException / StringIndexOutOfBoundsException on the last few starting
       positions.

    4. Not trusting/using the prefix-domination property, and instead trying to compare "all
       possible lengths per starting index" (looping over length as well as start), which is
       correct but needlessly re-introduces higher complexity — missing the proof that shorter
       truncations from the same start index can never beat the full-length candidate from that
       same start.
    ================================================================================================
    */

    /*
    ================================================================================================
    DEMO / MAIN — exercises all three approaches against the examples from Section 3
    ================================================================================================
    */
    public static void main(String[] args) {
        runDemo("dbca", 2); // expect "dbc"
        runDemo("gggg", 4); // expect "g"
        runDemo("aaaa", 2); // expect "aaa"
    }

    private static void runDemo(String word, int numFriends) {
        System.out.println("word=\"" + word + "\", numFriends=" + numFriends);
        System.out.println("  Approach 1 (brute force):        " + bruteForce(word, numFriends));
        System.out.println("  Approach 2 (quadratic, recommended): " + findLargestStringInBox(word, numFriends));
        System.out.println("  Approach 3 (hashing + binary search): " + optimalHashingApproach(word, numFriends));
        System.out.println();
    }
}
