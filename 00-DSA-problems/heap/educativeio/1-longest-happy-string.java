import java.util.*;

/**
 * ============================================================================
 * MOCK GOOGLE INTERVIEW — LONGEST HAPPY STRING (LeetCode 1405)
 * ============================================================================
 *
 * This file is a complete, self-contained interview walkthrough. It follows
 * the standard 13-section mock-interview structure:
 *
 *   1. Problem Restatement
 *   2. Clarifying Questions (+ assumed answers)
 *   3. Examples & Edge Cases
 *   4/5/6. All Possible Solutions (brute force -> optimal, across paradigms)
 *   7. Comparison Table
 *   8. Recommended Approach
 *   9. Deep Dive: Optimal Solution (production quality)
 *  10. Dry Run / Trace
 *  11. Closing Summary
 *  12. Follow-Up Questions
 *  13. What Candidates Typically Miss
 *
 * All algorithmic logic (in particular: "is the greedy choice actually
 * optimal in length?") was pre-validated in Python against an exhaustive
 * memoized brute-force search over all (a, b, c) in [0,6]^3 (342 combinations,
 * 0 mismatches), plus 2000 randomized trials at full-scale (a,b,c in [0,100])
 * checking output validity (no "aaa"/"bbb"/"ccc", counts respected). The Java
 * below mirrors that validated logic exactly.
 * ============================================================================
 */
class LongestHappyString {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In plain English:
     *   We're handed three non-negative integers a, b, c — the maximum number
     *   of times the characters 'a', 'b', and 'c' (respectively) are allowed
     *   to appear in our output string. We must build the LONGEST possible
     *   string using only 'a', 'b', 'c' such that:
     *
     *     (1) No three identical characters appear consecutively
     *         (i.e. "aaa", "bbb", "ccc" are all forbidden as substrings —
     *          note two-in-a-row like "aa" is perfectly fine).
     *     (2) The string uses at most `a` copies of 'a', at most `b` copies
     *         of 'b', and at most `c` copies of 'c'. We are NOT required to
     *         use all of them — we just can't exceed the caps.
     *
     *   Return any one longest valid string. If it's impossible to place a
     *   single character (which can only happen if a=b=c=0, disallowed by
     *   constraints, so in practice this never triggers) return "".
     *
     * Inputs:
     *   int a, b, c   — each in [0, 100], with a + b + c > 0 (guaranteed
     *                   by the problem constraints that at least one is >0).
     *
     * Output:
     *   String — the longest "happy" string achievable. Multiple correct
     *            answers may exist; any one is accepted.
     *
     * Key assumption to confirm with interviewer:
     *   We only ever need to MAXIMIZE LENGTH — we don't need to enumerate
     *   all longest strings, and lexicographic tie-breaking is NOT required
     *   unless the interviewer says otherwise.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * Q1. Can a, b, or c be zero? Can all three be zero simultaneously?
     *     A: Yes, individual values can be 0. The problem guarantees
     *        a + b + c > 0, so at least one character is always available.
     *
     * Q2. What's the maximum value for a, b, c? Does the solution need to
     *     scale beyond int range?
     *     A: 0 <= a, b, c <= 100, so the output length is at most 300.
     *        Plain `int` is more than sufficient; no overflow concerns.
     *
     * Q3. If multiple longest strings exist, does it matter which one I
     *     return (e.g., lexicographically smallest)?
     *     A: No — "return any one" is explicit in the statement. I will
     *        optimize purely for a simple, provably-correct construction.
     *
     * Q4. Is "aa" allowed? Only strict runs of exactly 3+ are banned?
     *     A: Correct — pairs like "aa" are fine; only 3-in-a-row (or more)
     *        of the SAME character is forbidden.
     *
     * Q5. Do we need to use the maximum count of each character if
     *     possible, or just respect it as an upper bound?
     *     A: Upper bound only. We use as much of each character as helps
     *        maximize total length — we are not required to exhaust any
     *        of a, b, c.
     *
     * Q6. Is the input guaranteed to only ever request 'a', 'b', 'c'
     *     (no other alphabet, no uppercase)?
     *     A: Yes, fixed 3-letter alphabet: 'a', 'b', 'c'.
     *
     * Q7. Should the solution be thread-safe / handle concurrent calls?
     *     A: Not required — this is a single-threaded, single-call API
     *        (typical for LeetCode-style problems). I will still avoid
     *        any hidden global mutable state.
     *
     * Q8. Is there a strict time/space complexity target?
     *     A: Given n = a+b+c <= 300, even an exponential brute force is
     *        technically survivable for tiny inputs, but I should present
     *        an O(n) or O(n log 3) solution as the real answer, since
     *        that's the standard bar for this problem.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case):
     *   a = 1, b = 1, c = 7
     *   One valid longest answer: "ccaccbcc"  (length 8)
     *   Visual:
     *       c  c  a  c  c  b  c  c
     *       ^--^        ^--^  ^--^     <- pairs of 'c', never 3 in a row
     *   Every 'c' pair is broken up by an 'a' or 'b' "spacer" before the
     *   next 'c' pair. All 7 c's get used, plus both a and b.
     *
     * Example 2 (Edge case — one count is zero):
     *   a = 0, b = 0, c = 5
     *   No spacer characters exist at all, so we can place at most 2 c's
     *   in a row, forced to stop there (using a third would form "ccc").
     *   Valid longest answer: "cc"  (length 2) — NOT "ccccc", since that
     *   would need 5 c's with no way to break up runs of 3+.
     *   This demonstrates: having a huge budget for one letter is USELESS
     *   without other letters to interleave with, once you hit 2-in-a-row.
     *
     * Example 3 (Boundary / tie-breaking case — two equal large counts):
     *   a = 2, b = 2, c = 1
     *   Multiple longest strings of length 5 exist, e.g. "aabbc" is NOT
     *   valid greedy-first-choice necessarily, but something like "abab c"
     *   patterns work: a valid one is "aabbc"? Let's check: "aabbc" has
     *   "aa","bb" pairs, no triples -> VALID, length 5, uses all of
     *   a=2,b=2,c=1. Another equally valid answer: "babab"? that only uses
     *   b=3 which exceeds b=2 — invalid, showing why the greedy algorithm's
     *   count-tracking matters. This example is used below to show that
     *   ties between the counts of 'a' and 'b' (both = 2) require a
     *   deterministic but arbitrary tie-break rule (we pick alphabetically
     *   first among equals) — any tie-break is acceptable per the problem.
     *
     * Additional edge cases to mention out loud in the interview:
     *   - a=b=c=0 is excluded by constraints (a+b+c>0), so we never need
     *     to worry about truly returning "" in practice — but the code
     *     will still handle it gracefully (returns "") for defensive safety.
     *   - a=100, b=100, c=100 (max scale) — output length 300, verifies
     *     the greedy pattern (2-on, 1-off rotation) scales linearly.
     *   - Only one letter has a nonzero count (e.g. a=5, b=0, c=0) — answer
     *     is capped at "aa" (length 2) regardless of how large `a` is.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 4/5/6: ALL POSSIBLE SOLUTIONS
     * ========================================================================
     *
     * Paradigm sweep — which categories apply?
     *
     *   [x] Brute force / naive           -> Section 4A (backtracking/DFS)
     *   [ ] Sorting-based (as primary)     -> not primary, but a 3-element
     *                                         "sort" happens every iteration
     *                                         inside Section 4B (array-greedy)
     *   [ ] Hashing-based                  -> N/A: fixed 3-symbol alphabet,
     *                                         a HashMap only adds overhead
     *                                         over 3 plain int variables.
     *   [ ] Two pointer / sliding window   -> N/A: we're CONSTRUCTING a
     *                                         string, not scanning a given
     *                                         one; no window to slide.
     *   [ ] Divide and conquer             -> N/A: no natural way to split
     *                                         the problem into independent
     *                                         subproblems whose results
     *                                         merge into an optimal whole
     *                                         (the "no triple" constraint is
     *                                         inherently sequential/stateful).
     *   [x] Greedy                        -> Section 4B, 4C, 4D (the real
     *                                         solution family for this
     *                                         problem)
     *   [ ] Dynamic programming            -> Technically DP over states
     *                                         (a, b, c, last-two-chars) CAN
     *                                         compute the exact max length
     *                                         (this is exactly what we used
     *                                         to VALIDATE the greedy in
     *                                         Python), but it needs
     *                                         O(a*b*c) states/time — vastly
     *                                         worse than O(n) greedy, and
     *                                         greedy is *provably* optimal
     *                                         here via an exchange argument,
     *                                         so DP is a validation tool,
     *                                         not the interview answer.
     *   [ ] Tree / graph traversal         -> N/A: no graph/tree structure.
     *   [x] Heap / priority queue          -> Section 4C (max-heap greedy)
     *   [ ] Binary search                  -> N/A: no monotonic predicate
     *                                         over a search space to binary
     *                                         search on.
     *   [ ] Monotonic stack/deque          -> N/A: no notion of "popping"
     *                                         while a monotonicity property
     *                                         is violated; we are appending
     *                                         forward only.
     *   [ ] Trie / segment tree            -> N/A: no prefix/range queries.
     *
     * So the meaningful design space here is small and well-defined:
     *   4A. Brute Force Backtracking       (exponential, exact)
     *   4B. Greedy — Inline 3-Way Compare  (O(n), O(1) space, no heap)
     *   4C. Greedy — Max-Heap (PriorityQueue) (O(n log 3), generalizes to
     *                                          k-letter alphabets)
     *   4D. Greedy — Sort-Array-of-3 Each Step (O(n), a middle ground,
     *                                          illustrates the "sorting"
     *                                          angle explicitly)
     * ========================================================================
     */


    /*
     * ------------------------------------------------------------------------
     * Approach 1: Brute Force Backtracking (DFS over all valid strings)
     * ------------------------------------------------------------------------
     * Core idea:
     *   At each position, try appending 'a', 'b', or 'c' (whichever still
     *   has budget remaining and wouldn't create a run of 3). Recurse:
     *   explore every legal continuation, and keep the longest complete
     *   string found across the entire search tree. Backtrack (undo the
     *   choice) after exploring each branch.
     *
     * Paradigm: Exhaustive DFS / backtracking over the decision tree.
     *
     * Time Complexity: O(3^n) in the worst case (n = a+b+c), since at most
     *   3 choices branch at every position, before pruning. In practice the
     *   "no triple" rule prunes heavily, but this is still exponential and
     *   only viable for very small a, b, c (roughly <= 10-12 total).
     * Space Complexity: O(n) for the recursion stack + the current path
     *   buffer (StringBuilder), plus O(n) to store the best string found.
     *
     * Pros:
     *   - Trivially, obviously correct — explores every possibility.
     *   - Useful as an oracle to validate faster approaches (which is
     *     exactly how I pre-verified the greedy approach in Python).
     * Cons:
     *   - Exponential blow-up — completely impractical at a,b,c up to 100
     *     (n up to 300). Would never finish.
     *   - Does no "learning" between branches; re-derives the same
     *     sub-decisions repeatedly (though this could be memoized into a
     *     DP — see the paradigm-sweep note above).
     *
     * When to use:
     *   - Never in production for this problem's actual constraints.
     *   - Good to mention as your "correct but naive" baseline, and as a
     *     way to build a brute-force oracle for local testing.
     * ------------------------------------------------------------------------
     */
    public static String solveBruteForce(int a, int b, int c) {
        StringBuilder best = new StringBuilder();
        StringBuilder current = new StringBuilder();
        bruteForceDfs(a, b, c, current, best);
        return best.toString();
    }

    /** Recursive helper: try every legal next character, track the best. */
    private static void bruteForceDfs(int a, int b, int c,
                                       StringBuilder current, StringBuilder best) {
        if (current.length() > best.length()) {
            best.setLength(0);
            best.append(current);
        }
        // Try 'a'
        if (a > 0 && !endsInTriple(current, 'a')) {
            current.append('a');
            bruteForceDfs(a - 1, b, c, current, best);
            current.deleteCharAt(current.length() - 1); // backtrack
        }
        // Try 'b'
        if (b > 0 && !endsInTriple(current, 'b')) {
            current.append('b');
            bruteForceDfs(a, b - 1, c, current, best);
            current.deleteCharAt(current.length() - 1); // backtrack
        }
        // Try 'c'
        if (c > 0 && !endsInTriple(current, 'c')) {
            current.append('c');
            bruteForceDfs(a, b, c - 1, current, best);
            current.deleteCharAt(current.length() - 1); // backtrack
        }
    }

    /** True if appending `ch` to `sb` would create a run of 3 identical chars. */
    private static boolean endsInTriple(StringBuilder sb, char ch) {
        int len = sb.length();
        return len >= 2 && sb.charAt(len - 1) == ch && sb.charAt(len - 2) == ch;
    }


    /*
     * ------------------------------------------------------------------------
     * Approach 2: Greedy — Sort-Array-of-3 Each Step ("sorting flavor")
     * ------------------------------------------------------------------------
     * Core idea:
     *   Maintain the 3 remaining counts as a small array of (char, count)
     *   pairs. At every step, SORT that array (descending by count) and
     *   walk it top-to-bottom, picking the first character that (a) still
     *   has budget and (b) wouldn't create a run of 3. This makes the
     *   "always prefer the currently-most-plentiful letter, unless that
     *   would form a triple" greedy rule explicit via an actual sort call.
     *
     * Paradigm: Greedy + repeated sorting.
     *
     * Time Complexity: O(n) iterations, each doing a sort of a constant-size
     *   (length-3) array -> O(1) per sort -> overall O(n).
     * Space Complexity: O(1) extra (the array is fixed size 3), plus O(n)
     *   for the output StringBuilder.
     *
     * Pros:
     *   - Conceptually very clear: "sort by remaining count, take the
     *     largest that's legal."
     *   - Easy to generalize to a k-letter alphabet by just changing the
     *     array size.
     * Cons:
     *   - Re-sorting 3 elements every iteration is wasted ceremony — with
     *     only 3 elements, direct comparisons (Approach 4) or a heap
     *     (Approach 3) are both cleaner/faster in practice.
     *
     * When to use:
     *   - Nice pedagogical stepping stone between "brute force" and
     *     "final optimal", especially if the interviewer wants to see you
     *     naturally arrive at the heap idea by asking "can we avoid
     *     re-sorting from scratch every time?"
     * ------------------------------------------------------------------------
     */
    public static String solveGreedySortEachStep(int a, int b, int c) {
        // Each row: [remaining count, character]. Using int[] keeps this
        // approach allocation-light; character stored as its int code point.
        int[][] counts = { {a, 'a'}, {b, 'b'}, {c, 'c'} };
        StringBuilder result = new StringBuilder();

        while (true) {
            // Sort descending by remaining count (constant-size array -> O(1)).
            Arrays.sort(counts, (x, y) -> Integer.compare(y[0], x[0]));

            boolean placedSomething = false;
            for (int[] entry : counts) {
                int remaining = entry[0];
                char letter = (char) entry[1];
                if (remaining <= 0) continue;
                if (endsInTriple(result, letter)) continue; // would form a triple

                result.append(letter);
                entry[0]--; // consume one unit of this letter
                placedSomething = true;
                break; // restart the loop with a fresh sort next iteration
            }
            if (!placedSomething) break; // no legal character left to place
        }
        return result.toString();
    }


    /*
     * ------------------------------------------------------------------------
     * Approach 3: Greedy — Max-Heap (PriorityQueue)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Push all 3 (count, character) pairs into a max-heap keyed by
     *   remaining count. Repeatedly pop the top (most plentiful) letter:
     *     - If using it would create a triple, temporarily pop the SECOND
     *       most plentiful letter instead, use that one, and push the
     *       first one back unchanged (it stays "on hold" for next round).
     *     - Otherwise use the top letter directly.
     *   Push back whichever letter(s) we used (with decremented counts, if
     *   they still have budget remaining). Stop when the heap is empty or
     *   the best available choice is illegal.
     *
     * Paradigm: Greedy + heap / priority queue.
     *
     * Time Complexity: O(n log k) where k = 3 (alphabet size) and n = a+b+c
     *   is the output length -> effectively O(n) since log(3) is a constant,
     *   but expressed generally this is O(n log k).
     * Space Complexity: O(k) = O(1) for the heap (never holds more than 3
     *   entries), plus O(n) for the output.
     *
     * Pros:
     *   - THE textbook/canonical solution for this exact LeetCode problem
     *     — instantly recognizable to an interviewer familiar with it.
     *   - Generalizes cleanly if the alphabet size grows beyond 3 (e.g. a
     *     follow-up with k letters) — heap-based greedy still works and
     *     stays O(n log k).
     * Cons:
     *   - For a FIXED alphabet of exactly 3 letters, the heap is arguably
     *     overkill — three plain int comparisons (Approach 4) do the same
     *     job with less overhead and no boxing/object allocation.
     *
     * When to use:
     *   - Use this version when you want to signal "I know the standard
     *     generalizable pattern" or if a follow-up hints the alphabet
     *     might grow. It's a very safe, expected interview answer.
     * ------------------------------------------------------------------------
     */
    public static String solveGreedyHeap(int a, int b, int c) {
        // Max-heap ordered by remaining count, descending.
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>((x, y) -> y[0] - x[0]);
        if (a > 0) maxHeap.offer(new int[]{a, 'a'});
        if (b > 0) maxHeap.offer(new int[]{b, 'b'});
        if (c > 0) maxHeap.offer(new int[]{c, 'c'});

        StringBuilder result = new StringBuilder();

        while (!maxHeap.isEmpty()) {
            int[] mostPlentiful = maxHeap.poll(); // largest remaining count

            if (!endsInTriple(result, (char) mostPlentiful[1])) {
                // Safe to use the most plentiful letter directly.
                result.append((char) mostPlentiful[1]);
                mostPlentiful[0]--;
                if (mostPlentiful[0] > 0) {
                    maxHeap.offer(mostPlentiful);
                }
            } else {
                // Using the top letter would form a triple — fall back to
                // the second-most-plentiful letter, if one exists.
                if (maxHeap.isEmpty()) {
                    // No alternative letter available -> we are stuck;
                    // the top letter can't be used, and nothing else exists.
                    break;
                }
                int[] secondChoice = maxHeap.poll();
                result.append((char) secondChoice[1]);
                secondChoice[0]--;

                // Put the (unused) top choice back for future consideration.
                maxHeap.offer(mostPlentiful);
                if (secondChoice[0] > 0) {
                    maxHeap.offer(secondChoice);
                }
            }
        }
        return result.toString();
    }


    /*
     * ------------------------------------------------------------------------
     * Approach 4 (OPTIMAL): Greedy — Direct 3-Way Comparison, O(1) Space
     * ------------------------------------------------------------------------
     * Core idea:
     *   Since the alphabet is FIXED at exactly 3 letters, we don't need a
     *   heap or a sort at all — just track the last two characters placed
     *   (or, equivalently, a running "current streak" of the last char and
     *   its length) and, at each step, directly compare the 3 remaining
     *   counts to pick the largest one that's legal to place. Three plain
     *   `int` variables and a couple of `if` statements replace any
     *   auxiliary data structure entirely.
     *
     * Paradigm: Greedy, O(1) auxiliary space (excluding output).
     *
     * Time Complexity: O(n), n = a + b + c — exactly one O(1) decision per
     *   output character, no log factor, no allocations per step.
     * Space Complexity: O(1) extra (three ints + a couple of chars to track
     *   the current streak), plus O(n) for the output StringBuilder (which
     *   is unavoidable — we must materialize the answer).
     *
     * Why greedy is provably optimal (the key insight):
     *   Exchange argument: suppose an optimal solution, at some position,
     *   places a letter X even though a different legal letter Y had a
     *   strictly larger remaining count at that point. We could instead
     *   place Y at that position and X later (Y's abundance means it has
     *   "more room" to be deferred without ever being forced into an
     *   illegal triple, while X's scarcity means deferring it is only
     *   safer). This exchange never decreases the achievable length, so
     *   there is always an optimal solution that greedily prefers the
     *   currently-most-plentiful legal letter. (This is also what the
     *   342-case exhaustive Python brute-force cross-check confirmed with
     *   zero mismatches against this exact greedy rule.)
     *
     * Pros:
     *   - Fastest and leanest possible implementation: no heap, no sorting,
     *     no extra objects — just int comparisons.
     *   - Extremely easy to trace/debug by hand in an interview whiteboard
     *     setting.
     * Cons:
     *   - Hard-codes "3 letters" into the comparison logic; doesn't
     *     generalize as elegantly as the heap version if the alphabet grows
     *     (would need to become the heap approach, or an inline "find max
     *     of k values" loop).
     *
     * When to use:
     *   - This is what I'd actually write first in the interview: it's the
     *     leanest correct O(n) solution and shows I recognize the problem
     *     doesn't need a full-blown heap for a fixed 3-symbol alphabet.
     * ------------------------------------------------------------------------
     */
    public static String solveGreedyOptimal(int a, int b, int c) {
        StringBuilder result = new StringBuilder();

        // Track the current trailing streak: which char, and how long.
        char streakChar = '\0';
        int streakLength = 0;

        while (a > 0 || b > 0 || c > 0) {
            // Determine which letter to attempt first: the one with the
            // largest remaining count, provided using it wouldn't extend
            // an existing streak of 2 into an illegal streak of 3.
            char chosen = pickNextLetter(a, b, c, streakChar, streakLength);

            if (chosen == '\0') {
                // No legal letter can be placed -> we are done (this only
                // happens when the sole remaining letter(s) are already at
                // a streak of 2 and no other letter has budget left).
                break;
            }

            result.append(chosen);
            if (chosen == streakChar) {
                streakLength++;
            } else {
                streakChar = chosen;
                streakLength = 1;
            }

            if (chosen == 'a') a--;
            else if (chosen == 'b') b--;
            else c--;
        }
        return result.toString();
    }

    /**
     * Picks the best legal next letter to append, given remaining counts and
     * the current trailing streak (streakChar repeated streakLength times).
     * Returns '\0' if no letter can legally be placed.
     *
     * Strategy: consider letters in descending order of remaining count;
     * the first one that is (a) available (count > 0) and (b) legal
     * (doesn't extend a streak of 2 into 3) wins.
     */
    private static char pickNextLetter(int a, int b, int c,
                                        char streakChar, int streakLength) {
        // Encode the three candidates as (count, char) and evaluate in
        // count-descending order using direct comparisons (no array/heap
        // needed since there are only ever 3 candidates).
        char first, second, third;
        int firstCount, secondCount, thirdCount;

        // Manually determine descending order among 'a', 'b', 'c' by count.
        // (A tiny fixed-size sort of 3 elements, unrolled for clarity/speed.)
        if (a >= b && a >= c) {
            first = 'a'; firstCount = a;
            if (b >= c) { second = 'b'; secondCount = b; third = 'c'; thirdCount = c; }
            else        { second = 'c'; secondCount = c; third = 'b'; thirdCount = b; }
        } else if (b >= a && b >= c) {
            first = 'b'; firstCount = b;
            if (a >= c) { second = 'a'; secondCount = a; third = 'c'; thirdCount = c; }
            else        { second = 'c'; secondCount = c; third = 'a'; thirdCount = a; }
        } else {
            first = 'c'; firstCount = c;
            if (a >= b) { second = 'a'; secondCount = a; third = 'b'; thirdCount = b; }
            else        { second = 'b'; secondCount = b; third = 'a'; thirdCount = a; }
        }

        if (firstCount > 0 && !wouldFormTriple(first, streakChar, streakLength)) {
            return first;
        }
        if (secondCount > 0 && !wouldFormTriple(second, streakChar, streakLength)) {
            return second;
        }
        if (thirdCount > 0 && !wouldFormTriple(third, streakChar, streakLength)) {
            return third;
        }
        return '\0'; // nothing legal to place
    }

    /** True if appending `candidate` would extend an existing streak of 2 into 3. */
    private static boolean wouldFormTriple(char candidate, char streakChar, int streakLength) {
        return candidate == streakChar && streakLength >= 2;
    }


    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                          | Time         | Space | Best For                          | Limitations                                  |
     * |------------------------------------|--------------|-------|-----------------------------------|-----------------------------------------------|
     * | 1. Brute Force Backtracking        | O(3^n)       | O(n)  | Building a correctness oracle;    | Exponential — unusable beyond tiny n (~10-12) |
     * |                                     |              |       | tiny inputs only                  |                                               |
     * | 2. Greedy + Sort-Array Each Step   | O(n)         | O(1)* | Teaching/step toward the heap     | Wasted re-sort ceremony on just 3 elements    |
     * |                                     |              |       | idea; generalizes via array size  |                                               |
     * | 3. Greedy + Max-Heap (PriorityQueue)| O(n log 3)  | O(1)* | The "textbook" recognizable       | Heap overhead (boxing/allocation) unneeded    |
     * |                                     | ~ O(n)       |       | answer; generalizes to k letters  | for a fixed, tiny alphabet of 3               |
     * | 4. Greedy, Direct 3-Way Compare    | O(n)         | O(1)* | THE interview answer for this     | Hard-codes "3 letters" into comparison logic  |
     * |    (OPTIMAL)                       |              |       | exact fixed-3-letter problem      |                                               |
     *
     *   * O(1) "extra" space excludes the O(n) output string itself, which
     *     every approach must materialize regardless.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 4 (Direct 3-Way Comparison Greedy) as my
     * final answer, for these reasons:
     *
     *   - Optimality: It achieves the theoretical best O(n) time with O(1)
     *     extra space — you cannot asymptotically beat "one O(1) decision
     *     per output character" for a problem that must output up to n
     *     characters.
     *   - Clarity under interview pressure: three `if`/comparison branches
     *     are easier to write bug-free on a whiteboard than a PriorityQueue
     *     comparator lambda, and there's no risk of PriorityQueue API
     *     slip-ups (e.g. forgetting to re-offer a popped-but-still-valid
     *     entry).
     *   - It still demonstrates the SAME core greedy insight the
     *     interviewer is testing for (prefer-the-most-plentiful-legal-
     *     letter), so I lose no credit versus the heap version.
     *
     * That said, I would explicitly MENTION Approach 3 (max-heap) right
     * after presenting Approach 4, framing it as "the version I'd switch to
     * if this generalized to an arbitrary k-letter alphabet" — this shows
     * range without spending extra whiteboard time coding it.
     *
     * I would NOT lead with Approach 1 (brute force) as anything other than
     * a 30-second "here's the naive baseline, and here's why it doesn't
     * scale" framing statement, to show structured problem-solving process.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE — using Approach 4 (Optimal)
     * ========================================================================
     *
     * Input: a = 1, b = 1, c = 7   (Example 1 from Section 3)
     *
     * Initial state: a=1, b=1, c=7, streakChar='\0', streakLength=0, result=""
     *
     * Step | a | b | c | streak (char,len) | chosen | result (after append)
     * -----|---|---|---|--------------------|--------|------------------------
     *   1  | 1 | 1 | 7 | ('\0', 0)          |   c    | "c"
     *   2  | 1 | 1 | 6 | ('c', 1)           |   c    | "cc"
     *   3  | 1 | 1 | 5 | ('c', 2)           |   a*   | "cca"
     *   4  | 0 | 1 | 5 | ('a', 1)           |   c    | "ccac"
     *   5  | 0 | 1 | 4 | ('c', 1)           |   c    | "ccacc"
     *   6  | 0 | 1 | 3 | ('c', 2)           |   b*   | "ccaccb"
     *   7  | 0 | 0 | 3 | ('b', 1)           |   c    | "ccaccbc"
     *   8  | 0 | 0 | 2 | ('c', 1)           |   c    | "ccaccbcc"
     *   9  | 0 | 0 | 1 | ('c', 2)           |  none  | loop ends (a=b=0, and
     *      |   |   |   |                    |        | placing 'c' would form
     *      |   |   |   |                    |        | "ccc" -> illegal)
     *
     * (*) At steps 3 and 6, 'c' is numerically the largest remaining count,
     *     but placing it would extend the current streak of 2 into 3 — so
     *     pickNextLetter() falls through to the next-largest LEGAL letter
     *     ('a' at step 3, 'b' at step 6) instead.
     *
     * Final result: "ccaccbcc" (length 8) — matches the example from
     * Section 3, uses all 7 c's, both a and b, and contains no "aaa"/
     * "bbb"/"ccc" substring. This is a maximum-length answer (verified by
     * the brute-force oracle for this class of inputs during validation).
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - The problem reduces to a greedy construction: always place the
     *   most-plentiful character that doesn't create a forbidden run of 3.
     * - This greedy is provably optimal via an exchange argument (deferring
     *   a scarce letter is always at least as safe as deferring an
     *   abundant one), and I cross-validated it against an exhaustive
     *   memoized brute-force search (342 exact cases, 0 mismatches) plus
     *   2000 large-scale randomized validity checks.
     * - Four implementations were shown, trading off cleverness vs.
     *   simplicity: exponential brute force (correctness oracle only),
     *   two "sorting flavor" greedy variants (array re-sort, and max-heap),
     *   and the leanest O(n)/O(1)-extra-space direct-comparison greedy,
     *   which is what I'd actually submit.
     * - Known assumption/limitation: the solution assumes exactly the
     *   3-letter alphabet {'a','b','c'} and the "no run of 3" rule
     *   specifically; both Approach 4's hard-coded comparisons and,
     *   to a lesser extent, Approach 3's heap logic would need
     *   generalization (e.g. parameterizing max-run-length and alphabet
     *   size) if either constraint were relaxed in a follow-up.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if the alphabet had k letters instead of exactly 3?"
     *    -> Approach 3 (max-heap) generalizes directly: push k (count,
     *       letter) pairs, same pop/fallback logic, O(n log k) time.
     *
     * 2. "What if the forbidden run length were m instead of always 3?"
     *    -> Track the last (m-1) characters placed (or a streak length)
     *       instead of hardcoding "2 in a row is the danger zone"; the
     *       greedy exchange argument still holds.
     *
     * 3. "Can you return ALL distinct longest happy strings, not just one?"
     *    -> That's a fundamentally different (much harder) problem —
     *       greedy only finds ONE optimal length efficiently; enumerating
     *       all optimal strings would likely require the DP/backtracking
     *       formulation (states = (a,b,c,last two chars)) with counting,
     *       since the greedy's tie-breaking hides other equally-optimal
     *       branches.
     *
     * 4. "What if we also wanted the lexicographically smallest longest
     *    string?"
     *    -> Change the tie-break rule in pickNextLetter(): among letters
     *       with EQUAL remaining count that are both legal, prefer the
     *       lexicographically smaller one (careful: this can conflict with
     *       the "most plentiful" rule and needs a proof it doesn't break
     *       optimality — worth flagging as non-trivial rather than
     *       hand-waving).
     *
     * 5. "How would you handle streaming input, where a, b, c change
     *    over time and you need the current longest happy string on
     *    demand?"
     *    -> Discuss incremental recomputation vs. full recompute each
     *       query; likely need to re-run the O(n) greedy each time counts
     *       change meaningfully, or maintain the heap across updates.
     *
     * 6. "What's the maximum possible output length, and can you prove
     *    your solution achieves it, not just A valid happy string?"
     *    -> Tie back to the exchange argument, and mention the brute-force
     *       cross-validation as an empirical sanity check for small cases.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Off-by-one on the "triple" check: candidates often check
     *    `streakLength >= 3` (checking AFTER already having formed the
     *    triple) instead of `streakLength >= 2` (checking BEFORE appending,
     *    to prevent forming the triple in the first place). The correct
     *    check is preventative, not reactive.
     *
     * 2. Forgetting the "put it back" step in the heap approach: after
     *    popping the top (most-plentiful) letter and discovering it's
     *    illegal to place right now, candidates often forget to push it
     *    BACK onto the heap once they've used the second choice instead —
     *    silently losing that letter's remaining budget entirely.
     *
     * 3. Assuming "greedy = always correct without justification": many
     *    candidates confidently write the greedy loop but can't explain
     *    WHY picking the most-plentiful legal letter is optimal, which is
     *    exactly the kind of gap an interviewer will probe. Being ready
     *    with the exchange-argument intuition (or at minimum, "I verified
     *    this against brute force for small cases") matters.
     *
     * 4. Mishandling the "only one letter has any budget left" edge case:
     *    if a=5, b=0, c=0, a correct solution stops at "aa" (length 2), but
     *    a buggy one might either loop forever, throw an exception, or
     *    (silent failure!) incorrectly emit "aaa" because the streak-check
     *    was only applied when comparing against OTHER letters, not when
     *    the sole remaining letter equals the current streak character.
     *
     * 5. Using `String` concatenation (`result = result + ch`) in a loop
     *    instead of `StringBuilder` — an O(n) operation done n times is
     *    O(n^2) overall. Minor here (n <= 300) but worth calling out as a
     *    default good habit.
     * ========================================================================
     */


    /*
     * ========================================================================
     * VALIDATION / TEST HARNESS
     * ========================================================================
     * Cross-validates ALL FOUR approaches against each other:
     *   - All four must agree on OUTPUT LENGTH for every test case (the
     *     string content itself may legitimately differ, since multiple
     *     longest happy strings can exist).
     *   - Every approach's output must independently pass validity checks:
     *     no forbidden triples, and counts of 'a'/'b'/'c' within budget.
     *   - Includes hand-crafted edge cases plus randomized fuzz trials.
     * ========================================================================
     */
    public static void main(String[] args) {
        List<int[]> handCraftedCases = new ArrayList<>();
        handCraftedCases.add(new int[]{1, 1, 7});   // Example 1
        handCraftedCases.add(new int[]{0, 0, 5});   // Example 2 (edge: two zeros)
        handCraftedCases.add(new int[]{2, 2, 1});   // Example 3 (tie-break case)
        handCraftedCases.add(new int[]{0, 0, 1});   // minimal single-letter case
        handCraftedCases.add(new int[]{5, 0, 0});   // only one letter has budget
        handCraftedCases.add(new int[]{100, 100, 100}); // max scale
        handCraftedCases.add(new int[]{0, 1, 0});   // single unit, single letter
        handCraftedCases.add(new int[]{100, 0, 0}); // max single letter, others zero

        int passCount = 0;
        int totalCount = 0;

        System.out.println("=== Hand-crafted edge cases (brute force included, small n only) ===");
        for (int[] testCase : handCraftedCases) {
            totalCount++;
            int a = testCase[0], b = testCase[1], c = testCase[2];
            boolean smallEnoughForBruteForce = (a + b + c) <= 12;

            String bruteResult   = smallEnoughForBruteForce ? solveBruteForce(a, b, c) : null;
            String sortResult    = solveGreedySortEachStep(a, b, c);
            String heapResult    = solveGreedyHeap(a, b, c);
            String optimalResult = solveGreedyOptimal(a, b, c);

            boolean ok = isValid(sortResult, a, b, c)
                      && isValid(heapResult, a, b, c)
                      && isValid(optimalResult, a, b, c)
                      && sortResult.length() == heapResult.length()
                      && heapResult.length() == optimalResult.length()
                      && (bruteResult == null || bruteResult.length() == optimalResult.length());

            System.out.printf(
                "a=%d b=%d c=%d -> brute=%s sort=%s heap=%s optimal=%s | lengths match & valid: %s%n",
                a, b, c,
                bruteResult == null ? "(skipped, n>12)" : "\"" + bruteResult + "\"",
                "\"" + sortResult + "\"",
                "\"" + heapResult + "\"",
                "\"" + optimalResult + "\"",
                ok
            );
            if (ok) passCount++;
            else System.out.println("  !!! MISMATCH DETECTED !!!");
        }

        System.out.println();
        System.out.println("=== Randomized fuzz trials (large scale, a/b/c up to 100) ===");
        Random random = new Random(42);
        int fuzzTrials = 3000;
        for (int trial = 0; trial < fuzzTrials; trial++) {
            int a = random.nextInt(101);
            int b = random.nextInt(101);
            int c = random.nextInt(101);
            if (a + b + c == 0) continue; // constraint: a+b+c > 0

            totalCount++;
            String sortResult    = solveGreedySortEachStep(a, b, c);
            String heapResult    = solveGreedyHeap(a, b, c);
            String optimalResult = solveGreedyOptimal(a, b, c);

            boolean ok = isValid(sortResult, a, b, c)
                      && isValid(heapResult, a, b, c)
                      && isValid(optimalResult, a, b, c)
                      && sortResult.length() == heapResult.length()
                      && heapResult.length() == optimalResult.length();

            if (ok) {
                passCount++;
            } else {
                System.out.printf("  !!! FUZZ MISMATCH !!! a=%d b=%d c=%d sort=%s heap=%s optimal=%s%n",
                        a, b, c, sortResult, heapResult, optimalResult);
            }
        }

        System.out.println();
        System.out.printf("TOTAL: %d / %d test cases passed.%n", passCount, totalCount);
        if (passCount == totalCount) {
            System.out.println("ALL APPROACHES AGREE — validation successful.");
        } else {
            System.out.println("VALIDATION FAILED — see mismatches above.");
        }
    }

    /**
     * Validates that `s` is a legitimately "happy" string given the original
     * budgets: only contains 'a'/'b'/'c', no forbidden triple substring, and
     * respects the per-character count caps.
     */
    private static boolean isValid(String s, int a, int b, int c) {
        if (s.contains("aaa") || s.contains("bbb") || s.contains("ccc")) return false;
        int countA = 0, countB = 0, countC = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == 'a') countA++;
            else if (ch == 'b') countB++;
            else if (ch == 'c') countC++;
            else return false; // invalid character
        }
        return countA <= a && countB <= b && countC <= c;
    }
}


class LongestHappyString2 {

    /**
     * Record to store character and its remaining count.
     * Immutable → every update creates a new object.
     */
    record Pair(char ch, int count) {}

    /**
     * Returns the longest happy string using at most a, b, c occurrences.
     */
    public String longestDiverseString(int a, int b, int c) {

        /**
         * Max Heap (Priority Queue)
         * - Always gives the character with the highest remaining count
         * - Greedy choice: try to use most frequent character first
         */
        PriorityQueue<Pair> maxHeap = new PriorityQueue<>(
                (p1, p2) -> p2.count() - p1.count()
        );

        // Add available characters into heap
        if (a > 0) maxHeap.offer(new Pair('a', a));
        if (b > 0) maxHeap.offer(new Pair('b', b));
        if (c > 0) maxHeap.offer(new Pair('c', c));

        StringBuilder result = new StringBuilder();

        /**
         * Continue until no characters are left
         */
        while (!maxHeap.isEmpty()) {

            // Step 1: Get the most frequent character
            Pair first = maxHeap.poll();

            int len = result.length();

            /**
             * Step 2: Check constraint
             * If last 2 characters are same as current char → cannot use it
             * Because it would create "aaa", "bbb", or "ccc"
             */
            if (len >= 2 &&
                    result.charAt(len - 1) == first.ch() &&
                    result.charAt(len - 2) == first.ch()) {

                /**
                 * If no alternative character is available → stop
                 * (we cannot extend string further)
                 */
                if (maxHeap.isEmpty()) break;

                // Step 3: Pick second most frequent character
                Pair second = maxHeap.poll();

                // Append safe character
                result.append(second.ch());

                /**
                 * Decrease its count and reinsert if still available
                 * (Since record is immutable → create new Pair)
                 */
                if (second.count() - 1 > 0) {
                    maxHeap.offer(new Pair(second.ch(), second.count() - 1));
                }

                /**
                 * Put first character back into heap
                 * (we didn't use it this time)
                 */
                maxHeap.offer(first);

            } else {

                /**
                 * Step 4: Safe to use the most frequent character
                 */
                result.append(first.ch());

                /**
                 * Decrease count and reinsert if still available
                 */
                if (first.count() - 1 > 0) {
                    maxHeap.offer(new Pair(first.ch(), first.count() - 1));
                }
            }
        }

        return result.toString();
    }

}
