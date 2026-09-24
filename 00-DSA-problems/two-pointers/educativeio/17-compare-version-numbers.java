import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW WALKTHROUGH
 * Problem: Compare Version Numbers  (LeetCode 165)
 * ============================================================================
 *
 * This file is structured exactly as a candidate should narrate a real
 * interview: understand -> clarify -> examples -> brute force -> optimize
 * -> compare -> recommend -> deep dive -> trace -> wrap up -> follow-ups.
 */
public class CompareVersionNumbers {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words:
     * I'm given two "version strings", e.g. "1.02" and "1.2.0.1". Each string
     * is a sequence of "revisions" separated by dots. Each revision is a
     * numeric segment, and leading zeros in that segment don't count toward
     * its value ("02" means 2).
     *
     * I need to compare the two versions revision-by-revision, left to right,
     * as if they were arrays of integers. If one version has fewer revisions
     * than the other, the missing ones are treated as 0 (so "1.0.0" == "1").
     *
     * INPUT:
     *   - version1: String, non-empty, matches a version format such as
     *     "1", "1.0", "1.0.0.0.0.1", etc.
     *   - version2: String, same format.
     *
     * OUTPUT:
     *   - int: -1 if version1 < version2
     *   -       1 if version1 > version2
     *   -       0 if they are equal
     *
     * KEY CONSTRAINTS (from problem statement):
     *   - Each revision fits in a 32-bit signed integer, so I can safely
     *     parse each segment into an int (or even a long, to be extra safe)
     *     without overflow concerns per-segment.
     *   - Revisions are separated strictly by '.'.
     *
     * ASSUMPTIONS I'll state to the interviewer (and confirm in Section 2):
     *   - Revisions consist only of digits (no letters, no pre-release
     *     suffixes like "-beta", unlike SemVer).
     *   - Strings are non-null and non-empty per LeetCode's constraints, but
     *     I'll still defensively guard against null/blank input in production
     *     code.
     */


    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * 1. Q: Can a revision be an empty string, e.g. "1..2" or a trailing dot
     *       like "1."?
     *    A (assumed): No — per LeetCode constraints, the input is guaranteed
     *       to be a valid version string. I will not spend interview time
     *       hardening against malformed input, but I'll mention that
     *       production code should validate this.
     *
     * 2. Q: Can revisions contain non-digit characters (letters, hyphens,
     *       pre-release tags like SemVer's "1.0.0-alpha")?
     *    A (assumed): No, only digits 0-9 per revision, as stated in the
     *       problem.
     *
     * 3. Q: Can a revision have leading zeros, and if so how do I handle
     *       them, e.g. "01" vs "1"?
     *    A (assumed): Yes, leading zeros are allowed and must be stripped
     *       when computing the numeric value — "01" == "1" == 1.
     *
     * 4. Q: What's the maximum length of each version string / number of
     *       revisions? Does that affect whether I should optimize for
     *       allocation-heavy solutions (e.g., String.split creating arrays)?
     *    A (assumed): LeetCode constraints cap total length at 500 chars per
     *       string, so allocation cost is negligible either way. I'll still
     *       present a zero-allocation two-pointer approach as the "optimal"
     *       one, since it's the same asymptotic complexity but is a good
     *       signal of low-level string handling ability.
     *
     * 5. Q: Can a revision value be negative?
     *    A (assumed): No, version numbers are non-negative by convention and
     *       by the problem's constraints.
     *
     * 6. Q: Does a revision value fit in a 32-bit int, or could it be
     *       arbitrarily large (needing BigInteger)?
     *    A (assumed): Guaranteed to fit in a 32-bit signed integer per the
     *       problem statement, so a plain `int` (or `long` for extra safety
     *       margin) is sufficient — no BigInteger needed.
     *
     * 7. Q: Is comparison case-sensitive / do I need locale-aware parsing?
     *    A (assumed): Not applicable — input is purely numeric digits and
     *       dots, no locale-sensitive characters.
     *
     * 8. Q: Should missing trailing revisions really be treated as 0, e.g.
     *       is "1.0" considered equal to "1"?
     *    A (assumed): Yes, explicitly stated in the problem — missing
     *       revisions are treated as 0.
     */


    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case):
     *   version1 = "1.01"
     *   version2 = "1.001"
     *   Revisions v1 = [1, 1], v2 = [1, 1]  (leading zeros stripped)
     *   -> Equal -> return 0
     *
     * Example 2 (Different lengths / missing-revision-as-zero case):
     *   version1 = "1.0"
     *   version2 = "1.0.0"
     *   Revisions v1 = [1, 0], v2 = [1, 0, 0]
     *   Missing 3rd revision of v1 treated as 0 -> [1,0,0] vs [1,0,0]
     *   -> Equal -> return 0
     *
     * Example 3 (Boundary / tie-breaking case — differ in a later revision):
     *   version1 = "1.2"
     *   version2 = "1.10"
     *   Revisions v1 = [1, 2], v2 = [1, 10]
     *   First revision ties (1 == 1). Second revision: 2 < 10 numerically
     *   (this is the classic trap — comparing "2" vs "10" as STRINGS would
     *   wrongly say "2" > "10"; must compare as INTEGERS)
     *   -> version1 < version2 -> return -1
     *
     * Additional edge cases worth mentioning out loud:
     *   - version1 == version2 exactly, e.g. "1.0.1" vs "1.0.1" -> 0
     *   - One version is a strict prefix of the other with trailing zeros
     *     removed, e.g. "1.0.0.0" vs "1" -> 0 (all missing/zero revisions)
     *   - One version is a strict prefix WITHOUT trailing zeros,
     *     e.g. "1.1" vs "1" -> version1 > version2 -> 1
     *   - Leading zeros within a revision, e.g. "1.0100" -> value 100
     */


    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Applicable paradigms:
     *   - Brute force / naive        -> YES (string split + manual parse)
     *   - Sorting-based              -> NOT APPLICABLE: there's nothing to
     *         sort; we're comparing two fixed sequences, not reordering data.
     *   - Hashing-based              -> NOT APPLICABLE: hashing helps with
     *         membership/lookup problems; comparison here is inherently
     *         positional/sequential, not a lookup problem.
     *   - Two pointer / sliding window -> YES (this is the optimal approach)
     *   - Divide and conquer         -> NOT APPLICABLE: no natural way to
     *         split the problem into independent subproblems that combine
     *         cheaper than the linear scan itself.
     *   - Greedy                     -> Arguably the two-pointer scan IS a
     *         greedy left-to-right decision (stop at first difference), so
     *         I'll fold that observation into Approach 3 rather than list it
     *         separately.
     *   - Dynamic programming        -> NOT APPLICABLE: no overlapping
     *         subproblems or optimal substructure to exploit; it's a direct
     *         linear comparison.
     *   - Tree / graph traversal     -> NOT APPLICABLE: no hierarchical or
     *         graph structure here.
     *   - Heap / priority queue      -> NOT APPLICABLE: no need for ordering
     *         by priority; revisions must be compared in strict positional
     *         order, not by extracting min/max repeatedly.
     *   - Binary search              -> NOT APPLICABLE: binary search needs
     *         a monotonic search space; comparing two fixed strings has no
     *         such space to search over.
     *   - Monotonic stack / deque    -> NOT APPLICABLE: no need to maintain
     *         a monotonic ordering of elements as we scan.
     *   - Trie / segment tree        -> NOT APPLICABLE: these shine for
     *         prefix queries over many strings or range queries; here we
     *         have exactly two strings and a one-time linear comparison.
     *
     * So realistically there are 3 meaningfully DIFFERENT approaches, which
     * I'll present in increasing order of polish.
     */

    /*
     * ------------------------------------------------------------------
     * Approach 1: Brute Force — Split, Convert to Integer Arrays, Pad, Compare
     * ------------------------------------------------------------------
     * Core idea:
     *   Split both strings on ".", parse every token to an int, figure out
     *   which array is shorter, and compare index by index, treating
     *   out-of-bounds indices as 0.
     *
     * Data structure / paradigm: arrays, basic string parsing.
     *
     * Time Complexity: O(N + M) where N, M are lengths of version1/version2
     *   respectively — split + parse each token once.
     * Space Complexity: O(N + M) — String.split allocates a new String[]
     *   plus new String objects for every token.
     *
     * Pros:
     *   - Very easy to write and reason about, low bug risk.
     *   - Uses built-in split/parseInt, minimal custom logic.
     * Cons:
     *   - Extra memory: creates two String arrays plus intermediate token
     *     strings that are immediately discarded.
     *   - String.split uses regex internally (mild overhead for such a
     *     simple delimiter).
     *
     * When to use: Fine for production code where readability trumps
     *   micro-optimization — this is likely what most engineers would ship.
     * When NOT to use: If asked "can you do this with O(1) extra space" as
     *   a follow-up, this approach won't satisfy that.
     * ------------------------------------------------------------------
     */
    public static int compareBruteForce(String version1, String version2) {
        // split("\\.") uses regex; "." is a regex metacharacter so it must be escaped
        String[] revisions1 = version1.split("\\.");
        String[] revisions2 = version2.split("\\.");

        int maxRevisionCount = Math.max(revisions1.length, revisions2.length);

        for (int index = 0; index < maxRevisionCount; index++) {
            // Missing revisions default to 0, as required by the problem
            int value1 = index < revisions1.length ? Integer.parseInt(revisions1[index]) : 0;
            int value2 = index < revisions2.length ? Integer.parseInt(revisions2[index]) : 0;

            if (value1 != value2) {
                return Integer.compare(value1, value2);
            }
        }
        return 0; // every revision matched (after zero-padding)
    }


    /*
     * ------------------------------------------------------------------
     * Approach 2: Hashing/Normalization — Build Canonical Strings, Then Compare
     * ------------------------------------------------------------------
     * Core idea:
     *   Not a true "hashing" approach (as established, hashing doesn't
     *   naturally fit this problem) — instead this is a "normalize then
     *   compare" idea some candidates propose: strip leading zeros from
     *   every revision, rejoin with dots, pad the shorter one with ".0"
     *   revisions, then do a structural compare. I include it here mainly
     *   to show WHY it's inferior to Approach 1, since candidates sometimes
     *   suggest it.
     *
     * Data structure / paradigm: StringBuilder-based normalization.
     *
     * Time Complexity: O(N + M) — still linear, but with more constant-
     *   factor overhead (extra string building, extra passes).
     * Space Complexity: O(N + M) — builds new normalized strings.
     *
     * Pros:
     *   - Can be handy if you need the "canonical form" of a version string
     *     for other purposes (e.g., display or caching a key).
     * Cons:
     *   - More code, more edge cases (how much padding to add, comparing
     *     the padded numeric substrings correctly), for no complexity gain
     *     over Approach 1.
     *   - Easy to introduce subtle bugs when padding revisions with
     *     different digit lengths (this is exactly the "2" vs "10" trap).
     *
     * When to use: Only if you specifically need a reusable normalized
     *   version string elsewhere in the system.
     * When NOT to use: As the primary interview answer — it's strictly more
     *   complex than Approach 1 for the same complexity class.
     * ------------------------------------------------------------------
     */
    public static int compareByNormalization(String version1, String version2) {
        List<Integer> normalized1 = normalizeToIntList(version1);
        List<Integer> normalized2 = normalizeToIntList(version2);

        int maxLength = Math.max(normalized1.size(), normalized2.size());
        for (int index = 0; index < maxLength; index++) {
            int value1 = index < normalized1.size() ? normalized1.get(index) : 0;
            int value2 = index < normalized2.size() ? normalized2.get(index) : 0;
            if (value1 != value2) {
                return Integer.compare(value1, value2);
            }
        }
        return 0;
    }

    // Helper: converts "1.02.3" -> [1, 2, 3], stripping leading zeros via parseInt
    private static List<Integer> normalizeToIntList(String version) {
        List<Integer> result = new ArrayList<>();
        for (String token : version.split("\\.")) {
            result.add(Integer.parseInt(token));
        }
        return result;
    }


    /*
     * ------------------------------------------------------------------
     * Approach 3 (OPTIMAL): Two-Pointer Linear Scan, No Splitting/Allocation
     * ------------------------------------------------------------------
     * Core idea:
     *   Walk both strings simultaneously with two independent index
     *   pointers. At each step, scan forward from the current pointer to
     *   the next '.' (or end of string) in EACH string to isolate one
     *   revision's substring bounds, parse just that slice into an int
     *   value (skipping leading zeros naturally, since parsing digits left
     *   to right into an int ignores leading zeros for free), compare the
     *   two integer values, and advance both pointers past their
     *   delimiters. Continue until both strings are exhausted.
     *
     * Data structure / paradigm: two-pointer technique (greedy left-to-right
     *   decision — stop and return as soon as a difference is found).
     *
     * Time Complexity: O(N + M) — each character of each string is visited
     *   a constant number of times.
     * Space Complexity: O(1) extra space — no arrays, no split, no
     *   substrings allocated; we parse digits directly from the char array
     *   using index arithmetic.
     *
     * Pros:
     *   - No extra allocations beyond a couple of int/index variables —
     *     genuinely O(1) space.
     *   - Short-circuits: returns the moment a differing revision is found,
     *     without needing to fully parse the rest of either string.
     *   - Demonstrates strong fundamentals: manual parsing, index
     *     management, careful loop bounds — good interview signal.
     * Cons:
     *   - Slightly more code/index bookkeeping than Approach 1, so higher
     *     chance of off-by-one bugs if written carelessly (must be tested
     *     with a clear dry run).
     *
     * When to use: This is what I'd write as my primary/final interview
     *   answer once asked to optimize, and it's a fine default in
     *   production too — as good on readability as Approach 1 once helper
     *   methods are extracted, with strictly better space usage.
     * When NOT to use: If code brevity/readability is valued far above
     *   micro-optimizing memory (e.g., a quick internal script), Approach 1
     *   is arguably faster to write and just as correct.
     * ------------------------------------------------------------------
     */
    public static int compareTwoPointer(String version1, String version2) {
        int pointer1 = 0; // current index into version1
        int pointer2 = 0; // current index into version2
        int length1 = version1.length();
        int length2 = version2.length();

        // Loop until we've consumed both strings completely.
        // Using OR (not AND) because a shorter string must still be treated
        // as having trailing zero revisions until the longer one finishes.
        while (pointer1 < length1 || pointer2 < length2) {
            long revisionValue1 = 0; // long as a defensive overflow buffer while parsing digits
            // Parse one revision (a maximal run of digits) from version1
            while (pointer1 < length1 && version1.charAt(pointer1) != '.') {
                revisionValue1 = revisionValue1 * 10 + (version1.charAt(pointer1) - '0');
                pointer1++;
            }

            long revisionValue2 = 0;
            // Parse one revision from version2
            while (pointer2 < length2 && version2.charAt(pointer2) != '.') {
                revisionValue2 = revisionValue2 * 10 + (version2.charAt(pointer2) - '0');
                pointer2++;
            }

            if (revisionValue1 != revisionValue2) {
                return Long.compare(revisionValue1, revisionValue2);
            }

            // Skip the delimiter '.' itself, if present, to start next revision
            if (pointer1 < length1) {
                pointer1++; // move past '.'
            }
            if (pointer2 < length2) {
                pointer2++; // move past '.'
            }
        }
        return 0; // fully consumed both strings with no differing revision
    }


    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                     | Time      | Space | Best For              | Limitations                          |
     * |-------------------------------|-----------|-------|------------------------|---------------------------------------|
     * | 1. Brute Force (split+parse)  | O(N + M)  | O(N+M)| Quick, readable code   | Extra allocations from split/tokens  |
     * | 2. Normalize-then-Compare     | O(N + M)  | O(N+M)| Reusable canonical form| More code, no complexity gain, more bug surface |
     * | 3. Two-Pointer (optimal)      | O(N + M)  | O(1)  | Interview "optimize" ask, production hot paths | Slightly more index bookkeeping |
     *
     * (N, M = lengths of version1, version2 respectively)
     */


    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would START by coding Approach 1 (split + parse) out loud, since
     * it's fastest to produce correctly and shows I can nail the core logic
     * (zero-padding missing revisions, comparing as integers not strings)
     * without getting bogged down in index arithmetic. Once that's working
     * and I've verified it against the examples, I would PROACTIVELY offer:
     * "This uses O(N+M) extra space from split(); if we want O(1) extra
     * space I can rewrite this as a two-pointer scan" — and then implement
     * Approach 3.
     *
     * This mirrors real Google interview expectations: show a correct
     * solution fast, then demonstrate you can optimize when asked "can you
     * do better on space?" Approach 3 is what I'd want left on the board
     * as my final, polished answer.
     */


    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ========================================================================
     */
    public static final class VersionComparator {

        // Named constant for clarity instead of a magic character literal
        private static final char REVISION_DELIMITER = '.';

        /**
         * Compares two dot-delimited version strings by their numeric
         * revision values, treating missing trailing revisions as zero.
         *
         * @param version1 first version string, e.g. "1.02.3"; must be
         *                 non-null and contain only digits and '.' delimiters
         * @param version2 second version string, same format
         * @return -1 if version1 &lt; version2, 1 if version1 &gt; version2,
         *         0 if equal
         * @throws NullPointerException     if either argument is null
         * @throws IllegalArgumentException if either argument is empty,
         *                                   contains an invalid character,
         *                                   or has an empty revision
         *                                   (e.g. "1..2", ".1", "1.")
         */
        public static int compare(String version1, String version2) {
            // Defensive checks: guard clauses first, as is idiomatic in
            // production code, even though LeetCode guarantees valid input.
            Objects.requireNonNull(version1, "version1 must not be null");
            Objects.requireNonNull(version2, "version2 must not be null");
            if (version1.isEmpty() || version2.isEmpty()) {
                throw new IllegalArgumentException("Version strings must not be empty");
            }

            final int length1 = version1.length();
            final int length2 = version2.length();

            int pointer1 = 0;
            int pointer2 = 0;

            // Continue while either string still has unparsed revisions.
            while (pointer1 < length1 || pointer2 < length2) {
                RevisionParseResult parsedRevision1 = parseNextRevision(version1, pointer1, length1);
                RevisionParseResult parsedRevision2 = parseNextRevision(version2, pointer2, length2);

                pointer1 = parsedRevision1.nextIndexAfterDelimiter;
                pointer2 = parsedRevision2.nextIndexAfterDelimiter;

                if (parsedRevision1.value != parsedRevision2.value) {
                    return Long.compare(parsedRevision1.value, parsedRevision2.value);
                }
            }
            return 0;
        }

        /**
         * Parses exactly one revision (a maximal run of digit characters)
         * starting at {@code startIndex}, and returns both its numeric value
         * and the index at which the NEXT revision should begin (i.e., past
         * the delimiter, if one was present).
         */
        private static RevisionParseResult parseNextRevision(String version, int startIndex, int length) {
            long revisionValue = 0;
            int currentIndex = startIndex;
            boolean sawAtLeastOneDigit = false;

            while (currentIndex < length && version.charAt(currentIndex) != REVISION_DELIMITER) {
                char currentChar = version.charAt(currentIndex);
                if (currentChar < '0' || currentChar > '9') {
                    throw new IllegalArgumentException(
                        "Invalid character '" + currentChar + "' in version string: " + version);
                }
                revisionValue = revisionValue * 10 + (currentChar - '0');
                sawAtLeastOneDigit = true;
                currentIndex++;
            }

            if (!sawAtLeastOneDigit) {
                // Catches malformed input like "1..2", "1.", or leading "."
                throw new IllegalArgumentException(
                    "Empty revision segment encountered in version string: " + version);
            }

            // Skip the delimiter itself so the caller starts the next
            // revision cleanly on the following iteration.
            int nextIndexAfterDelimiter = currentIndex < length ? currentIndex + 1 : currentIndex;

            return new RevisionParseResult(revisionValue, nextIndexAfterDelimiter);
        }

        // Small immutable value holder — avoids returning multiple values
        // via output parameters or arrays-of-length-2.
        private record RevisionParseResult(long value, int nextIndexAfterDelimiter) {
        }

        private VersionComparator() {
            // Utility class: prevent instantiation
        }
    }


    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing VersionComparator.compare("1.2", "1.10") — the boundary/tie-
     * breaking example from Section 3.
     *
     * Initial state:
     *   version1 = "1.2"   (length1 = 3)
     *   version2 = "1.10"  (length2 = 4)
     *   pointer1 = 0, pointer2 = 0
     *
     * --- Iteration 1 ---
     * parseNextRevision(version1, startIndex=0):
     *   currentIndex=0: char '1' -> revisionValue = 0*10 + 1 = 1; currentIndex=1
     *   currentIndex=1: char '.' -> stop loop (delimiter hit)
     *   sawAtLeastOneDigit = true
     *   nextIndexAfterDelimiter = 1 + 1 = 2   (skip past the '.')
     *   => parsedRevision1 = { value = 1, nextIndexAfterDelimiter = 2 }
     *
     * parseNextRevision(version2, startIndex=0):
     *   currentIndex=0: char '1' -> revisionValue = 1; currentIndex=1
     *   currentIndex=1: char '.' -> stop loop
     *   nextIndexAfterDelimiter = 1 + 1 = 2
     *   => parsedRevision2 = { value = 1, nextIndexAfterDelimiter = 2 }
     *
     * pointer1 <- 2, pointer2 <- 2
     * Compare values: 1 == 1 -> no return yet, loop continues
     *
     * --- Iteration 2 ---
     * Loop condition check: pointer1(2) < length1(3) TRUE -> continue
     *
     * parseNextRevision(version1, startIndex=2):
     *   currentIndex=2: char '2' -> revisionValue = 2; currentIndex=3
     *   currentIndex=3 == length1(3) -> loop stops (end of string reached)
     *   sawAtLeastOneDigit = true
     *   nextIndexAfterDelimiter = 3 (no delimiter to skip, currentIndex == length)
     *   => parsedRevision1 = { value = 2, nextIndexAfterDelimiter = 3 }
     *
     * parseNextRevision(version2, startIndex=2):
     *   currentIndex=2: char '1' -> revisionValue = 0*10+1 = 1; currentIndex=3
     *   currentIndex=3: char '0' -> revisionValue = 1*10+0 = 10; currentIndex=4
     *   currentIndex=4 == length2(4) -> loop stops
     *   => parsedRevision2 = { value = 10, nextIndexAfterDelimiter = 4 }
     *
     * pointer1 <- 3, pointer2 <- 4
     * Compare values: 2 != 10 -> return Long.compare(2, 10) = -1
     *
     * FINAL RESULT: -1  (version1 "1.2" < version2 "1.10") ✓ matches expectation
     *
     * Note how the trace makes the classic trap explicit: had we compared
     * the substrings "2" and "10" lexicographically instead of numerically,
     * we would have wrongly concluded "2" > "10".
     */


    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * All three approaches share the same O(N + M) time complexity, since
     * every character of both strings must be inspected at least once in
     * the worst case (when the versions are equal, we can't short-circuit
     * early).
     *
     * The differentiator is SPACE:
     *   - Approach 1 (split + parse): O(N + M) space from split()'s array
     *     and token Strings. Best when code clarity and speed-of-writing
     *     matter more than memory (most everyday code).
     *   - Approach 2 (normalize-then-compare): also O(N + M) space, more
     *     code, no benefit over Approach 1 — mainly useful if you need a
     *     reusable canonical representation elsewhere.
     *   - Approach 3 (two-pointer, optimal): O(1) extra space, same time
     *     complexity, with the added benefit of short-circuiting on the
     *     first differing revision without needing to finish parsing
     *     either string.
     *
     * Known assumptions/limitations of the final VersionComparator:
     *   - Assumes valid input: only digits and single '.' delimiters, no
     *     empty revisions, no leading '.' or trailing '.'. Violations throw
     *     IllegalArgumentException rather than silently misbehaving.
     *   - Uses `long` internally per revision as a safety margin, though the
     *     problem guarantees each revision fits in a 32-bit int.
     *   - Does not support SemVer-style pre-release/build metadata suffixes
     *     (e.g. "1.0.0-alpha+001") — out of scope per the problem statement.
     */


    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS
     * ========================================================================
     *
     * 1. "What if version strings could contain pre-release tags like
     *    SemVer (e.g. '1.0.0-alpha' vs '1.0.0')?" -> Would need to parse and
     *    compare the pre-release segment separately, with SemVer's rule
     *    that a pre-release version has LOWER precedence than the
     *    associated normal version.
     *
     * 2. "What if you had to compare a LIST of many version strings and
     *    sort them?" -> Convert each to a Comparable (e.g. implement
     *    Comparator<String> using this same parsing logic) and use
     *    Collections.sort / Arrays.sort, O(K log K * L) where K is the
     *    number of versions and L is average length.
     *
     * 3. "Can you do this without using the '.' character check directly —
     *    e.g. using regex or Scanner?" -> Yes, e.g. Scanner with a '.'
     *    delimiter or Pattern/Matcher; discuss trade-offs (regex overhead
     *    vs. manual parsing performance).
     *
     * 4. "What if the version strings were extremely long (millions of
     *    revisions)?" -> Two-pointer approach still wins since it avoids
     *    building large intermediate arrays; could also stream-parse if
     *    input arrived as a stream rather than a full in-memory String.
     *
     * 5. "How would you handle concurrent calls to this comparator from
     *    multiple threads?" -> The method is already stateless/pure (no
     *    shared mutable state), so it's inherently thread-safe as written;
     *    I'd just confirm no caching layer introduces shared mutable state.
     *
     * 6. "What if a revision could be negative (e.g. '-1.2')?" -> Would need
     *    to explicitly parse an optional leading '-' per revision and adjust
     *    the parsing/comparison logic (and clarify whether "-" is even a
     *    valid revision start per the new spec).
     */


    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Comparing revisions as STRINGS instead of INTEGERS — e.g. wrongly
     *    concluding "10" < "2" via lexicographic string comparison, instead
     *    of numeric comparison (10 > 2). This is the single most common bug.
     *
     * 2. Forgetting to strip/ignore LEADING ZEROS — e.g. treating "01" as
     *    different from "1" if doing a naive string-equality check instead
     *    of parsing to an integer first.
     *
     * 3. Off-by-one errors in the two-pointer approach around the delimiter
     *    skip — forgetting to advance past the '.' character after parsing
     *    a revision, causing an infinite loop or re-parsing the same
     *    delimiter repeatedly.
     *
     * 4. Not handling DIFFERENT-LENGTH version strings correctly — using an
     *    AND instead of OR in the loop condition (e.g. stopping as soon as
     *    ONE string is exhausted) instead of continuing until BOTH are
     *    exhausted, which breaks the "missing revision == 0" rule (e.g.
     *    "1.1" vs "1.1.0.0.1" would be wrongly judged equal if the loop
     *    stops too early).
     */


    /*
     * ========================================================================
     * RUNNABLE DEMO / TEST HARNESS
     * ========================================================================
     */
    public static void main(String[] args) {
        // A small set of test cases covering normal, edge, and boundary cases
        String[][] testCases = {
            {"1.01", "1.001"},   // expect 0  (leading zeros)
            {"1.0", "1.0.0"},    // expect 0  (missing revision as zero)
            {"1.2", "1.10"},     // expect -1 (numeric, not lexicographic, comparison)
            {"1.0.0.0", "1"},    // expect 0  (all-zero trailing revisions)
            {"1.1", "1"},        // expect 1  (extra non-zero revision)
            {"1.0.1", "1.0.1"},  // expect 0  (exact equality)
        };

        System.out.println("Approach 1 (Brute Force):");
        for (String[] testCase : testCases) {
            System.out.printf("  compare(\"%s\", \"%s\") = %d%n",
                testCase[0], testCase[1], compareBruteForce(testCase[0], testCase[1]));
        }

        System.out.println("Approach 2 (Normalize-then-Compare):");
        for (String[] testCase : testCases) {
            System.out.printf("  compare(\"%s\", \"%s\") = %d%n",
                testCase[0], testCase[1], compareByNormalization(testCase[0], testCase[1]));
        }

        System.out.println("Approach 3 (Two-Pointer, optimal):");
        for (String[] testCase : testCases) {
            System.out.printf("  compare(\"%s\", \"%s\") = %d%n",
                testCase[0], testCase[1], compareTwoPointer(testCase[0], testCase[1]));
        }

        System.out.println("Deep Dive: VersionComparator (production-quality):");
        for (String[] testCase : testCases) {
            System.out.printf("  compare(\"%s\", \"%s\") = %d%n",
                testCase[0], testCase[1], VersionComparator.compare(testCase[0], testCase[1]));
        }

        // Sanity check: all four approaches must agree on every test case
        boolean allAgree = true;
        for (String[] testCase : testCases) {
            int r1 = compareBruteForce(testCase[0], testCase[1]);
            int r2 = compareByNormalization(testCase[0], testCase[1]);
            int r3 = compareTwoPointer(testCase[0], testCase[1]);
            int r4 = VersionComparator.compare(testCase[0], testCase[1]);
            if (r1 != r2 || r2 != r3 || r3 != r4) {
                allAgree = false;
                System.out.printf("  MISMATCH on (%s, %s): %d, %d, %d, %d%n",
                    testCase[0], testCase[1], r1, r2, r3, r4);
            }
        }
        System.out.println("All approaches agree on all test cases: " + allAgree);
    }
}
