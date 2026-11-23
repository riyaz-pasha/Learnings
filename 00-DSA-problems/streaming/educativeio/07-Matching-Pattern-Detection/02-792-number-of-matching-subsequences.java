/**
 * 0. ## Problem Statement
 * Given a string `s` and an array of strings `words`, return the number of `words[i]`
 * that are a subsequence of `s`.
 *
 * A subsequence of a string is a new string generated from the original string with some
 * characters (can be none) deleted without changing the relative order of the remaining characters.
 *   - Example 1: s = "abcde", words = ["a", "bb", "acd", "ace"] -> Output: 3 ("a", "acd", "ace")
 *   - Example 2: s = "dsahjpjauf", words = ["ahjpjau", "ja", "ahbwzgqnuk", "tnmlanowax"] -> Output: 2
 *
 * Constraints:
 * - 1 <= s.length <= 5 * 10^4
 * - 1 <= words.length <= 5000
 * - 1 <= words[i].length <= 50
 * - `s` and `words[i]` consist strictly of lowercase English letters (`'a'`-`'z'`).
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can `words` contain duplicate strings, and if so, should each duplicate occurrence be counted?"
 *    -> Prevents putting `words` into a `HashSet` and undercounting; "number of `words[i]`" means
 *       every matching index counts (e.g., `["a", "a"]` in `"a"` returns 2).
 * 2. "What is the character set — strictly lowercase `'a'-'z'`, or full ASCII / Unicode?"
 *    -> Determines whether we can use a compact 26-slot array `buckets[26]` vs. a `HashMap<Character, ...>`.
 * 3. "Can any `words[i]` be empty, or longer than `s`?"
 *    -> Constraints say `words[i].length >= 1`, preventing an immediate empty-string edge case;
 *       words longer than `s` are possible and must simply fail to match.
 * 4. "Are we given all `words` upfront (batch), or will words arrive one-by-one as online queries against a fixed `s`?"
 *    -> Drives the architecture! Batch `words` favors Single-Pass Buckets (O(N + W*L)); online
 *       queries against a fixed `s` favor Preprocessing `s` with Binary Search or a `nextPos` table.
 * 5. "Could `s` be huge (e.g., a 10GB file or infinite stream) while `words` is small?"
 *    -> Rules out storing index lists of `s` (O(N) memory) and highlights why Next-Pointer Buckets
 *       (O(W) memory, single streaming pass over `s`) is strictly superior in memory.
 * 6. "Do we only need the count of matching words, or the actual list of matching words/indices?"
 *    -> Returning the count needs just an `int`; returning indices just stores `wordIdx` in the cursor.
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: `s.length` (`N`) is up to `5 * 10^4`, while `words.length` (`W`) is up to
 * `5,000` and each word has length `L <= 50`. Notice the huge asymmetry: `N` is 1,000x larger
 * than `L`, and total word characters `W * L <= 2.5 * 10^5` is small, whereas `W * N = 2.5 * 10^8`
 * is dangerously close to TLE (and collapses completely if `N = 10^6`).
 *
 * --- Approach 1: Brute Force (Two-Pointer Subsequence Check per Word) ---
 * 0. Idea/Intuition: For every `word` in `words`, run the standard two-pointer `isSubsequence(word, s)`
 *    check: scan `s` from index 0 to `N - 1`, advancing the word pointer whenever characters match.
 * 1. What I'd naturally try: Solve the 1-word subproblem ("Is Subsequence", LC 392) and wrap it in a loop.
 * 2. Why it works: Greedily matching each character of `word` at its earliest available position
 *    in `s` leaves the maximum remaining suffix of `s` for future characters.
 * 3. Why it's too slow/costly: On a worst-case input like `s = "aaa...a"` (50,000 `'a'`s) and
 *    5,000 words `"az"`, every single word scans all 50,000 characters of `s` looking for `'z'`
 *    -> `5,000 * 50,000 = 2.5 * 10^8` iterations!
 * 4. What work is being repeated: We walk through the ENTIRE string `s` 5,000 separate times,
 *    even though `s` never changes — and we spend 99.9% of our time skipping over characters in `s`
 *    that don't match the character the current word is looking for!
 * 5. Observation that removes the bottleneck: Instead of stepping through `s` one character at a
 *    time to hunt for the next occurrence of `c`, preprocess `s` ONCE by recording the sorted
 *    list of indices where each letter `'a'..'z'` appears. Then jump straight to the next
 *    occurrence of `c` via Binary Search!
 * 6. Leads to Approach 2: Character Index Lists + Binary Search.
 *    Time Complexity: O(W * (N + L)) = O(W * N) — because each of the W words may scan all N
 *    characters of `s` before failing or finishing.
 *    Space Complexity: O(1) auxiliary — only two integer pointers (`i`, `j`) and a match counter.
 *
 * --- Approach 2: Character Index Lists + Binary Search (Preprocess `s`) ---
 * 0. Idea/Intuition: Make one O(N) pass over `s` to populate `List<Integer>[] pos = new List[26]`,
 *    where `pos[c - 'a']` holds all indices of `c` in `s` (automatically sorted ascending!).
 *    For each `word`: start with `prevIdx = -1`. For each char `c` in `word`, binary search
 *    `pos[c - 'a']` for the smallest index strictly `> prevIdx`. If none exists, `word` fails;
 *    otherwise set `prevIdx = foundIdx` and continue.
 * 1. What I'd naturally try: "Don't scan `s` linearly for each letter — index `s` by character
 *    and binary-search the next valid position!"
 * 2. Why it works: Because each `pos[c]` list is sorted, binary search finds the exact earliest
 *    valid occurrence of `c` after `prevIdx` in O(log N) instead of O(N).
 * 3. Why it's still suboptimal:
 *    - We still pay an O(log N) binary search factor on every character of every word.
 *    - We must store all `N` indices of `s` in memory (O(N) space), which breaks if `s` is a
 *      massive stream that doesn't fit in RAM.
 * 4. What work is being repeated: Each word independently asks `s` "where is my next character?"
 *    by searching `s`. Flip the question around!
 * 5. Observation that removes the bottleneck: Instead of matching 1 word against `s` at a time,
 *    walk `s` **ONCE** from left to right and advance **ALL** words waiting for `s.charAt(i)`
 *    in parallel! Group all unfinished words into 26 buckets based on the single next character
 *    they are currently waiting for. When `'c'` arrives in `s`, ONLY words sitting in `buckets['c']`
 *    can make progress — every other word is untouched!
 * 6. Leads to Approach 3: 26 Next-Character Buckets (Parallel State Machine).
 *    Time Complexity: O(N + W * L * log N) — O(N) to build the 26 index lists, plus up to `W * L`
 *    binary searches, each costing O(log N) in the worst case (when a single letter dominates `s`).
 *    Space Complexity: O(N) auxiliary — the 26 `ArrayList<Integer>`s store all N indices of `s`.
 *
 * --- Approach 3: 26 Next-Character Buckets / Parallel State Machine (Optimal) ---
 * 0. Idea/Intuition: Create 26 queues `buckets[0..25]`. Each word is represented by a lightweight
 *    mutable cursor `WordCursor(word, pos)` (initially `pos = 0`) placed into
 *    `buckets[word.charAt(0) - 'a']`.
 *    Now scan each character `c` of `s` ONCE:
 *    - Snapshot `int waiting = buckets[c - 'a'].size()` (CRITICAL! Prevents advancing a word
 *      twice on the same character `c` when `word` has consecutive identical letters like `"bb"`).
 *    - Loop `waiting` times: poll `cursor`, increment `cursor.pos++`.
 *      - If `cursor.pos == cursor.word.length()`, the word finished -> `matched++`!
 *      - Else re-bucket `cursor` into `buckets[cursor.word.charAt(cursor.pos) - 'a']`.
 * 1. Why someone thinks of it: Classic "invert the loop" trick — instead of looping over `words`
 *    and scanning `s`, loop over `s` once and maintain the frontier of all `words` waiting on
 *    each character.
 * 2. Why it works: Each cursor always sits in the exact bucket of the next unmatched character
 *    of its word, and only moves forward when that exact character appears in `s`. Because we
 *    snapshot `waiting` before draining, each character of `s` advances each word by at most 1
 *    position — faithfully reproducing greedy earliest-match subsequence matching for all W words
 *    simultaneously.
 *    Time Complexity: O(N + sum(words[i].length)) = O(N + W * L) — each of the N characters in `s`
 *    is visited once (O(1) lookup), and each of the <= W * L word characters causes at most one
 *    bucket poll + one bucket offer (O(1) each). Completely eliminates the `log N` factor!
 *    Space Complexity: O(W) auxiliary — exactly W `WordCursor` objects live across the 26 buckets
 *    (plus 26 empty `ArrayDeque` headers), completely independent of `N = s.length()`!
 *
 * Interview Verdict:
 * Write **Approach 3 (26 Next-Character Buckets)** in a 45-minute interview!
 * It achieves the theoretical optimal O(N + W * L) time AND O(W) space, works in a single
 * streaming pass over `s`, is ~25 lines of clean Java, and avoids binary-search off-by-one bugs.
 * Mention Approach 2 (Index Lists + Binary Search) when the interviewer asks "what if `s` is
 * fixed and `words` arrive later as separate online queries?"
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Consecutive identical characters in a word (`s = "ab"`, `words = ["bb", "aa"]` -> 0):
 *   THE #1 bug test for Approach 3! Without snapshotting `bucket.size()`, `"bb"` pops on `'b'`,
 *   re-enters `buckets['b']`, and pops AGAIN on the exact same `'b'`, falsely returning 1.
 * - Duplicate words in `words` (`s = "abc"`, `words = ["ac", "ac", "ac"]` -> 3):
 *   Every index in `words` must be counted separately; never deduplicate into a `Set` without
 *   preserving multiplicities.
 * - Word longer than `s` (`s = "ab"`, `words = ["abc", "aaa"]` -> 0): Cursors simply remain
 *   unfinished in their buckets when `s` ends.
 * - Word containing characters absent from `s` (`s = "abc"`, `words = ["az", "z"]` -> 0):
 *   In Approach 2, `pos['z' - 'a']` is empty (binary search must handle empty list cleanly).
 * - Single-character `s` and words (`s = "a"`, `words = ["a", "b", "a"]` -> 2): Cursor finishes
 *   immediately on its first advance (`pos == 1 == length`).
 * - Exact match (`s = "abcde"`, `words = ["abcde"]` -> 1) & all words match vs. zero match.
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "Why am I scanning `s` 5,000 times when `s` only has 26 possible
 * characters and every word is just waiting for ONE specific next character?"
 * Correctness intuition: At any point while reading `s`, each unfinished word only cares about
 * its single next unmatched character `word[pos]`. Bucketing all words by `word[pos]` means
 * when character `c` arrives in `s`, ONLY words in `buckets[c]` can advance — and snapshotting
 * `buckets[c].size()` ensures no word consumes the same character of `s` twice. Every word
 * character is processed at most once, proving O(N + sum(len(words))) time.
 *
 * -- Examples & ASCII Diagram --
 * Example 1: s = "abcde", words = ["a", "bb", "acd", "ace"]
 *
 * Initial 26 Buckets (showing `word` with `^` marking `pos`):
 *   buckets['a']: [ ^a, ^acd, ^ace ]
 *   buckets['b']: [ ^bb ]
 *
 * Read s[0] = 'a' (snapshot size = 3):
 *   - ^a   -> DONE! (matched = 1)
 *   - ^acd -> a^cd -> move to buckets['c']
 *   - ^ace -> a^ce -> move to buckets['c']
 *   Buckets now: 'b':[ ^bb ], 'c':[ a^cd, a^ce ]
 *
 * Read s[1] = 'b' (snapshot size = 1):
 *   - ^bb  -> b^b  -> move BACK to buckets['b'] (snapshot prevents popping it again!)
 *   Buckets now: 'b':[ b^b ], 'c':[ a^cd, a^ce ]
 *
 * Read s[2] = 'c' (snapshot size = 2):
 *   - a^cd -> ac^d -> move to buckets['d']
 *   - a^ce -> ac^e -> move to buckets['e']
 *   Buckets now: 'b':[ b^b ], 'd':[ ac^d ], 'e':[ ac^e ]
 *
 * Read s[3] = 'd' -> ac^d DONE! (matched = 2)
 * Read s[4] = 'e' -> ac^e DONE! (matched = 3)
 * End of `s` -> return 3! (`b^b` is left stranded in `buckets['b']`.)
 *
 * -- Dry run 1: Optimal Buckets on Example 1: s = "abcde", words = ["a", "bb", "acd", "ace"] --
 *   Step | s[i] | Snapshot Size | Polled & Advanced                     | Non-Empty Buckets After        | Matched
 *   -----+------+---------------+---------------------------------------+--------------------------------+--------
 *   init |  -   | -             | -                                     | a:[a(0),acd(0),ace(0)],b:[bb(0)]| 0
 *    0   | 'a'  | 3             | a(1)->DONE; acd(1)->c; ace(1)->c      | b:[bb(0)], c:[acd(1),ace(1)]   | 1
 *    1   | 'b'  | 1             | bb(1)->b (re-added AFTER snapshot!)   | b:[bb(1)], c:[acd(1),ace(1)]   | 1
 *    2   | 'c'  | 2             | acd(2)->d; ace(2)->e                  | b:[bb(1)], d:[acd(2)], e:[ace(2)]| 1
 *    3   | 'd'  | 1             | acd(3)->DONE                          | b:[bb(1)], e:[ace(2)]          | 2
 *    4   | 'e'  | 1             | ace(3)->DONE                          | b:[bb(1)]                      | 3
 *
 * -- Dry run 2: Failing-Approach Trap — Consecutive Duplicate Letters `s = "b"`, `words = ["bb"]` --
 *   WRONG (no snapshot: `while (!buckets['b'].isEmpty())`):
 *     - Poll `bb(0)` -> advance to `bb(1)` -> re-offer to `buckets['b']`.
 *     - Loop sees `buckets['b']` is STILL non-empty! Polls `bb(1)` on the SAME `'b'` -> `bb(2)` DONE!
 *     - Returns 1 (WRONG! `"bb"` is NOT a subsequence of `"b"`!).
 *   CORRECT (with snapshot `int waiting = buckets['b'].size()` = 1):
 *     - Loop runs `waiting = 1` time: poll `bb(0)` -> advance to `bb(1)` -> offer to `buckets['b']`.
 *     - Loop terminates! Stream ends -> returns 0 (CORRECT!).
 *
 * -- Pitfalls --
 * 1. Forgetting to snapshot `bucket.size()` before draining: Causes consecutive identical letters
 *    (`"bb"`, `"aa"`) to consume the same character of `s` multiple times!
 * 2. Slicing strings (`word.substring(1)`) instead of storing `(word, pos)`: In Java 7+,
 *    `substring()` copies the backing `byte[]` array, turning O(W * L) into O(W * L^2) time and
 *    massive GC churn. Always advance an `int pos` cursor instead!
 * 3. Calling `Collections.binarySearch` carelessly in Approach 2: Searching for `prevIdx` instead
 *    of `prevIdx + 1` can match `prevIdx` itself (reusing the same index in `s` twice!). Always
 *    search for `prevIdx + 1` (strictly greater than `prevIdx`).
 * 4. Java-specific gotcha — Creating generic arrays: `new ArrayDeque<WordCursor>[26]` fails to
 *    compile in Java ("generic array creation"). Use `@SuppressWarnings("unchecked") Deque<WordCursor>[] buckets = new ArrayDeque[26];`.
 *
 * -- Pattern recognition --
 * When you see: "Match MANY small target strings against ONE long string (or stream)"
 * -> Think **Invert the Loop / 26 Next-Character Buckets** (if batch) or **Character Index
 *    Precomputation + Binary Search** (if online queries).
 * Transfers directly to:
 *   - Is Subsequence (LeetCode 392 — the famous follow-up: "what if there are 10^9 incoming `t` strings?")
 *   - Shortest Way to Form String (LeetCode 1055) — uses the exact same `pos[26]` + Binary Search
 *     or `nextPos[N+1][26]` automaton!
 *
 * -- Interview script --
 * "Running the standard two-pointer subsequence check independently for each word takes O(W * N),
 * which scans `s` 5,000 times and approaches 2.5 * 10^8 operations. We could preprocess `s` into
 * 26 sorted index lists and binary search each character in O(N + W * L log N), which is great
 * for online queries. Even better, since all words are given upfront, we can invert the loop and
 * scan `s` just ONCE. I'll maintain 26 queues — one per lowercase letter — where each word sits
 * as a lightweight `(word, pos)` cursor in the bucket of the next character it's waiting for.
 * As I read each character `c` of `s`, I snapshot `buckets[c].size()` so no word advances twice
 * on the same letter, poll each waiting cursor, increment `pos`, and either count a match if
 * `pos == word.length()` or move it to its next character's bucket. Because `s` is scanned once
 * and each word character moves buckets at most once, that gives us O(N + W * L) time and O(W) space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: What if `s` is FIXED upfront, and `words` arrive one-by-one as ONLINE queries (`isSubsequence(word)`)?
 * A1: Approach 3 (Buckets) requires knowing `words` before scanning `s`. For online queries:
 *     - Option A (Approach 2): Preprocess `pos[26]` in O(N) time / O(N) space -> each query takes
 *       O(L log N).
 *     - Option B (Next-Occurrence Automaton): Preprocess `int[][] nextPos = new int[N + 1][26]`
 *       right-to-left in O(26 * N) time / O(26 * N) space, where `nextPos[i][c]` is the smallest
 *       index `>= i` where `c` appears in `s` (or `-1`). Then EVERY online query runs in strict
 *       **O(L)** time with ZERO binary search!
 *
 * Q2: What if `words` has 1,000,000 strings, most of which are duplicates or share long prefixes
 *     (e.g., `"apple"`, `"apples"`, `"application"`)?
 * A2: - Duplicates: Group `words` into a `Map<String, Integer> wordCount` first, attach `count`
 *       to each `WordCursor`, and add `cursor.count` when it finishes.
 *     - Shared prefixes: Insert `words` into a **Trie** (storing `wordCount` at terminal nodes)
 *       and bucket **Trie nodes** (instead of individual words!) by their outgoing edge letters.
 *       A shared prefix of 10,000 words then advances in a SINGLE step instead of 10,000 steps!
 *
 * Q3: What if `s` is a 100GB stream arriving over a network (cannot fit in RAM)?
 * A3: Approach 3 (26 Buckets) ALREADY solves this with zero changes! It reads `s` sequentially
 *     one character at a time and uses only O(W) memory (just the `WordCursor`s for the dictionary),
 *     completely independent of how many gigabytes `s` is.
 * =========================================================================================
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class NumberOfMatchingSubsequencesStudyNote {

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force Two-Pointer per Word (O(W * N) time, O(1) space)
    // ----------------------------------------------------------------------
    public static int numMatchingSubseqBruteForce(String s, String[] words) {
        int matched = 0;
        for (String word : words) {
            if (isSubsequence(word, s)) {
                matched++;
            }
        }
        return matched;
    }

    private static boolean isSubsequence(String word, String s) {
        int wi = 0;
        int si = 0;
        while (wi < word.length() && si < s.length()) {
            if (word.charAt(wi) == s.charAt(si)) {
                wi++;
            }
            si++;
        }
        return wi == word.length();
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Character Index Lists + Binary Search
    // (O(N + W * L * log N) time, O(N) auxiliary space — ideal for online queries)
    // ----------------------------------------------------------------------
    public static int numMatchingSubseqBinarySearch(String s, String[] words) {
        // pos[c] stores every index in `s` where letter ('a' + c) occurs, naturally sorted ascending.
        // Example for s = "abcda": pos['a'-'a'] = [0, 4], pos['b'-'a'] = [1], ...
        @SuppressWarnings("unchecked")
        List<Integer>[] pos = new ArrayList[26];
        for (int c = 0; c < 26; c++) {
            pos[c] = new ArrayList<>();
        }
        for (int i = 0; i < s.length(); i++) {
            pos[s.charAt(i) - 'a'].add(i);
        }

        int matched = 0;
        for (String word : words) {
            if (matchesWithBinarySearch(word, pos)) {
                matched++;
            }
        }
        return matched;
    }

    private static boolean matchesWithBinarySearch(String word, List<Integer>[] pos) {
        int prevIdx = -1; // Last matched index in `s`; next char must appear strictly AFTER this.

        for (int i = 0; i < word.length(); i++) {
            List<Integer> indices = pos[word.charAt(i) - 'a'];
            // Find the smallest index in `indices` that is >= (prevIdx + 1).
            int nextIdx = firstIndexAtLeast(indices, prevIdx + 1);
            if (nextIdx == -1) {
                return false; // No remaining occurrence of this character in `s`.
            }
            prevIdx = nextIdx;
        }
        return true;
    }

    // Standard lower-bound binary search: returns smallest value in sorted `list` >= `target`, or -1.
    private static int firstIndexAtLeast(List<Integer> list, int target) {
        int lo = 0;
        int hi = list.size(); // Exclusive upper bound so `lo == list.size()` signals "not found".
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (list.get(mid) < target) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return (lo == list.size()) ? -1 : list.get(lo);
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: 26 Next-Character Buckets / Parallel State Machine (OPTIMAL)
    // (O(N + W * L) time, O(W) auxiliary space — single streaming pass over `s`!)
    // ----------------------------------------------------------------------

    // Lightweight mutable cursor tracking a single word's progress without slicing substrings
    // or allocating new objects during transitions.
    // Example: word = "acd", pos = 1 means 'a' matched and this word is now waiting for 'c'.
    private static class WordCursor {
        final String word;
        int pos;

        WordCursor(String word) {
            this.word = word;
            this.pos = 0;
        }
    }

    public static int numMatchingSubseqBuckets(String s, String[] words) {
        // buckets[c] holds all unfinished words currently waiting for character ('a' + c) next.
        // Example for words = ["a", "bb", "acd", "ace"]:
        //   buckets['a'-'a'] = [("a",0), ("acd",0), ("ace",0)]
        //   buckets['b'-'a'] = [("bb",0)]
        @SuppressWarnings("unchecked")
        Deque<WordCursor>[] buckets = new ArrayDeque[26];
        for (int c = 0; c < 26; c++) {
            buckets[c] = new ArrayDeque<>();
        }

        // Seed every word into the bucket of its first character.
        for (String word : words) {
            buckets[word.charAt(0) - 'a'].offerLast(new WordCursor(word));
        }

        int matched = 0;

        // Walk `s` ONCE from left to right, advancing only the words waiting for s.charAt(i).
        for (int i = 0; i < s.length(); i++) {
            Deque<WordCursor> bucket = buckets[s.charAt(i) - 'a'];

            // CRITICAL: Snapshot current size before draining!
            // If a word like "bb" needs another 'b' next, it gets re-added to THIS same bucket;
            // looping only `waiting` times ensures it does NOT consume the same 'b' twice.
            int waiting = bucket.size();
            for (int k = 0; k < waiting; k++) {
                WordCursor cursor = bucket.pollFirst();
                cursor.pos++;

                if (cursor.pos == cursor.word.length()) {
                    // Every character of this word has now been matched in order!
                    matched++;
                } else {
                    // Move cursor to the bucket of its next required character.
                    char nextNeeded = cursor.word.charAt(cursor.pos);
                    buckets[nextNeeded - 'a'].offerLast(cursor);
                }
            }
        }
        return matched;
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 3 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private static void verifyCase(String name, String s, String[] words, int expected) {
        int rBrute = numMatchingSubseqBruteForce(s, words);
        int rBin = numMatchingSubseqBinarySearch(s, words);
        int rBuck = numMatchingSubseqBuckets(s, words);

        boolean pass = (rBrute == expected) && (rBin == expected) && (rBuck == expected);

        System.out.printf("%-38s | %s | Output: %d%n",
                name, pass ? "PASS" : "FAIL", rBuck);

        if (!pass) {
            System.out.printf("  Expected=%d, Brute=%d, BinarySearch=%d, Buckets=%d%n",
                    expected, rBrute, rBin, rBuck);
        }
    }

    public static void main(String[] args) {
        verifyCase("LC Example 1",
                "abcde",
                new String[]{"a", "bb", "acd", "ace"},
                3);

        verifyCase("LC Example 2",
                "dsahjpjauf",
                new String[]{"ahjpjau", "ja", "ahbwzgqnuk", "tnmlanowax"},
                2);

        verifyCase("Edge: Consecutive dupes (snapshot!)",
                "ab",
                new String[]{"bb", "aa", "ab", "ba"},
                1);

        verifyCase("Edge: Duplicate words in input",
                "abc",
                new String[]{"ac", "ac", "ac", "ca"},
                3);

        verifyCase("Edge: Words longer than s",
                "ab",
                new String[]{"abc", "aab", "ab", "a"},
                2);

        verifyCase("Edge: Missing characters in s",
                "abc",
                new String[]{"z", "az", "cz"},
                0);

        verifyCase("Edge: Single-char s and words",
                "a",
                new String[]{"a", "b", "a", "aa"},
                2);

        verifyCase("Edge: Exact full-string match",
                "abcde",
                new String[]{"abcde", "abcdf", "bcde"},
                2);
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Inverted Loop / 26 Next-Character Buckets (Parallel State Machine).
 * - Key observation: Instead of scanning `s` once per word (`O(W * N)`), scan `s` ONCE and
 *   bucket all unfinished words by the single next character they are waiting for — advancing
 *   each word character at most once (`O(N + W * L)` time, `O(W)` space).
 * - Worth memorizing vs. understanding: Understand *why* inverting the loop eliminates both
 *   re-scanning `s` and the `log N` binary-search factor; memorize snapshotting `bucket.size()`
 *   before draining and using a mutable `(word, pos)` cursor instead of `substring()`.
 * - Most common trap: Draining `while (!bucket.isEmpty())` without snapshotting size, which
 *   lets consecutive identical letters like `"bb"` match a single `'b'` in `s`.
 * - One-line mental trigger: "Many small subsequences in one long string? Bucket cursors by next needed char and scan `s` once."
 * =========================================================================================
 */








































import java.util.*;

class Solution {

    /*
     * Each waiting word is represented by:
     *
     * word  = the original word
     * index = the index of the next character we need to match
     *
     * A record is useful here because it provides an immutable,
     * concise data carrier.
     */
    private record WaitingWord(String word, int index) {}

    public int numMatchingSubseq(String s, String[] words) {

        /*
         * 26 buckets, one for each lowercase English character.
         *
         * buckets[c] contains all words currently waiting
         * for character c.
         */
        Queue<WaitingWord>[] buckets = new ArrayDeque[26];

        for (int i = 0; i < 26; i++) {
            buckets[i] = new ArrayDeque<>();
        }

        /*
         * Initially, every word waits for its first character.
         *
         * Example:
         * "abc" goes into bucket 'a'.
         * "dog" goes into bucket 'd'.
         */
        for (String word : words) {

            int character = word.charAt(0) - 'a';

            buckets[character].offer(
                    new WaitingWord(word, 0)
            );
        }

        int answer = 0;

        /*
         * Traverse s exactly once.
         */
        for (char ch : s.toCharArray()) {

            int bucketIndex = ch - 'a';

            /*
             * Process only words waiting for the current character.
             *
             * Important:
             * Capture the initial bucket size.
             *
             * Why?
             * Some words may move back into the same bucket.
             * We must NOT process those words again using
             * the same occurrence of ch.
             */
            int size = buckets[bucketIndex].size();

            for (int i = 0; i < size; i++) {

                WaitingWord current =
                        buckets[bucketIndex].poll();

                String word = current.word();

                /*
                 * We have successfully matched the character
                 * that this word was waiting for.
                 */
                int nextIndex = current.index() + 1;

                /*
                 * If all characters are matched,
                 * this word is a subsequence.
                 */
                if (nextIndex == word.length()) {

                    answer++;

                } else {

                    /*
                     * Otherwise, move the word into the bucket
                     * corresponding to its next required character.
                     */
                    int nextCharacter =
                            word.charAt(nextIndex) - 'a';

                    buckets[nextCharacter].offer(
                            new WaitingWord(word, nextIndex)
                    );
                }
            }
        }

        return answer;
    }
}

/*
 * TIME COMPLEXITY:
 *
 * Let:
 * N = s.length()
 * W = words.length
 * L = maximum word length
 *
 * 1. Creating 26 buckets:
 *    O(26) = O(1)
 *
 * 2. Initializing the waiting words:
 *    O(W)
 *
 * 3. Traversing s:
 *    O(N)
 *
 * 4. Processing waiting words:
 *    Every character of every word is processed at most once.
 *    Therefore, total processing is O(W * L).
 *
 * Overall:
 * O(N + W * L)
 *
 * SPACE COMPLEXITY:
 *
 * O(W)
 *
 * Every word has exactly one active WaitingWord entry
 * in exactly one bucket at any point in time.
 *
 * Total number of waiting entries is at most W.
 *
 * The record objects and 26 queue references require O(W + 26).
 *
 * Therefore:
 * O(W)
 *
 * WHY IS THIS OPTIMAL?
 *
 * We must at least inspect the input words and the main string
 * in the general case.
 *
 * This solution processes the main string once and advances
 * each word only as its required characters appear.
 *
 * No repeated scanning of s.
 * No binary searches.
 *
 * This is the standard optimal solution for this problem.
 */











































import java.util.*;

// Represents a word waiting for its next character to appear.
record WaitingWord(String word, int index) {

    // Returns the character this word is currently waiting for.
    char nextCharacter() {
        return word.charAt(index);
    }

    // Returns a new state after matching the current character.
    WaitingWord advance() {
        return new WaitingWord(word, index + 1);
    }

    // Checks whether every character of this word is matched.
    boolean isComplete() {
        return index == word.length();
    }
}


// Responsible for matching multiple words against a string.
class SubsequenceMatcher {

    private static final int ALPHABET_SIZE = 26;

    /*
     * Each index represents a character:
     *
     * buckets.get(0)  -> words waiting for 'a'
     * buckets.get(1)  -> words waiting for 'b'
     * ...
     * buckets.get(25) -> words waiting for 'z'
     */
    private final List<Queue<WaitingWord>> buckets;

    private int matchedCount;

    SubsequenceMatcher(String[] words) {

        buckets = new ArrayList<>(ALPHABET_SIZE);

        initializeBuckets();
        addWords(words);
    }

    // Creates 26 empty queues.
    private void initializeBuckets() {

        for (int i = 0; i < ALPHABET_SIZE; i++) {
            buckets.add(new ArrayDeque<>());
        }
    }

    // Initially, every word waits for its first character.
    private void addWords(String[] words) {

        for (String word : words) {

            WaitingWord waitingWord = new WaitingWord(word, 0);

            addToBucket(waitingWord);
        }
    }

    // Adds a word to the bucket corresponding to its next character.
    private void addToBucket(WaitingWord waitingWord) {

        int bucketIndex = waitingWord.nextCharacter() - 'a';

        buckets.get(bucketIndex).offer(waitingWord);
    }

    // Processes the main string and returns the matching word count.
    int countMatchingSubsequences(String s) {

        for (char ch : s.toCharArray()) {
            processCharacter(ch);
        }

        return matchedCount;
    }

    // Advances all words waiting for the given character.
    private void processCharacter(char ch) {

        Queue<WaitingWord> bucket = buckets.get(ch - 'a');

        /*
         * Capture the initial queue size.
         *
         * Some words might move back into this same bucket.
         * We must not process those words again using
         * the same occurrence of the character.
         */
        int size = bucket.size();

        for (int i = 0; i < size; i++) {

            WaitingWord current = bucket.poll();

            advanceWord(current);
        }
    }

    // Advances one word by one character.
    private void advanceWord(WaitingWord current) {

        WaitingWord next = current.advance();

        // All characters matched: this is a valid subsequence.
        if (next.isComplete()) {

            matchedCount++;

        } else {

            // Wait for the next required character.
            addToBucket(next);
        }
    }
}


// LeetCode entry point.
class Solution {

    public int numMatchingSubseq(String s, String[] words) {

        SubsequenceMatcher matcher = new SubsequenceMatcher(words);

        return matcher.countMatchingSubsequences(s);
    }
}
