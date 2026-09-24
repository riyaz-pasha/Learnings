import java.util.*;

/**
 * =====================================================================================
 * GOOGLE ONSITE MOCK INTERVIEW TRANSCRIPT
 * Problem: Subarrays with K Different Integers  (LeetCode 992, Hard, Google-tagged)
 * =====================================================================================
 *
 * Sibling-pattern note (for your personal pattern library):
 * This belongs to the same "count subarrays satisfying a monotonic window property"
 * family as LC 1493 (Longest Subarray of 1's After Deleting One Element) and
 * LC 340 / 159 (Longest Substring with At Most K Distinct Characters). The core
 * trick — "exactly(K) = atMost(K) - atMost(K-1)" — is a reusable transformation
 * whenever a problem asks for an EXACT count/length constraint but the underlying
 * window property (distinct count, sum, etc.) is only monotonic for "at most."
 */
class SubarraysWithKDistinctIntegers {

    /*
     * =================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * =================================================================================
     * We are given an integer array `nums` and an integer `k`. We must return the
     * total number of contiguous subarrays of `nums` that contain EXACTLY k distinct
     * integers (not "at least" and not "at most" — exactly).
     *
     * Inputs:
     *   - nums: int[], 1 <= nums.length <= 2 * 10^4 (per LeetCode constraints)
     *   - k: int, 1 <= k <= nums.length
     *   - nums[i] values are bounded (LeetCode says 1 <= nums[i] <= nums.length),
     *     but I will not rely on that bound unless the interviewer confirms it,
     *     since a HashMap-based solution works regardless.
     *
     * Output:
     *   - A single integer: the count of contiguous subarrays with exactly k
     *     distinct values.
     *
     * Key assumption to confirm: "distinct integers" means distinct VALUES within
     * the subarray window, not distinct by index. E.g., [1,2,1] has 2 distinct
     * integers (1 and 2), not 3.
     *
     * Example given in the prompt: [1,2,3,1,2], the subarray [1,2,3] has 3 distinct
     * values (1, 2, 3). This confirms the "distinct values in the window" reading.
     */

    /*
     * =================================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * =================================================================================
     * 1. Q: Can `nums` be empty, or can k be 0?
     *    A (assumed): No — nums.length >= 1 and k >= 1. I will still guard against
     *    a null/empty array defensively in code.
     *
     * 2. Q: Are array values guaranteed to be positive / within a bounded range
     *    (e.g., 1..n), or could they be arbitrary integers, including negatives?
     *    A (assumed): Treat values as arbitrary 32-bit integers for generality;
     *    I'll use a HashMap for frequency counting rather than an array-indexed
     *    counter, so the solution is correct regardless of value range. (If the
     *    interviewer confirms values are bounded to [1, n], I can swap the HashMap
     *    for an int[] frequency array as a micro-optimization — I'll mention this.)
     *
     * 3. Q: Do we count overlapping subarrays separately? E.g., is [1,2] at index
     *    [0,1] a different subarray from [1,2] at index [2,3] even if values match?
     *    A (assumed): Yes. We count by (start, end) index pairs, not by distinct
     *    value-content. Two subarrays with identical contents but different
     *    positions both count.
     *
     * 4. Q: Is k always achievable (i.e., k <= number of distinct values in the
     *    whole array), or could the true answer be 0?
     *    A (assumed): The answer can legitimately be 0 if fewer than k distinct
     *    values exist anywhere, or if k exceeds subarray length constraints. My
     *    solution must handle that gracefully, not throw.
     *
     * 5. Q: What's the expected input size, and what time complexity is expected?
     *    A (assumed): n up to ~2 * 10^4, so O(n^2) would likely pass but O(n) or
     *    O(n log n) is preferred and is what I'll aim to deliver as the final
     *    solution.
     *
     * 6. Q: Should I return a count that could overflow a 32-bit int for very
     *    large n?
     *    A (assumed): For n = 2*10^4, max subarrays ~ n*(n+1)/2 ≈ 2*10^8, which
     *    fits in a signed 32-bit int (max ~2.1*10^9), but I'll use `long` as the
     *    accumulator internally to be safe and defensive, then narrow only if the
     *    interviewer insists on an `int` return type.
     *
     * 7. Q: Is this a single query, or will `k` vary across many repeated queries
     *    on the same array (which would change the optimal design toward
     *    precomputation)?
     *    A (assumed): Single query per call — no need to design for repeated
     *    amortized queries, though I'll mention it as a follow-up extension.
     *
     * 8. Q: Do we need to return the subarrays themselves, or just the count?
     *    A (assumed): Just the count (as stated), which simplifies bookkeeping —
     *    we never need to materialize or store the actual subarrays.
     */

    /*
     * =================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * =================================================================================
     *
     * Example 1 (Normal case):
     *   nums = [1,2,1,2,3], k = 2
     *   Good subarrays (exactly 2 distinct): [1,2], [2,1], [1,2], [2,3], [1,2,1],
     *   [2,1,2]  -> Expected answer: 7
     *   (This is the canonical LeetCode example.)
     *
     * Example 2 (Edge case — k equals array's total distinct count, whole array
     * is the only qualifying superset boundary):
     *   nums = [1,2,1,3,4], k = 3
     *   Let's verify: distinct values overall = {1,2,3,4} = 4 distinct, so k=3 is
     *   achievable via sub-windows, not just the whole array.
     *   Subarrays with exactly 3 distinct: [1,2,1,3] (distinct {1,2,3}),
     *   [2,1,3] (distinct {1,2,3}), [1,3,4] (distinct {1,3,4}) -> Expected answer: 3
     *
     * Example 3 (Boundary / tie-breaking case — k = 1, and repeated identical
     * runs, testing that we don't overcount or undercount runs of the same value):
     *   nums = [1,1,1], k = 1
     *   Every contiguous subarray here has exactly 1 distinct value.
     *   Subarrays: [1](i=0), [1](i=1), [1](i=2), [1,1](0,1), [1,1](1,2),
     *   [1,1,1](0,2) -> 6 subarrays total, all qualify since only value "1" appears.
     *   Expected answer: 6 = n*(n+1)/2 for n=3. This stresses the "exactly k"
     *   logic against runs, which is the classic off-by-one trap in this problem.
     *
     * Edge case worth noting explicitly: k > total distinct values in nums.
     *   nums = [1,2,3], k = 5 -> no subarray can have 5 distinct values since only
     *   3 distinct values exist at all. Expected answer: 0. My solution must
     *   naturally return 0 here without special-casing.
     */

    /*
     * =================================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS
     * =================================================================================
     *
     * Paradigms considered but explicitly NOT applicable (stated up front per
     * interview best practice — shows breadth of the paradigm survey):
     *   - Divide & Conquer: distinct-count-in-a-window is not a property that
     *     combines cleanly across a merge step (unlike, say, max subarray sum),
     *     so D&C offers no asymptotic benefit here.
     *   - Tree / Graph traversal: no graph or hierarchical structure in the input;
     *     not applicable.
     *   - Heap / Priority Queue: no "top-k" or ordering requirement; a heap adds
     *     log-factor overhead with no benefit over direct frequency counting.
     *   - Binary Search: the number of distinct values in a window is NOT
     *     monotonic as a function of window content in a way that supports
     *     binary search directly on subarray boundaries for a fixed left index
     *     the way, e.g., sorted-array search would; however, note that
     *     "at most K" AS A FUNCTION OF RIGHT POINTER for a fixed left pointer IS
     *     monotonic — that's precisely why the sliding window works. So binary
     *     search is subsumed by / superseded by the sliding window technique
     *     rather than being a separate viable approach.
     *   - Monotonic Stack/Deque: no "next greater/smaller element" or
     *     range-min/max structure is relevant here; not applicable.
     *   - Trie / Segment Tree: no prefix-string matching or range-query-with-
     *     updates structure needed; a segment tree could track distinct counts
     *     under updates, but this problem is a single static-array query, so
     *     it would be needless overhead.
     *   - Dynamic Programming (classic memoized state-transition sense): there's
     *     no clean optimal-substructure recurrence that beats the sliding window
     *     here; the "last occurrence index" idea some DP formulations use
     *     collapses into the same sliding-window bookkeeping, so I present it
     *     as sliding window rather than a distinct DP approach.
     *
     * Paradigms genuinely applicable: Brute Force, Hashing (frequency maps),
     * Sliding Window (the star of this problem), and Greedy-style window
     * shrinking (folded into the sliding window approaches below).
     */

    /* ---------------------------------------------------------------------------
     * APPROACH 1: Brute Force (Triple Loop, Recount Distinct Every Time)
     * ---------------------------------------------------------------------------
     * Core idea: For every possible (start, end) pair, extract the subarray and
     * count distinct values from scratch using a fresh HashSet.
     * Data structure: HashSet<Integer> per subarray.
     * Time Complexity: O(n^3) — O(n^2) subarrays, each requiring O(n) work to
     * build the set and count distinct values.
     * Space Complexity: O(n) for the HashSet per subarray (not counted
     * cumulatively since sets are discarded after each subarray).
     * Pros: Trivial to reason about correctness; useful only as a brute-force
     * oracle for testing.
     * Cons: Far too slow for n = 2*10^4 (would be ~8*10^12 operations worst case);
     * never acceptable as a final interview answer.
     * When to use: Only to sanity-check smaller optimized solutions in a test
     * harness — never in production or as the final interview answer.
     */
    public long bruteForceTripleLoop(int[] nums, int k) {
        int n = nums.length;
        long count = 0;
        for (int start = 0; start < n; start++) {
            for (int end = start; end < n; end++) {
                Set<Integer> distinctValues = new HashSet<>();
                for (int index = start; index <= end; index++) {
                    distinctValues.add(nums[index]);
                }
                if (distinctValues.size() == k) {
                    count++;
                }
            }
        }
        return count;
    }

    /* ---------------------------------------------------------------------------
     * APPROACH 2: Hashing + Incremental Double Loop (Brute Force, Optimized)
     * ---------------------------------------------------------------------------
     * Core idea: Fix the start index, then extend `end` one step at a time,
     * incrementally updating a frequency HashMap instead of recomputing distinct
     * count from scratch. This removes the innermost O(n) recount loop.
     * Data structure: HashMap<Integer, Integer> frequency counter, reset per
     * start index.
     * Time Complexity: O(n^2) — n choices for start, each doing O(n) incremental
     * work for end.
     * Space Complexity: O(n) worst case for the frequency map (all distinct
     * values in one subarray).
     * Pros: Much better than pure brute force; still simple to explain and code
     * under interview time pressure; correctly demonstrates incremental-update
     * thinking, which is a stepping stone to the optimal sliding window.
     * Cons: Still O(n^2), which risks TLE at n = 2*10^4 (up to ~4*10^8 basic ops).
     * When to use: As an intermediate "clean and correct" solution to state
     * verbally before presenting the optimal approach — good for demonstrating
     * incremental progress in an interview, but not the final answer.
     */
    public long hashingIncrementalDoubleLoop(int[] nums, int k) {
        int n = nums.length;
        long count = 0;
        for (int start = 0; start < n; start++) {
            Map<Integer, Integer> frequency = new HashMap<>();
            int distinctCount = 0;
            for (int end = start; end < n; end++) {
                int currentValue = nums[end];
                int previousFrequency = frequency.getOrDefault(currentValue, 0);
                if (previousFrequency == 0) {
                    distinctCount++;
                }
                frequency.put(currentValue, previousFrequency + 1);

                if (distinctCount == k) {
                    count++;
                } else if (distinctCount > k) {
                    // Once distinctCount exceeds k, extending `end` further from
                    // this `start` can never bring it back down to exactly k,
                    // since we only ever add values going right. Safe to break.
                    break;
                }
            }
        }
        return count;
    }

    /* ---------------------------------------------------------------------------
     * APPROACH 3 (OPTIMAL): Sliding Window via "At Most K" Trick
     * ---------------------------------------------------------------------------
     * Core idea: Directly counting "exactly k distinct" subarrays with a single
     * sliding window is awkward because distinct-count is not monotonic when the
     * window can both grow AND the "exactly k" target can be crossed in either
     * direction as we shrink from the left. However, "AT MOST k distinct" IS
     * monotonic in a very clean way: for a fixed left boundary, as right boundary
     * increases, distinct count never decreases. This lets us compute, for every
     * right boundary, the number of valid left boundaries via a classic
     * two-pointer sliding window in O(n).
     *
     * The key transformation:
     *     exactly(k) = atMost(k) - atMost(k - 1)
     *
     * atMost(K) is computed by maintaining a window [left, right] that always
     * has at most K distinct values, greedily shrinking from the left whenever
     * distinct count exceeds K. For each right pointer position, the number of
     * valid subarrays ending at `right` with at most K distinct values is
     * exactly (right - left + 1) — every subarray from any start in [left, right]
     * to `right` also has at most K distinct values (since shrinking a window
     * can only keep or reduce distinct count).
     *
     * Data structure: HashMap<Integer, Integer> frequency counter + two pointers.
     * Paradigm: Sliding window / two pointer, combined with a greedy shrink rule.
     *
     * Time Complexity: O(n) — each of left and right pointers moves forward at
     * most n times total across the whole scan (amortized), and atMost(K) is
     * called twice, so overall O(n).
     * Space Complexity: O(k) — the frequency map holds at most K+1 distinct keys
     * at any time before a shrink is triggered.
     *
     * Pros: Optimal linear time; reuses a single well-tested helper method
     * twice, minimizing surface area for bugs; the "exactly = atMost(k) -
     * atMost(k-1)" trick is a well-known, elegant, and highly reusable pattern.
     * Cons: The subtraction trick is a "known trick" — if you haven't seen it
     * before, deriving it live under interview pressure is non-trivial; also,
     * it requires convincing yourself (and the interviewer) of the monotonicity
     * argument, which needs to be verbalized clearly, not just coded.
     * When to use: This is the production-quality, interview-final answer for
     * this exact problem. Use it whenever the target is an EXACT distinct-count
     * constraint over a static array processed once.
     */
    public long slidingWindowAtMostKTrick(int[] nums, int k) {
        // exactly(k) = atMost(k) - atMost(k - 1)
        return atMostKDistinct(nums, k) - atMostKDistinct(nums, k - 1);
    }

    /**
     * Counts the number of contiguous subarrays containing AT MOST
     * `maxDistinct` distinct values. Returns 0 immediately if maxDistinct < 0,
     * which correctly handles the k = 0 edge case when called as atMost(k - 1)
     * with k = 0... though per our assumed constraint k >= 1, this guard is
     * purely defensive.
     */
    private long atMostKDistinct(int[] nums, int maxDistinct) {
        if (maxDistinct < 0) {
            return 0;
        }
        int n = nums.length;
        Map<Integer, Integer> frequency = new HashMap<>();
        long subarrayCount = 0;
        int left = 0;
        int distinctCount = 0;

        for (int right = 0; right < n; right++) {
            int rightValue = nums[right];
            int rightFrequency = frequency.getOrDefault(rightValue, 0);
            if (rightFrequency == 0) {
                distinctCount++;
            }
            frequency.put(rightValue, rightFrequency + 1);

            // Greedily shrink the window from the left while we have too many
            // distinct values. Each shrink either removes a value entirely
            // (distinctCount--) or just reduces its frequency.
            while (distinctCount > maxDistinct) {
                int leftValue = nums[left];
                int leftFrequency = frequency.get(leftValue);
                if (leftFrequency == 1) {
                    frequency.remove(leftValue);
                    distinctCount--;
                } else {
                    frequency.put(leftValue, leftFrequency - 1);
                }
                left++;
            }

            // Every subarray starting anywhere in [left, right] and ending at
            // `right` has at most `maxDistinct` distinct values, because
            // shrinking the start further right can only reduce or maintain
            // the distinct count, never increase it.
            subarrayCount += (right - left + 1);
        }
        return subarrayCount;
    }

    /* ---------------------------------------------------------------------------
     * APPROACH 4: Direct "Exactly K" Sliding Window with Two Left Pointers
     * ---------------------------------------------------------------------------
     * Core idea: Instead of calling atMost() twice, maintain two left pointers
     * simultaneously: `lowerBound` (the leftmost index such that the window has
     * at most k distinct values) and `upperBound` (the leftmost index such that
     * the window has at most k-1 distinct values, i.e., upperBound >= lowerBound
     * always). For each right pointer, the count of subarrays ending at `right`
     * with EXACTLY k distinct values is (upperBound - lowerBound). This is
     * mathematically identical to the atMost(k) - atMost(k-1) trick, just fused
     * into a single pass with two frequency maps instead of two separate scans.
     * Data structure: Two HashMaps, two left pointers, one right pointer.
     * Paradigm: Sliding window / two pointer (fused variant).
     * Time Complexity: O(n) — same amortized argument as Approach 3, single pass.
     * Space Complexity: O(k) for each of the two frequency maps -> O(k) overall.
     * Pros: Single pass instead of two separate calls to atMost(); can feel more
     * "direct" once understood; same asymptotic complexity as Approach 3.
     * Cons: More state to track simultaneously (two maps, two pointers), making
     * it more error-prone to code correctly under interview time pressure
     * compared to the cleaner two-call decomposition in Approach 3. I'd only
     * reach for this if explicitly asked for a single-pass fused version.
     * When to use: When an interviewer specifically pushes for "can you do it
     * in one pass without calling a helper twice?" — otherwise Approach 3 is
     * cleaner and equally optimal asymptotically.
     */
    public long slidingWindowFusedTwoPointer(int[] nums, int k) {
        int n = nums.length;
        Map<Integer, Integer> frequencyAtMostK = new HashMap<>();
        Map<Integer, Integer> frequencyAtMostKMinusOne = new HashMap<>();
        int distinctAtMostK = 0;
        int distinctAtMostKMinusOne = 0;
        int lowerBound = 0;  // leftmost index keeping window within "at most k"
        int upperBound = 0;  // leftmost index keeping window within "at most k-1"
        long count = 0;

        for (int right = 0; right < n; right++) {
            int value = nums[right];

            int freqK = frequencyAtMostK.getOrDefault(value, 0);
            if (freqK == 0) distinctAtMostK++;
            frequencyAtMostK.put(value, freqK + 1);

            int freqKMinusOne = frequencyAtMostKMinusOne.getOrDefault(value, 0);
            if (freqKMinusOne == 0) distinctAtMostKMinusOne++;
            frequencyAtMostKMinusOne.put(value, freqKMinusOne + 1);

            while (distinctAtMostK > k) {
                int leftValue = nums[lowerBound];
                int freq = frequencyAtMostK.get(leftValue);
                if (freq == 1) {
                    frequencyAtMostK.remove(leftValue);
                    distinctAtMostK--;
                } else {
                    frequencyAtMostK.put(leftValue, freq - 1);
                }
                lowerBound++;
            }

            while (distinctAtMostKMinusOne > Math.max(k - 1, 0)) {
                int leftValue = nums[upperBound];
                int freq = frequencyAtMostKMinusOne.get(leftValue);
                if (freq == 1) {
                    frequencyAtMostKMinusOne.remove(leftValue);
                    distinctAtMostKMinusOne--;
                } else {
                    frequencyAtMostKMinusOne.put(leftValue, freq - 1);
                }
                upperBound++;
            }

            // upperBound is always >= lowerBound; the gap is exactly the number
            // of valid start positions giving exactly k distinct values ending
            // at `right`.
            count += (upperBound - lowerBound);
        }
        return count;
    }

    /*
     * =================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * =================================================================================
     *
     * | Approach                                   | Time     | Space | Best For                          | Limitations                                  |
     * |---------------------------------------------|----------|-------|-----------------------------------|-----------------------------------------------|
     * | 1. Brute Force Triple Loop                  | O(n^3)   | O(n)  | Oracle for testing only           | Unusable at scale; far too slow               |
     * | 2. Hashing + Incremental Double Loop         | O(n^2)   | O(n)  | Intermediate "clean" solution      | TLE risk at n ~ 2*10^4                        |
     * | 3. Sliding Window "At Most K" Trick (BEST)   | O(n)     | O(k)  | Final interview answer             | Requires knowing/deriving the subtraction trick|
     * | 4. Fused Two-Pointer Direct "Exactly K"      | O(n)     | O(k)  | Single-pass follow-up variant       | More stateful, more error-prone to code live   |
     */

    /*
     * =================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * =================================================================================
     * I would present Approach 3 (Sliding Window via "At Most K" Trick) as my
     * final answer, for these reasons:
     *
     * 1. Clarity: The atMost(K) helper is a single, well-understood building
     *    block (itself a simpler, very standard sliding-window pattern —
     *    "count subarrays with at most K distinct values"), and the exactly(k)
     *    = atMost(k) - atMost(k-1) identity is a clean, easily-verbalized
     *    mathematical argument rather than a tangle of simultaneous pointer state.
     * 2. Coding speed: Writing one correct helper and calling it twice is faster
     *    and less bug-prone under time pressure than juggling two frequency maps
     *    and two pointers simultaneously (as in Approach 4).
     * 3. Interviewer expectations: For a Hard-rated, Google-tagged problem like
     *    this, interviewers expect you to recognize that "exactly" constraints
     *    often decompose into "at most" differences — demonstrating that
     *    recognition is itself a signal of strong pattern-matching ability.
     * 4. Optimality: O(n) time, O(k) space — asymptotically optimal, since we
     *    must at least read every element once.
     *
     * My interview delivery plan: verbally state the brute-force O(n^3) idea
     * first for a baseline, quickly note the O(n^2) incremental improvement,
     * then pivot to explaining the atMost(K) monotonicity insight, derive the
     * exactly(k) = atMost(k) - atMost(k-1) identity on the whiteboard, and code
     * Approach 3 as the final, fully-tested solution. I would only code Approach
     * 4 if explicitly asked for a single-pass fused variant as a follow-up.
     */

    /*
     * =================================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * =================================================================================
     * See `slidingWindowAtMostKTrick` and its helper `atMostKDistinct` above —
     * those ARE the polished, final versions, already carrying full inline
     * comments explaining every design decision (greedy shrink, monotonicity
     * argument, and the subtraction identity). I re-list the public entry point
     * here for clarity as the one true "call this in production" method:
     *
     *     public long slidingWindowAtMostKTrick(int[] nums, int k)
     *
     * Production hardening notes I'd mention verbally:
     *   - Defensive null/empty checks would be added at the public API boundary
     *     in a real production setting (omitted here per LeetCode-style
     *     constraints assuming valid, non-null input).
     *   - Using `long` for the accumulator avoids any theoretical overflow risk
     *     even though it's not strictly necessary at n = 2*10^4.
     *   - HashMap is chosen over an int[] array for generality (arbitrary value
     *     ranges); if the interviewer confirms nums[i] is bounded to [1, n], I
     *     would swap in an int[] frequency array of size (n+1) to shave constant
     *     factors (avoiding boxing/unboxing and hash collisions).
     */

    /*
     * =================================================================================
     * SECTION 10: DRY RUN / TRACE
     * =================================================================================
     * Tracing `slidingWindowAtMostKTrick(nums = [1,2,1,2,3], k = 2)`
     *
     * Step A: Compute atMost(2) via atMostKDistinct(nums, 2)
     * ---------------------------------------------------------------
     * right=0 (val=1): freq={1:1}, distinctCount=1 (<=2, no shrink)
     *                  window=[0,0], contributes (0-0+1)=1  -> subarrayCount=1
     * right=1 (val=2): freq={1:1,2:1}, distinctCount=2 (<=2, no shrink)
     *                  window=[0,1], contributes (1-0+1)=2  -> subarrayCount=3
     * right=2 (val=1): freq={1:2,2:1}, distinctCount=2 (<=2, no shrink)
     *                  window=[0,2], contributes (2-0+1)=3  -> subarrayCount=6
     * right=3 (val=2): freq={1:2,2:2}, distinctCount=2 (<=2, no shrink)
     *                  window=[0,3], contributes (3-0+1)=4  -> subarrayCount=10
     * right=4 (val=3): freq={1:2,2:2,3:1}, distinctCount=3 (>2, must shrink!)
     *     shrink: left=0, leftValue=1, freq[1]=2 -> decrement to 1 (not removed,
     *             distinctCount stays 3) left becomes 1
     *     distinctCount still 3 (>2), shrink again: left=1, leftValue=2,
     *             freq[2]=2 -> decrement to 1, distinctCount stays 3, left becomes 2
     *     distinctCount still 3 (>2), shrink again: left=2, leftValue=1,
     *             freq[1]=1 -> decrement to 0 -> REMOVE key 1, distinctCount
     *             drops to 2, left becomes 3. Loop stops (2 <= 2).
     *     window=[3,4], contributes (4-3+1)=2  -> subarrayCount=12
     * atMost(2) = 12
     *
     * Step B: Compute atMost(1) via atMostKDistinct(nums, 1)
     * ---------------------------------------------------------------
     * right=0 (val=1): freq={1:1}, distinctCount=1 (<=1) window=[0,0]
     *                  contributes 1 -> subarrayCount=1
     * right=1 (val=2): freq={1:1,2:1}, distinctCount=2 (>1, shrink!)
     *     shrink: left=0, leftValue=1, freq[1]=1->remove, distinctCount=1,
     *             left becomes 1. Loop stops (1<=1).
     *     window=[1,1], contributes 1 -> subarrayCount=2
     * right=2 (val=1): freq={2:1,1:1}, distinctCount=2 (>1, shrink!)
     *     shrink: left=1, leftValue=2, freq[2]=1->remove, distinctCount=1,
     *             left becomes 2. Loop stops.
     *     window=[2,2], contributes 1 -> subarrayCount=3
     * right=3 (val=2): freq={1:1,2:1}, distinctCount=2 (>1, shrink!)
     *     shrink: left=2, leftValue=1, freq[1]=1->remove, distinctCount=1,
     *             left becomes 3. Loop stops.
     *     window=[3,3], contributes 1 -> subarrayCount=4
     * right=4 (val=3): freq={2:1,3:1}, distinctCount=2 (>1, shrink!)
     *     shrink: left=3, leftValue=2, freq[2]=1->remove, distinctCount=1,
     *             left becomes 4. Loop stops.
     *     window=[4,4], contributes 1 -> subarrayCount=5
     * atMost(1) = 5
     *
     * Final: exactly(2) = atMost(2) - atMost(1) = 12 - 5 = 7
     * This matches the expected answer of 7 from Section 3, Example 1.
     */

    /*
     * =================================================================================
     * SECTION 11: CLOSING SUMMARY
     * =================================================================================
     * - Brute force (O(n^3)) and the incremental double loop (O(n^2)) are both
     *   correct but too slow for the stated constraints (n up to 2*10^4);
     *   they're useful only as stepping stones and testing oracles.
     * - The optimal solution runs in O(n) time and O(k) space by exploiting the
     *   monotonicity of "at most K distinct" under a sliding window, then using
     *   the identity exactly(k) = atMost(k) - atMost(k-1).
     * - Assumptions baked into the final solution: nums is non-null with
     *   length >= 1, k >= 1, and values may be arbitrary integers (hence
     *   HashMap-based frequency counting rather than array-indexed counting).
     * - Known limitation: if the interviewer reveals nums[i] is tightly bounded
     *   (e.g., 1..n), an int[] frequency array would be a strictly faster
     *   constant-factor alternative to the HashMap, though same O(n) asymptotic
     *   complexity.
     */

    /*
     * =================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * =================================================================================
     * 1. "Can you solve this with 'at least k distinct' instead of 'exactly k'?"
     *    -> atLeast(k) = totalSubarrays - atMost(k-1), reusing the same helper.
     * 2. "What if k varies across many queries on the same fixed array — can you
     *    amortize the cost across queries?" -> Discuss precomputing per-right-
     *    pointer structures or Mo's algorithm for offline range-distinct queries.
     * 3. "What if the array can be updated (point updates) between queries?"
     *    -> This would push toward a Fenwick Tree / segment tree over
     *    "last occurrence" indices, since our simple two-pointer approach
     *    assumes a static array processed once.
     * 4. "Can you do this with O(1) extra space instead of O(k)?" -> Only if
     *    values are tightly bounded and you're allowed to mutate an auxiliary
     *    fixed-size array reused across calls; otherwise O(k) frequency storage
     *    is essentially required.
     * 5. "How would you parallelize this over a very large array?" -> Discuss
     *    splitting into chunks, solving each chunk's internal atMost(K) counts
     *    in parallel, then handling boundary-crossing subarrays separately — a
     *    non-trivial merge step, good for demonstrating systems-level thinking.
     * 6. "What if we wanted the actual list of subarrays, not just the count?"
     *    -> Discuss the memory blowup risk (up to O(n^2) subarrays) and how
     *    you'd need to decide on a lazy/streaming representation instead of
     *    materializing everything.
     */

    /*
     * =================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * =================================================================================
     * 1. Trying to solve "exactly k" with a single naive sliding window directly
     *    (expanding/shrinking to hit exactly k) — this breaks because distinct
     *    count is not monotonic in the way needed; candidates get stuck oscillating
     *    the window and mishandling the case where distinctCount jumps from
     *    below k to above k without ever equaling k exactly for some window shapes.
     * 2. Forgetting the `k - 1` edge case when k = 1, i.e., not guarding
     *    atMost(k-1) against negative k, which should simply return 0 rather
     *    than throwing or misbehaving.
     * 3. Off-by-one errors in the contribution formula (right - left + 1) —
     *    candidates often write (right - left) and undercount, or forget that
     *    this formula must be applied AFTER the shrink loop has fully
     *    stabilized the window for the current `right`, not before.
     * 4. Assuming frequency map entries can go to 0 and remain in the map —
     *    forgetting to actually `remove()` the key (leaving a stale 0-count
     *    entry) silently corrupts the distinctCount bookkeeping on subsequent
     *    iterations, since `getOrDefault` would see a stale non-null value if
     *    checked incorrectly, or a naive `.size()`-based distinct count check
     *    would overcount.
     */

    /*
     * =================================================================================
     * TEST HARNESS — assertion-based correctness oracle (brute force vs optimal)
     * =================================================================================
     */
    public static void main(String[] args) {
        SubarraysWithKDistinctIntegers solver = new SubarraysWithKDistinctIntegers();

        // --- Worked examples from Section 3 ---
        int[] example1 = {1, 2, 1, 2, 3};
        assert solver.bruteForceTripleLoop(example1, 2) == 7;
        assert solver.hashingIncrementalDoubleLoop(example1, 2) == 7;
        assert solver.slidingWindowAtMostKTrick(example1, 2) == 7;
        assert solver.slidingWindowFusedTwoPointer(example1, 2) == 7;
        System.out.println("Example 1 passed: exactly(2) on [1,2,1,2,3] = 7");

        int[] example2 = {1, 2, 1, 3, 4};
        assert solver.slidingWindowAtMostKTrick(example2, 3) == 3;
        assert solver.slidingWindowFusedTwoPointer(example2, 3) == 3;
        System.out.println("Example 2 passed: exactly(3) on [1,2,1,3,4] = 3");

        int[] example3 = {1, 1, 1};
        assert solver.slidingWindowAtMostKTrick(example3, 1) == 6;
        assert solver.slidingWindowFusedTwoPointer(example3, 1) == 6;
        System.out.println("Example 3 passed: exactly(1) on [1,1,1] = 6");

        // --- Edge case: k exceeds total distinct values present ---
        int[] edgeCase = {1, 2, 3};
        assert solver.slidingWindowAtMostKTrick(edgeCase, 5) == 0;
        System.out.println("Edge case passed: exactly(5) on [1,2,3] = 0");

        // --- Randomized stress test: brute force vs optimal, cross-validation ---
        Random random = new Random(42);
        for (int trial = 0; trial < 500; trial++) {
            int length = 1 + random.nextInt(10);
            int[] randomArray = new int[length];
            for (int index = 0; index < length; index++) {
                randomArray[index] = 1 + random.nextInt(4); // small value range
            }
            int k = 1 + random.nextInt(length);

            long bruteResult = solver.bruteForceTripleLoop(randomArray, k);
            long optimalResult = solver.slidingWindowAtMostKTrick(randomArray, k);
            long fusedResult = solver.slidingWindowFusedTwoPointer(randomArray, k);

            if (bruteResult != optimalResult || bruteResult != fusedResult) {
                throw new AssertionError(
                    "Mismatch on trial " + trial +
                    " array=" + Arrays.toString(randomArray) +
                    " k=" + k +
                    " brute=" + bruteResult +
                    " optimal=" + optimalResult +
                    " fused=" + fusedResult
                );
            }
        }
        System.out.println("Randomized stress test passed: 500/500 trials matched across all approaches.");

        System.out.println("All tests passed.");
    }
}
