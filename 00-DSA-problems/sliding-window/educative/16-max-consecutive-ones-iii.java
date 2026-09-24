import java.util.*;

/*
================================================================================
 GOOGLE-STYLE MOCK INTERVIEW
 Problem: Max Consecutive Ones III   (LeetCode 1004)
================================================================================
*/

/*
================================================================================
 SECTION 1: RESTATE THE PROBLEM
================================================================================
 In plain English:

   We are given a binary array `nums` (every element is 0 or 1) and an
   integer `k`. We are allowed to "flip" at most `k` of the zeros into ones
   (we choose which zeros, and we don't have to use all k flips). After
   flipping, we want the length of the longest contiguous run of 1's we can
   produce.

   Equivalently: find the length of the longest subarray (contiguous window)
   of `nums` that contains AT MOST `k` zeros. We don't actually need to
   simulate flipping -- any window with <= k zeros can be turned into an
   all-ones run of that length by flipping exactly those zeros.

 Inputs:
   - int[] nums : binary array (elements are 0 or 1)
   - int k      : max number of zeros we may flip, k >= 0

 Output:
   - int : length of the longest contiguous run of 1's achievable

 Key constraints (typical LeetCode framing, to be confirmed with interviewer
 in Section 2):
   - 1 <= nums.length <= 10^5
   - nums[i] is 0 or 1
   - 0 <= k <= nums.length

 Assumptions going in (to be validated):
   - We only care about the LENGTH of the best run, not which indices were
     flipped or the modified array itself.
   - "At most k" -- using fewer than k flips is allowed if that's optimal
     (it always is optimal to use as many as helpful, but the point is we
     are not forced to use exactly k).
*/

/*
================================================================================
 SECTION 2: CLARIFYING QUESTIONS
================================================================================
 Questions I would ask the interviewer, with the answers I will assume if
 they say "use your judgment":

 1. Q: What is the maximum size of `nums`? Does it matter for approach choice?
    A (assumed): Up to ~10^5, so we should target O(n) or O(n log n); O(n^2)
    brute force is acceptable as a warm-up/first-pass but not the final answer.

 2. Q: Can `k` be 0? Can `k` be larger than the number of zeros in the array,
    or even larger than nums.length?
    A (assumed): Yes to both. k=0 means "no flips, just find the longest run
    of pure 1's." If k >= total zeros, the whole array becomes one run of 1's
    (answer = nums.length).

 3. Q: Can the array be empty?
    A (assumed): Constraints usually guarantee length >= 1, but I will guard
    for an empty array defensively and return 0 in that case.

 4. Q: Do we need to return which indices to flip, or just the length?
    A (assumed): Just the integer length. (If they want indices too, I'd
    extend the sliding window to record the best window's [left, right]
    bounds -- I'll mention this as a natural extension.)

 5. Q: Are there only 0s and 1s in the array, or could there be other values
    (e.g., need validation)?
    A (assumed): Guaranteed binary (0/1) per problem statement; I won't
    spend interview time on input validation, but I'd mention it's a
    2-line guard if this were production code.

 6. Q: Is `nums` mutated by the caller elsewhere / do we need to avoid
    modifying it? Any concurrency concerns (multiple threads calling this
    on the same array)?
    A (assumed): No mutation needed by our algorithm at all (sliding window
    is read-only over nums), so this is naturally thread-safe for concurrent
    reads.

 7. Q: Do duplicate/adjacent equal values need special handling (e.g., runs
    of zeros)? Is there any tie-breaking needed if multiple windows share
    the same max length?
    A (assumed): No special handling -- we just want the maximum length;
    if multiple windows tie for the max, any one of them (or just the
    length) is fine since we only return an integer.

 8. Q: Should the solution be iterative or is recursion acceptable (stack
    depth concerns for large n)?
    A (assumed): Iterative preferred given n up to 10^5 -- recursion risks
    stack overflow and adds no benefit here.
*/

/*
================================================================================
 SECTION 3: EXAMPLES & EDGE CASES
================================================================================

 Example 1 (Normal case):
   nums = [1,1,1,0,0,0,1,1,1,1,0], k = 2
   We can flip the two 0's at indices 5 and 6... let's just trust the
   sliding window: the best window is indices [3..9] -> [0,0,1,1,1,1] wait,
   let's recompute properly: the well-known answer for this LeetCode example
   is 6, achieved by flipping the zeros in the window nums[5..10] region.
   Answer: 6

 Example 2 (Edge case -- k = 0):
   nums = [0,0,0], k = 0
   No flips allowed, and there are no 1's at all.
   Answer: 0

 Example 3 (Boundary case -- k covers all zeros / entire array becomes 1's):
   nums = [0,0,1,1,0,0,1,1,1,0,1,1], k = 4
   There are exactly 4 zeros in the whole array (indices 0,1,4,5,9 -- let's
   count: positions 0,1,4,5,9 -> that's actually 5 zeros). If k equals or
   exceeds the total zero count, the entire array can become one run.
   With k = 5 here, answer = nums.length = 12.
   This is the "flip everything" boundary -- important to make sure the
   sliding window's right pointer can run to the very end without the left
   pointer needlessly shrinking.

   Additional micro boundary case: nums has length 1.
     nums = [0], k = 0 -> answer 0
     nums = [0], k = 1 -> answer 1
*/

/*
================================================================================
 SECTION 4 & 5: ALL POSSIBLE APPROACHES
 (Paradigm sweep: I will explicitly state which paradigms apply and which
  are ruled out, before diving into full implementations.)

 Paradigm sweep:
   - Brute force              -> APPLICABLE (baseline correctness oracle)
   - Sorting-based             -> NOT APPLICABLE: order/contiguity of nums is
                                   semantically required (we need a
                                   *contiguous* run); sorting destroys the
                                   positional information we need.
   - Hashing-based              -> NOT APPLICABLE: there's nothing to
                                   deduplicate or look up by key here; the
                                   problem is purely about contiguous ranges.
   - Two pointer / sliding window -> APPLICABLE and OPTIMAL. The "number of
                                   zeros in window <= k" condition is
                                   monotonic as the window grows/shrinks,
                                   which is the classic signature for
                                   sliding window.
   - Divide and conquer         -> NOT NATURALLY APPLICABLE: there's no clean
                                   way to combine "best window with <= k
                                   zeros" answers from two halves in
                                   sub-linear merge time; would just add
                                   complexity for no gain over sliding window.
   - Greedy                     -> APPLICABLE (the sliding window itself is a
                                   greedy expand-then-shrink strategy: always
                                   expand right greedily, only shrink left
                                   when forced).
   - Dynamic programming        -> NOT NEEDED: there's no overlapping
                                   subproblem structure here that isn't
                                   already captured by the O(n) window
                                   invariant; a DP formulation would just be
                                   a slower, more complex re-derivation of
                                   the same sliding window recurrence.
   - Tree / graph traversal      -> NOT APPLICABLE: no tree/graph structure
                                   in the input.
   - Heap / priority queue        -> NOT APPLICABLE: we don't need ordering
                                   by priority; all elements are processed
                                   in fixed left-to-right order.
   - Binary search               -> APPLICABLE as an alternative: binary
                                   search on "window length L is achievable"
                                   combined with prefix sums of zero-counts,
                                   O(n log n). Worth mentioning as a valid
                                   but non-optimal alternative.
   - Monotonic stack / deque      -> APPLICABLE as an alternative
                                   implementation detail: instead of a zero
                                   counter, keep a deque/queue of the
                                   indices of zeros currently in the window;
                                   still O(n), just a different bookkeeping
                                   style (useful if interviewer asks "what
                                   if you also need the flipped indices?").
   - Trie / segment tree           -> NOT APPLICABLE: no prefix-matching or
                                   range-query-with-updates structure that
                                   would justify these; overkill for a
                                   static array single pass.
================================================================================
*/

class MaxConsecutiveOnesIII {

    /*
    ----------------------------------------------------------------------
     APPROACH 1: Brute Force
    ----------------------------------------------------------------------
     Core idea: For every possible starting index `left`, extend `right`
     as far as possible while counting zeros seen so far in the window.
     Stop extending once the zero count exceeds k. Track the best window
     length seen across all starting points.

     Data structure / paradigm: none beyond simple counters -- pure
     brute-force enumeration of all O(n^2) windows.

     Time Complexity: O(n^2) -- for each of n starting indices, we may scan
       up to n elements before breaking.
     Space Complexity: O(1) -- only a few counters.

     Pros:
       - Trivial to reason about and verify correctness.
       - Zero risk of subtle off-by-one bugs; great as a correctness oracle
         to cross-validate the optimized solution against.
     Cons:
       - Too slow for n up to 10^5 (10^10 operations worst case) -- would
         TLE (Time Limit Exceeded) on the real constraints.

     When to use: Only as a warm-up / sanity-check baseline, or for
     extremely small inputs (n < ~1000) where simplicity beats performance.
     Not something to submit as a final interview answer for this problem's
     stated constraints.
    ----------------------------------------------------------------------
    */
    public static int maxConsecutiveOnesBruteForce(int[] nums, int k) {
        final int n = nums.length;
        int bestLength = 0;

        for (int left = 0; left < n; left++) {
            int zerosInWindow = 0;
            for (int right = left; right < n; right++) {
                if (nums[right] == 0) {
                    zerosInWindow++;
                }
                if (zerosInWindow > k) {
                    // Can't extend this window further with this many flips.
                    break;
                }
                bestLength = Math.max(bestLength, right - left + 1);
            }
        }
        return bestLength;
    }

    /*
    ----------------------------------------------------------------------
     APPROACH 2: Prefix Sum + Binary Search
    ----------------------------------------------------------------------
     Core idea: Build a prefix-sum array `zerosPrefix` where zerosPrefix[i]
     = number of zeros in nums[0..i-1]. Then, for a fixed window length L,
     we can check in O(1) whether SOME window of length L has <= k zeros,
     by sliding an index and comparing zerosPrefix[right] - zerosPrefix[left].
     Because "does there exist a window of length L with <= k zeros" is a
     monotonic predicate in L (if length L works, every smaller length also
     has a valid window), we binary search on L in [0, n], and for each
     candidate L do an O(n) scan using the prefix sums to check feasibility.

     Data structure / paradigm: prefix sums + binary search on the answer.

     Time Complexity: O(n log n) -- binary search over O(n) possible lengths
       (log n iterations), each doing an O(n) feasibility scan using
       prefix sums.
     Space Complexity: O(n) -- for the prefix sum array.

     Pros:
       - Demonstrates the "binary search on the answer" pattern, which is a
         valuable general technique to showcase in interviews.
       - Still comfortably within time limits for n = 10^5.
     Cons:
       - Strictly worse than the O(n) sliding window in both time and space
         for this exact problem -- extra log factor and extra array for no
         benefit.
       - More code, more places to introduce bugs (binary search boundary
         conditions), for no gain in this specific problem.

     When to use: I would mention this approach verbally to show breadth,
     but would not implement it as my primary answer, since the sliding
     window strictly dominates it here. This pattern becomes essential in
     variants where the monotonic-window trick doesn't directly apply.
    ----------------------------------------------------------------------
    */
    public static int maxConsecutiveOnesBinarySearch(int[] nums, int k) {
        final int n = nums.length;
        if (n == 0) {
            return 0;
        }

        // zerosPrefix[i] = number of zeros among nums[0..i-1]
        int[] zerosPrefix = new int[n + 1];
        for (int i = 0; i < n; i++) {
            zerosPrefix[i + 1] = zerosPrefix[i] + (nums[i] == 0 ? 1 : 0);
        }

        int lo = 0;
        int hi = n;
        int bestLength = 0;

        // Binary search for the largest window length L such that some
        // window of that length has <= k zeros.
        while (lo <= hi) {
            int candidateLength = lo + (hi - lo) / 2;
            if (candidateLength > 0 && existsWindowWithAtMostKZeros(zerosPrefix, n, candidateLength, k)) {
                bestLength = candidateLength;
                lo = candidateLength + 1; // try to find an even larger feasible length
            } else if (candidateLength == 0) {
                // Length 0 is trivially always "feasible" but not useful as an answer;
                // move on to test larger lengths.
                lo = candidateLength + 1;
            } else {
                hi = candidateLength - 1; // too large, shrink search space
            }
        }
        return bestLength;
    }

    // Helper: does there exist a window of exactly `length` with <= k zeros?
    private static boolean existsWindowWithAtMostKZeros(int[] zerosPrefix, int n, int length, int k) {
        for (int start = 0; start + length <= n; start++) {
            int zerosInWindow = zerosPrefix[start + length] - zerosPrefix[start];
            if (zerosInWindow <= k) {
                return true;
            }
        }
        return false;
    }

    /*
    ----------------------------------------------------------------------
     APPROACH 3: Sliding Window / Two Pointer  (RECOMMENDED / OPTIMAL)
    ----------------------------------------------------------------------
     Core idea: Maintain a window [left, right] that always contains AT
     MOST k zeros. Expand `right` one step at a time (greedily grow the
     window). Whenever the zero count inside the window exceeds k, shrink
     from the left (advance `left`) until the window is valid again. At
     every step, the window is the largest *valid* window ending at
     `right`, so we track the maximum window size seen.

     Key insight: we never need to shrink the window's tracked maximum size
     -- once we've achieved a window of size W, we never need to consider a
     smaller max again (the window can slide but its best-seen length is
     monotonically non-decreasing), so we don't even need to explicitly
     recompute (right - left + 1) as "best" -- we can simply note the window
     never shrinks in size once it reaches its widest point (a well-known
     optimization), though for clarity I will just take the max directly,
     which is equally O(n) and easier to explain live.

     Data structure / paradigm: two pointers / sliding window, greedy
     expand-and-shrink.

     Time Complexity: O(n) -- `right` advances n times total; `left` also
       advances at most n times total across the whole run (it never
       resets backward), so total pointer movements are O(n).
     Space Complexity: O(1) -- only a few integer counters, no auxiliary
       arrays.

     Pros:
       - Optimal time and space.
       - Short, easy to write correctly and explain live in an interview.
       - Directly generalizes to related problems (longest substring with
         at most k distinct characters, longest subarray with at most k
         replacements, etc.) -- a pattern worth highlighting to the
         interviewer.
     Cons:
       - Relies on the monotonic-window property; if the problem were
         changed so that the "validity" condition wasn't monotonic in
         window size, this technique would not directly apply.

     When to use: This is the approach I would code as my primary/final
     answer in an interview -- optimal complexity, minimal code surface,
     and it's the textbook-correct paradigm match for "at most k" /
     "longest contiguous subarray satisfying a monotonic constraint"
     problems.
    ----------------------------------------------------------------------
    */
    public static int maxConsecutiveOnesSlidingWindow(int[] nums, int k) {
        int left = 0;
        int zerosInWindow = 0;
        int bestLength = 0;

        for (int right = 0; right < nums.length; right++) {
            if (nums[right] == 0) {
                zerosInWindow++;
            }

            // Shrink from the left while we have more zeros than flips allowed.
            while (zerosInWindow > k) {
                if (nums[left] == 0) {
                    zerosInWindow--;
                }
                left++;
            }

            // At this point [left, right] is guaranteed valid (<= k zeros).
            bestLength = Math.max(bestLength, right - left + 1);
        }
        return bestLength;
    }

    /*
    ----------------------------------------------------------------------
     APPROACH 4: Sliding Window via Deque of Zero Indices (Alternative
                 bookkeeping -- Monotonic Queue style)
    ----------------------------------------------------------------------
     Core idea: Functionally identical sliding window, but instead of a
     single zero counter, store the INDICES of zeros currently inside the
     window in a Deque (acting as a FIFO queue). When the queue size
     exceeds k, pop the oldest zero index from the front and move `left` to
     just past that index. This variant is useful when a follow-up asks
     "which indices did you flip?" -- the deque directly gives that answer
     (its contents at the point of maximum window length).

     Data structure / paradigm: deque used as a monotonic FIFO queue of
     zero positions; still fundamentally a two-pointer sliding window.

     Time Complexity: O(n) -- each index is pushed onto the deque at most
       once and popped at most once.
     Space Complexity: O(k) -- the deque holds at most k+1 zero indices at
       any time (worst case, right before a pop).

     Pros:
       - Directly exposes which zeros were flipped for the best window,
         useful if the interviewer extends the problem to require indices.
     Cons:
       - Extra space O(k) vs O(1) for Approach 3, for a benefit (index
         tracking) that isn't required by the base problem statement.
       - Slightly more code than the plain counter version for the same
         core logic.

     When to use: Mention as a natural extension if asked "what if I also
     need to know which zeros to flip?" -- otherwise Approach 3's plain
     counter is simpler and preferred.
    ----------------------------------------------------------------------
    */
    public static int maxConsecutiveOnesDequeVariant(int[] nums, int k) {
        Deque<Integer> zeroIndices = new ArrayDeque<>();
        int left = 0;
        int bestLength = 0;

        for (int right = 0; right < nums.length; right++) {
            if (nums[right] == 0) {
                zeroIndices.addLast(right);
            }

            if (zeroIndices.size() > k) {
                int oldestZeroIndex = zeroIndices.pollFirst();
                left = oldestZeroIndex + 1;
            }

            bestLength = Math.max(bestLength, right - left + 1);
        }
        return bestLength;
    }

    /*
    ================================================================================
     SECTION 7: APPROACHES COMPARISON TABLE
    ================================================================================

     Approach                        | Time       | Space | Best For                        | Limitations
     ---------------------------------------------------------------------------------------------------------------------
     1. Brute Force                  | O(n^2)     | O(1)  | Correctness oracle, tiny n      | Far too slow for n = 10^5
     2. Prefix Sum + Binary Search   | O(n log n) | O(n)  | Showcasing binary-search-on-    | Strictly dominated by sliding
                                       |            |       | answer pattern                  | window here; extra log factor
     3. Sliding Window (counter)     | O(n)       | O(1)  | THE interview answer -- optimal | Needs monotonic validity
                                       |            |       | time & space, simple to code    | property to apply
     4. Sliding Window (deque of     | O(n)       | O(k)  | When flipped indices must be    | Extra space vs Approach 3 for
        zero indices)                 |            |       | reported, not just the length   | no benefit on the base problem
    ================================================================================
    */

    /*
    ================================================================================
     SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
    ================================================================================
     I would present Approach 3 (Sliding Window with a zero counter) as my
     final answer, for these reasons:

       - Optimality: O(n) time, O(1) space -- cannot do asymptotically
         better since we must at least look at every element once.
       - Clarity & coding speed: it's ~10 lines of code with no auxiliary
         data structures, easy to write correctly under interview time
         pressure and easy to narrate line-by-line.
       - Interviewer expectations: "at most k" + "longest contiguous
         subarray" is the canonical signature for sliding window; presenting
         this immediately (after briefly mentioning brute force as the
         naive baseline) shows pattern recognition, which is exactly what
         Google interviewers are screening for.
       - Extensibility: if asked a follow-up (e.g., "what if you need the
         actual indices flipped?"), I can smoothly pivot to Approach 4
         (deque variant) or simply track left/right bounds at the point
         bestLength was last updated -- showing depth without having
         over-engineered the first answer.

     My interview strategy: state the brute force in one sentence to show
     I've considered the naive baseline, immediately note its complexity
     is too slow, then pivot straight to coding the sliding window as the
     primary solution -- mentioning binary search + prefix sum only if
     asked to enumerate alternative paradigms.
    ================================================================================
    */

    /*
    ================================================================================
     SECTION 9: DEEP DIVE -- OPTIMAL SOLUTION (PRODUCTION QUALITY)
    ================================================================================
    */

    /** Sentinel used purely for documentation clarity; not required by the algorithm. */
    private static final int FLIP_TARGET_VALUE = 1;

    /**
     * Returns the length of the longest contiguous subarray of {@code nums}
     * that can be turned entirely into 1's by flipping at most {@code k}
     * zeros.
     *
     * <p>Approach: sliding window. We maintain the invariant that the
     * window {@code [windowStart, windowEnd]} always contains at most
     * {@code k} zeros. We greedily expand {@code windowEnd}; whenever the
     * zero count exceeds {@code k}, we advance {@code windowStart} until
     * the invariant is restored. Because both pointers only move forward,
     * the total work across the whole array is O(n).
     *
     * @param nums binary array (elements are guaranteed to be 0 or 1)
     * @param k    maximum number of zeros allowed to be flipped, k >= 0
     * @return length of the longest achievable run of 1's
     * @throws IllegalArgumentException if nums is null or k is negative
     */
    public static int longestOnesAfterFlips(int[] nums, int k) {
        // --- Defensive checks (production-quality guard clauses) ---
        if (nums == null) {
            throw new IllegalArgumentException("nums must not be null");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k must be non-negative, got: " + k);
        }
        if (nums.length == 0) {
            return 0; // No elements -> no run of any length possible.
        }

        int windowStart = 0;      // Left boundary of the current valid window (inclusive).
        int zerosInWindow = 0;    // Count of zeros currently inside [windowStart, windowEnd].
        int longestRunLength = 0; // Best (longest) valid window length seen so far.

        for (int windowEnd = 0; windowEnd < nums.length; windowEnd++) {
            // Defensive check: confirm binary input as we go (cheap, O(1) per element).
            if (nums[windowEnd] != 0 && nums[windowEnd] != FLIP_TARGET_VALUE) {
                throw new IllegalArgumentException(
                    "nums must contain only 0s and 1s; found: " + nums[windowEnd] + " at index " + windowEnd);
            }

            if (nums[windowEnd] == 0) {
                zerosInWindow++;
            }

            // Restore the invariant: window may contain at most k zeros.
            while (zerosInWindow > k) {
                if (nums[windowStart] == 0) {
                    zerosInWindow--;
                }
                windowStart++;
            }

            // [windowStart, windowEnd] is now guaranteed valid; record its length.
            int currentWindowLength = windowEnd - windowStart + 1;
            longestRunLength = Math.max(longestRunLength, currentWindowLength);
        }

        return longestRunLength;
    }

    /*
    ================================================================================
     SECTION 10: DRY RUN / TRACE
    ================================================================================
     Tracing longestOnesAfterFlips on Example 1:
       nums = [1,1,1,0,0,0,1,1,1,1,0], k = 2
       indices:  0 1 2 3 4 5 6 7 8 9 10

     Initial: windowStart=0, zerosInWindow=0, longestRunLength=0

     windowEnd=0  nums[0]=1                  zerosInWindow=0  window=[0,0] len=1  best=1
     windowEnd=1  nums[1]=1                  zerosInWindow=0  window=[0,1] len=2  best=2
     windowEnd=2  nums[2]=1                  zerosInWindow=0  window=[0,2] len=3  best=3
     windowEnd=3  nums[3]=0 -> zerosInWindow=1                window=[0,3] len=4  best=4
     windowEnd=4  nums[4]=0 -> zerosInWindow=2                window=[0,4] len=5  best=5
     windowEnd=5  nums[5]=0 -> zerosInWindow=3 (> k=2!)
         shrink: nums[windowStart=0]=1 (not zero) -> windowStart=1, zerosInWindow still 3
         shrink: nums[windowStart=1]=1 (not zero) -> windowStart=2, zerosInWindow still 3
         shrink: nums[windowStart=2]=1 (not zero) -> windowStart=3, zerosInWindow still 3
         shrink: nums[windowStart=3]=0 (zero!)    -> zerosInWindow=2, windowStart=4
         invariant restored (zerosInWindow=2 <= k=2)
         window=[4,5] len=2  best remains 5
     windowEnd=6  nums[6]=1                  zerosInWindow=2  window=[4,6] len=3  best remains 5
     windowEnd=7  nums[7]=1                  zerosInWindow=2  window=[4,7] len=4  best remains 5
     windowEnd=8  nums[8]=1                  zerosInWindow=2  window=[4,8] len=5  best remains 5
     windowEnd=9  nums[9]=1                  zerosInWindow=2  window=[4,9] len=6  best=6
     windowEnd=10 nums[10]=0 -> zerosInWindow=3 (> k=2!)
         shrink: nums[windowStart=4]=0 (zero!) -> zerosInWindow=2, windowStart=5
         invariant restored
         window=[5,10] len=6  best remains 6

     Final answer: 6  (matches expected result for this well-known example)
    ================================================================================
    */

    /*
    ================================================================================
     SECTION 11: CLOSING SUMMARY
    ================================================================================
     - Brute force (O(n^2)) is useful only as a correctness oracle; it does
       not scale to the stated constraints (n up to 10^5).
     - Prefix sum + binary search (O(n log n)) is a valid alternative that
       showcases the "binary search on the answer" pattern, but is strictly
       dominated here by the sliding window approach.
     - Sliding window with a zero counter (O(n) time, O(1) space) is the
       optimal and recommended solution: it directly exploits the
       monotonic "zeros in window <= k" property.
     - The deque-of-zero-indices variant (O(n) time, O(k) space) is
       functionally equivalent but additionally exposes which indices would
       need to be flipped -- useful only if that information is required.

     Known assumptions / limitations of the final solution:
       - Assumes nums contains only 0s and 1s (validated defensively, throws
         on malformed input rather than silently producing a wrong answer).
       - Assumes k >= 0 (validated defensively).
       - Returns 0 for an empty array rather than throwing, since "longest
         run in an empty array" is well-defined as 0.
       - Does not report which indices were flipped -- only the length. See
         Approach 4 or the extension noted in Section 12 for that variant.
    ================================================================================
    */

    /*
    ================================================================================
     SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ================================================================================
     1. "Can you modify the solution to also return the actual flipped
         indices, not just the length?" (Track windowStart/windowEnd at the
         moment longestRunLength is updated, or use the Approach 4 deque
         variant and snapshot its contents at the optimal point.)
     2. "What if nums were a stream (values arrive one at a time and you
         can't store the whole array), and you needed the running best
         length so far?" (The sliding window already processes elements
         one at a time and only needs O(1)/O(k) state, so it adapts
         naturally to a streaming model.)
     3. "What if instead of flipping 0->1, you could flip at most k
         elements of EITHER value (i.e., you want the longest run of a
         single repeated value, not necessarily 1)?" (Generalize to: for
         each candidate value v in {0,1}, run the same sliding window
         counting occurrences of the *other* value; take the max over both.)
     4. "What if k could be a very large number, up to 10^9, while n is
         still 10^5?" (No change needed -- k simply becomes effectively
         "unlimited" once it exceeds the total zero count; the window will
         simply never need to shrink, and the answer becomes n. Might add
         an O(1) short-circuit: if k >= total zero count, return n directly.)
     5. "How would this change if we needed the longest run of 1's with
         EXACTLY k flips (not at most k)?" (Trickier -- would need to
         track feasibility of using exactly k, e.g., via
         longestWithAtMostK(k) minus consideration of whether unused flips
         can be "wasted" on already-1 positions at the window boundary;
         typically still solvable by adjusting the sliding window
         condition or with a DP over (position, flips used).)
     6. "Can you solve this with O(1) extra space if input is guaranteed
         sorted or has some other special structure?" (Already O(1) extra
         space for the general case; would discuss whether any additional
         structure allows an early-exit optimization, e.g., precomputed
         run-lengths of existing 1's blocks separated by single zeros.)
    ================================================================================
    */

    /*
    ================================================================================
     SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ================================================================================
     1. Forgetting that `left`/`windowStart` should NEVER move backward --
        some candidates mistakenly reset it to 0 or recompute it from
        scratch on each shrink, turning an O(n) solution back into O(n^2).
     2. Off-by-one errors in the window length calculation: it must be
        (windowEnd - windowStart + 1), not (windowEnd - windowStart); this
        is a very common slip under interview pressure.
     3. Using `if` instead of `while` when shrinking the window: a single
        `if` only shrinks by one position per outer iteration, which fails
        when multiple shrinks are needed to restore the invariant (though
        for THIS specific problem, since the zero count only ever exceeds k
        by exactly 1 per step, a single `if` happens to suffice -- but
        candidates should be able to justify why, or default to `while` for
        safety in case the invariant-breaking step could overshoot by more
        than one, as in some problem variants).
     4. Not handling k >= total number of zeros (or k >= nums.length) as a
        trivial "the whole array works" case -- functionally the sliding
        window handles this correctly on its own, but candidates sometimes
        add unnecessary special-case code for it, or conversely fail to
        realize the general algorithm already covers it, wasting time
        second-guessing correct code.
    ================================================================================
    */

    /*
    ================================================================================
     TEST HARNESS (cross-validates all four approaches against each other)
    ================================================================================
    */
    public static void main(String[] args) {
        List<TestCase> testCases = new ArrayList<>();
        testCases.add(new TestCase(new int[]{1,1,1,0,0,0,1,1,1,1,0}, 2, 6));
        testCases.add(new TestCase(new int[]{0,0,0}, 0, 0));
        testCases.add(new TestCase(new int[]{0,0,1,1,0,0,1,1,1,0,1,1}, 5, 12));
        testCases.add(new TestCase(new int[]{0}, 0, 0));
        testCases.add(new TestCase(new int[]{0}, 1, 1));
        testCases.add(new TestCase(new int[]{1,1,1,1}, 0, 4));
        testCases.add(new TestCase(new int[]{}, 0, 0));

        int passCount = 0;
        for (TestCase testCase : testCases) {
            int bruteResult = maxConsecutiveOnesBruteForce(testCase.nums, testCase.k);
            int binarySearchResult = maxConsecutiveOnesBinarySearch(testCase.nums, testCase.k);
            int slidingWindowResult = maxConsecutiveOnesSlidingWindow(testCase.nums, testCase.k);
            int dequeResult = maxConsecutiveOnesDequeVariant(testCase.nums, testCase.k);
            int optimalResult = longestOnesAfterFlips(testCase.nums, testCase.k);

            boolean allMatch = bruteResult == testCase.expected
                && binarySearchResult == testCase.expected
                && slidingWindowResult == testCase.expected
                && dequeResult == testCase.expected
                && optimalResult == testCase.expected;

            System.out.printf(
                "nums=%-45s k=%d expected=%d | brute=%d binSearch=%d slidingWindow=%d deque=%d optimal=%d -> %s%n",
                Arrays.toString(testCase.nums), testCase.k, testCase.expected,
                bruteResult, binarySearchResult, slidingWindowResult, dequeResult, optimalResult,
                allMatch ? "PASS" : "FAIL"
            );

            if (allMatch) {
                passCount++;
            }
        }
        System.out.println(passCount + "/" + testCases.size() + " test cases passed.");
    }

    /** Simple immutable holder for a test case: input array, k, and expected answer. */
    private static final class TestCase {
        final int[] nums;
        final int k;
        final int expected;

        TestCase(int[] nums, int k, int expected) {
            this.nums = nums;
            this.k = k;
            this.expected = expected;
        }
    }
}
