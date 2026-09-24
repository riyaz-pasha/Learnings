import java.util.*;

/**
 * ============================================================================
 * SECTION 1: RESTATE THE PROBLEM
 * ============================================================================
 *
 * In plain English:
 *
 *   We're given a row of trees, `fruits[i]` = the fruit TYPE growing on the
 *   i-th tree (an integer label, not a quantity). We walk along the row from
 *   left to right, starting at some tree of our choosing, and we must pick
 *   exactly one fruit from every tree we pass (we cannot skip a tree once
 *   we've started). We are carrying exactly two baskets, and each basket can
 *   hold ONLY ONE fruit type, but an unlimited quantity of that type. The
 *   walk ends the instant we hit a tree whose fruit type doesn't match
 *   either basket (both baskets are already "committed" to two other types).
 *
 *   This is equivalent to: "Find the length of the longest contiguous
 *   subarray of `fruits` that contains at most 2 distinct values."
 *   The answer we return is that maximum length (= max fruits collected).
 *
 * Inputs:
 *   - int[] fruits — fruit type at each tree, 0 <= fruits.length,
 *     0 <= fruits[i] <= some upper bound (LeetCode states fruits[i] <= 10^5).
 *
 * Output:
 *   - An int: the maximum number of fruits collectible under the 2-basket
 *     rule, i.e., the longest contiguous run containing at most 2 distinct
 *     fruit types.
 *
 * Key constraints (from LeetCode 904, "Fruit Into Baskets"):
 *   - 1 <= fruits.length <= 10^5
 *   - 0 <= fruits[i] <= fruits.length - 1 (fruit types are bounded by array
 *     length in the official constraints, but we won't rely on that bound in
 *     our general solution — a HashMap-based approach works regardless).
 *
 * Assumptions I'll state explicitly (and confirm with the interviewer in
 * Section 2):
 *   - The array is non-empty (length >= 1).
 *   - We must collect from a CONTIGUOUS block of trees (no skipping ahead).
 *   - "Basket holds one type, unlimited quantity" literally means: within
 *     our contiguous window, there can be at most 2 distinct integer values.
 */

/**
 * ============================================================================
 * SECTION 2: CLARIFYING QUESTIONS
 * ============================================================================
 *
 * Q1: What is the maximum size of `fruits`? Could it be up to 10^5 or 10^6+?
 *     A (assumed): Up to 10^5, per LeetCode's stated constraints. This rules
 *     out anything worse than O(n log n) as a safe ceiling, and O(n) is
 *     expected for full credit.
 *
 * Q2: Can `fruits` be empty?
 *     A (assumed): No — length >= 1. If asked to handle it defensively
 *     anyway, we'll return 0 for an empty array.
 *
 * Q3: Are fruit type labels guaranteed to be small non-negative integers
 *     bounded by array length, or could they be arbitrary integers
 *     (including negative, or very large)?
 *     A (assumed): Treat them as arbitrary integers for robustness — we'll
 *     use a HashMap rather than assuming values fit in a small array, unless
 *     told we can rely on the tighter bound for a micro-optimization.
 *
 * Q4: Do we need to return the actual subarray (start/end indices) or just
 *     its length (the fruit count)?
 *     A (assumed): Just the length/count, per the problem statement
 *     ("return the maximum number of fruits").
 *
 * Q5: If all trees have the same fruit type, is the answer just the whole
 *     array length?
 *     A (assumed): Yes — a single type trivially fits in one basket, so the
 *     whole array qualifies.
 *
 * Q6: Should the solution be thread-safe or handle streaming input (fruits
 *     arriving one at a time) rather than a fully materialized array?
 *     A (assumed): No concurrency requirement. Input is a fixed, fully
 *     available array. I'll mention the streaming variant as a follow-up.
 *
 * Q7: Is it acceptable to mutate the input array, or must it stay unmodified?
 *     A (assumed): Must stay unmodified — sorting or in-place edits are off
 *     the table anyway, since order encodes adjacency, which is essential to
 *     the problem (this is a strong hint that sorting-based approaches don't
 *     apply here).
 *
 * Q8: What should we return if length is 0 (defensive case) — 0, or throw?
 *     A (assumed): Return 0. Throwing would be reasonable too; I'll note the
 *     choice in a comment rather than debate it at length.
 */

/**
 * ============================================================================
 * SECTION 3: EXAMPLES & EDGE CASES
 * ============================================================================
 *
 * Example 1 (normal case):
 *   fruits = [1, 2, 1]
 *   Every tree fits in 2 baskets (types {1, 2}). Answer = 3 (collect all).
 *
 * Example 2 (forces a "cut", i.e., basket eviction):
 *   fruits = [0, 1, 2, 2]
 *   - Starting at index 0: {0,1,2,2} has 3 distinct types -> can't take all.
 *   - Best window: indices [1..3] = [1,2,2] -> types {1,2}, length 3.
 *   Answer = 3.
 *
 * Example 3 (edge case: single tree):
 *   fruits = [5]
 *   Only one tree, one type. Answer = 1.
 *
 * Example 4 (edge case: all same type):
 *   fruits = [4, 4, 4, 4]
 *   Only one distinct type ever appears. Answer = 4 (whole array).
 *
 * Example 5 (boundary / tie-breaking case — multiple maximal windows of the
 * same best length, and a window that starts באMID-array, not at index 0):
 *   fruits = [1, 2, 3, 2, 2]
 *   - Window [1,2] (indices 0-1): length 2, types {1,2}.
 *   - Window [2,3,2,2] (indices 1-4): types {2,3}, length 4.
 *   - Window [3,2,2] (indices 2-4): types {2,3}, length 3.
 *   The best is the window starting at index 1, length 4 — note the optimal
 *   window does NOT have to start at index 0. This is the classic trap: a
 *   candidate who only tries "start from the very first tree" will get this
 *   wrong. Answer = 4.
 */

class FruitIntoBaskets {

    /*
     * ========================================================================
     * SECTION 4 & 5 & 6: ALL POSSIBLE SOLUTIONS
     * ========================================================================
     *
     * Paradigm sweep — which categories apply, and which don't:
     *
     *   - Brute force / naive        -> APPLICABLE (Approach 1)
     *   - Sorting-based              -> NOT APPLICABLE: sorting destroys the
     *                                    contiguity/adjacency of trees, which
     *                                    is the entire constraint we must
     *                                    respect. The answer depends on
     *                                    ORIGINAL positional order.
     *   - Hashing-based              -> APPLICABLE (Approach 2, frequency map
     *                                    inside a sliding window)
     *   - Two pointer / sliding window -> APPLICABLE (Approaches 2 & 3); this
     *                                    is the natural fit since we want the
     *                                    longest contiguous run satisfying a
     *                                    monotonic-shrinkable constraint
     *                                    ("at most K distinct").
     *   - Divide and conquer         -> NOT NATURALLY APPLICABLE: there's no
     *                                    clean way to combine the answer from
     *                                    two halves, because the best window
     *                                    can straddle the midpoint, and
     *                                    merging two side-answers requires
     *                                    re-scanning across the boundary
     *                                    anyway — no asymptotic win over the
     *                                    linear sliding window.
     *   - Greedy                     -> The sliding window IS a greedy/
     *                                    two-pointer technique (always push
     *                                    the right pointer, only pull the
     *                                    left pointer when forced). Not
     *                                    listed as a separate approach since
     *                                    it's subsumed by Approaches 2 & 3.
     *   - Dynamic programming        -> NOT NEEDED: you *could* define
     *                                    dp[i] = longest valid window ending
     *                                    at i, but computing dp[i] still
     *                                    requires knowing the last two
     *                                    distinct types and their most recent
     *                                    positions — i.e., you'd reinvent the
     *                                    sliding window with extra bookkeeping
     *                                    and strictly more overhead. No
     *                                    complexity benefit, so I won't
     *                                    present it as a distinct approach.
     *   - Tree / graph traversal     -> NOT APPLICABLE: no graph/tree
     *                                    structure underlies this problem.
     *   - Heap / priority queue      -> NOT APPLICABLE: we don't need
     *                                    ordering by priority; we need
     *                                    "which type was seen least
     *                                    recently", which a simple map or
     *                                    two variables handle in O(1).
     *   - Binary search              -> NOT APPLICABLE: there's no monotonic
     *                                    predicate over a sorted search space
     *                                    to exploit. "Can I find a valid
     *                                    window of length >= L?" isn't a
     *                                    simpler check than just running the
     *                                    O(n) sliding window directly, so
     *                                    binary-searching the answer buys
     *                                    nothing here.
     *   - Monotonic stack / deque    -> NOT APPLICABLE: no "next greater/
     *                                    smaller element" structure or
     *                                    ordering property to maintain.
     *   - Trie / segment tree        -> NOT APPLICABLE (overkill): a segment
     *                                    tree could answer "distinct count in
     *                                    range" queries, but we don't have
     *                                    multiple arbitrary range queries —
     *                                    we have ONE longest-window question,
     *                                    which the O(n) sliding window
     *                                    answers directly without extra
     *                                    machinery.
     *
     * So the meaningful approaches are:
     *   Approach 1: Brute Force (check every subarray)
     *   Approach 2: Sliding Window + HashMap frequency counts (generalizes to
     *               K baskets)
     *   Approach 3: Sliding Window, O(1) space, specialized for exactly 2
     *               basket types (tracks only the two most recent distinct
     *               types and the last-seen index of the "older" one)
     */

    /**
     * -------------------------------------------------------------------
     * Approach 1: Brute Force (all subarrays)
     * -------------------------------------------------------------------
     * Core idea: For every possible starting tree, walk right as far as
     * possible while the number of distinct fruit types in the current
     * window stays <= 2, tracking counts in a HashMap. Record the best
     * window length seen across all starting points.
     *
     * Data structure / paradigm: Nested loops + HashMap<Integer,Integer> for
     * counting distinct types in the current window ("brute force + hashing
     * for the inner check").
     *
     * Time Complexity: O(n^2) worst case — for each of the n starting
     * indices, we may walk up to n trees to the right before stopping.
     * Space Complexity: O(1) distinct keys in the map at any time (bounded
     * by 3, since we stop as soon as a 3rd type appears), so O(1) auxiliary
     * space, though the map object itself is re-created O(n) times.
     *
     * Pros:
     *   - Extremely easy to reason about and verify by inspection.
     *   - Zero risk of subtle sliding-window bugs (no pointer arithmetic).
     * Cons:
     *   - O(n^2) is too slow for n = 10^5 (10^10 operations) — would TLE.
     *   - Repeats a lot of work: re-scans overlapping windows from scratch.
     *
     * When to use: Only as a warm-up / correctness oracle for small inputs
     * during development, or if asked to "just get something working first."
     * Never acceptable as a final answer for the stated constraints.
     */
    public static int maxFruitsBruteForce(int[] fruits) {
        int treeCount = fruits.length;
        if (treeCount == 0) {
            return 0;
        }

        int bestWindowLength = 0;

        for (int windowStart = 0; windowStart < treeCount; windowStart++) {
            Map<Integer, Integer> typeCounts = new HashMap<>();
            int windowEnd = windowStart;

            while (windowEnd < treeCount) {
                int fruitType = fruits[windowEnd];

                // Would adding this fruit introduce a 3rd distinct type?
                boolean introducesNewType = !typeCounts.containsKey(fruitType);
                if (introducesNewType && typeCounts.size() == 2) {
                    break; // Can't fit this fruit type in either basket.
                }

                typeCounts.merge(fruitType, 1, Integer::sum);
                windowEnd++;
            }

            int currentWindowLength = windowEnd - windowStart;
            bestWindowLength = Math.max(bestWindowLength, currentWindowLength);
        }

        return bestWindowLength;
    }

    /**
     * -------------------------------------------------------------------
     * Approach 2: Sliding Window + HashMap Frequency Counts
     * -------------------------------------------------------------------
     * Core idea: Maintain a window [windowStart, windowEnd] and a frequency
     * map of fruit types currently inside it. Expand windowEnd by one tree
     * at a time. Whenever the map holds more than 2 distinct keys, shrink
     * from windowStart (decrementing / removing counts) until we're back
     * down to <= 2 distinct types. Track the max window size seen.
     *
     * This is the "textbook" solution and it generalizes trivially to the
     * "K baskets" version of this problem (just replace the constant 2 with
     * K), which is exactly why I'd mention it even though Approach 3 is
     * slightly faster in practice for this specific K = 2 case.
     *
     * Data structure / paradigm: Sliding window (two pointers) + HashMap for
     * frequency counting.
     *
     * Time Complexity: O(n) — windowEnd advances n times total; windowStart
     * advances at most n times total across the whole run (each index is
     * added to and removed from the window at most once), so total work is
     * O(n), i.e., amortized O(1) per tree.
     * Space Complexity: O(1) — the map holds at most 3 keys at any instant
     * (2 valid + the 1 that triggered the shrink), independent of n.
     *
     * Pros:
     *   - Optimal O(n) time.
     *   - Directly generalizes to "at most K distinct types" with a one-line
     *     change (compare typeCounts.size() > K).
     *   - Very readable; the invariant ("window always valid after the while
     *     loop") is easy to state and defend in an interview.
     * Cons:
     *   - Slightly more constant-factor overhead than Approach 3 due to
     *     HashMap operations (hashing, boxing of Integer keys/values).
     *
     * When to use: This is my default "first working solution" to present
     * out loud, especially if there's any chance of a K-baskets follow-up —
     * it's the most defensible, general, and easy to extend on the fly.
     */
    public static int maxFruitsSlidingWindowHashMap(int[] fruits) {
        int treeCount = fruits.length;
        if (treeCount == 0) {
            return 0;
        }

        final int maxDistinctTypesAllowed = 2; // The "2 baskets" rule.
        Map<Integer, Integer> typeCounts = new HashMap<>();
        int windowStart = 0;
        int bestWindowLength = 0;

        for (int windowEnd = 0; windowEnd < treeCount; windowEnd++) {
            int incomingFruitType = fruits[windowEnd];
            typeCounts.merge(incomingFruitType, 1, Integer::sum);

            // Shrink from the left while we have too many distinct types.
            while (typeCounts.size() > maxDistinctTypesAllowed) {
                int outgoingFruitType = fruits[windowStart];
                int updatedCount = typeCounts.get(outgoingFruitType) - 1;

                if (updatedCount == 0) {
                    typeCounts.remove(outgoingFruitType);
                } else {
                    typeCounts.put(outgoingFruitType, updatedCount);
                }
                windowStart++;
            }

            int currentWindowLength = windowEnd - windowStart + 1;
            bestWindowLength = Math.max(bestWindowLength, currentWindowLength);
        }

        return bestWindowLength;
    }

    /**
     * -------------------------------------------------------------------
     * Approach 3: Sliding Window, O(1) Space, Specialized for Exactly 2 Types
     * -------------------------------------------------------------------
     * Core idea: Since we only ever care about the two MOST RECENTLY seen
     * distinct fruit types, we don't need a general-purpose map at all. Track:
     *   - lastType:            the fruit type most recently encountered
     *   - secondLastType:      the OTHER distinct type currently in our
     *                          window (the one that isn't lastType)
     *   - lastOccurrenceOfSecondLastType: the most recent index at which
     *                          secondLastType appeared
     *   - windowStart:         left edge of the current valid window
     *
     * When a 3rd distinct type shows up at index windowEnd, we know exactly
     * where to jump windowStart: to (lastOccurrenceOfSecondLastType + 1).
     * Everything from windowStart up to lastOccurrenceOfSecondLastType is of
     * type secondLastType-or-lastType, so jumping there in O(1) is always
     * safe and correct — no need to shrink one-by-one.
     *
     * Data structure / paradigm: Sliding window with O(1) auxiliary
     * variables instead of a map ("two pointers with constant state").
     *
     * Time Complexity: O(n) — a single left-to-right pass; windowStart only
     * ever jumps forward, never backward, so total movement is bounded by n.
     * Space Complexity: O(1) — a fixed handful of int variables, no
     * collections at all.
     *
     * Pros:
     *   - Fastest in practice: no hashing, no boxing, no map allocations.
     *   - True O(1) space (not just "O(1) distinct keys in a map").
     * Cons:
     *   - Specific to K = 2; doesn't generalize cleanly to K baskets (that
     *     would need to track K types + K last-occurrence indices, at which
     *     point Approach 2's map becomes the cleaner code).
     *   - Slightly more fiddly to get exactly right (see "common mistakes"
     *     in Section 13) — more state to juggle than Approach 2.
     *
     * When to use: This is what I'd present as the "optimal follow-up" once
     * Approach 2 is on the board and working, to show I can squeeze out the
     * constant-factor / space improvement when the interviewer presses for
     * it. I would NOT lead with this from scratch — it's higher risk to get
     * right live, and Approach 2 already gets full marks on complexity.
     */
    public static int maxFruitsSlidingWindowOptimized(int[] fruits) {
        int treeCount = fruits.length;
        if (treeCount == 0) {
            return 0;
        }
        if (treeCount == 1) {
            return 1;
        }

        int windowStart = 0;
        int lastType = fruits[0];
        int secondLastType = -1; // Sentinel: "no second type seen yet".
        int lastOccurrenceOfSecondLastType = -1;
        int bestWindowLength = 1;

        for (int windowEnd = 1; windowEnd < treeCount; windowEnd++) {
            int currentType = fruits[windowEnd];

            if (currentType == lastType) {
                // Same as the most recent type — window always still valid,
                // nothing to update except we'll recompute length below.
            } else if (currentType == secondLastType || secondLastType == -1) {
                // Either it matches our "other" basket, or our other basket
                // is still empty (only 1 distinct type seen so far). Either
                // way, promote currentType to be "the most recent type".
                secondLastType = lastType;
                lastOccurrenceOfSecondLastType = windowEnd - 1;
                lastType = currentType;
            } else {
                // A genuine 3rd distinct type appeared: both baskets are
                // already committed to lastType and secondLastType. Jump
                // windowStart to just past the last place secondLastType
                // occurred — everything before that point is now stale.
                windowStart = lastOccurrenceOfSecondLastType + 1;
                secondLastType = lastType;
                lastOccurrenceOfSecondLastType = windowEnd - 1;
                lastType = currentType;
            }

            int currentWindowLength = windowEnd - windowStart + 1;
            bestWindowLength = Math.max(bestWindowLength, currentWindowLength);
        }

        return bestWindowLength;
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                          | Time      | Space | Best For                                  | Limitations                                      |
     * |------------------------------------|-----------|-------|-------------------------------------------|---------------------------------------------------|
     * | 1. Brute Force                     | O(n^2)    | O(1)  | Warm-up / correctness oracle for testing   | Too slow for n = 10^5; never ship this as final    |
     * | 2. Sliding Window + HashMap         | O(n)      | O(1)  | The general, defensible, interview-ready   | Slight constant-factor overhead (hashing/boxing);  |
     * |                                     |           |       | answer; generalizes to K baskets           | not needed if K is fixed at exactly 2              |
     * | 3. Sliding Window, O(1) space (K=2) | O(n)      | O(1)  | Squeezing out max performance for the      | Specific to K=2; more state to track correctly;    |
     * |                                     |           |       | exact 2-basket version of this problem     | doesn't generalize to K baskets as cleanly          |
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 2 (Sliding Window + HashMap) first, as the
     * primary solution, for these reasons:
     *
     *   - Clarity: the "expand right, shrink left while invalid" pattern is
     *     immediately recognizable to the interviewer as the correct
     *     paradigm, and the map makes the invariant ("at most 2 distinct
     *     keys") trivial to state and verify out loud.
     *   - Coding speed: it's fast to write correctly under pressure — fewer
     *     edge-case branches than Approach 3's three-way if/else-if/else.
     *   - Interviewer expectations: for LeetCode 904 (a Medium), O(n) time
     *     with a clean, well-justified implementation is exactly what's
     *     expected. It already hits optimal time complexity.
     *   - Optimality & extensibility: if the interviewer follows up with
     *     "what if there were K baskets instead of 2?", Approach 2
     *     generalizes with a single constant change, whereas Approach 3
     *     would need a redesign. That flexibility itself is a strong signal
     *     to demonstrate.
     *
     * I would then proactively mention Approach 3 as a "since we know K is
     * fixed at exactly 2, here's how I'd shave off the map overhead for O(1)
     * true space and fewer constant-factor costs" follow-up — showing
     * depth without over-engineering the first pass.
     *
     * For the Deep Dive below, I'll implement Approach 2 as the polished,
     * production-quality version, since it's the one I'd actually want
     * merged/shipped, and I'll include Approach 3 fully above as the
     * "optimized follow-up" I'd offer live.
     */

    /**
     * ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ========================================================================
     *
     * Polished version of Approach 2: Sliding Window + HashMap.
     *
     * Returns the maximum number of fruits collectible from a contiguous
     * run of trees, given exactly two baskets, each restricted to a single
     * fruit type but unlimited quantity.
     *
     * @param fruits fruit type grown at each tree, indexed left-to-right.
     *               Must be non-null. May be empty (returns 0 in that case).
     * @return the length of the longest contiguous subarray of `fruits`
     *         containing at most 2 distinct values (i.e., max fruit count
     *         collectible under the 2-basket rule).
     * @throws NullPointerException if fruits is null (fail fast rather than
     *         silently returning a misleading 0 — a null array is a caller
     *         bug, not a valid "empty farm" input).
     */
    public static int maxFruitsCollected(int[] fruits) {
        Objects.requireNonNull(fruits, "fruits array must not be null");

        final int treeCount = fruits.length;
        if (treeCount == 0) {
            return 0; // Defensive: no trees, no fruit, by definition.
        }

        // Named constant instead of a magic number — makes it obvious this
        // solution generalizes to "K baskets" by changing one line, and
        // documents *why* the number 2 appears throughout the method.
        final int basketCapacityInDistinctTypes = 2;

        // Frequency count of each fruit type currently inside our window.
        // At most (basketCapacityInDistinctTypes + 1) keys will ever be
        // present at once, since we shrink the instant we exceed the limit.
        Map<Integer, Integer> typeCountsInWindow = new HashMap<>();

        int windowStart = 0;
        int longestValidWindowLength = 0;

        for (int windowEnd = 0; windowEnd < treeCount; windowEnd++) {
            int incomingFruitType = fruits[windowEnd];

            // Admit the new tree's fruit into the window unconditionally;
            // we'll correct the window afterward if this made it invalid.
            typeCountsInWindow.merge(incomingFruitType, 1, Integer::sum);

            // Shrink from the left until at most `basketCapacityInDistinctTypes`
            // distinct fruit types remain in the window. Each tree is added
            // once and removed at most once across the entire run, so this
            // inner loop contributes only amortized O(1) per outer iteration.
            while (typeCountsInWindow.size() > basketCapacityInDistinctTypes) {
                int outgoingFruitType = fruits[windowStart];
                int remainingCount = typeCountsInWindow.get(outgoingFruitType) - 1;

                if (remainingCount == 0) {
                    // Fully evict this type so the map's size accurately
                    // reflects the number of DISTINCT types in the window.
                    typeCountsInWindow.remove(outgoingFruitType);
                } else {
                    typeCountsInWindow.put(outgoingFruitType, remainingCount);
                }
                windowStart++;
            }

            // The window [windowStart, windowEnd] is now guaranteed valid.
            int currentWindowLength = windowEnd - windowStart + 1;
            if (currentWindowLength > longestValidWindowLength) {
                longestValidWindowLength = currentWindowLength;
            }
        }

        return longestValidWindowLength;
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing maxFruitsCollected() on Example 5 from Section 3:
     *   fruits = [1, 2, 3, 2, 2]   (indices:  0  1  2  3  4)
     *
     * Initial state: windowStart = 0, typeCountsInWindow = {}, best = 0
     *
     * windowEnd = 0, fruit = 1
     *   merge -> typeCountsInWindow = {1:1}
     *   size() = 1, not > 2, no shrink
     *   currentWindowLength = 0 - 0 + 1 = 1
     *   best = max(0, 1) = 1
     *
     * windowEnd = 1, fruit = 2
     *   merge -> typeCountsInWindow = {1:1, 2:1}
     *   size() = 2, not > 2, no shrink
     *   currentWindowLength = 1 - 0 + 1 = 2
     *   best = max(1, 2) = 2
     *
     * windowEnd = 2, fruit = 3
     *   merge -> typeCountsInWindow = {1:1, 2:1, 3:1}
     *   size() = 3, > 2 -> shrink:
     *     outgoingFruitType = fruits[0] = 1, remainingCount = 1-1 = 0
     *       -> remove key 1. typeCountsInWindow = {2:1, 3:1}
     *       windowStart = 1
     *     size() = 2, stop shrinking
     *   currentWindowLength = 2 - 1 + 1 = 2
     *   best = max(2, 2) = 2
     *
     * windowEnd = 3, fruit = 2
     *   merge -> typeCountsInWindow = {2:2, 3:1}
     *   size() = 2, not > 2, no shrink
     *   currentWindowLength = 3 - 1 + 1 = 3
     *   best = max(2, 3) = 3
     *
     * windowEnd = 4, fruit = 2
     *   merge -> typeCountsInWindow = {2:3, 3:1}
     *   size() = 2, not > 2, no shrink
     *   currentWindowLength = 4 - 1 + 1 = 4
     *   best = max(3, 4) = 4
     *
     * Final answer: 4  (window [1..4] = [2,3,2,2], types {2,3})
     *
     * This matches our hand-derived expectation from Section 3, and
     * confirms the optimal window does NOT need to start at index 0.
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Approach 1 (Brute Force, O(n^2)) is only useful as a correctness
     *   oracle for small inputs during development/testing; it will not
     *   scale to n = 10^5 within typical time limits.
     * - Approach 2 (Sliding Window + HashMap, O(n) time / O(1) space) is the
     *   approach I'd present and ship: optimal time complexity, easy to
     *   verify correctness of the invariant, and trivially generalizes to
     *   "K baskets" by parameterizing the distinct-type limit.
     * - Approach 3 (Sliding Window, true O(1) space, K=2 specialized) trades
     *   a bit of code complexity/fragility for removing the HashMap
     *   overhead entirely — a good "and here's how I'd optimize further"
     *   follow-up once Approach 2 is working.
     * - Known assumptions/limitations of the final solution
     *   (maxFruitsCollected): assumes a non-null input array; treats fruit
     *   type labels as arbitrary integers (works correctly regardless of
     *   their range, since we use a HashMap rather than an array indexed by
     *   fruit type); assumes a single-threaded, one-shot (non-streaming)
     *   input.
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if there were K baskets instead of exactly 2?"
     *    -> Change basketCapacityInDistinctTypes to K; Approach 2 needs no
     *       other modification. Approach 3 would need to track K types and
     *       K last-occurrence indices (more naturally done with a
     *       LinkedHashMap acting as an ordered "least-recently-used" cache).
     *
     * 2. "What if fruits could be removed/added to the row dynamically
     *     (an online/streaming variant), and we need the current answer
     *     after each update?"
     *    -> That's a much harder "sliding window over a dynamic sequence"
     *       problem; likely needs a balanced structure (e.g., an order-
     *       statistics tree or a Fenwick/segment tree over positions) to
     *       support insert/delete plus max-valid-window queries in
     *       sublinear time.
     *
     * 3. "What if we wanted to return the actual subarray (indices), not
     *     just its length?"
     *    -> Track bestWindowStart/bestWindowEnd alongside longestValidWindowLength,
     *       updating them whenever we find a new best, exactly as in
     *       Approach 2/3 (trivial O(1) extra bookkeeping).
     *
     * 4. "What if fruit types could be negative, or the array could contain
     *     up to 10^9 elements (too large to fit in memory)?"
     *    -> Negative types: no change needed, since we use a HashMap keyed
     *       by boxed Integer, not an array indexed by type value.
     *       10^9 elements: would need a streaming/external approach reading
     *       the array in chunks, since O(n) time is still fine but we can't
     *       hold it all in memory — though our O(1) auxiliary space means
     *       we could process it in a single left-to-right streaming pass
     *       without ever storing the whole array, if allowed.
     *
     * 5. "Can you do this without extra space at all — not even a map?"
     *    -> Yes — that's exactly Approach 3, specialized for K=2, using a
     *       constant number of int variables instead of a map.
     *
     * 6. "What if we wanted the K-th longest valid window, not just the
     *     longest?"
     *    -> Would need to track all valid window lengths (e.g., in a
     *       min-heap of size K) rather than a single running maximum,
     *       changing the space complexity to O(K) and adding O(log K) work
     *       per window update.
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Assuming the best window must start at index 0. As shown in
     *    Example 5, the optimal contiguous run can start anywhere in the
     *    array — candidates who only try prefixes get the wrong answer.
     *
     * 2. Off-by-one in window length: forgetting the "+1" in
     *    `windowEnd - windowStart + 1` (inclusive-inclusive range), which
     *    silently undercounts every window by one.
     *
     * 3. Forgetting to fully REMOVE a key from the frequency map once its
     *    count hits zero (rather than leaving it at 0). If you leave a
     *    stale zero-count key in the map, `typeCountsInWindow.size()` will
     *    over-report the number of distinct types still present, causing
     *    the window to shrink far more aggressively than necessary and
     *    producing an answer that's too small.
     *
     * 4. In the O(1)-space specialized version (Approach 3): mishandling
     *    the very first "only one type seen so far" state (forgetting the
     *    sentinel case), or jumping windowStart to the wrong index (it must
     *    be `lastOccurrenceOfSecondLastType + 1`, not
     *    `lastOccurrenceOfSecondLastType`) — an off-by-one here silently
     *    keeps one extra stale tree in the window, corrupting all
     *    subsequent length calculations.
     */

    /*
     * ========================================================================
     * TEST HARNESS — cross-validates all three approaches against each other
     * and against hand-computed expected answers from Section 3.
     * ========================================================================
     */
    public static void main(String[] args) {
        List<TestCase> testCases = List.of(
                new TestCase("Normal case", new int[]{1, 2, 1}, 3),
                new TestCase("Forces a cut", new int[]{0, 1, 2, 2}, 3),
                new TestCase("Single tree", new int[]{5}, 1),
                new TestCase("All same type", new int[]{4, 4, 4, 4}, 4),
                new TestCase("Optimal window not at index 0", new int[]{1, 2, 3, 2, 2}, 4),
                new TestCase("Empty array (defensive)", new int[]{}, 0),
                new TestCase("Two trees, two types", new int[]{7, 9}, 2),
                new TestCase("Long alternating run", new int[]{3, 3, 3, 1, 2, 1, 1, 2, 3, 3, 4}, 5)
        );

        int failureCount = 0;

        for (TestCase testCase : testCases) {
            int bruteForceResult = maxFruitsBruteForce(testCase.fruits);
            int hashMapResult = maxFruitsSlidingWindowHashMap(testCase.fruits);
            int optimizedResult = maxFruitsSlidingWindowOptimized(testCase.fruits);
            int productionResult = maxFruitsCollected(testCase.fruits);

            boolean allAgree = bruteForceResult == testCase.expected
                    && hashMapResult == testCase.expected
                    && optimizedResult == testCase.expected
                    && productionResult == testCase.expected;

            System.out.printf(
                    "[%s] fruits=%s expected=%d bruteForce=%d hashMapWindow=%d optimizedWindow=%d production=%d -> %s%n",
                    testCase.name,
                    Arrays.toString(testCase.fruits),
                    testCase.expected,
                    bruteForceResult,
                    hashMapResult,
                    optimizedResult,
                    productionResult,
                    allAgree ? "PASS" : "FAIL"
            );

            if (!allAgree) {
                failureCount++;
            }
        }

        System.out.println();
        if (failureCount == 0) {
            System.out.println("All test cases passed across all four implementations.");
        } else {
            System.out.println(failureCount + " test case(s) FAILED — see above.");
        }
    }

    /** Simple immutable holder for a test case: name, input, expected output. */
    private record TestCase(String name, int[] fruits, int expected) {
    }


    class Solution {

        /**
         * Returns the maximum number of fruits that can be collected.
         *
         * Sliding Window:
         * - Expand the window by moving 'right'.
         * - If more than two fruit types exist,
         * shrink from the left.
         *
         * Time : O(n)
         * Space : O(1) because at most 3 keys exist
         * (temporarily before shrinking).
         */
        public int totalFruit(int[] fruits) {

            // Stores frequency of each fruit type inside current window.
            Map<Integer, Integer> frequency = new HashMap<>();

            int left = 0;
            int maxFruits = 0;

            for (int right = 0; right < fruits.length; right++) {

                // Include current fruit into the window.
                frequency.merge(fruits[right], 1, Integer::sum);

                /*
                 * If window contains more than two fruit types,
                 * shrink it until it becomes valid again.
                 */
                while (frequency.size() > 2) {

                    int leftFruit = fruits[left];

                    frequency.put(leftFruit,
                            frequency.get(leftFruit) - 1);

                    // Remove fruit type completely if count becomes zero.
                    if (frequency.get(leftFruit) == 0) {
                        frequency.remove(leftFruit);
                    }

                    left++;
                }

                // Current window is always valid.
                maxFruits = Math.max(maxFruits, right - left + 1);
            }

            return maxFruits;
        }
    }

}
