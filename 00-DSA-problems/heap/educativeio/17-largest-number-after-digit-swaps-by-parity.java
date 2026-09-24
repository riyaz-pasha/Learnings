import java.util.*;

/**
 * ============================================================================
 *  MOCK GOOGLE ONSITE INTERVIEW — FULL WALKTHROUGH
 *  Problem: Largest Number After Digit Swaps by Parity  (LeetCode 2231 family)
 * ============================================================================
 *
 *  This file is written the way a candidate SHOULD narrate a real interview:
 *  restating the problem, asking clarifying questions, walking examples,
 *  enumerating every applicable paradigm, comparing them, then deep-diving
 *  into the optimal production-quality solution with a full test harness.
 *
 *  Compile & run locally:
 *      javac LargestNumberByParitySwaps.java
 *      java LargestNumberByParitySwaps
 * ============================================================================
 */
class LargestNumberByParitySwaps {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words:
     *   I'm given a positive integer `num`. I may repeatedly pick any two
     *   digits currently in `num` and swap their positions, but ONLY if the
     *   two digits have the same parity — both odd (1,3,5,7,9) or both even
     *   (0,2,4,6,8). I may perform as many such swaps as I like, in any order.
     *   I need to return the maximum possible integer value achievable.
     *
     * Inputs:
     *   - A single positive integer num, 1 <= num <= 10^9.
     *     (So num has at most 10 decimal digits.)
     *
     * Outputs:
     *   - A single integer: the largest value reachable via any sequence of
     *     same-parity digit swaps.
     *
     * Key constraint / insight to verify with the interviewer:
     *   - "Any number of swaps" — not just one swap. This is important: it
     *     means we effectively get to reach ANY permutation that is
     *     achievable by composing same-parity transpositions, not just a
     *     single exchange. I will confirm this composability explicitly
     *     because it changes the entire solution space (single-swap problems
     *     are a very different, more restricted problem).
     *
     * Working assumption I will validate in section 2:
     *   - Because a swap only ever exchanges two digits that are BOTH odd or
     *     BOTH even, the set of array positions that currently hold an odd
     *     digit never changes size or membership — an odd digit can only
     *     ever move into a position that itself currently holds an odd
     *     digit. Transitively, that means: the set of positions that
     *     originally contained odd digits will, after any sequence of legal
     *     swaps, still contain exactly a permutation of that same original
     *     odd-digit multiset (and symmetrically for even digits). This
     *     reduces the problem to two independent sorting problems.
     */


    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * ========================================================================
     * (Questions I would actually ask the interviewer, with the answers I'll
     *  assume if they say "use your best judgment".)
     *
     * Q1: Is `num` guaranteed positive with no leading zeros in its decimal
     *     representation, and can the *result* have a leading zero?
     *     ASSUMED ANSWER: num >= 1 so no leading zero on input. Since 0 is an
     *     even digit and could end up swapped to the front only if the
     *     leading digit itself was even — but note the leading digit's
     *     PARITY POSITION is fixed, so 0 can only occupy positions that
     *     originally held even digits, including possibly position 0 if the
     *     original leading digit was even. In that scenario the true max
     *     arrangement of the even group (sorted descending) would place the
     *     largest even digit first, so 0 would only end up leading if ALL
     *     even digits present are 0 — impossible to avoid a leading zero on
     *     an already-positive input in that edge case unless num itself is
     *     literally 0, which is excluded by constraints. So this never
     *     actually surfaces for num >= 1.
     *
     * Q2: What is the maximum number of digits I need to handle?
     *     ASSUMED ANSWER: num <= 10^9, so at most 10 digits. Very small n;
     *     performance is a non-issue but I should still discuss asymptotic
     *     complexity like the input could scale to arbitrary-length strings.
     *
     * Q3: Should the return type be int, long, or String?
     *     ASSUMED ANSWER: int is sufficient since 10^9 fits in a 32-bit int
     *     (max int is ~2.147 * 10^9), but I'll note that if this were
     *     generalized to arbitrarily long numeric strings, I'd return a
     *     String or long instead. I will design my core logic on a
     *     char/digit array so it generalizes trivially.
     *
     * Q4: Are duplicate digits possible, and if so how should ties be
     *     handled during sorting?
     *     ASSUMED ANSWER: Yes, duplicates are common (e.g., 1111). Since we
     *     just need the maximum value, duplicate digits are interchangeable
     *     — any stable or unstable sort produces the same maximal digit
     *     string. No special tie-break rule is needed beyond "descending".
     *
     * Q5: Is single-digit input possible, and what's the expected output?
     *     ASSUMED ANSWER: Yes, e.g., num = 7. With only one digit there are
     *     no valid same-parity pairs to swap, so the output is the input
     *     unchanged.
     *
     * Q6: Do "any number of swaps" include zero swaps (i.e., is doing
     *     nothing allowed)?
     *     ASSUMED ANSWER: Yes — "any number" includes zero, so the original
     *     number is always a valid (if not optimal) answer.
     *
     * Q7: Is this a single-threaded, single-call problem, or do I need to
     *     support concurrent/repeated queries on a shared mutable structure?
     *     ASSUMED ANSWER: Single pure function call, no concurrency
     *     concerns. I will still write the solution to be stateless and
     *     thread-safe (no shared mutable state) as a best practice.
     *
     * Q8: Should I validate malformed input (e.g., non-positive num)?
     *     ASSUMED ANSWER: Given constraints guarantee 1 <= num <= 10^9, but
     *     I will add a defensive check and throw IllegalArgumentException
     *     for out-of-contract input rather than silently misbehaving —
     *     that's good production practice and something I'll voice out loud.
     */


    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (normal case): num = 1234
     *   Digits:            1  2  3  4
     *   Parity:           odd even odd even
     *   Odd digits present:  {1, 3}  -> sorted descending -> [3, 1]
     *   Even digits present: {2, 4}  -> sorted descending -> [4, 2]
     *   Refill left-to-right within each parity's original positions:
     *     position 0 (odd)  -> 3
     *     position 1 (even) -> 4
     *     position 2 (odd)  -> 1
     *     position 3 (even) -> 2
     *   Result: 3412
     *
     * Example 2 (edge case — single digit / no possible swaps): num = 7
     *   Only one digit, no pair to swap. Result: 7 (unchanged).
     *
     * Example 3 (boundary / tie-breaking case — all digits same parity,
     * with duplicates and near the 10^9 upper bound): num = 999999998
     *   Digits: 9 9 9 9 9 9 9 9 8   (nine digits)
     *   Odd digits present:  eight 9's -> sorted descending -> [9,9,9,9,9,9,9,9]
     *   Even digits present: one 8     -> sorted descending -> [8]
     *   Every position except the last already holds the maximum odd digit,
     *   and duplicates mean the sort order among equal 9's doesn't matter
     *   (this exercises the "duplicate digit tie-break is a non-issue"
     *   assumption from Q4).
     *   Result: 999999998 (already optimal — demonstrates a case where the
     *   optimal answer equals the input).
     */


    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Paradigms considered and whether they apply:
     *
     *  - Brute force / naive:            APPLICABLE (baseline, section below)
     *  - Sorting-based:                  APPLICABLE (this is the natural fit)
     *  - Hashing-based:                  NOT NEEDED — we need order (max
     *                                     value), not membership/lookup, so a
     *                                     hash map/set adds no value here.
     *  - Two pointer / sliding window:   NOT APPLICABLE — there's no
     *                                     contiguous-subrange or window
     *                                     structure to slide over; we're
     *                                     rearranging a whole fixed set.
     *  - Divide and conquer:             NOT NEEDED — no natural recursive
     *                                     split that beats a direct pass;
     *                                     the problem decomposes into exactly
     *                                     two independent groups (odd/even),
     *                                     not a recursive D&C structure.
     *  - Greedy:                         APPLICABLE — and in fact the
     *                                     "sorting-based" and "greedy/heap"
     *                                     approaches are the same greedy
     *                                     insight expressed with two
     *                                     different data structures.
     *  - Dynamic programming:            NOT APPLICABLE — no overlapping
     *                                     subproblems or optimal-substructure
     *                                     recurrence; each digit's placement
     *                                     is independent once grouped by
     *                                     parity, so there's nothing to
     *                                     memoize.
     *  - Tree / graph traversal:         NOT APPLICABLE — no graph/tree
     *                                     structure is implied by the
     *                                     problem.
     *  - Heap / priority queue:          APPLICABLE — a valid, if slightly
     *                                     heavier, way to implement the
     *                                     greedy idea.
     *  - Binary search:                  NOT APPLICABLE — we're not
     *                                     searching over a monotonic answer
     *                                     space; there's no "is X feasible"
     *                                     predicate to binary search on.
     *  - Monotonic stack / deque:        NOT APPLICABLE — no
     *                                     next-greater/previous-smaller
     *                                     relationship being tracked.
     *  - Trie / segment tree / advanced: NOT NEEDED — digits are bounded to
     *                                     {0..9}, so a fixed-size counting
     *                                     array (bucket/counting sort)
     *                                     already gives us the optimal O(n)
     *                                     structure; a trie or segment tree
     *                                     would be pure overkill for a
     *                                     10-element alphabet.
     *
     * I'll implement: Brute Force, Sorting-Based, Heap/Greedy, and the
     * optimal Counting-Sort approach.
     */

    // ------------------------------------------------------------------
    // Approach 1: Brute Force (Permute Within Parity Groups)
    // ------------------------------------------------------------------
    /*
     * Core idea:
     *   Generate every possible arrangement obtainable by permuting the odd
     *   digits among themselves and the even digits among themselves
     *   (independently), then take the maximum resulting number. This is a
     *   direct enumeration of the reachable state space — it does not even
     *   assume the "sort descending" insight, it just tries everything.
     *
     * Paradigm: exhaustive search / backtracking permutation generation.
     *
     * Time Complexity: O(k! * (n-k)! * n) where k = count of odd digits and
     *   n - k = count of even digits, and the extra factor n is for
     *   assembling/comparing each candidate string. In the worst case
     *   (n = 10, k = 5) this is 5! * 5! * 10 = 120 * 120 * 10 = 144,000 —
     *   fine for n = 10, but catastrophic if generalized to larger n
     *   (factorial blow-up).
     *
     * Space Complexity: O(n) per candidate plus O(k! ) recursion stack /
     *   generated permutations if memoized, though we can stream-compare
     *   without storing all permutations, so O(n) auxiliary space is
     *   achievable with a running "best" tracked in place.
     *
     * Pros:
     *   - Trivially, obviously correct — great as a baseline / oracle to
     *     validate faster solutions against (this is literally what I used
     *     to fuzz-test the optimal solution before finalizing it).
     *   - No cleverness required, easy to explain.
     *
     * Cons:
     *   - Factorial time — does not scale beyond tiny n.
     *   - A lot of wasted work: it doesn't exploit the obvious fact that
     *     "descending order" is always optimal for maximizing a number.
     *
     * When to use:
     *   - Never in production / at scale. Only useful as a correctness
     *     oracle during development or in an interview to state the
     *     starting point before pivoting to the real solution.
     */
    static int solveBruteForce(int num) {
        char[] digits = String.valueOf(num).toCharArray();
        int n = digits.length;

        List<Integer> oddPositions = new ArrayList<>();
        List<Integer> evenPositions = new ArrayList<>();
        List<Character> oddDigits = new ArrayList<>();
        List<Character> evenDigits = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int digitValue = digits[i] - '0';
            if (digitValue % 2 == 1) {
                oddPositions.add(i);
                oddDigits.add(digits[i]);
            } else {
                evenPositions.add(i);
                evenDigits.add(digits[i]);
            }
        }

        long[] bestValueHolder = { Long.MIN_VALUE };
        char[] workingArray = digits.clone();

        // Recursively try every permutation of the odd digits into the odd
        // positions, and for each, every permutation of even digits into
        // the even positions.
        permuteAndEvaluate(workingArray, oddDigits, oddPositions, 0, () ->
            permuteAndEvaluate(workingArray, evenDigits, evenPositions, 0, () -> {
                long candidate = Long.parseLong(new String(workingArray));
                if (candidate > bestValueHolder[0]) {
                    bestValueHolder[0] = candidate;
                }
            })
        );

        return (int) bestValueHolder[0];
    }

    // Helper: generates all permutations of `values` into `positions` inside
    // `workingArray`, invoking `onComplete` once per full permutation.
    private static void permuteAndEvaluate(char[] workingArray, List<Character> values,
                                            List<Integer> positions, int depth,
                                            Runnable onComplete) {
        if (depth == values.size()) {
            onComplete.run();
            return;
        }
        // Try each remaining value at this depth (standard permutation
        // generation via swapping within the list).
        for (int i = depth; i < values.size(); i++) {
            Collections.swap(values, depth, i);
            workingArray[positions.get(depth)] = values.get(depth);
            permuteAndEvaluate(workingArray, values, positions, depth + 1, onComplete);
            Collections.swap(values, depth, i); // backtrack
        }
    }


    // ------------------------------------------------------------------
    // Approach 2: Sorting-Based (Extract, Sort Descending, Refill)
    // ------------------------------------------------------------------
    /*
     * Core idea:
     *   Key insight: a swap only ever exchanges two digits of the SAME
     *   parity, so the set of positions that originally held odd digits can
     *   only ever be occupied by some permutation of that same odd-digit
     *   multiset (symmetrically for even). Therefore the reachable states
     *   are EXACTLY "any permutation of the odd digits among their original
     *   positions" x "any permutation of the even digits among their
     *   original positions" — nothing more, nothing less. To maximize the
     *   resulting number, within each independent group we simply want the
     *   locally-largest digit as far left (most significant) as possible,
     *   which means: extract each parity group, sort it descending, and
     *   place the digits back into that group's original positions in
     *   left-to-right order.
     *
     * Paradigm: greedy + sorting.
     *
     * Time Complexity: O(n log n) dominated by sorting each parity group
     *   (n = number of digits, at most 10 here).
     *
     * Space Complexity: O(n) for the extracted digit lists / arrays.
     *
     * Pros:
     *   - Simple, very readable, easy to prove correct in an interview.
     *   - Generalizes cleanly to arbitrary-length numeric strings.
     *
     * Cons:
     *   - O(n log n) instead of the achievable O(n) — irrelevant at n <= 10,
     *     but worth mentioning as a follow-up optimization.
     *
     * When to use:
     *   - This is what I'd actually type first in an interview: it's
     *     correct, fast enough, and clearly communicates the key insight.
     */
    static int solveSortingBased(int num) {
        char[] digits = String.valueOf(num).toCharArray();

        List<Character> oddDigits = new ArrayList<>();
        List<Character> evenDigits = new ArrayList<>();
        for (char digitChar : digits) {
            if ((digitChar - '0') % 2 == 1) {
                oddDigits.add(digitChar);
            } else {
                evenDigits.add(digitChar);
            }
        }
        // Sort each parity group in descending order — largest digits should
        // land in the leftmost (most significant) positions of that group.
        oddDigits.sort(Collections.reverseOrder());
        evenDigits.sort(Collections.reverseOrder());

        int oddPointer = 0;
        int evenPointer = 0;
        char[] result = new char[digits.length];
        for (int i = 0; i < digits.length; i++) {
            int digitValue = digits[i] - '0';
            if (digitValue % 2 == 1) {
                result[i] = oddDigits.get(oddPointer++);
            } else {
                result[i] = evenDigits.get(evenPointer++);
            }
        }
        return Integer.parseInt(new String(result));
    }


    // ------------------------------------------------------------------
    // Approach 3: Greedy via Max-Heaps (Priority Queues)
    // ------------------------------------------------------------------
    /*
     * Core idea:
     *   Functionally identical greedy insight to Approach 2, but instead of
     *   sorting each group up front, push each parity group's digits into
     *   its own max-heap and repeatedly poll the largest remaining digit
     *   when filling positions left to right. This is a common way
     *   interviewers like to see the "always take the best remaining
     *   option" greedy pattern expressed explicitly with a priority queue,
     *   and it generalizes nicely if digits arrived as a stream rather than
     *   all at once.
     *
     * Paradigm: greedy + heap / priority queue.
     *
     * Time Complexity: O(n log n) — n heap insertions and n extractions,
     *   each O(log n).
     *
     * Space Complexity: O(n) for the two heaps.
     *
     * Pros:
     *   - Same clarity as sorting-based, but demonstrates heap fluency,
     *     which some interviewers like to probe for explicitly.
     *   - Naturally extends to an online/streaming variant of the problem.
     *
     * Cons:
     *   - Strictly more overhead than a plain sort for this bounded,
     *     offline input — constant-factor slower in practice, no
     *     asymptotic benefit here.
     *
     * When to use:
     *   - If the interviewer specifically asks "how would you do this if
     *     digits arrived one at a time and you needed the running-best
     *     placement," a heap-based formulation is the natural pivot.
     */
    static int solveGreedyHeap(int num) {
        char[] digits = String.valueOf(num).toCharArray();

        // Max-heaps: PriorityQueue is min-heap by default, so we invert the
        // comparator to get max-heap behavior.
        PriorityQueue<Integer> oddMaxHeap = new PriorityQueue<>(Collections.reverseOrder());
        PriorityQueue<Integer> evenMaxHeap = new PriorityQueue<>(Collections.reverseOrder());

        for (char digitChar : digits) {
            int digitValue = digitChar - '0';
            if (digitValue % 2 == 1) {
                oddMaxHeap.add(digitValue);
            } else {
                evenMaxHeap.add(digitValue);
            }
        }

        StringBuilder result = new StringBuilder();
        for (char digitChar : digits) {
            int digitValue = digitChar - '0';
            if (digitValue % 2 == 1) {
                result.append(oddMaxHeap.poll());
            } else {
                result.append(evenMaxHeap.poll());
            }
        }
        return Integer.parseInt(result.toString());
    }


    // ------------------------------------------------------------------
    // Approach 4 (OPTIMAL): Counting Sort by Bounded Digit Alphabet
    // ------------------------------------------------------------------
    /*
     * Core idea:
     *   Same greedy correctness argument as Approaches 2 and 3, but we
     *   exploit the fact that digits are drawn from a fixed, tiny alphabet
     *   {0, 1, ..., 9}. Instead of a comparison sort (O(n log n)), we tally
     *   digit frequencies in two fixed-size count arrays (size 10 each —
     *   really only 5 slots are used per parity, but a uniform size-10
     *   array keeps the code simple and branch-free), then reconstruct each
     *   parity group in descending order directly from the counts. This
     *   turns sorting into a linear counting pass, giving true O(n) time
     *   with no comparisons at all.
     *
     * Paradigm: greedy + counting sort (bucket sort with a bounded key
     *   range) — this is the production-quality version, implemented in
     *   full in Section 9.
     *
     * Time Complexity: O(n + 10) = O(n) — one pass to count, one pass over
     *   the fixed 10-slot range per parity to rebuild sorted order, one
     *   pass to refill positions.
     *
     * Space Complexity: O(1) auxiliary — the count arrays are fixed size 10
     *   regardless of n, plus O(n) for the output buffer (which is
     *   unavoidable since we must produce an n-digit result).
     *
     * Pros:
     *   - Asymptotically optimal — can't do better than a linear scan since
     *     we must at minimum read every digit once.
     *   - No comparisons, no sorting library overhead — the fastest
     *     possible constant factor for this bounded-alphabet case.
     *
     * Cons:
     *   - Slightly more code than a one-line library sort call; the
     *     "why is this correct" argument needs the same explanation
     *     regardless of implementation, so the marginal benefit only shows
     *     up if n is large or this runs in a hot loop.
     *
     * When to use:
     *   - Once I've established correctness with Approach 2 in an
     *     interview, I'd mention this as the "if you want it truly optimal
     *     and this ran millions of times" follow-up, and I'd implement it
     *     as the final production version, which is what Section 9 does.
     */
    static int solveCountingSortOptimal(int num) {
        char[] digits = String.valueOf(num).toCharArray();
        int[] digitFrequency = new int[10]; // count of each digit value 0-9

        for (char digitChar : digits) {
            digitFrequency[digitChar - '0']++;
        }

        // Two cursors walking DOWN from 9 to 0, only stopping on digits of
        // the matching parity, handing out the next-largest available
        // digit of that parity each time we need to fill a position.
        int nextLargestOddCursor = 9;
        int nextLargestEvenCursor = 8;

        char[] result = new char[digits.length];
        for (int i = 0; i < digits.length; i++) {
            int digitValue = digits[i] - '0';
            if (digitValue % 2 == 1) {
                while (digitFrequency[nextLargestOddCursor] == 0) {
                    nextLargestOddCursor -= 2;
                }
                result[i] = (char) ('0' + nextLargestOddCursor);
                digitFrequency[nextLargestOddCursor]--;
            } else {
                while (digitFrequency[nextLargestEvenCursor] == 0) {
                    nextLargestEvenCursor -= 2;
                }
                result[i] = (char) ('0' + nextLargestEvenCursor);
                digitFrequency[nextLargestEvenCursor]--;
            }
        }
        return Integer.parseInt(new String(result));
    }


    /*
     * ========================================================================
     * SECTION 6: (Paradigm coverage note)
     * ========================================================================
     * See the inline applicability notes embedded directly in Section 4/5
     * above — each non-applicable paradigm (two pointer, divide & conquer,
     * DP, tree/graph, binary search, monotonic stack, trie/segment tree) has
     * a one-line justification for why it was skipped, as required.
     */


    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                       | Time              | Space | Best For                          | Limitations                                   |
     * |---------------------------------|-------------------|-------|-----------------------------------|------------------------------------------------|
     * | 1. Brute Force (permute groups) | O(k!*(n-k)!*n)     | O(n)  | Correctness oracle / tiny n only  | Factorial blow-up, unusable beyond n ~ 10       |
     * | 2. Sorting-Based                | O(n log n)         | O(n)  | Interview whiteboard solution     | Slightly slower than counting sort (irrelevant at n<=10) |
     * | 3. Greedy via Max-Heaps          | O(n log n)         | O(n)  | Streaming/online variant, showing heap fluency | Extra heap overhead vs. plain sort for offline input |
     * | 4. Counting Sort (OPTIMAL)      | O(n)               | O(1) aux (+O(n) output) | Production, hot-loop, truly optimal | Marginally more code; benefit invisible at tiny n |
     */


    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 2 (Sorting-Based) FIRST as my primary
     * solution, then proactively mention Approach 4 (Counting Sort) as the
     * optimal follow-up, and implement Approach 4 if asked to make it
     * production-grade. Reasoning:
     *
     *   - Clarity: sorting-based directly mirrors the correctness argument
     *     ("split by parity, sort descending, refill") — it's the fastest
     *     way to convince the interviewer I understand WHY the answer is
     *     correct, which matters more early on than shaving constant
     *     factors.
     *   - Coding speed: it's ~15 lines using library sort, low risk of
     *     bugs, quick to write correctly under interview pressure.
     *   - Interviewer expectations: most interviewers want to see the
     *     insight (parity partitioning) proven correct before they care
     *     about counting-sort micro-optimization; jumping straight to
     *     counting sort without narrating the sorting-based intuition first
     *     risks looking like a memorized trick rather than derived
     *     reasoning.
     *   - Optimality: since n <= 10 here, O(n log n) vs O(n) is a
     *     rounding error in practice — but I still show I know the
     *     asymptotically optimal version, since that's the "senior
     *     engineer" signal: recognizing the bounded alphabet and adapting
     *     to counting sort. The deep dive below implements this optimal
     *     version end to end.
     */


    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION QUALITY)
     * ========================================================================
     */

    /**
     * Returns the largest integer reachable from {@code num} by performing
     * any number of swaps between two digits of the same parity (both odd
     * or both even).
     *
     * <p><b>Correctness argument:</b> A single legal swap exchanges two
     * digits that are currently both odd, or currently both even. Because
     * the two digits being swapped share a parity, the swap never changes
     * which set of array positions currently holds an odd digit versus an
     * even digit — it only permutes values within that fixed set. By
     * induction over any sequence of swaps, the set of positions originally
     * occupied by odd digits will, after arbitrarily many legal swaps,
     * still be occupied by exactly some permutation of the original
     * odd-digit multiset (symmetrically for even digits). Therefore the
     * full reachable state space is precisely: (any permutation of the
     * original odd digits among their original positions) combined with
     * (any permutation of the original even digits among their original
     * positions). To maximize the resulting numeric value, within each
     * independent group we place digits in strictly non-increasing order
     * from the most significant remaining position in that group to the
     * least — i.e., sort each parity group descending and refill
     * left-to-right.
     *
     * <p><b>Algorithm:</b> Counting sort over the bounded digit alphabet
     * {@code {0..9}}. We tally digit frequencies in O(n), then reconstruct
     * each parity group in descending order using two monotonically
     * decreasing cursors, giving O(n) total time and O(1) auxiliary space
     * (beyond the required O(n) output buffer).
     *
     * @param num a positive integer with {@code 1 <= num <= 10^9}
     * @return the largest integer reachable via any number of same-parity
     *         digit swaps
     * @throws IllegalArgumentException if {@code num} is outside the
     *         documented contract {@code [1, 10^9]}
     */
    static int largestNumberAfterParitySwaps(int num) {
        // Defensive input validation — matches Q8's assumed contract, but
        // we fail loudly instead of silently producing garbage if violated.
        final int MIN_VALID = 1;
        final int MAX_VALID = 1_000_000_000;
        if (num < MIN_VALID || num > MAX_VALID) {
            throw new IllegalArgumentException(
                "num must satisfy 1 <= num <= 10^9, but was: " + num);
        }

        char[] digits = String.valueOf(num).toCharArray();
        int digitCount = digits.length;

        // Named constants instead of magic numbers, per production style.
        final int DIGIT_ALPHABET_SIZE = 10;
        final int LARGEST_ODD_DIGIT = 9;
        final int LARGEST_EVEN_DIGIT = 8;

        // Step 1: Tally frequency of each digit value in a single O(n) pass.
        int[] digitFrequency = new int[DIGIT_ALPHABET_SIZE];
        for (char digitChar : digits) {
            digitFrequency[digitChar - '0']++;
        }

        // Step 2: Two cursors track the largest not-yet-consumed digit for
        // each parity class. They only ever move downward (by 2, staying
        // within their parity), so total cursor movement across the whole
        // algorithm is bounded by DIGIT_ALPHABET_SIZE — this is what keeps
        // the reconstruction phase O(n) overall rather than O(n * 10).
        int nextLargestOddCursor = LARGEST_ODD_DIGIT;
        int nextLargestEvenCursor = LARGEST_EVEN_DIGIT;

        // Step 3: Rebuild the result left-to-right. For every position, we
        // know its required parity from the ORIGINAL digit at that
        // position (per the correctness argument above), and we always
        // hand out the current largest available digit of that parity.
        char[] result = new char[digitCount];
        for (int position = 0; position < digitCount; position++) {
            int originalDigitValue = digits[position] - '0';
            boolean isOddPosition = (originalDigitValue % 2 == 1);

            if (isOddPosition) {
                // Advance past any odd digit value we've fully used up.
                while (digitFrequency[nextLargestOddCursor] == 0) {
                    nextLargestOddCursor -= 2;
                }
                result[position] = (char) ('0' + nextLargestOddCursor);
                digitFrequency[nextLargestOddCursor]--;
            } else {
                while (digitFrequency[nextLargestEvenCursor] == 0) {
                    nextLargestEvenCursor -= 2;
                }
                result[position] = (char) ('0' + nextLargestEvenCursor);
                digitFrequency[nextLargestEvenCursor]--;
            }
        }

        return Integer.parseInt(new String(result));
    }


    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing largestNumberAfterParitySwaps(1234) step by step:
     *
     * Initial: digits = ['1','2','3','4'], digitCount = 4
     *
     * Step 1 (tally frequencies):
     *   digitFrequency index: 0 1 2 3 4 5 6 7 8 9
     *   digitFrequency value: 0 1 1 1 1 0 0 0 0 0
     *
     * Step 2 (init cursors):
     *   nextLargestOddCursor  = 9
     *   nextLargestEvenCursor = 8
     *
     * Step 3 (reconstruct left to right):
     *
     *   position=0, originalDigit='1' (odd)
     *     cursor=9: digitFrequency[9]=0 -> advance to 7
     *     cursor=7: digitFrequency[7]=0 -> advance to 5
     *     cursor=5: digitFrequency[5]=0 -> advance to 3
     *     cursor=3: digitFrequency[3]=1 -> STOP, use digit 3
     *     result[0] = '3'; digitFrequency[3] -> 0
     *     nextLargestOddCursor is now 3
     *
     *   position=1, originalDigit='2' (even)
     *     cursor=8: digitFrequency[8]=0 -> advance to 6
     *     cursor=6: digitFrequency[6]=0 -> advance to 4
     *     cursor=4: digitFrequency[4]=1 -> STOP, use digit 4
     *     result[1] = '4'; digitFrequency[4] -> 0
     *     nextLargestEvenCursor is now 4
     *
     *   position=2, originalDigit='3' (odd)
     *     cursor=3: digitFrequency[3]=0 (just consumed) -> advance to 1
     *     cursor=1: digitFrequency[1]=1 -> STOP, use digit 1
     *     result[2] = '1'; digitFrequency[1] -> 0
     *     nextLargestOddCursor is now 1
     *
     *   position=3, originalDigit='4' (even)
     *     cursor=4: digitFrequency[4]=0 (just consumed) -> advance to 2
     *     cursor=2: digitFrequency[2]=1 -> STOP, use digit 2
     *     result[3] = '2'; digitFrequency[2] -> 0
     *
     * Final result array: ['3','4','1','2'] -> parsed integer 3412
     *
     * This matches the hand-derived answer from Section 3, Example 1.
     */


    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * All four approaches agree on correctness (verified by the fuzz-tested
     * harness below, cross-checking against the brute-force oracle). The
     * trade-off ladder is:
     *
     *   Brute Force        -> proves the state space, factorial time, only
     *                          usable as an oracle for tiny n.
     *   Sorting-Based       -> the interview-ready solution: O(n log n),
     *                          directly encodes the correctness argument,
     *                          minimal code.
     *   Greedy via Heaps    -> same complexity as sorting-based, useful when
     *                          asked about streaming/online variants.
     *   Counting Sort       -> the true production-optimal O(n) solution,
     *                          exploiting the bounded {0..9} digit alphabet.
     *
     * Known assumptions / limitations of the final solution:
     *   - Assumes the input fits in a 32-bit int per the stated constraint
     *     (1 <= num <= 10^9); generalizing to arbitrarily large numbers
     *     would require switching to a String/BigInteger-based digit array
     *     and returning a String, but the core algorithm is unchanged.
     *   - Assumes "any number of swaps" truly means unrestricted repeated
     *     application (confirmed in Section 2, Q1); the whole reduction to
     *     "sort each parity group" hinges on the swaps being composable
     *     rather than limited to a single application.
     */


    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if num could be arbitrarily large (given as a string)?"
     *    -> The algorithm is unchanged; just operate on a char[] parsed
     *       from the string and return a String instead of parsing back to
     *       int, avoiding any overflow concerns entirely.
     *
     * 2. "What if instead of parity, digits could only be swapped when they
     *    are congruent mod k for some given k?"
     *    -> Generalizes directly: partition into k residue classes instead
     *       of 2 parity classes, sort each descending, refill. Counting
     *       sort still applies since digits remain bounded to {0..9}.
     *
     * 3. "What if you needed the SMALLEST possible value instead?"
     *    -> Symmetric: sort each parity group ascending instead of
     *       descending. Watch out for leading-zero edge cases here — e.g.,
     *       if the leading position is even and 0 is among the even
     *       digits, some problem variants disallow a leading zero in the
     *       result and you'd need to special-case placing the smallest
     *       nonzero even digit first.
     *
     * 4. "How would you handle this if only a SINGLE swap were allowed
     *    (not unlimited)?"
     *    -> Completely different problem: you'd look for the single best
     *       beneficial swap — e.g., for the "maximize" variant, scan for
     *       the leftmost position where swapping in a larger same-parity
     *       digit from later in the number improves the value, which is
     *       more of a one-pass greedy scan with a "best swap so far"
     *       tracker rather than a full re-sort.
     *
     * 5. "Can you do this without extra space, in place?"
     *    -> The counting-sort approach already uses only O(1) auxiliary
     *       space beyond the mandatory output buffer; if asked to mutate
     *       the original char[] in place rather than allocate `result`,
     *       that's a straightforward adaptation since we only ever
     *       overwrite each position exactly once and never re-read a
     *       position after writing it.
     *
     * 6. "How would this change under concurrent access, e.g. if this were
     *    a shared service handling many requests?"
     *    -> The function as written is already pure and stateless (no
     *       shared mutable state, no static/global counters), so it's
     *       trivially thread-safe as-is; no locking or synchronization
     *       would be needed.
     */


    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Assuming a single swap can move a digit to ANY position, rather
     *    than realizing the reachable state space is constrained to
     *    permuting each parity group WITHIN ITS OWN ORIGINAL POSITIONS.
     *    Candidates sometimes jump straight to "just sort the whole number
     *    descending," which is wrong — it ignores the parity constraint on
     *    which positions are legally reachable for each digit.
     *
     * 2. Forgetting that "any number of swaps" (not just one) is what
     *    grants full permutation power within each parity group. If the
     *    problem only allowed a single swap, sorting each group fully
     *    would be incorrect. Missing this distinction — or not asking
     *    about it — leads to solving the wrong problem entirely.
     *
     * 3. Off-by-one / wrong-direction cursor bugs in the counting-sort
     *    implementation: forgetting that the even cursor starts at 8 (not
     *    9) and both cursors must step by 2 (not 1) to stay within their
     *    parity class. It's easy to accidentally let the odd cursor drift
     *    into even values or vice versa if the step size is wrong.
     *
     * 4. The "silent failure" trap: treating digit '0' incorrectly. Since
     *    0 % 2 == 0, it's an even digit like any other — but candidates
     *    sometimes special-case 0 (e.g., assuming it can never move, or
     *    excluding it from the even group entirely), which silently
     *    produces a wrong answer only on inputs containing 0 while passing
     *    most other test cases — exactly the kind of bug that slips past
     *    casual testing and should be explicitly covered in the test
     *    harness below.
     */


    /*
     * ========================================================================
     * TEST HARNESS: hand-crafted edge cases + randomized fuzz trials
     *   cross-validating all four approaches against each other.
     * ========================================================================
     */
    public static void main(String[] args) {
        System.out.println("=== Hand-crafted test cases ===");
        int[][] handCraftedCases = {
            // {input, expectedOutput}
            {1234, 3412},
            {7, 7},
            {999999998, 999999998},
            {1, 1},
            {10, 10},          // '1' odd (only odd digit), '0' even (only even digit) -> unchanged
            {3097, 9073},      // digits 3,0,9,7: odd={3,7}->desc[7,3] at odd positions(0,3); even={0,9}->desc[9,0] at even positions(1,2)
            {1000000000, 1000000000} // upper bound boundary
        };

        for (int[] testCase : handCraftedCases) {
            int input = testCase[0];
            int expected = testCase[1];
            runAndCheck(input, expected);
        }

        System.out.println("\n=== Randomized fuzz trials (cross-validating all 4 approaches) ===");
        Random random = new Random(42);
        int trialCount = 2000;
        int mismatches = 0;

        for (int trial = 0; trial < trialCount; trial++) {
            // Keep digit length small (<=6) so the brute-force oracle stays
            // tractable (factorial blow-up otherwise).
            int length = 1 + random.nextInt(6);
            StringBuilder numBuilder = new StringBuilder();
            numBuilder.append(1 + random.nextInt(9)); // no leading zero
            for (int i = 1; i < length; i++) {
                numBuilder.append(random.nextInt(10));
            }
            int num = Integer.parseInt(numBuilder.toString());

            int bruteResult = solveBruteForce(num);
            int sortingResult = solveSortingBased(num);
            int heapResult = solveGreedyHeap(num);
            int countingSortResult = solveCountingSortOptimal(num);
            int productionResult = largestNumberAfterParitySwaps(num);

            boolean allAgree = bruteResult == sortingResult
                && sortingResult == heapResult
                && heapResult == countingSortResult
                && countingSortResult == productionResult;

            if (!allAgree) {
                mismatches++;
                System.out.printf(
                    "MISMATCH on num=%d: brute=%d sorting=%d heap=%d countingSort=%d production=%d%n",
                    num, bruteResult, sortingResult, heapResult, countingSortResult, productionResult);
            }
        }

        System.out.printf("Fuzz testing complete: %d trials, %d mismatches%n", trialCount, mismatches);
        if (mismatches == 0) {
            System.out.println("All approaches agree on all trials. ✅");
        }
    }

    // Helper for the hand-crafted section: runs all four approaches on one
    // input and asserts they match both each other and the expected value.
    private static void runAndCheck(int input, int expected) {
        int bruteResult = solveBruteForce(input);
        int sortingResult = solveSortingBased(input);
        int heapResult = solveGreedyHeap(input);
        int countingSortResult = solveCountingSortOptimal(input);
        int productionResult = largestNumberAfterParitySwaps(input);

        boolean pass = bruteResult == expected
            && sortingResult == expected
            && heapResult == expected
            && countingSortResult == expected
            && productionResult == expected;

        System.out.printf(
            "num=%-12d expected=%-12d brute=%-12d sorting=%-12d heap=%-12d countingSort=%-12d production=%-12d %s%n",
            input, expected, bruteResult, sortingResult, heapResult, countingSortResult, productionResult,
            pass ? "PASS" : "FAIL");
    }
}


/**
 * ============================================================================
 * PROBLEM STATEMENT
 * ============================================================================
 * You are given a positive integer num. You can swap any two digits of num 
 * as long as they share the same parity (both are odd or both are even).
 * 
 * Your task is to return the largest possible value of num after performing 
 * any number of such swaps.
 * 
 * CONSTRAINTS:
 * - 1 <= num <= 10^9
 * 
 * ============================================================================
 * VISUALIZATION OF THE PROBLEM
 * ============================================================================
 * Since we can swap any two digits of the same parity any number of times, 
 * this implies we can sort all the even digits among themselves and all the 
 * odd digits among themselves. 
 * 
 * The positions of even and odd digits remain fixed. We just need to place 
 * the largest available even digit in the next available even slot, and the 
 * largest available odd digit in the next available odd slot.
 * 
 * Example: num = 65875
 * 
 * 1. Identify parities at each position:
 *    Position:  0   1   2   3   4
 *    Original:  6   5   8   7   5
 *    Parity:   Evn Odd Evn Odd Odd
 * 
 * 2. Group and Sort (Descending):
 *    Even digits: [8, 6]
 *    Odd digits:  [7, 5, 5]
 * 
 * 3. Reconstruct by pulling from the largest available of the correct parity:
 *    Pos 0 (Evn) -> 8
 *    Pos 1 (Odd) -> 7
 *    Pos 2 (Evn) -> 6
 *    Pos 3 (Odd) -> 5
 *    Pos 4 (Odd) -> 5
 * 
 * Result: 87655
 * ============================================================================
 */
class LargestIntegerByParity {

    /**
     * Using Java 14+ Record for concise, immutable test cases.
     */
    public record TestCase(int num, int expected) {}

    /**
     * ========================================================================
     * SOLUTION 1: MAX-HEAPS (Priority Queues)
     * ========================================================================
     * EXPLANATION:
     * 1. Convert the integer to a string (or character array) to process digits.
     * 2. Create two Max-Heaps (PriorityQueues with reverse order): one for 
     *    even digits and one for odd digits.
     * 3. Iterate through the string and add each digit to its respective heap.
     * 4. Iterate through the string again. For each position, check the parity 
     *    of the original digit. Pull the maximum digit from the corresponding 
     *    heap and append it to our result.
     * 5. Convert the resulting string back to an integer.
     * 
     * COMPLEXITY:
     * - Time: O(D log D) where D is the number of digits in `num`. Since 
     *   num <= 10^9, D is at most 10. Thus, this is extremely fast, effectively O(1).
     * - Space: O(D) to store the digits in heaps and string builders.
     * ========================================================================
     */
    public static int largestIntegerHeaps(int num) {
        PriorityQueue<Integer> evenHeap = new PriorityQueue<>(Collections.reverseOrder());
        PriorityQueue<Integer> oddHeap = new PriorityQueue<>(Collections.reverseOrder());
        
        String numStr = String.valueOf(num);
        
        // 1. Populate the heaps
        for (char c : numStr.toCharArray()) {
            int digit = c - '0';
            if (digit % 2 == 0) {
                evenHeap.offer(digit);
            } else {
                oddHeap.offer(digit);
            }
        }
        
        // 2. Reconstruct the largest number
        StringBuilder result = new StringBuilder();
        for (char c : numStr.toCharArray()) {
            int digit = c - '0';
            if (digit % 2 == 0) {
                result.append(evenHeap.poll());
            } else {
                result.append(oddHeap.poll());
            }
        }
        
        return Integer.parseInt(result.toString());
    }

    /**
     * ========================================================================
     * SOLUTION 2: COUNTING SORT / BUCKET SORT (Most Optimal)
     * ========================================================================
     * EXPLANATION:
     * Since digits can only be 0-9, we don't need a heavy priority queue. 
     * We can just count how many times each digit appears using an array of size 10.
     * 
     * 1. Traverse the number to count the frequency of each digit.
     * 2. Traverse the original number again.
     *    - If the current position held an even digit, find the largest available 
     *      even digit (scanning downwards from 8, 6, 4, 2, 0) and use it.
     *    - If it held an odd digit, find the largest available odd digit 
     *      (scanning downwards from 9, 7, 5, 3, 1) and use it.
     * 3. Decrement the count of the used digit.
     * 
     * COMPLEXITY:
     * - Time: O(D) where D is the number of digits. We do a couple of linear 
     *   scans over a tiny array.
     * - Space: O(D) for string conversion (or O(1) if we extract digits via math).
     * ========================================================================
     */
    public static int largestIntegerCounting(int num) {
        char[] digits = String.valueOf(num).toCharArray();
        int[] counts = new int[10];
        
        // Count frequencies of each digit
        for (char c : digits) {
            counts[c - '0']++;
        }
        
        int currentEven = 8;
        int currentOdd = 9;
        
        for (int i = 0; i < digits.length; i++) {
            int originalDigit = digits[i] - '0';
            
            if (originalDigit % 2 == 0) {
                // Find the largest available even digit
                while (counts[currentEven] == 0) {
                    currentEven -= 2;
                }
                digits[i] = (char) (currentEven + '0');
                counts[currentEven]--;
            } else {
                // Find the largest available odd digit
                while (counts[currentOdd] == 0) {
                    currentOdd -= 2;
                }
                digits[i] = (char) (currentOdd + '0');
                counts[currentOdd]--;
            }
        }
        
        return Integer.parseInt(new String(digits));
    }

    /**
     * ========================================================================
     * MAIN METHOD: Executing and verifying the examples
     * ========================================================================
     */
    public static void main(String[] args) {
        List<TestCase> tests = List.of(
            new TestCase(1234, 3412),
            new TestCase(65875, 87655),
            new TestCase(2468, 8642), // All even
            new TestCase(1357, 7531), // All odd
            new TestCase(10, 10),     // Edge case
            new TestCase(2046, 6420)
        );

        for (int i = 0; i < tests.size(); i++) {
            TestCase tc = tests.get(i);
            System.out.println("Test Case " + (i + 1) + ":");
            System.out.println("Input Num: " + tc.num());
            System.out.println("Expected:  " + tc.expected());
            
            int resHeap = largestIntegerHeaps(tc.num());
            int resCounting = largestIntegerCounting(tc.num());
            
            System.out.println("Heap Solution:     " + resHeap);
            System.out.println("Counting Solution: " + resCounting);
            System.out.println("--------------------------------------------------");
        }
    }
}
