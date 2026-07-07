import java.util.Random;

/*
====================================================================================================
 MOCK GOOGLE ONSITE INTERVIEW TRANSCRIPT
 Problem: Longest Subarray of 1's After Removing Exactly One Element
 (LeetCode-style, Google-tagged, Medium difficulty)
====================================================================================================
*/

public class LongestSubarrayOfOnesAfterRemovingOneElement {

    /*
    ================================================================================================
    SECTION 1: RESTATE THE PROBLEM
    ================================================================================================
    In plain English:

        We are given a binary array `nums` (every element is either 0 or 1). We are REQUIRED to
        remove exactly one element from the array -- no matter what that element's value is, even
        if it's a 1 that would otherwise "help" us, and even if the array only has a single 1 and
        nothing else. After removing that one element, we look at the resulting array and find the
        longest contiguous run (subarray) made entirely of 1's. We return the length of that run.
        If, after the mandatory removal, there is no non-empty run of 1's at all (e.g. the array
        becomes empty, or every remaining element is 0), we return 0.

    Key constraints / inputs / outputs:
        - Input: int[] nums, where each nums[i] is 0 or 1.
        - We MUST delete exactly one element (this is not optional, and it is exactly one -- not
          "at most one").
        - Output: a single int -- the length of the longest contiguous block of 1's in the array
          that remains after deleting that one element.
        - If nums.length == 1, deleting the only element leaves an empty array -> answer is 0
          regardless of whether that element was 0 or 1.

    Assumptions I'm stating out loud to the interviewer:
        - "Binary array" means every value is strictly 0 or 1 (no other integers).
        - We want the length of the run, not the run itself (no need to return indices or the
          subarray contents).
        - "Subarray" means contiguous elements, not an arbitrary subsequence.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 2: CLARIFYING QUESTIONS (asked to the interviewer, with assumed answers)
    ================================================================================================
    1. Q: What is the maximum size of nums? Do we need to worry about very large inputs (10^5, 10^6)?
       A (assumed): Up to ~10^5 elements. Solution should be O(n) or O(n log n) to be safe.

    2. Q: Can nums be empty (length 0)?
       A (assumed): No, nums.length >= 1 is guaranteed by the problem constraints.

    3. Q: Are the only valid values 0 and 1, or could there be other integers I need to validate/reject?
       A (assumed): Guaranteed binary array; no need to validate input values.

    4. Q: Is "remove exactly one element" truly mandatory even if nums is all 1's or has only one
          element?
       A (assumed): Yes -- exactly one removal always happens, no exceptions.

    5. Q: If there are multiple runs of 1's tied for the longest length after the best possible
          removal, do we need to report which one, or just the length?
       A (assumed): Just the length -- ties don't matter since all we return is an integer.

    6. Q: Should the solution be a single pass over the array, or is a small constant number of
          passes (e.g., 2-3) acceptable?
       A (assumed): A single pass (or a small constant number of linear passes) is preferred; O(n)
       time is the bar to clear, and O(1) extra space is a bonus but not strictly required.

    7. Q: Is nums provided as a mutable array I'm allowed to modify in place, or should I treat it
          as read-only?
       A (assumed): Treat it as read-only; don't mutate the caller's array.

    8. Q: Is this a single-threaded, single-call function, or do we need to worry about concurrent
          calls / streaming input?
       A (assumed): Single-threaded, one-shot function call. No concurrency concerns.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 3: EXAMPLES & EDGE CASES (manually verified by hand)
    ================================================================================================

    Example 1 (NORMAL CASE): nums = [1, 1, 0, 1, 1]
        There is exactly one zero, at index 2.
        - Remove index 2 (the 0)      -> [1,1,1,1]      -> longest run of 1's = 4
        - Remove index 0 (a 1)        -> [1,0,1,1]      -> longest run = 2
        - Remove index 3 or 4 (a 1)   -> [1,1,0,1] etc. -> longest run <= 3
        Best answer: 4 (achieved by removing the single zero).

    Example 2 (EDGE CASE: all ones, no zero to remove):
        nums = [1, 1, 1]
        We are still forced to remove exactly one element even though every element is "useful".
        Removing any single 1 leaves exactly 2 ones. Answer: 2 (= n - 1).
        This edge case matters because it's easy to forget that "no zero present" still forces a
        real removal, shrinking the answer by 1 from the full array length.

    Example 3 (BOUNDARY / TIE-BREAKING CASE: two zeros, must choose the better one to remove):
        nums = [1, 1, 0, 1, 0, 1, 1]
        Zeros sit at index 2 and index 4.
        - Remove index 2 (0) -> [1,1,1,0,1,1] -> longest run = 3 (the leading "1,1,1")
        - Remove index 4 (0) -> [1,1,0,1,1,1] -> longest run = 3 (the trailing "1,1,1")
        - Remove index 3 (the lone 1 between the zeros) -> [1,1,0,0,1,1] -> longest run = 2
        Both zero-removal choices tie at 3, and no other removal beats that. Answer: 3.
        This example is important because it shows the algorithm must correctly "let go of" an old
        zero from its window when a second zero enters, i.e., handle more than one zero in view.

    Extra tiny edge cases worth mentioning out loud:
        - nums = [0]           -> removing the only element leaves [] -> answer 0
        - nums = [1]           -> removing the only element leaves [] -> answer 0
        - nums = [0, 0, 0]     -> no matter which 0 we remove, remaining elements are all 0 -> 0
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 4 & 5: ALL POSSIBLE APPROACHES
    ================================================================================================

    Paradigms considered and explicitly SKIPPED, with one-line justification:
        - Sorting-based:     Sorting destroys the contiguity/order of the array, which is exactly
                              the property we need to preserve (we care about RUNS), so sorting is
                              actively harmful here.
        - Hashing-based:     There's no "look up a complement" or "count occurrences" structure to
                              exploit; hashing doesn't help track contiguous runs.
        - Divide & conquer:  You could split at the midpoint and combine, similar to max-subarray
                              D&C, but it adds O(log n) recursion overhead and complexity for zero
                              benefit over the O(n) sliding window below -- not worth it in practice.
        - Tree / graph:      No graph/tree structure is implied by the problem; nothing to traverse.
        - Heap / priority q: No "top-K" or ordering-by-priority need exists here.
        - Binary search:     There's no monotonic predicate over a sorted search space to binary
                              search on (the answer doesn't have a clean monotonic "feasibility"
                              check the way, say, "minimum capacity to ship packages" does).
        - Monotonic stack:   No nested/ordering relationship between elements that a monotonic
                              stack would exploit (that pattern fits problems like "next greater
                              element", not contiguous binary run problems).
        - Trie / segment tree: Way overkill for a single linear-scan problem with no range-update or
                              range-query requirement across multiple queries.

    Paradigms that DO genuinely apply, covered in full below:
        - Brute force / naive
        - Dynamic programming (precompute run-lengths from both directions)
        - Two-pointer / sliding window (the optimal solution; also has a greedy flavor -- we
          greedily keep the window as large as possible while allowing "at most one zero" inside it)
    ================================================================================================
    */

    /*
    ------------------------------------------------------------------------------------------------
    APPROACH 1: Brute Force / Naive -- "Try removing every index, rescan for the longest run"
    ------------------------------------------------------------------------------------------------
    Core idea (plain English):
        For every possible index we could remove, simulate the resulting array by simply skipping
        that index while scanning, and track the longest consecutive run of 1's seen. Take the max
        across all n choices of removal index.

    Data structure / paradigm: none beyond simple counters -- this is pure brute force enumeration.

    Time Complexity: O(n^2) -- for each of the n candidate removal indices, we do an O(n) scan.
    Space Complexity: O(1) extra space (we never materialize a new array; we just skip an index
                       while scanning the original).

    Pros:
        - Trivial to reason about and verify correct; great as a correctness oracle.
        - No tricky edge-case logic around "shrinking a window".
    Cons:
        - O(n^2) is too slow for n up to ~10^5 (10^10 operations) -- would time out.
        - Doesn't scale, not something you'd ship, but perfect as a sanity check against faster
          solutions during development/testing.

    When to use in practice:
        - Never in production for large inputs; but very useful in an interview as a "let me start
          with something correct, then optimize" opening move, and as a test oracle.
    ------------------------------------------------------------------------------------------------
    */
    static int bruteForce(int[] nums) {
        int n = nums.length;
        int bestOverall = 0;

        // Try removing each index exactly once.
        for (int removeIndex = 0; removeIndex < n; removeIndex++) {
            int currentRunLength = 0;
            int bestForThisRemoval = 0;

            for (int i = 0; i < n; i++) {
                if (i == removeIndex) {
                    // Simulate deletion by simply skipping this position.
                    continue;
                }
                if (nums[i] == 1) {
                    currentRunLength++;
                    bestForThisRemoval = Math.max(bestForThisRemoval, currentRunLength);
                } else {
                    currentRunLength = 0;
                }
            }
            bestOverall = Math.max(bestOverall, bestForThisRemoval);
        }
        return bestOverall;
    }

    /*
    ------------------------------------------------------------------------------------------------
    APPROACH 2: Dynamic Programming -- "Precompute consecutive-ones runs from both directions"
    ------------------------------------------------------------------------------------------------
    Core idea (plain English):
        For every index i, precompute:
            leftOnesEndingAt[i]  = length of the run of 1's ending exactly at i (inclusive)
            rightOnesStartingAt[i] = length of the run of 1's starting exactly at i (inclusive)
        Then, for every index i where nums[i] == 0, "removing that zero" would stitch together the
        run of 1's immediately to its left with the run of 1's immediately to its right. The
        candidate answer for removing that zero is leftOnesEndingAt[i-1] + rightOnesStartingAt[i+1].
        We take the max of this candidate across every zero position. If there are NO zeros in the
        array at all, we are still forced to remove one (a 1), so the answer defaults to n - 1.

    Data structure / paradigm: Dynamic programming (two auxiliary arrays built via simple linear
    recurrences: leftOnesEndingAt[i] = nums[i]==1 ? leftOnesEndingAt[i-1] + 1 : 0, and symmetric for
    the right-to-left pass).

    Time Complexity: O(n) -- three linear passes (build left array, build right array, scan zeros).
    Space Complexity: O(n) -- two auxiliary arrays of size n.

    Pros:
        - Conceptually very clear: "stitch the left run and right run around a hole".
        - Easy to explain and easy to extend (e.g., if the problem allowed removing up to k zeros,
          you could adapt a similar precomputed-run idea, though sliding window generalizes better).
    Cons:
        - Uses O(n) extra space, which the sliding window approach avoids entirely.
        - Slightly more bookkeeping (two arrays, boundary checks for i-1 and i+1) than the sliding
          window version.

    When to use in practice:
        - Good middle-ground solution if you want something more obviously correct than a sliding
          window trick, and O(n) extra memory is not a concern. Also a nice way to cross-validate
          the optimal sliding-window solution during development.
    ------------------------------------------------------------------------------------------------
    */
    static int dpApproach(int[] nums) {
        int n = nums.length;

        // leftOnesEndingAt[i]: length of the run of consecutive 1's ending at (and including) i.
        int[] leftOnesEndingAt = new int[n];
        // rightOnesStartingAt[i]: length of the run of consecutive 1's starting at (and including) i.
        int[] rightOnesStartingAt = new int[n];

        leftOnesEndingAt[0] = (nums[0] == 1) ? 1 : 0;
        for (int i = 1; i < n; i++) {
            leftOnesEndingAt[i] = (nums[i] == 1) ? leftOnesEndingAt[i - 1] + 1 : 0;
        }

        rightOnesStartingAt[n - 1] = (nums[n - 1] == 1) ? 1 : 0;
        for (int i = n - 2; i >= 0; i--) {
            rightOnesStartingAt[i] = (nums[i] == 1) ? rightOnesStartingAt[i + 1] + 1 : 0;
        }

        int bestAnswer = 0;
        boolean sawAnyZero = false;

        for (int i = 0; i < n; i++) {
            if (nums[i] == 0) {
                sawAnyZero = true;
                int leftContribution = (i - 1 >= 0) ? leftOnesEndingAt[i - 1] : 0;
                int rightContribution = (i + 1 < n) ? rightOnesStartingAt[i + 1] : 0;
                bestAnswer = Math.max(bestAnswer, leftContribution + rightContribution);
            }
        }

        // If there was no zero at all, the array is all 1's -- we're still forced to remove one
        // element, so the best we can do is the full length minus 1.
        if (!sawAnyZero) {
            bestAnswer = n - 1;
        }

        return bestAnswer;
    }

    /*
    ------------------------------------------------------------------------------------------------
    APPROACH 3 (OPTIMAL): Two-Pointer / Sliding Window -- "At most one zero in the window"
    ------------------------------------------------------------------------------------------------
    Core idea (plain English):
        Reframe the problem: "the longest run of 1's after deleting exactly one element" is the same
        as "the longest window that contains AT MOST one zero, minus 1 for the mandatory deletion".
        Why minus 1? Because within any valid window (at most one zero), we are always going to
        delete exactly one element from it -- either the single zero inside it (turning the rest
        into pure 1's), or, if the window happens to be all 1's, we still must sacrifice one element
        of it. Either way, (window length - 1) is exactly the length of the longest run of 1's we
        can form by deleting one element from that window. We slide a right pointer forward,
        expanding the window; whenever the window contains more than one zero, we shrink from the
        left until it's back down to at most one zero. At every step we update the best answer as
        (right - left), which equals (windowLength - 1) since windowLength = right - left + 1.

    Data structure / paradigm: Two-pointer / sliding window. This also has a greedy flavor: we
    greedily keep expanding the window as far as possible before being forced to shrink, since a
    larger "at most one zero" window can never make the answer worse.

    Time Complexity: O(n) -- both left and right pointers only ever move forward, each element is
                      visited by "right" once (to enter) and possibly by "left" once (to leave), so
                      total work is O(n), not O(n^2).
    Space Complexity: O(1) -- only a few scalar counters, no auxiliary arrays.

    Pros:
        - Optimal in both time and space.
        - Single forward pass, no extra memory -- ideal for very large inputs / streaming-like use.
        - Once you see the "at most one zero, subtract one" reframing, the code is very short.
    Cons:
        - The reframing trick ("subtract 1 unconditionally") is not obvious the first time you see
          this class of problem; easy to get wrong if you don't reason through why the -1 is always
          correct (including in the "no zero at all" case).
        - Slightly less "obviously correct by inspection" than the DP approach for someone reviewing
          the code cold.

    When to use in practice:
        - This is the production-quality answer: O(n) time, O(1) space, single pass. This is what
          I'd write out fully and defend in an interview.
    ------------------------------------------------------------------------------------------------
    */
    static int slidingWindowOptimal(int[] nums) {
        int n = nums.length;

        int leftPointer = 0;      // left edge of the current "at most one zero" window
        int zeroCountInWindow = 0; // how many zeros are currently inside [leftPointer, rightPointer]
        int bestAnswer = 0;

        for (int rightPointer = 0; rightPointer < n; rightPointer++) {
            if (nums[rightPointer] == 0) {
                zeroCountInWindow++;
            }

            // If we've picked up a second zero, shrink from the left until at most one remains.
            while (zeroCountInWindow > 1) {
                if (nums[leftPointer] == 0) {
                    zeroCountInWindow--;
                }
                leftPointer++;
            }

            // Window [leftPointer, rightPointer] has at most one zero.
            // windowLength - 1 = (rightPointer - leftPointer + 1) - 1 = rightPointer - leftPointer.
            bestAnswer = Math.max(bestAnswer, rightPointer - leftPointer);
        }

        return bestAnswer;
    }

    /*
    ================================================================================================
    SECTION 7: APPROACHES COMPARISON TABLE
    ================================================================================================

    | Approach                        | Time      | Space | Best For                              | Limitations                                            |
    |----------------------------------|-----------|-------|----------------------------------------|----------------------------------------------------------|
    | 1. Brute Force                  | O(n^2)    | O(1)  | Small n, correctness oracle           | Far too slow for n ~ 10^5; not production-viable         |
    | 2. DP (left/right run arrays)   | O(n)      | O(n)  | Clear stitch-the-hole reasoning       | Uses linear extra memory vs. the O(1)-space alternative   |
    | 3. Sliding Window (optimal)     | O(n)      | O(1)  | Production use, large n, single pass  | The "-1" reframing trick takes a moment to justify        |
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
    ================================================================================================
    I would present Approach 3 (Sliding Window) as my final answer, but I would get there by first
    saying the brute force out loud (to show I understand the problem and have a correctness
    baseline), then pivoting to the sliding window.

    Why sliding window over the DP approach, specifically:
        - It's strictly better on space (O(1) vs O(n)) at the same O(n) time complexity, with no
          real downside once the "-1" reframing is explained clearly.
        - It's the answer most interviewers at Google expect for this problem -- it signals
          familiarity with the "at most K" sliding window family (this is literally the K=1 case of
          the more general "longest subarray with at most K zeros" pattern), which is a well-known
          reusable pattern.
        - It's fast to code correctly under interview time pressure once you've internalized the
          invariant, and it's easy to verbally justify: "shrink whenever we have more than one zero,
          and the answer is always window length minus one."
        - Interviewers like seeing a candidate recognize a DP solution works, explicitly compare it
          to the sliding window solution, and then justify WHY they're picking the leaner one --
          that trade-off discussion is itself a signal of seniority.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 9: DEEP DIVE -- POLISHED, PRODUCTION-QUALITY OPTIMAL SOLUTION
    ================================================================================================
    (Implemented above as `slidingWindowOptimal`; re-presented here as the method a candidate would
    actually submit, now with the full inline reasoning a senior candidate would narrate live.)
    ------------------------------------------------------------------------------------------------
    */
    static int longestSubarrayAfterRemovingOneElement(int[] nums) {
        // Defensive check reflecting the clarifying-question assumption that nums.length >= 1.
        // (We don't throw for length 0 in a real interview submission, but it's worth stating this
        // assumption out loud -- LeetCode's constraints guarantee nums.length >= 1.)
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("nums must be non-null and non-empty per problem constraints");
        }

        int windowStart = 0;       // left boundary of the sliding window (inclusive)
        int zerosInsideWindow = 0; // count of zeros currently inside [windowStart, windowEnd]
        int longestAfterOneRemoval = 0;

        for (int windowEnd = 0; windowEnd < nums.length; windowEnd++) {
            // Step 1: absorb the new right-hand element into the window.
            if (nums[windowEnd] == 0) {
                zerosInsideWindow++;
            }

            // Step 2: if the window now has more than one zero, it's no longer a valid candidate
            // for "delete exactly one element and have all 1's remain" -- shrink from the left
            // until at most one zero remains. Each shrink step permanently discards the leftmost
            // element from consideration; if that discarded element was itself a zero, decrement
            // our zero counter accordingly.
            while (zerosInsideWindow > 1) {
                if (nums[windowStart] == 0) {
                    zerosInsideWindow--;
                }
                windowStart++;
            }

            // Step 3: the window [windowStart, windowEnd] now contains at most one zero. Deleting
            // exactly one element from this window (the zero, if present, or any single 1 if the
            // window is all 1's) always leaves (windowLength - 1) consecutive 1's. Track the best
            // seen so far.
            int windowLength = windowEnd - windowStart + 1;
            longestAfterOneRemoval = Math.max(longestAfterOneRemoval, windowLength - 1);
        }

        return longestAfterOneRemoval;
    }

    /*
    ================================================================================================
    SECTION 10: DRY RUN / TRACE
    ================================================================================================
    Tracing `longestSubarrayAfterRemovingOneElement` on Example 1: nums = [1, 1, 0, 1, 1]

    Initial state: windowStart = 0, zerosInsideWindow = 0, longestAfterOneRemoval = 0

    windowEnd = 0, nums[0] = 1
        -> not a zero, zerosInsideWindow stays 0
        -> while-loop skipped (0 is not > 1)
        -> windowLength = 0 - 0 + 1 = 1; candidate = 1 - 1 = 0
        -> longestAfterOneRemoval = max(0, 0) = 0

    windowEnd = 1, nums[1] = 1
        -> not a zero, zerosInsideWindow stays 0
        -> while-loop skipped
        -> windowLength = 1 - 0 + 1 = 2; candidate = 2 - 1 = 1
        -> longestAfterOneRemoval = max(0, 1) = 1

    windowEnd = 2, nums[2] = 0
        -> is a zero, zerosInsideWindow becomes 1
        -> while-loop skipped (1 is not > 1)
        -> windowLength = 2 - 0 + 1 = 3; candidate = 3 - 1 = 2
        -> longestAfterOneRemoval = max(1, 2) = 2

    windowEnd = 3, nums[3] = 1
        -> not a zero, zerosInsideWindow stays 1
        -> while-loop skipped
        -> windowLength = 3 - 0 + 1 = 4; candidate = 4 - 1 = 3
        -> longestAfterOneRemoval = max(2, 3) = 3

    windowEnd = 4, nums[4] = 1
        -> not a zero, zerosInsideWindow stays 1
        -> while-loop skipped
        -> windowLength = 4 - 0 + 1 = 5; candidate = 5 - 1 = 4
        -> longestAfterOneRemoval = max(3, 4) = 4

    Loop ends. Return 4. Matches our hand-verified expected answer of 4 from Section 3.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 11: CLOSING SUMMARY
    ================================================================================================
    - Brute force (O(n^2)/O(1)) is correct but not viable at scale; useful only as a test oracle.
    - DP with left/right run arrays (O(n)/O(n)) is clear and easy to trust, trading memory for
      conceptual simplicity.
    - Sliding window (O(n)/O(1)) is the recommended, production-grade solution: linear time,
      constant extra space, single pass, and it generalizes cleanly to the "at most K zeros"
      variant of this problem family.
    - Known assumptions/limitations of the final solution:
        * Assumes nums.length >= 1 (guaranteed by problem constraints); we throw for null/empty
          purely as a defensive guard, not as part of the core algorithm's contract.
        * Assumes values are strictly 0/1; no input validation is performed on element values.
        * The "-1" in the final formula is only correct because we are told the removal is
          MANDATORY and EXACTLY ONE element -- if the problem instead allowed "at most one"
          removal (i.e., zero or one), the formula would need to change (no unconditional -1).
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ================================================================================================
    1. "What if you could remove up to k elements instead of exactly one?"
       -> Generalizes to the classic "longest subarray with at most k zeros" sliding window, with
          the final answer NOT subtracting anything extra (since removal is now "at most k", not
          "exactly one" forced removal) -- the reframing changes subtly and is worth discussing.

    2. "What if nums contained arbitrary integers instead of just 0/1, and we wanted the longest
       subarray with at most one element not equal to a target value?"
       -> Same sliding window shape, just redefine "zero" as "does not equal target".

    3. "What if the array were streamed to you and you couldn't store it all in memory -- could you
       still solve this in O(1) extra memory with a single pass?"
       -> Yes -- the sliding window approach here already only needs O(1) state and processes the
          stream left-to-right, making it naturally streaming-friendly (though "shrinking the
          window" on a true one-directional stream needs care, since you can't re-read the left
          pointer's value unless you buffer at least the current window).

    4. "How would the solution change if removal were optional (i.e., 'at most one' rather than
       'exactly one')?"
       -> You'd take the max of (longest run of 1's with zero removals) and (longest run achievable
          with exactly one removal); effectively drop the unconditional "-1" and instead compare
          windowLength when zerosInsideWindow == 0 against windowLength - 1 when zerosInsideWindow == 1.

    5. "Can you prove why sliding the left pointer forward monotonically (never resetting it
       backward) is safe, i.e., why we never miss a better answer by not backtracking?"
       -> Because once a window's zero count exceeds 1, every larger window starting at the same
          left boundary will only accumulate more zeros, never fewer -- so there's no benefit to
          revisiting a smaller left boundary once we've moved past it for a given right boundary.

    6. "What's the largest n and value range you'd want to stress test against, and how would you
       generate adversarial test cases (e.g., alternating 0s/1s, all zeros, all ones, one zero)?"
       -> Discuss constructing tests like [0]*n, [1]*n, alternating patterns, single interior zero,
          zero at each boundary, and large random arrays cross-validated against the brute force.
    ================================================================================================
    */

    /*
    ================================================================================================
    SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ================================================================================================
    1. Forgetting that removal is MANDATORY even when the array is all 1's -- candidates often
       return `n` instead of `n - 1` for an all-ones array, forgetting they still must delete one
       element.

    2. Off-by-one errors in the DP approach's boundary checks -- forgetting to guard `i - 1 >= 0`
       and `i + 1 < n` when a zero sits at index 0 or index n-1, causing an ArrayIndexOutOfBounds
       or silently wrong results if not guarded.

    3. In the sliding window approach, decrementing `zerosInsideWindow` unconditionally when
       shrinking, instead of only decrementing when the element actually being evicted (at the OLD
       `windowStart`, before incrementing it) was itself a zero -- this is the single most common
       bug in this pattern.

    4. Confusing this "at most one zero, then subtract one" pattern with the plain "at most k zeros,
       no subtraction" pattern from problems like "Max Consecutive Ones III" -- the subtraction is
       ONLY correct here because deletion is mandatory and exactly one; candidates who've seen the
       sibling problem sometimes copy its formula verbatim and forget the "-1".
    ================================================================================================
    */

    /*
    ================================================================================================
    MAIN METHOD: cross-validate all three approaches against each other with assertions,
    including hand-verified examples, edge cases, and randomized stress tests.
    ================================================================================================
    */
    public static void main(String[] args) {

        // ---- Hand-verified examples from Section 3 ----
        int[] example1 = {1, 1, 0, 1, 1};                 // expected 4
        int[] example2 = {1, 1, 1};                       // expected 2
        int[] example3 = {1, 1, 0, 1, 0, 1, 1};           // expected 3

        // ---- Extra edge cases ----
        int[] singleZero = {0};                           // expected 0
        int[] singleOne = {1};                            // expected 0
        int[] allZeros = {0, 0, 0};                        // expected 0
        int[] allOnesLong = {1, 1, 1, 1, 1, 1};            // expected 5
        int[] zeroAtStart = {0, 1, 1, 1};                  // expected 3
        int[] zeroAtEnd = {1, 1, 1, 0};                    // expected 3
        int[] alternating = {1, 0, 1, 0, 1, 0, 1};         // expected 2

        int[][] testCases = {
            example1, example2, example3,
            singleZero, singleOne, allZeros, allOnesLong,
            zeroAtStart, zeroAtEnd, alternating
        };
        int[] expected = {4, 2, 3, 0, 0, 0, 5, 3, 3, 2};

        for (int t = 0; t < testCases.length; t++) {
            int bruteForceResult = bruteForce(testCases[t]);
            int dpResult = dpApproach(testCases[t]);
            int slidingResult = slidingWindowOptimal(testCases[t]);
            int productionResult = longestSubarrayAfterRemovingOneElement(testCases[t]);

            // Cross-validate all four implementations against each other.
            assert bruteForceResult == dpResult
                : "Mismatch (brute vs dp) on test case " + t;
            assert dpResult == slidingResult
                : "Mismatch (dp vs sliding) on test case " + t;
            assert slidingResult == productionResult
                : "Mismatch (sliding vs production) on test case " + t;

            // Cross-validate against our hand-computed expected value.
            assert productionResult == expected[t]
                : "Wrong answer on test case " + t + ": got " + productionResult
                    + " expected " + expected[t];

            System.out.println("Test case " + t + ": " + java.util.Arrays.toString(testCases[t])
                + " -> " + productionResult + " (expected " + expected[t] + ")");
        }

        // ---- Randomized stress test: cross-validate brute force, DP, and sliding window ----
        Random random = new Random(42); // fixed seed for reproducibility
        int numRandomTrials = 500;
        for (int trial = 0; trial < numRandomTrials; trial++) {
            int length = 1 + random.nextInt(30); // lengths from 1 to 30
            int[] randomArray = new int[length];
            for (int i = 0; i < length; i++) {
                randomArray[i] = random.nextInt(2); // 0 or 1
            }

            int bruteForceResult = bruteForce(randomArray);
            int dpResult = dpApproach(randomArray);
            int slidingResult = slidingWindowOptimal(randomArray);

            assert bruteForceResult == dpResult
                : "Random trial " + trial + " mismatch (brute vs dp) on " + java.util.Arrays.toString(randomArray);
            assert dpResult == slidingResult
                : "Random trial " + trial + " mismatch (dp vs sliding) on " + java.util.Arrays.toString(randomArray);
        }

        System.out.println("All " + testCases.length + " hand-verified test cases and "
            + numRandomTrials + " randomized stress tests passed (run with -ea to enable assertions).");
    }
}
