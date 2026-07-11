import java.util.HashSet;
import java.util.Set;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: HAPPY NUMBER (LeetCode 202)
 * ============================================================================
 *
 * This file is structured as a full interview walkthrough, from problem
 * restatement to a production-quality optimal solution with dry-run tracing.
 */
class HappyNumber {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words:
     * Given a positive integer n, I repeatedly replace it with the sum of the
     * squares of its decimal digits. I keep doing this until one of two
     * things happens:
     *   (a) The number becomes 1 -> n is a "happy number" -> return true.
     *   (b) The number enters a repeating cycle that never reaches 1 ->
     *       n is not happy -> return false.
     *
     * Key observations / constraints:
     *   - Input: a single integer n (per LeetCode, 1 <= n <= 2^31 - 1).
     *   - Output: boolean (true = happy, false = not happy).
     *   - The "digit-square-sum" transformation is deterministic: given the
     *     same number, it always produces the same next number. This means
     *     if we ever see a repeated value, we are guaranteed to be in an
     *     infinite loop (no randomness, no branching).
     *   - Because the transformation is deterministic, this is fundamentally
     *     a "cycle detection in a functional graph" problem, similar in
     *     spirit to detecting a cycle in a linked list.
     *   - It is a mathematical fact that ALL non-happy numbers eventually
     *     fall into the SAME cycle: 4 -> 16 -> 37 -> 58 -> 89 -> 145 -> 42
     *     -> 20 -> 4 -> ... This fact enables an O(1)-space "known cycle"
     *     optimization, discussed later.
     */


    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * ========================================================================
     *
     * 1. Q: What is the valid range of n? Can it be 0 or negative?
     *    A (assumed): n is a positive integer, 1 <= n <= 2^31 - 1 (fits in
     *       a Java int). We do not need to handle 0 or negatives.
     *
     * 2. Q: Should the function handle n = 1 directly (trivially happy)?
     *    A (assumed): Yes, n = 1 should immediately return true since the
     *       process starts there and 1 already satisfies the condition.
     *
     * 3. Q: Is n guaranteed to fit in a 32-bit int, or could it be a long/
     *       BigInteger for extremely large inputs?
     *    A (assumed): For this interview, n fits in a standard 32-bit int.
     *       I'll note that the sum-of-squares transformation is "shrinking"
     *       for any large number (a k-digit number's max possible next
     *       value is 81*k, which is far smaller once k > 3), so overflow
     *       is not a practical concern even for very large starting n.
     *
     * 4. Q: Do we need to return anything other than a boolean, e.g., the
     *       actual cycle, or the number of steps to reach 1?
     *    A (assumed): No, just a boolean true/false is required.
     *
     * 5. Q: Should I optimize for time, space, or both?
     *    A (assumed): Discuss trade-offs, but favor an O(1) extra space
     *       solution (Floyd's cycle detection) as the "optimal" answer,
     *       since that is the canonical follow-up to the hashing approach.
     *
     * 6. Q: Is this a single query, or will this function be called many
     *       times (e.g., over a large batch of numbers)? Does caching
     *       across calls matter?
     *    A (assumed): Single query per call; no shared state/cache is
     *       required, though I'll mention memoization as a possible
     *       extension for repeated/batch queries.
     *
     * 7. Q: Should I worry about concurrency (multiple threads calling
     *       isHappy() simultaneously)?
     *    A (assumed): No shared mutable state is used in the optimal
     *       solution, so it is inherently thread-safe. Not a concern here.
     *
     * 8. Q: Are leading zeros or non-decimal bases (e.g., binary, hex) in
     *       play?
     *    A (assumed): Standard base-10 digits only.
     */


    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case): n = 19 -> HAPPY (true)
     *   19 -> 1^2 + 9^2 = 1 + 81 = 82
     *   82 -> 8^2 + 2^2 = 64 + 4 = 68
     *   68 -> 6^2 + 8^2 = 36 + 64 = 100
     *   100 -> 1^2 + 0^2 + 0^2 = 1
     *   Reached 1 -> happy -> return true.
     *
     * Example 2 (Not-happy / cycle case): n = 2 -> NOT HAPPY (false)
     *   2 -> 4
     *   4 -> 16
     *   16 -> 1 + 36 = 37
     *   37 -> 9 + 49 = 58
     *   58 -> 25 + 64 = 89
     *   89 -> 64 + 81 = 145
     *   145 -> 1 + 16 + 25 = 42
     *   42 -> 16 + 4 = 20
     *   20 -> 4 + 0 = 4   <-- we've seen 4 before! Cycle detected -> false.
     *
     * Example 3 (Boundary / trivial case): n = 1 -> HAPPY (true)
     *   Already equals 1, zero transformations needed. This is the trivial
     *   base case and must be handled correctly by the loop condition
     *   (i.e., don't require at least one iteration before checking).
     */


    /* ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Paradigm scan (called out explicitly, including what does NOT apply):
     *   - Brute force / naive:       APPLICABLE (simulate with no memory)
     *   - Hashing-based:              APPLICABLE (HashSet to detect repeats)
     *   - Two pointer (fast/slow):    APPLICABLE (Floyd's cycle detection)
     *   - Math / precomputed set:     APPLICABLE (known 8-number cycle)
     *   - Sorting-based:               NOT APPLICABLE - no ordering to exploit;
     *                                   there's nothing to sort.
     *   - Divide and conquer:          NOT APPLICABLE - the transformation is
     *                                   sequential/stateful, not decomposable.
     *   - Greedy:                      NOT APPLICABLE - there's no sequence of
     *                                   locally-optimal choices to make; the
     *                                   transformation is fully deterministic.
     *   - Dynamic programming:         NOT APPLICABLE - no overlapping
     *                                   subproblems/optimal substructure to
     *                                   exploit for a single query (though
     *                                   memoization across MANY queries is a
     *                                   valid follow-up optimization).
     *   - Tree / graph traversal:      CONCEPTUALLY APPLICABLE - the problem
     *                                   IS a functional graph (each number
     *                                   points to exactly one next number),
     *                                   but it collapses to simple cycle
     *                                   detection rather than BFS/DFS.
     *   - Heap / priority queue:       NOT APPLICABLE - no notion of priority
     *                                   or "top-k" ordering involved.
     *   - Binary search:               NOT APPLICABLE - no sorted/monotonic
     *                                   search space.
     *   - Monotonic stack/deque:       NOT APPLICABLE - no need to track a
     *                                   monotonic sequence of elements.
     *   - Trie / segment tree:         NOT APPLICABLE - no prefix/range query
     *                                   structure needed.
     */

    // ------------------------------------------------------------------
    // Approach 1: Brute Force / Naive (Unbounded Simulation, No Memory)
    // ------------------------------------------------------------------
    // Core idea: Just simulate the transformation forever, hoping it
    // terminates. WITHOUT a way to detect cycles, this approach can loop
    // infinitely for non-happy numbers, so it is only "naive" in that it's
    // incomplete/incorrect as stated -- included here to show why we need
    // cycle detection at all. To make it runnable, we cap iterations with
    // an arbitrary large bound (a hack, not a real solution).
    //
    // Data structure/paradigm: none (plain iteration).
    // Time Complexity: technically unbounded/undefined for non-happy inputs
    //   (would run forever without an artificial cap).
    // Space Complexity: O(1).
    // Pros: Trivial to write, no extra memory.
    // Cons: INCORRECT in general -- cannot reliably distinguish "not happy"
    //   from "just needs more iterations" without an arbitrary cutoff.
    // When to use: Never in production; only useful as a motivating strawman
    //   to justify why cycle detection is required.
    static boolean isHappyBruteForceCapped(int n) {
        int current = n;
        int arbitraryIterationCap = 1000; // hack: no theoretical justification
        for (int step = 0; step < arbitraryIterationCap; step++) {
            if (current == 1) {
                return true;
            }
            current = sumOfSquaredDigits(current);
        }
        return false; // WARNING: this just means "didn't reach 1 within cap"
    }

    // ------------------------------------------------------------------
    // Approach 2: Hashing-Based (HashSet Cycle Detection)
    // ------------------------------------------------------------------
    // Core idea: Track every number we've seen in a HashSet. If we ever
    // compute a number we've already seen, we know we're in a cycle (since
    // the transformation is deterministic) and the number is not happy.
    // If we reach 1, it's happy.
    //
    // Data structure/paradigm: Hashing (HashSet) for O(1) average
    //   membership checks; this is the classic "detect a repeat" pattern.
    // Time Complexity: O(log n) per transformation step to extract digits,
    //   and it's a known result that the sequence enters a small cycle
    //   (bounded above by a small constant of numbers <= 243 for any
    //   32-bit int, since 9^2 * 10 = 810 is an upper bound on the sum of
    //   squares of digits of any number with up to 10 digits) after few
    //   steps -- so overall this runs in O(log n) practically, treated as
    //   O(1)-ish per query but formally bounded by the digit-count work.
    // Space Complexity: O(k) where k is the number of distinct values
    //   visited before a repeat -- bounded by a small constant in practice,
    //   but not O(1) in the worst-case theoretical sense.
    // Pros: Simple, correct, easy to reason about and explain.
    // Cons: Uses extra memory proportional to the cycle length (though
    //   small in practice); requires hashing overhead (boxing Integer,
    //   hashCode/equals calls).
    // When to use: Great as a first correct solution in an interview --
    //   demonstrates the key insight (deterministic function -> repeat
    //   implies cycle) clearly and quickly. Good "first pass" answer.
    static boolean isHappyHashSet(int n) {
        Set<Integer> seenNumbers = new HashSet<>();
        int current = n;
        while (current != 1 && !seenNumbers.contains(current)) {
            seenNumbers.add(current);
            current = sumOfSquaredDigits(current);
        }
        return current == 1;
    }

    // ------------------------------------------------------------------
    // Approach 3: Two Pointer -- Floyd's Cycle Detection (Fast & Slow)
    // ------------------------------------------------------------------
    // Core idea: Treat the sequence of transformations as an implicit
    // linked list, where each number "points to" the next number produced
    // by sumOfSquaredDigits(). Use a slow pointer that advances one step
    // at a time and a fast pointer that advances two steps at a time
    // (Floyd's "tortoise and hare" algorithm). If the sequence is happy,
    // the fast pointer reaches 1 first. If it's not happy, the fast and
    // slow pointers will eventually land on the SAME number, proving a
    // cycle exists, without needing to store any visited history.
    //
    // Data structure/paradigm: Two pointers over an implicit linked
    //   structure (classic Floyd's cycle detection, same technique used
    //   for "Linked List Cycle" problems).
    // Time Complexity: O(log n) -- same reasoning as above: the sequence
    //   quickly drops into a small bounded range, and Floyd's algorithm
    //   detects a cycle within a bounded number of extra steps once inside
    //   the cycle.
    // Space Complexity: O(1) -- only two integer pointers are stored,
    //   regardless of input size. This is the key advantage over hashing.
    // Pros: Constant extra space; same asymptotic time as hashing;
    //   elegant reuse of a well-known technique.
    // Cons: Slightly trickier to explain/code correctly under pressure
    //   (must get the "slow moves once, fast moves twice" logic and loop
    //   condition right); marginally more function calls per iteration.
    // When to use: This is the answer to present as the "optimized"
    //   follow-up once asked "can you do this in O(1) space?" -- it is the
    //   expected optimal solution for this problem in most interviews.
    static boolean isHappyFloydCycle(int n) {
        int slowPointer = n;
        int fastPointer = sumOfSquaredDigits(n);
        while (fastPointer != 1 && slowPointer != fastPointer) {
            slowPointer = sumOfSquaredDigits(slowPointer);               // 1 step
            fastPointer = sumOfSquaredDigits(sumOfSquaredDigits(fastPointer)); // 2 steps
        }
        return fastPointer == 1;
    }

    // ------------------------------------------------------------------
    // Approach 4: Math / Precomputed Known-Cycle Set
    // ------------------------------------------------------------------
    // Core idea: It is a proven mathematical fact that EVERY non-happy
    // positive integer eventually falls into the exact same 8-number
    // cycle: {4, 16, 37, 58, 89, 145, 42, 20}. So instead of doing general
    // cycle detection, we can simply check membership in this fixed,
    // precomputed set as our "not happy" stopping condition.
    //
    // Data structure/paradigm: Precomputed constant HashSet (essentially a
    //   lookup table / memo of a known mathematical result).
    // Time Complexity: O(log n) -- identical iteration pattern to the
    //   others, just a different stopping condition.
    // Space Complexity: O(1) -- the known-cycle set has a fixed, constant
    //   size (8 elements) regardless of input.
    // Pros: Combines the O(1) space benefit of Floyd's approach with the
    //   simplicity/readability of the hashing approach (no fast/slow
    //   pointer bookkeeping needed).
    // Cons: Relies on "knowing" (or being able to prove/cite) a
    //   non-obvious mathematical fact about this specific problem; an
    //   interviewer may push back and ask you to justify or derive it,
    //   which is harder to do confidently on the spot than Floyd's
    //   algorithm, which is self-justifying.
    // When to use: Good as a "fun fact" alternative to mention after
    //   presenting Floyd's solution, showing breadth of thinking -- but
    //   Floyd's is the more defensible and general answer to lead with.
    private static final Set<Integer> KNOWN_UNHAPPY_CYCLE = Set.of(
            4, 16, 37, 58, 89, 145, 42, 20
    );

    static boolean isHappyKnownCycle(int n) {
        int current = n;
        while (current != 1 && !KNOWN_UNHAPPY_CYCLE.contains(current)) {
            current = sumOfSquaredDigits(current);
        }
        return current == 1;
    }

    // ------------------------------------------------------------------
    // Shared helper: compute the sum of the squares of the decimal digits
    // of a positive integer. Used by all approaches above.
    // ------------------------------------------------------------------
    private static int sumOfSquaredDigits(int number) {
        int sum = 0;
        while (number > 0) {
            int digit = number % 10;
            sum += digit * digit;
            number /= 10;
        }
        return sum;
    }


    /* ========================================================================
     * SECTION 6: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * Approach                     | Time      | Space | Best For                          | Limitations
     * ------------------------------------------------------------------------------------------------------------------
     * 1. Brute Force (Capped)      | Undefined | O(1)  | Illustrating why detection needed | Not actually correct;
     *                               (unbounded)|       |                                    | relies on arbitrary cap
     * 2. HashSet Cycle Detection    | O(log n)  | O(k)  | Clear, quick first-pass solution  | Extra memory for
     *                                                    proportional to cycle size          | visited set (small but
     *                                                                                          | nonzero); boxing overhead
     * 3. Floyd's Cycle Detection    | O(log n)  | O(1)  | Optimal general solution;         | Slightly trickier to
     *   (Fast/Slow Pointers)                             the expected "best" answer          | code/explain correctly
     * 4. Known-Cycle Set (Math)     | O(log n)  | O(1)  | Elegant if the fact is accepted    | Requires justifying/
     *                                                                                          | citing a non-obvious
     *                                                                                          | mathematical claim
     */


    /* ========================================================================
     * SECTION 7: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 2 (HashSet) FIRST, since it's fast to write,
     * obviously correct, and clearly demonstrates the key insight: the
     * transformation is deterministic, so a repeated value proves a cycle.
     * This gets a working, defensible solution on the board quickly.
     *
     * Then, when asked "can you reduce the space complexity?" (which a
     * strong interviewer almost always will), I'd pivot to Approach 3
     * (Floyd's Cycle Detection), presenting it as the true optimal
     * solution: O(1) auxiliary space with the same time complexity, using
     * a well-known and easily justified technique (same as detecting a
     * cycle in a linked list). This is what I'd want to leave on the board
     * as my final answer, since it's optimal, general (doesn't rely on
     * memorizing a specific numeric fact), and demonstrates pattern reuse
     * across problem domains -- a strong signal in a Google-style interview.
     *
     * I would mention Approach 4 verbally as a fun aside/optimization but
     * would NOT lead with it, since depending on a specific unproven
     * (from the candidate's derivation) numeric fact is a weaker thing to
     * hang your primary solution on than a general algorithmic technique.
     */


    /* ========================================================================
     * SECTION 8: DEEP DIVE -- OPTIMAL SOLUTION (PRODUCTION QUALITY)
     * ========================================================================
     *
     * Polished version of Approach 3 (Floyd's Cycle Detection), with
     * input validation, named constants, and full Javadoc.
     */

    /**
     * Determines whether a given positive integer is a "happy number".
     * <p>
     * A happy number is one that, through repeated replacement with the
     * sum of the squares of its digits, eventually reaches 1. Numbers
     * that are not happy are guaranteed to enter an infinite cycle
     * instead (a proven mathematical property of this transformation).
     * <p>
     * This implementation uses Floyd's cycle detection algorithm
     * ("tortoise and hare") to detect that cycle using only O(1) extra
     * space, rather than storing a history of every value seen.
     *
     * @param n a positive integer to test; must satisfy {@code n >= 1}
     * @return {@code true} if {@code n} is a happy number, {@code false}
     *         if it enters a cycle without ever reaching 1
     * @throws IllegalArgumentException if {@code n < 1}
     */
    public static boolean isHappyNumber(int n) {
        if (n < 1) {
            throw new IllegalArgumentException(
                    "Happy number check is only defined for positive integers, got: " + n);
        }

        // The "slow" pointer (tortoise) advances one transformation per loop.
        int slowPointer = n;

        // The "fast" pointer (hare) advances two transformations per loop.
        // If the sequence is happy, fastPointer will reach exactly 1 before
        // slowPointer ever catches up to it.
        int fastPointer = transformOnce(n);

        // Loop until either:
        //   (a) fastPointer reaches 1 -> happy number, or
        //   (b) slowPointer catches up to fastPointer -> cycle detected,
        //       meaning n is NOT a happy number.
        while (fastPointer != HAPPY_TARGET && slowPointer != fastPointer) {
            slowPointer = transformOnce(slowPointer);
            fastPointer = transformOnce(transformOnce(fastPointer));
        }

        return fastPointer == HAPPY_TARGET;
    }

    /** The target value that signifies a happy number. */
    private static final int HAPPY_TARGET = 1;

    /** Base of the decimal digit system we're summing squares over. */
    private static final int DECIMAL_BASE = 10;

    /**
     * Computes the sum of the squares of the decimal digits of a positive
     * integer. For example, transformOnce(19) = 1^2 + 9^2 = 82.
     *
     * @param number a positive integer
     * @return the sum of the squares of its decimal digits
     */
    private static int transformOnce(int number) {
        int digitSquareSum = 0;
        int remaining = number;
        while (remaining > 0) {
            int lastDigit = remaining % DECIMAL_BASE;
            digitSquareSum += lastDigit * lastDigit;
            remaining /= DECIMAL_BASE;
        }
        return digitSquareSum;
    }


    /* ========================================================================
     * SECTION 9: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing isHappyNumber(19) step by step using Floyd's cycle detection:
     *
     * Initialization:
     *   slowPointer = 19
     *   fastPointer = transformOnce(19) = 1^2 + 9^2 = 82
     *
     * Loop check: fastPointer (82) != 1, slowPointer (19) != fastPointer (82)
     *   -> enter loop
     *
     * Iteration 1:
     *   slowPointer = transformOnce(19) = 82
     *   fastPointer = transformOnce(transformOnce(82))
     *       transformOnce(82)  = 8^2 + 2^2 = 68
     *       transformOnce(68)  = 6^2 + 8^2 = 100
     *     -> fastPointer = 100
     *   State: slowPointer = 82, fastPointer = 100
     *
     * Loop check: fastPointer (100) != 1, slowPointer (82) != fastPointer (100)
     *   -> enter loop
     *
     * Iteration 2:
     *   slowPointer = transformOnce(82) = 68
     *   fastPointer = transformOnce(transformOnce(100))
     *       transformOnce(100) = 1^2 + 0^2 + 0^2 = 1
     *       transformOnce(1)   = 1^2 = 1
     *     -> fastPointer = 1
     *   State: slowPointer = 68, fastPointer = 1
     *
     * Loop check: fastPointer == 1 -> STOP loop.
     *
     * Return: fastPointer == 1 -> true.
     *
     * Result: isHappyNumber(19) returns true. Matches our manual
     * computation in Section 3, Example 1.
     */


    /* ========================================================================
     * SECTION 10: CLOSING SUMMARY
     * ========================================================================
     *
     * - The core insight is that the digit-square-sum transformation is a
     *   deterministic function, so repeated values imply an infinite loop.
     * - The HashSet approach (Approach 2) is the simplest correct solution,
     *   trading O(k) space for straightforward logic.
     * - Floyd's cycle detection (Approach 3) is the optimal general
     *   solution: same time complexity, O(1) space, using a widely
     *   applicable technique (also used in linked-list cycle detection).
     * - The known-cycle-set trick (Approach 4) is a cute O(1)-space
     *   shortcut but relies on a specific memorized/derived mathematical
     *   fact rather than a general technique, making it a weaker primary
     *   answer in an interview setting.
     * - Assumptions: n is a positive 32-bit int (n >= 1); no concurrency or
     *   batch-caching requirements were assumed necessary for this problem.
     * - Known limitation: this solution does not cache results across
     *   multiple calls; a batch/service context might benefit from memoizing
     *   known happy/unhappy numbers, discussed as a follow-up below.
     */


    /* ========================================================================
     * SECTION 11: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "Can you solve this without a HashSet, in O(1) extra space?"
     *    -> Leads directly to Floyd's cycle detection (Approach 3).
     *
     * 2. "What if we called this function millions of times on overlapping
     *     ranges of numbers (e.g., counting happy numbers from 1 to 10^9)?
     *     How would you optimize for that batch scenario?"
     *    -> Discuss memoization: cache results (or intermediate results
     *       along a chain) so repeated sub-chains aren't recomputed;
     *       consider a bitset for a bounded range of known results.
     *
     * 3. "Can you prove that a non-happy number is GUARANTEED to enter a
     *     cycle rather than growing unboundedly?"
     *    -> Discuss the digit-count bound: for any number with d digits,
     *       the maximum possible next value is 81*d (since 9^2 = 81 per
     *       digit). Once d exceeds 3, 81*d becomes smaller than the
     *       original number's lower bound (10^(d-1)), so the sequence is
     *       guaranteed to shrink into a bounded range (below 243) and,
     *       by the pigeonhole principle, must eventually repeat a value.
     *
     * 4. "What if the input could be a very large number represented as a
     *     string (beyond 32/64-bit integer range)?"
     *    -> Discuss adapting sumOfSquaredDigits to operate on a String/
     *       char array instead of int arithmetic; note the result after
     *       one transformation is always small regardless of the original
     *       string's length, so subsequent steps stay in normal int range.
     *
     * 5. "How would you extend this to a generalized 'happy number in base
     *     b' problem?"
     *    -> Discuss parameterizing DECIMAL_BASE and adjusting digit
     *       extraction (remaining % base, remaining / base); the same
     *       cycle-detection logic applies unchanged.
     *
     * 6. "Is this solution thread-safe if exposed as a shared utility
     *     method in a multi-threaded service?"
     *    -> Yes: no shared/static mutable state is modified; all state is
     *       local to the method invocation (stack-local ints), so it's
     *       inherently safe for concurrent calls.
     */


    /* ========================================================================
     * SECTION 12: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Forgetting the trivial base case n = 1: some candidates write a
     *    do-while loop that always performs at least one transformation
     *    before checking, which can produce subtly wrong results or extra
     *    unnecessary computation for n = 1. The check for "already equal
     *    to target" should happen before any transformation is applied,
     *    or the loop condition must account for it correctly.
     *
     * 2. Off-by-one in Floyd's algorithm: incorrectly initializing both
     *    slowPointer and fastPointer to the SAME starting value with no
     *    initial fast-forward of the fast pointer can cause the loop to
     *    terminate immediately as "equal" on the very first check (since
     *    slow == fast trivially at start), producing an incorrect "not
     *    happy" result. The fast pointer must be advanced at least once
     *    before the comparison loop begins (as done above with
     *    fastPointer = transformOnce(n)).
     *
     * 3. Assuming the wrong cycle-detection target: candidates sometimes
     *    check "if current appears twice" using structural/reference
     *    equality bugs when they mistakenly use wrapper Integer objects
     *    and rely on == instead of .equals() (Integer caching only works
     *    reliably for values -128 to 127!). Using a HashSet<Integer>'s
     *    .contains() method (Approach 2) avoids this trap entirely, but
     *    hand-rolled comparisons must be careful.
     *
     * 4. Not recognizing that EVERY non-happy number funnels into the
     *    SAME 8-number cycle: candidates sometimes assume they need to
     *    detect arbitrary/different cycles for different unhappy inputs,
     *    leading to overly complex solutions, when in fact a single fixed
     *    known-cycle set (or general cycle detection) suffices for all
     *    non-happy inputs universally.
     */


    /* ========================================================================
     * TEST HARNESS -- Cross-validates all four approaches against known
     * happy/unhappy numbers.
     * ========================================================================
     */
    public static void main(String[] args) {
        int[] testNumbers = {1, 7, 19, 2, 4, 100, 23, 116, 999999999};
        boolean[] expected = {true, true, true, false, false, true, true, false, false};

        for (int index = 0; index < testNumbers.length; index++) {
            int number = testNumbers[index];
            boolean bruteForceResult = isHappyBruteForceCapped(number);
            boolean hashSetResult = isHappyHashSet(number);
            boolean floydResult = isHappyFloydCycle(number);
            boolean knownCycleResult = isHappyKnownCycle(number);
            boolean productionResult = isHappyNumber(number);

            boolean allAgree = (bruteForceResult == hashSetResult)
                    && (hashSetResult == floydResult)
                    && (floydResult == knownCycleResult)
                    && (knownCycleResult == productionResult)
                    && (productionResult == expected[index]);

            System.out.printf(
                    "n=%-12d expected=%-6b brute=%-6b hashSet=%-6b floyd=%-6b knownCycle=%-6b production=%-6b allMatch=%b%n",
                    number, expected[index], bruteForceResult, hashSetResult,
                    floydResult, knownCycleResult, productionResult, allAgree
            );

            if (!allAgree) {
                throw new AssertionError("Mismatch detected for n = " + number);
            }
        }

        System.out.println("All approaches agree on all test cases.");
    }
}
