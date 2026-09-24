import java.util.*;

/* =====================================================================================
 * GOOGLE ONSITE MOCK INTERVIEW TRANSCRIPT
 * Problem: Get the Maximum Score (path merge over two sorted arrays of distinct ints)
 * Language: Java 24
 * =====================================================================================
 */
public class MaximumScorePath {

    private static final long MOD = 1_000_000_007L;

    /* =================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ---------------------------------------------------------------------------------
     * We are given two arrays, nums1 and nums2, each sorted in strictly increasing
     * order, and each containing distinct integers within itself (an integer may
     * still appear in BOTH arrays — those are the "common elements").
     *
     * We build a single traversal path:
     *   - Start at index 0 of EITHER nums1 or nums2 (our choice).
     *   - Walk strictly left-to-right through the chosen array.
     *   - Whenever the current value also exists in the OTHER array (a common
     *     element, not necessarily at the same index), we may optionally "jump"
     *     to that value's position in the other array and keep walking forward
     *     from there. We are not forced to jump — staying is also legal.
     *   - Every distinct value visited along the path contributes to the score.
     *     A common element is counted only ONCE even though it conceptually
     *     "belongs" to both arrays.
     *
     * Goal: maximize the total sum of visited values, i.e., maximum path score,
     * returned modulo 1_000_000_007 because the raw sum could overflow reasonable
     * bounds if arrays are long and values are large.
     *
     * Key assumptions from the prompt:
     *   - Both arrays are already sorted ascending.
     *   - Each individual array has distinct elements (no internal duplicates).
     *   - Cross-array duplicates (common elements) are exactly the "switch points."
     *
     * This is functionally identical to LeetCode 1537 "Get the Maximum Score."
     * =================================================================================
     */

    /* =================================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * ---------------------------------------------------------------------------------
     * 1. Q: What are the array length bounds (n = nums1.length, m = nums2.length)?
     *    A (assumed): 1 <= n, m <= 10^5, so we need an O(n + m) or O((n+m) log(n+m))
     *       solution; anything exponential is a discussion-only approach.
     *
     * 2. Q: What is the value range for elements?
     *    A (assumed): 1 <= nums1[i], nums2[i] <= 10^7. Sums can exceed 32-bit range,
     *       so we accumulate in `long` and only reduce mod 1e9+7 at the very end
     *       (since scores are non-negative, no negative-mod correction is needed).
     *
     * 3. Q: Can either array be empty?
     *    A (assumed): No, per typical constraints n, m >= 1. I will still defensively
     *       handle empty arrays in code.
     *
     * 4. Q: Are elements strictly positive, or can they be zero/negative?
     *    A (assumed): Strictly positive (1 <= value). This matters because it means
     *       "greedily take the larger running sum before a merge point" is always
     *       correct — there's no incentive to ever discard a positive contribution
     *       except by choosing the smaller of two mutually exclusive branch sums.
     *
     * 5. Q: Can the same value appear in nums1 more than once (internal duplicates)?
     *    A (assumed): No — each array individually holds distinct values (stated in
     *       the prompt). This guarantees a common value corresponds to exactly one
     *       index in each array, so "switch points" are unambiguous.
     *
     * 6. Q: If a common element exists, must we switch, or is staying allowed?
     *    A (assumed): Staying is allowed. The switch is optional and only taken when
     *       it improves the total score (that's exactly why this is an optimization
     *       problem and not a fixed simulation).
     *
     * 7. Q: Do we need to reconstruct the actual path, or just the numeric score?
     *    A (assumed): Just the maximum score. (I'll mention path reconstruction as a
     *       natural follow-up.)
     *
     * 8. Q: Is this a single-threaded, single-call problem, or would it be invoked
     *       repeatedly on streaming/concurrent input?
     *    A (assumed): Single call, single-threaded, offline batch input — no
     *       concurrency concerns for the core solution.
     * =================================================================================
     */

    /* =================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ---------------------------------------------------------------------------------
     * Example 1 (Normal case, one crossing point):
     *   nums1 = [2, 4, 5, 8, 10]
     *   nums2 = [4, 6, 8, 9]
     *   Common elements: 4 and 8.
     *   Best path: start nums1 -> 2 -> 4 (switch to nums2, since path to 4 via nums2
     *   so far is just {4} but staying gives {2,4}; take the larger prefix) -> 6
     *   -> 8 (switch back to nums1's continuation if better) -> 10.
     *   Walking it precisely with the two-pointer algorithm gives score 30.
     *
     * Example 2 (Edge case: no common elements at all):
     *   nums1 = [1, 3, 5]
     *   nums2 = [2, 4, 6]
     *   No switching is ever possible, so the answer is simply
     *   max(sum(nums1), sum(nums2)) = max(9, 12) = 12.
     *   This validates that the algorithm degrades gracefully to "pick the bigger
     *   array's total" when there are zero merge points.
     *
     * Example 3 (Boundary / tie-breaking case: running sums equal at a merge point):
     *   nums1 = [2, 3, 7]
     *   nums2 = [1, 4, 7]
     *   Common element: 7 (only one).
     *   Tracing the merge: nums1 contributes {2, 3} = 5 before reaching 7; nums2
     *   contributes {1, 4} = 5 before reaching 7. The two running sums are EXACTLY
     *   equal (5 == 5) at the moment we hit the shared value 7. Since both segments
     *   are mutually exclusive alternatives that tie, max(5, 5) + 7 = 12 is correct
     *   regardless of which side we "credit" — this is the genuine tie-breaking
     *   boundary case: Math.max simply returns either operand when they're equal,
     *   and correctness does not depend on which one is chosen.
     * =================================================================================
     */

    /* =================================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * (Paradigms considered but NOT applicable, with justification:)
     *   - Sorting-based: N/A as a standalone approach — inputs are already sorted;
     *     re-sorting would be wasted work and would destroy the index alignment
     *     we rely on for detecting common elements via two pointers.
     *   - Heap / Priority Queue: N/A — there's no "top-k" or streaming-min/max
     *     selection happening; every element on the chosen path must be taken,
     *     not selectively popped.
     *   - Tree / Graph traversal (explicit): Technically you COULD model this as a
     *     DAG (nodes = array positions, edges = "advance" and "switch") and run a
     *     longest-path DP over it, but since the DAG is a straight line with only
     *     branch points at common elements, this collapses exactly into the DP /
     *     two-pointer approaches below — modeling it as an explicit graph adds
     *     overhead without new insight, so I will not implement it separately.
     *   - Binary search: Could be used to locate whether nums1[i] exists in nums2
     *     (O(log m) per lookup), but since both arrays are sorted and we scan them
     *     once anyway, a two-pointer walk finds common elements in O(1) amortized
     *     per step — binary search would only add a log factor with no benefit.
     *   - Monotonic stack / deque: N/A — there is no "next greater/smaller element"
     *     structure or sliding window constraint here.
     *   - Trie / segment tree: N/A — no prefix/range query structure is needed;
     *     we only need O(1) lookups of "is this value shared," which sorted
     *     two-pointer traversal already gives for free.
     * =================================================================================
     */

    /* ---------------------------------------------------------------------------------
     * Approach 1: Brute Force Recursive Backtracking (Exhaustive Path Exploration)
     * -----------------------------------------------------------------------------
     * Core idea: Literally try every legal path. Start recursion once from nums1[0]
     * and once from nums2[0]. At every index, if the current value also exists in
     * the other array, branch into two recursive calls: "stay" and "switch." Take
     * the max of all fully-explored paths.
     *
     * Data structure / paradigm: Plain recursion / backtracking, brute-force
     * exhaustive search over an implicit binary decision tree.
     *
     * Time Complexity: O(2^C * (n + m)) where C = number of common elements,
     *   because every common element doubles the number of explored path variants,
     *   and each explored path can cost O(n + m) to walk to completion.
     * Space Complexity: O(n + m) for recursion depth (call stack), ignoring the
     *   exponential blowup in total calls made.
     *
     * Pros: Trivial to reason about correctness; a great warm-up to state out loud
     *   in an interview to show you understand the problem before optimizing.
     * Cons: Exponential blowup — completely infeasible for n, m in the 10^4-10^5
     *   range that real constraints would specify.
     * When to use: Never in production; only as a correctness baseline / stepping
     *   stone toward the DP formulation, and useful for brute-force testing small
     *   inputs against the optimal solution.
     * ---------------------------------------------------------------------------------
     */
    static long bruteForceMaxScore(int[] nums1, int[] nums2) {
        if (nums1.length == 0 && nums2.length == 0) return 0;
        long best = Math.max(
                bruteForceHelper(nums1, nums2, 0, 0, new HashSet<>()),
                bruteForceHelper(nums2, nums1, 0, 1, new HashSet<>())
        );
        return best % MOD;
    }

    // whichStart: 0 means we started in nums1, 1 means we started in nums2 — only used
    // so the helper knows which array is "current" vs "other" at the call site.
    private static long bruteForceHelper(int[] current, int[] other, int currentIndex,
                                          int whichStart, Set<Integer> visitedValues) {
        if (currentIndex >= current.length) return 0; // ran off the end of current array
        int value = current[currentIndex];
        if (visitedValues.contains(value)) {
            // Already counted this value via the other branch's shared element — do not
            // double count; just continue forward on the current array.
            return bruteForceHelper(current, other, currentIndex + 1, whichStart, visitedValues);
        }
        Set<Integer> withValue = new HashSet<>(visitedValues);
        withValue.add(value);

        // Option A: stay on the current array and keep walking.
        long stayScore = value + bruteForceHelper(current, other, currentIndex + 1, whichStart, withValue);

        // Option B: if this value also exists in "other", we may switch there.
        int otherIndex = linearIndexOf(other, value);
        long switchScore = Long.MIN_VALUE;
        if (otherIndex != -1) {
            switchScore = value + bruteForceHelper(other, current, otherIndex + 1, whichStart, withValue);
        }
        return Math.max(stayScore, switchScore);
    }

    private static int linearIndexOf(int[] array, int target) {
        for (int index = 0; index < array.length; index++) {
            if (array[index] == target) return index;
        }
        return -1;
    }

    /* ---------------------------------------------------------------------------------
     * Approach 2: Hashing + Segment Simulation
     * -----------------------------------------------------------------------------
     * Core idea: Use a HashSet to precompute which values are common to both arrays
     * in O(n + m). Then do a single pass merging both arrays by value (like a merge
     * step in merge sort), accumulating running sums for each array, and whenever we
     * hit a common value, add max(runningSum1, runningSum2) + commonValue to the
     * answer and reset both running sums to zero. This is functionally the same
     * merge idea as the optimal approach, but explicitly leans on hashing to detect
     * "is this value common" rather than relying purely on pointer comparison.
     *
     * Data structure / paradigm: Hashing (HashSet for O(1) membership check) combined
     * with a linear merge scan.
     *
     * Time Complexity: O(n + m) — one pass to build the hash set of common values
     *   (or just check membership on the fly), one pass to merge & accumulate.
     * Space Complexity: O(min(n, m)) for the HashSet of one array's values used for
     *   fast common-element lookups.
     *
     * Pros: Very easy to explain ("mark which values are shared, then do one pass");
     *   robust even if arrays were NOT sorted (though here they are).
     * Cons: Uses O(min(n, m)) extra space for the hash set, which the optimal
     *   two-pointer approach avoids entirely since sorted order gives us common-
     *   element detection for free via direct comparison.
     * When to use: Good middle-ground answer if you want extra safety margin on
     *   correctness before shaving off the hash set in the final optimal pass, or if
     *   you were unsure whether input was guaranteed sorted.
     * ---------------------------------------------------------------------------------
     */
    static long hashingSimulationMaxScore(int[] nums1, int[] nums2) {
        Set<Integer> valuesInNums2 = new HashSet<>();
        for (int value : nums2) valuesInNums2.add(value);

        long result = 0;
        long runningSum1 = 0;
        long runningSum2 = 0;
        int pointer1 = 0;
        int pointer2 = 0;

        while (pointer1 < nums1.length && pointer2 < nums2.length) {
            int value1 = nums1[pointer1];
            int value2 = nums2[pointer2];
            if (value1 < value2) {
                runningSum1 += value1;
                pointer1++;
            } else if (value1 > value2) {
                runningSum2 += value2;
                pointer2++;
            } else {
                // value1 == value2 -> confirmed common element via the hash-backed check
                // (valuesInNums2 lookup would fire here in a pure-hash variant; since
                // arrays are sorted, direct equality already tells us this is common).
                result += Math.max(runningSum1, runningSum2) + value1;
                runningSum1 = 0;
                runningSum2 = 0;
                pointer1++;
                pointer2++;
            }
        }
        // Drain whichever array still has leftover elements.
        while (pointer1 < nums1.length) { runningSum1 += nums1[pointer1]; pointer1++; }
        while (pointer2 < nums2.length) { runningSum2 += nums2[pointer2]; pointer2++; }

        result += Math.max(runningSum1, runningSum2);
        return result % MOD;
    }

    /* ---------------------------------------------------------------------------------
     * Approach 3: Top-Down Dynamic Programming with Memoization
     * -----------------------------------------------------------------------------
     * Core idea: Observe that between two consecutive common elements, the path is
     * completely FORCED — there is no branching, you just walk forward summing
     * values. Real decisions only happen AT common elements: stay or switch. So we
     * define state as "(pointer position in nums1, pointer position in nums2)" at a
     * moment where both pointers are synchronized at a common value (or at the very
     * start). solve(i, j) = best achievable score from this synchronized checkpoint
     * to the end of both arrays. We memoize on (i, j) so each checkpoint pair is only
     * computed once.
     *
     * Data structure / paradigm: Dynamic Programming (top-down / memoized recursion).
     *
     * Time Complexity: O(n + m). Although this LOOKS like it could revisit (i, j)
     *   pairs, the memoization guarantees each of the O(min(n,m)) checkpoint states
     *   is computed once, and each computation does O(distance to next checkpoint)
     *   work, and those distances sum to O(n + m) overall (similar amortized
     *   argument to how merge sort's merge step is O(n+m) despite two pointers).
     * Space Complexity: O(n + m) for the memo map plus O(n + m) recursion depth.
     *
     * Pros: Demonstrates DP thinking explicitly (great if the interviewer specifically
     *   wants to hear "memoized recursion" as a checkpoint before the final greedy
     *   simplification); naturally extends if the problem were tweaked so that
     *   "switching" had a variable cost (making the greedy invariant break).
     * Cons: Recursion overhead (function call stack, boxing for memo keys) makes it
     *   strictly slower in practice than the flat iterative version for no added
     *   benefit here, since the optimal substructure collapses into a simple linear
     *   scan; risk of stack overflow on very large inputs (deep recursion).
     * When to use: Present this only if asked to "show the DP formulation" or if the
     *   problem is modified so segment costs are no longer simply additive (e.g., a
     *   switching penalty) — then genuine overlapping subproblems could exist and
     *   memoization would matter more.
     * ---------------------------------------------------------------------------------
     */
    private static Map<Long, Long> memoCache;

    static long dpMemoizedMaxScore(int[] nums1, int[] nums2) {
        memoCache = new HashMap<>();
        long result = dpSolve(nums1, nums2, 0, 0);
        return result % MOD;
    }

    // dpSolve(i, j): both pointers are at a synchronized checkpoint (start, or a
    // common element already credited by the caller). Returns best score from here on.
    private static long dpSolve(int[] nums1, int[] nums2, int indexInNums1, int indexInNums2) {
        long key = (((long) indexInNums1) << 32) ^ (indexInNums2 & 0xffffffffL);
        Long cached = memoCache.get(key);
        if (cached != null) return cached;

        long sumOverNums1 = 0;
        int pointer1 = indexInNums1;
        long sumOverNums2 = 0;
        int pointer2 = indexInNums2;

        // Walk forward on both simultaneously (conceptually two independent forks)
        // until we either exhaust one array or hit the next common value.
        while (pointer1 < nums1.length && pointer2 < nums2.length && nums1[pointer1] != nums2[pointer2]) {
            if (nums1[pointer1] < nums2[pointer2]) {
                sumOverNums1 += nums1[pointer1];
                pointer1++;
            } else {
                sumOverNums2 += nums2[pointer2];
                pointer2++;
            }
        }

        long remainder;
        if (pointer1 < nums1.length && pointer2 < nums2.length) {
            // Found the next common checkpoint at nums1[pointer1] == nums2[pointer2].
            int commonValue = nums1[pointer1];
            long future = dpSolve(nums1, nums2, pointer1 + 1, pointer2 + 1);
            remainder = commonValue + future;
        } else {
            // One array ran out first; drain whichever still has leftovers.
            while (pointer1 < nums1.length) { sumOverNums1 += nums1[pointer1]; pointer1++; }
            while (pointer2 < nums2.length) { sumOverNums2 += nums2[pointer2]; pointer2++; }
            remainder = 0;
        }

        long total = Math.max(sumOverNums1, sumOverNums2) + remainder;
        memoCache.put(key, total);
        return total;
    }

    /* ---------------------------------------------------------------------------------
     * Approach 4 (OPTIMAL): Two-Pointer Greedy Merge
     * -----------------------------------------------------------------------------
     * Core idea: Since both arrays are sorted, walk them simultaneously like the
     * merge step of merge sort. Maintain two running sums, one per array, since the
     * last synchronization point. Whenever the two pointers land on the SAME value
     * (a common element), that is a mandatory synchronization point: greedily commit
     * to whichever running sum is larger (since all values are positive, the larger
     * prefix sum can never hurt us), add the shared value once, and reset both
     * running sums to zero. At the very end, commit whichever running sum is larger
     * one final time. This greedy choice is provably safe because the two segments
     * between consecutive common elements are mutually exclusive alternatives — you
     * can only have walked ONE of them to reach the shared checkpoint, so always
     * picking the larger one is optimal, and it never affects any future segment
     * (future sums start fresh at zero regardless of which side you "came from").
     *
     * Data structure / paradigm: Two-pointer technique + Greedy choice at each merge
     * checkpoint (also equivalent to the optimal substructure exploited by the DP
     * above, but expressed iteratively with O(1) extra space).
     *
     * Time Complexity: O(n + m) — each pointer advances at most n and m times total,
     *   respectively, with strictly O(1) work per step.
     * Space Complexity: O(1) extra space (excluding the input and the output long).
     *
     * Pros: Optimal time AND space; no recursion, no hashing, no extra collections;
     *   trivial to reason about correctness once the greedy argument is accepted;
     *   the version I would actually submit and defend in a real onsite.
     * Cons: Requires the "greedy prefix choice is safe" insight, which needs to be
     *   articulated clearly (interviewers will probe this); relies on the guarantee
     *   that all values are positive (if negative values were allowed, "always take
     *   the larger running sum" could still hold, but the reasoning framing should
     *   explicitly acknowledge that assumption).
     * When to use: This is the production-quality answer for the stated constraints;
     *   always prefer this once you've verified the greedy invariant with the
     *   interviewer.
     * ---------------------------------------------------------------------------------
     */
    static long optimalTwoPointerMaxScore(int[] nums1, int[] nums2) {
        int pointer1 = 0;
        int pointer2 = 0;
        long runningSumNums1 = 0; // sum accumulated on nums1 since the last checkpoint
        long runningSumNums2 = 0; // sum accumulated on nums2 since the last checkpoint
        long totalScore = 0;

        while (pointer1 < nums1.length && pointer2 < nums2.length) {
            if (nums1[pointer1] < nums2[pointer2]) {
                runningSumNums1 += nums1[pointer1];
                pointer1++;
            } else if (nums1[pointer1] > nums2[pointer2]) {
                runningSumNums2 += nums2[pointer2];
                pointer2++;
            } else {
                // Common element found: commit to the better of the two running sums,
                // then count the shared value exactly once, and reset both sums.
                totalScore += Math.max(runningSumNums1, runningSumNums2) + nums1[pointer1];
                runningSumNums1 = 0;
                runningSumNums2 = 0;
                pointer1++;
                pointer2++;
            }
        }

        // Exactly one of these loops (possibly neither) will run — drain the tail of
        // whichever array was not fully consumed by the merge above.
        while (pointer1 < nums1.length) {
            runningSumNums1 += nums1[pointer1];
            pointer1++;
        }
        while (pointer2 < nums2.length) {
            runningSumNums2 += nums2[pointer2];
            pointer2++;
        }

        totalScore += Math.max(runningSumNums1, runningSumNums2);
        return totalScore % MOD;
    }

    /* =================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ---------------------------------------------------------------------------------
     * | Approach                          | Time              | Space     | Best For                          | Limitations                              |
     * |------------------------------------|-------------------|-----------|-----------------------------------|-------------------------------------------|
     * | 1. Brute Force Backtracking         | O(2^C * (n+m))    | O(n+m)    | Correctness baseline / teaching   | Exponential; unusable beyond tiny inputs   |
     * | 2. Hashing + Segment Simulation     | O(n+m)            | O(min(n,m))| Extra safety net, unsorted inputs| Wastes space vs. optimal; sorted-array gain unused |
     * | 3. Top-Down DP with Memoization     | O(n+m)            | O(n+m)    | Showing explicit DP structure     | Recursion overhead, stack depth risk       |
     * | 4. Two-Pointer Greedy Merge (BEST)  | O(n+m)            | O(1)      | Production / interview submission | Requires justifying the greedy invariant   |
     * =================================================================================
     */

    /* =================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ---------------------------------------------------------------------------------
     * I would present Approach 4 (Two-Pointer Greedy Merge) as my final submission.
     * Reasoning:
     *   - It is asymptotically optimal in BOTH time (O(n+m)) and space (O(1) extra),
     *     matching the theoretical lower bound (you must at least look at every
     *     element once).
     *   - It is fast to code correctly under interview time pressure — it's a single
     *     while-loop with three branches, no auxiliary data structures.
     *   - It demonstrates the key interview signal: recognizing that "sorted + need
     *     to detect shared elements" screams two-pointer merge, and that "maximize a
     *     sum over mutually exclusive positive-valued segments" screams greedy
     *     local-max-commit.
     *   - I would still mention Approaches 1-3 briefly out loud (brute force for
     *     correctness grounding, DP to show I recognize the optimal substructure,
     *     hashing as a fallback if sortedness weren't guaranteed) before converging
     *     on Approach 4 — this narrative is exactly what Google interviewers want to
     *     see: a progression of reasoning, not just a memorized final answer.
     * =================================================================================
     */

    /* =================================================================================
     * SECTION 9: DEEP DIVE — POLISHED OPTIMAL SOLUTION
     * ---------------------------------------------------------------------------------
     * (This mirrors optimalTwoPointerMaxScore above but is restated here, fully
     * commented, as the final production-quality submission I'd write on a whiteboard
     * or shared doc in the actual interview.)
     * =================================================================================
     */
    public static long getMaximumScore(int[] nums1, int[] nums2) {
        // Defensive handling: per problem statement both arrays have length >= 1, but
        // we guard anyway since production code should not assume unchecked invariants.
        if (nums1 == null) nums1 = new int[0];
        if (nums2 == null) nums2 = new int[0];

        int pointerInNums1 = 0;
        int pointerInNums2 = 0;

        // Running sums represent "the score accumulated on this array since the last
        // time we were forced to synchronize at a shared value." Only one of these
        // two segments can ever actually be walked in a real path, so at each sync
        // point we keep the larger one and discard the other — that's the core
        // greedy insight that makes this algorithm correct and O(1) in extra space.
        long scoreSinceLastSyncNums1 = 0;
        long scoreSinceLastSyncNums2 = 0;
        long totalScore = 0;

        while (pointerInNums1 < nums1.length && pointerInNums2 < nums2.length) {
            int valueInNums1 = nums1[pointerInNums1];
            int valueInNums2 = nums2[pointerInNums2];

            if (valueInNums1 < valueInNums2) {
                // nums1's current value cannot possibly be in nums2 yet (nums2 hasn't
                // reached it, and nums2 is sorted ascending), so it's safe to just
                // accumulate it and move on.
                scoreSinceLastSyncNums1 += valueInNums1;
                pointerInNums1++;
            } else if (valueInNums1 > valueInNums2) {
                scoreSinceLastSyncNums2 += valueInNums2;
                pointerInNums2++;
            } else {
                // valueInNums1 == valueInNums2: a genuine common element and the only
                // point at which the path could have legally switched arrays. Commit
                // to the larger of the two mutually exclusive segment sums, credit the
                // shared value exactly once, then reset both running sums to start the
                // next segment fresh.
                totalScore += Math.max(scoreSinceLastSyncNums1, scoreSinceLastSyncNums2) + valueInNums1;
                scoreSinceLastSyncNums1 = 0;
                scoreSinceLastSyncNums2 = 0;
                pointerInNums1++;
                pointerInNums2++;
            }
        }

        // At most one of the two arrays has leftover elements at this point; drain it.
        while (pointerInNums1 < nums1.length) {
            scoreSinceLastSyncNums1 += nums1[pointerInNums1];
            pointerInNums1++;
        }
        while (pointerInNums2 < nums2.length) {
            scoreSinceLastSyncNums2 += nums2[pointerInNums2];
            pointerInNums2++;
        }

        // Final commit: pick whichever tail segment is larger.
        totalScore += Math.max(scoreSinceLastSyncNums1, scoreSinceLastSyncNums2);

        return totalScore % MOD;
    }

    /* =================================================================================
     * SECTION 10: DRY RUN / TRACE
     * ---------------------------------------------------------------------------------
     * Tracing getMaximumScore on Example 1:
     *   nums1 = [2, 4, 5, 8, 10]   (indices 0..4)
     *   nums2 = [4, 6, 8, 9]       (indices 0..3)
     *
     * Initial state: pointerInNums1=0, pointerInNums2=0,
     *                scoreSinceLastSyncNums1=0, scoreSinceLastSyncNums2=0, totalScore=0
     *
     * Step 1: valueInNums1=2, valueInNums2=4 -> 2 < 4 -> accumulate into nums1's sum.
     *   State: scoreSinceLastSyncNums1=2, pointerInNums1=1
     *
     * Step 2: valueInNums1=4, valueInNums2=4 -> EQUAL (common element 4).
     *   Commit: totalScore += max(2, 0) + 4 = 6  ->  totalScore=6
     *   Reset: scoreSinceLastSyncNums1=0, scoreSinceLastSyncNums2=0
     *   Advance: pointerInNums1=2, pointerInNums2=1
     *
     * Step 3: valueInNums1=5, valueInNums2=6 -> 5 < 6 -> accumulate into nums1's sum.
     *   State: scoreSinceLastSyncNums1=5, pointerInNums1=3
     *
     * Step 4: valueInNums1=8, valueInNums2=6 -> 8 > 6 -> accumulate into nums2's sum.
     *   State: scoreSinceLastSyncNums2=6, pointerInNums2=2
     *
     * Step 5: valueInNums1=8, valueInNums2=8 -> EQUAL (common element 8).
     *   Commit: totalScore += max(5, 6) + 8 = 14  ->  totalScore = 6 + 14 = 20
     *   Reset: scoreSinceLastSyncNums1=0, scoreSinceLastSyncNums2=0
     *   Advance: pointerInNums1=4, pointerInNums2=3
     *
     * Step 6: valueInNums1=10, valueInNums2=9 -> 10 > 9 -> accumulate into nums2's sum.
     *   State: scoreSinceLastSyncNums2=9, pointerInNums2=4 (nums2 now exhausted)
     *
     * Loop ends (pointerInNums2 == nums2.length == 4).
     * Drain nums1's tail: value 10 remains -> scoreSinceLastSyncNums1 += 10 = 10,
     *   pointerInNums1=5 (nums1 now exhausted).
     * Drain nums2's tail: nothing left.
     *
     * Final commit: totalScore += max(10, 9) = 10  ->  totalScore = 20 + 10 = 30
     * Result: 30 % (1e9+7) = 30.
     *
     * This matches the intuitive path: 2 -> 4(switch) -> 6 -> 8(switch back) -> 10
     * i.e. 2+4+6+8+10 = 30. ✔
     * =================================================================================
     */

    /* =================================================================================
     * SECTION 11: CLOSING SUMMARY
     * ---------------------------------------------------------------------------------
     * - All four approaches agree on correctness; they trade off differently on time,
     *   space, and how explicitly they surface the underlying DP / greedy structure.
     * - The brute-force approach is exponential and exists purely to build intuition
     *   and to serve as a testing oracle for small random inputs.
     * - The hashing approach and the memoized DP approach are both O(n+m) but carry
     *   avoidable constant-factor and space overhead versus the final greedy merge.
     * - The two-pointer greedy merge is optimal in time and space, and is what I
     *   would defend as my final answer.
     * - Known assumptions baked into the optimal solution: arrays are pre-sorted
     *   ascending, values are non-negative (so "always keep the larger prefix sum"
     *   is unconditionally safe), and each array individually has no internal
     *   duplicates. If any of these assumptions were relaxed, I would revisit the
     *   greedy argument before trusting this implementation.
     * =================================================================================
     */

    /* =================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ---------------------------------------------------------------------------------
     * 1. "Can you reconstruct the actual path (sequence of values), not just the
     *    score?" -> Requires tracking, at each sync point, which side contributed
     *    the max, then replaying / storing pointers segment by segment.
     * 2. "What if the arrays were NOT sorted?" -> We would need to sort first
     *    (O((n+m) log(n+m))) or hash to detect common elements and reconstruct
     *    a DAG-based longest path, losing the O(n+m) / O(1) guarantees.
     * 3. "What if there could be more than two arrays to merge/switch between?"
     *    -> Generalizes to a k-way merge with a priority queue to always advance
     *      the smallest current pointer, and per-array running sums reset at any
     *      k-way common element; complexity becomes O(N log k).
     * 4. "What if switching arrays had a fixed cost (a 'toll')?" -> The simple
     *    greedy "always take the larger running sum" breaks, because sometimes
     *    it's better to NOT switch even at a common element if the toll outweighs
     *    the gain — this reintroduces genuine overlapping subproblems, and the
     *    memoized DP formulation (Approach 3) becomes the right lens again.
     * 5. "How would you handle streaming input where nums1/nums2 arrive
     *    incrementally?" -> We could maintain running sums online, but any
     *    "not yet arrived" common element blocks a final decision, so we would
     *    need to buffer up to the next confirmed sync point before finalizing
     *    a chunk of the score.
     * 6. "Can you do this with O(1) extra space AND without modifying the input
     *    arrays, if we could only use recursion?" -> Discuss tail-call style
     *    simulation vs. true iterative constant space; note the JVM does not
     *    guarantee tail-call optimization, so true O(1) auxiliary space favors
     *    the iterative version regardless.
     * =================================================================================
     */

    /* =================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ---------------------------------------------------------------------------------
     * 1. Double-counting the common element: forgetting that the shared value must
     *    be added exactly ONCE at the sync point, not once per array (an easy
     *    off-by-one style bug that silently inflates the answer).
     * 2. Forgetting the FINAL commit after the main loop: candidates often merge
     *    correctly at every common element but forget that after the last common
     *    element (or if there are zero common elements), you still need one final
     *    max(runningSum1, runningSum2) to account for the trailing segment.
     * 3. Assuming you must always switch at a common element: the switch is
     *    OPTIONAL. Some candidates write code that unconditionally jumps arrays at
     *    every shared value instead of comparing prefix sums, which silently
     *    produces a suboptimal (or just wrong) answer whenever staying was better.
     * 4. Overflow handling: using `int` accumulators instead of `long` before taking
     *    the modulus, causing silent overflow on large inputs with large values —
     *    the modulo operation must happen at the END on a `long` accumulation, not
     *    be sprinkled in mid-computation in a way that breaks the max() comparisons
     *    (you must never take an element-wise mod before comparing two running
     *    sums with Math.max, or you can corrupt the greedy comparison).
     * =================================================================================
     */

    /* =================================================================================
     * MAIN METHOD: Local verification harness (compile & run with `javac` / `java`)
     * =================================================================================
     */
    public static void main(String[] args) {
        // Example 1: Normal case, two crossing points.
        int[] example1Nums1 = {2, 4, 5, 8, 10};
        int[] example1Nums2 = {4, 6, 8, 9};
        long expected1 = 30;
        checkAllApproaches("Example 1 (normal)", example1Nums1, example1Nums2, expected1);

        // Example 2: Edge case, no common elements at all.
        int[] example2Nums1 = {1, 3, 5};
        int[] example2Nums2 = {2, 4, 6};
        long expected2 = 12;
        checkAllApproaches("Example 2 (no common elements)", example2Nums1, example2Nums2, expected2);

        // Example 3: Boundary / tie-breaking case -- running sums equal (5 == 5) at the
        // single common element 7, verifying Math.max's tie behavior is harmless.
        int[] example3Nums1 = {2, 3, 7};
        int[] example3Nums2 = {1, 4, 7};
        long expected3 = 12; // max(5,5) + 7 = 12
        checkAllApproaches("Example 3 (tie/boundary)", example3Nums1, example3Nums2, expected3);

        System.out.println("All assertions passed across all four approaches.");
    }

    private static void checkAllApproaches(String label, int[] nums1, int[] nums2, long expected) {
        long bruteForceResult = bruteForceMaxScore(nums1, nums2);
        long hashingResult = hashingSimulationMaxScore(nums1, nums2);
        long dpResult = dpMemoizedMaxScore(nums1, nums2);
        long optimalResult = getMaximumScore(nums1, nums2);

        System.out.printf(
                "%s -> brute=%d, hashing=%d, dp=%d, optimal=%d (expected=%d)%n",
                label, bruteForceResult, hashingResult, dpResult, optimalResult, expected
        );

        assert bruteForceResult == expected : label + " brute force mismatch";
        assert hashingResult == expected : label + " hashing approach mismatch";
        assert dpResult == expected : label + " dp approach mismatch";
        assert optimalResult == expected : label + " optimal approach mismatch";
    }
}
