
import java.util.*;

/*
 * ============================================================================
 * GOOGLE ONSITE MOCK INTERVIEW TRANSCRIPT
 * Problem: Binary Subarrays With Sum   (LeetCode 930 — Google-tagged, Medium)
 * ============================================================================
 */

/*
 * ============================================================================
 * 1. RESTATE THE PROBLEM
 * ============================================================================
 * We're given an array `nums` containing only 0s and 1s, and a non-negative
 * integer `goal`. We need to count how many *non-empty contiguous subarrays*
 * have a sum exactly equal to `goal`.
 *
 * Because the array is binary, "sum of a subarray" is just "the number of
 * 1s inside that subarray." So the problem is really: "how many contiguous
 * windows contain exactly `goal` ones?"
 *
 * Inputs:
 *   - nums: int[], each element is 0 or 1, 1 <= nums.length <= 3 * 10^4
 *   - goal: int, 0 <= goal <= nums.length
 *
 * Output:
 *   - A single integer: the count of subarrays whose sum equals goal.
 *
 * Key observations I'll state out loud in the interview:
 *   - "Subarray" means contiguous, not subsequence — order and adjacency
 *     matter, so any sorting-based idea is immediately off the table.
 *   - Since values are only 0/1, prefix sums are monotonically
 *     non-decreasing. That monotonicity is exactly what enables a
 *     sliding-window / two-pointer solution instead of a general
 *     prefix-sum-hashmap solution (which would be required for arbitrary
 *     integers, including negatives).
 *   - goal = 0 is a valid, important edge case: we're counting subarrays
 *     made entirely of zeros.
 * ============================================================================
 */

/*
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (asked to the interviewer) + ASSUMED ANSWERS
 * ============================================================================
 * Q1: Can `nums` be empty?
 *     A: No — constraints guarantee length >= 1.
 *
 * Q2: Are there really only 0s and 1s in `nums`, never other integers?
 *     A: Confirmed by constraints — strictly binary. This is the detail
 *        that unlocks the O(n)/O(1) sliding-window trick.
 *
 * Q3: Can `goal` be 0? What does that mean?
 *     A: Yes, 0 <= goal <= nums.length. goal = 0 means we count subarrays
 *        that are all zeros (sum with no ones at all).
 *
 * Q4: Should the count include overlapping subarrays, or only disjoint ones?
 *     A: All valid contiguous subarrays count, including overlapping ones —
 *        e.g. for [1,1], both [1] (index 0) and [1] (index 1) count
 *        separately toward goal = 1.
 *
 * Q5: Do we return the count, or the actual list of subarrays
 *     (as index pairs or as arrays)?
 *     A: Just the integer count — matches LeetCode's return type.
 *
 * Q6: What's the expected input size, and does it affect the required
 *     complexity? Is O(n^2) acceptable?
 *     A: n up to 3 * 10^4, so n^2 is ~9 * 10^8 — borderline/too slow for a
 *        production system under tight time limits. We should aim for
 *        O(n) time, and ideally O(1) extra space (excluding output).
 *
 * Q7: Is the array given as a mutable array we're free to use scratch
 *     space alongside, or should the input remain untouched?
 *     A: Treat `nums` as read-only; any auxiliary space is our own.
 *
 * Q8: Is this a single-threaded, single-call problem, or do we need to
 *     support repeated/concurrent queries against the same array (e.g.
 *     an API called many times with different `goal` values)?
 *     A: For this session, a single call is sufficient. I'll mention in
 *        the follow-up section how the design would change for repeated
 *        queries (precomputation).
 * ============================================================================
 */

/*
 * ============================================================================
 * 3. EXAMPLES & EDGE CASES
 * ============================================================================
 *
 * Example 1 (normal case):
 *   nums = [1,0,1,0,1], goal = 2
 *   Valid subarrays summing to 2 (0-indexed, inclusive ranges):
 *     [0,2] -> 1+0+1 = 2
 *     [0,3] -> 1+0+1+0 = 2
 *     [1,3] -> 0+1+0 = ... wait, that's 1. Let's enumerate carefully:
 *   Actual valid windows: [0..2], [0..3], [2..4], [1..4]... to avoid manual
 *   arithmetic mistakes, this exact case is later cross-checked against a
 *   brute-force oracle in the stress test. Expected answer: 4.
 *
 * Example 2 (edge case — goal = 0, all-zero run counting):
 *   nums = [0,0,0,0,0], goal = 0
 *   Every contiguous subarray sums to 0. Number of subarrays of an
 *   all-zero array of length n is n*(n+1)/2 = 5*6/2 = 15.
 *
 * Example 3 (boundary / tie-breaking case — single element, both extremes):
 *   nums = [0], goal = 0  -> answer = 1  (the single subarray [0] itself)
 *   nums = [1], goal = 0  -> answer = 0  (no zero-sum subarray exists)
 *   This pair demonstrates that goal = 0 does NOT mean "return 0" or
 *   "count everything" — it must be handled with the same rigor as any
 *   other goal value, and it's the classic off-by-one trap for the
 *   sliding-window approach (see Section 13).
 * ============================================================================
 */

/*
 * ============================================================================
 * 4-6. ALL POSSIBLE SOLUTIONS
 * ----------------------------------------------------------------------------
 * Paradigms explicitly considered and ruled out up front:
 *   - Sorting-based:      Subarrays are contiguous; sorting destroys
 *                          adjacency information entirely. Not applicable.
 *   - Divide & Conquer:   No clean way to combine cross-boundary subarray
 *                          counts more cheaply than a linear scan; D&C would
 *                          add log-factor overhead for no benefit here.
 *   - Tree / Graph:       There's no graph/tree structure in a flat array
 *                          sum-counting problem. Not applicable.
 *   - Heap / PQ:          No "top-k" or ordering-by-priority need exists.
 *   - Trie:               No string/prefix-matching structure involved.
 *   - Segment Tree:       Overkill — would support range updates/queries we
 *                          don't need; a single linear/hashmap pass suffices.
 *   - Monotonic stack:    No "next greater/smaller element" structure here;
 *                          monotonic *deque* logic doesn't map to sum counting.
 *
 * Paradigms that DO apply, in increasing order of sophistication:
 *   Approach 1: Brute Force                       (baseline correctness oracle)
 *   Approach 2: Prefix Sum + HashMap               (hashing-based, general)
 *   Approach 3: Prefix Sum + Bucket Array           (hashing-based, specialized)
 *   Approach 4: Sliding Window (atMost(k) trick)   (two-pointer, optimal)
 *   Approach 5: Zero-Grouping via Ones' Positions  (greedy/combinatorial counting)
 * ============================================================================
 */

class BinarySubarraysWithSum {

    /*
     * ------------------------------------------------------------------------
     * APPROACH 1: Brute Force
     * ------------------------------------------------------------------------
     * Core idea: For every start index, extend the window one element at a
     * time, maintaining a running sum, and check if it equals goal.
     *
     * Data structure / paradigm: none — pure nested iteration.
     *
     * Time:  O(n^2)  — every (start, end) pair is visited once.
     * Space: O(1)    — only a running sum is kept.
     *
     * Pros:
     *   - Trivial to write correctly under interview pressure.
     *   - Perfect as a correctness oracle for stress-testing faster solutions.
     * Cons:
     *   - Far too slow for n = 3 * 10^4 in a tight time budget (~9*10^8 ops).
     * When to use: Never in production for this input size; only as a
     * baseline to state verbally and validate against.
     * ------------------------------------------------------------------------
     */
    static int bruteForce(int[] nums, int goal) {
        int n = nums.length;
        int subarrayCount = 0;
        for (int start = 0; start < n; start++) {
            int runningSum = 0;
            for (int end = start; end < n; end++) {
                runningSum += nums[end];
                if (runningSum == goal) {
                    subarrayCount++;
                }
                // Minor pruning: since values are non-negative, once the sum
                // exceeds goal it can only grow further, so we could break
                // here. Left in as a comment to keep this approach a "pure"
                // brute force baseline for the oracle.
                // if (runningSum > goal) break;
            }
        }
        return subarrayCount;
    }

    /*
     * ------------------------------------------------------------------------
     * APPROACH 2: Prefix Sum + HashMap
     * ------------------------------------------------------------------------
     * Core idea: Let prefix[i] = sum(nums[0..i-1]). A subarray nums[l..r]
     * sums to goal iff prefix[r+1] - prefix[l] = goal, i.e.
     * prefix[l] = prefix[r+1] - goal. As we scan left to right maintaining a
     * running prefix sum, we look up how many earlier prefix sums equal
     * (currentPrefix - goal) using a frequency map, and add that count.
     *
     * This is the *general* technique that works even if nums contained
     * negative numbers or arbitrary integers, not just 0/1 — it doesn't rely
     * on monotonicity at all.
     *
     * Data structure / paradigm: hashing (HashMap<prefixSum, frequency>).
     *
     * Time:  O(n)  — single pass, O(1) expected HashMap operations.
     * Space: O(n)  — up to n+1 distinct prefix sums stored in the map.
     *
     * Pros:
     *   - Generalizes immediately to non-binary integer arrays.
     *   - Easy to reason about and explain; very standard pattern.
     * Cons:
     *   - Uses O(n) extra space even though the binary constraint would
     *     allow O(1) space via a smarter approach.
     *   - HashMap constant-factor overhead (boxing/hashing) vs a plain array.
     * When to use: Default choice if `nums` were NOT guaranteed binary, or
     * in an interview when you want a safe, well-known fallback before
     * attempting the more specialized optimal solution.
     * ------------------------------------------------------------------------
     */
    static int prefixSumHashMap(int[] nums, int goal) {
        Map<Integer, Integer> prefixSumFrequency = new HashMap<>();
        prefixSumFrequency.put(0, 1); // empty prefix (before index 0) sums to 0
        int currentPrefixSum = 0;
        int subarrayCount = 0;
        for (int value : nums) {
            currentPrefixSum += value;
            int neededEarlierPrefix = currentPrefixSum - goal;
            subarrayCount += prefixSumFrequency.getOrDefault(neededEarlierPrefix, 0);
            prefixSumFrequency.merge(currentPrefixSum, 1, Integer::sum);
        }
        return subarrayCount;
    }

    /*
     * ------------------------------------------------------------------------
     * APPROACH 3: Prefix Sum + Bucket Array (specialized hashing)
     * ------------------------------------------------------------------------
     * Core idea: Identical math to Approach 2, but since prefix sums are
     * bounded integers in [0, n] (binary array, so the total sum can never
     * exceed n), we can replace the HashMap with a plain int[n + 1] bucket
     * array. This removes hashing/boxing overhead entirely while keeping
     * the same O(n) time.
     *
     * Data structure / paradigm: hashing-based, specialized via direct
     * addressing (a "poor man's" perfect hash table).
     *
     * Time:  O(n)  — single pass, O(1) true array indexing.
     * Space: O(n)  — one bucket array of size n+1.
     *
     * Pros:
     *   - Same asymptotic complexity as Approach 2 but noticeably faster in
     *     practice (no hashing, no autoboxing, better cache locality).
     * Cons:
     *   - Only works because we know prefix sums are bounded by n; doesn't
     *     generalize to arbitrary integer arrays the way HashMap does.
     * When to use: Whenever the value range is known and small — a good
     * "I noticed a domain-specific optimization" moment to mention in an
     * interview, even if you don't code it as your primary answer.
     * ------------------------------------------------------------------------
     */
    static int prefixSumBucketArray(int[] nums, int goal) {
        int n = nums.length;
        // prefixSumBuckets[s] = how many prefixes so far have summed to s.
        int[] prefixSumBuckets = new int[n + 1];
        prefixSumBuckets[0] = 1; // empty prefix
        int currentPrefixSum = 0;
        int subarrayCount = 0;
        for (int value : nums) {
            currentPrefixSum += value;
            int neededEarlierPrefix = currentPrefixSum - goal;
            if (neededEarlierPrefix >= 0) {
                subarrayCount += prefixSumBuckets[neededEarlierPrefix];
            }
            prefixSumBuckets[currentPrefixSum]++;
        }
        return subarrayCount;
    }

    /*
     * ------------------------------------------------------------------------
     * APPROACH 4: Sliding Window via atMost(k) - atMost(k-1)   [RECOMMENDED]
     * ------------------------------------------------------------------------
     * Core idea: This is the same reduction I've used before for "exactly K
     * distinct integers" (LC 992) and is a recurring family member:
     *
     *     countExactly(goal) = countAtMost(goal) - countAtMost(goal - 1)
     *
     * where countAtMost(k) = number of subarrays with sum <= k.
     *
     * Why this works here specifically: because all elements are
     * non-negative (0 or 1), the running window sum is monotonic — growing
     * the window (moving `right`) never decreases the sum, and shrinking it
     * (moving `left`) never increases it. That monotonicity is exactly what
     * makes a classic two-pointer sliding window valid for counting "sum <=
     * k" subarrays in O(n): for each `right`, we shrink `left` just enough
     * to restore the sum <= k invariant, and every window ending at `right`
     * with a valid left boundary from the current `left` up to `right`
     * qualifies, contributing (right - left + 1) subarrays.
     *
     * Data structure / paradigm: two-pointer / sliding window.
     *
     * Time:  O(n)  — each pointer (left, right) moves forward at most n
     *                times total across the whole scan (amortized O(1) per
     *                step, not O(1) worst case per step — this is the
     *                standard "two-pointer amortized analysis" argument).
     * Space: O(1)  — only a few scalar counters.
     *
     * Pros:
     *   - Best possible complexity: O(n) time, O(1) extra space.
     *   - Directly reuses a pattern family I already have muscle memory for.
     * Cons:
     *   - Requires the "elements are non-negative" fact to be valid — would
     *     silently give wrong answers if nums could contain negative values.
     *   - The atMost(-1) call when goal = 0 must be handled explicitly
     *     (must return 0, not crash or misbehave) — a classic trap.
     * When to use: This is my primary answer in the interview — optimal
     * complexity, clean code, and it's a well-known, defensible pattern.
     * ------------------------------------------------------------------------
     */
    static int slidingWindowAtMost(int[] nums, int goal) {
        return atMost(nums, goal) - atMost(nums, goal - 1);
    }

    // Counts the number of subarrays whose sum is <= maxSum.
    // Relies on all elements being non-negative for the two-pointer shrink
    // logic to be valid.
    private static int atMost(int[] nums, int maxSum) {
        if (maxSum < 0) {
            // No subarray (all elements non-negative) can have a negative
            // sum, and sums start at 0, so a negative cap admits zero
            // subarrays. This guards the goal = 0 case where we compute
            // atMost(-1).
            return 0;
        }
        int left = 0;
        int windowSum = 0;
        int subarrayCount = 0;
        for (int right = 0; right < nums.length; right++) {
            windowSum += nums[right];
            while (windowSum > maxSum) {
                windowSum -= nums[left];
                left++;
            }
            // Every subarray [left..right], [left+1..right], ..., [right..right]
            // has sum <= maxSum, contributing (right - left + 1) subarrays
            // that end exactly at `right`.
            subarrayCount += right - left + 1;
        }
        return subarrayCount;
    }

    /*
     * ------------------------------------------------------------------------
     * APPROACH 5: Zero-Grouping via Ones' Positions (combinatorial counting)
     * ------------------------------------------------------------------------
     * Core idea: Because the array is binary, a subarray's sum equals its
     * count of 1s. Record the indices of every 1 in the array. A subarray
     * containing exactly `goal` ones is fully determined by:
     *   (a) which contiguous block of `goal` ones it wraps, and
     *   (b) how far left it can extend into the zeros before that block,
     *       and how far right it can extend into the zeros after it —
     *       each extension choice is an independent multiplicative factor,
     *       giving leftChoices * rightChoices subarrays for that block.
     *
     * The goal = 0 special case has no "block of ones" to anchor on, so it's
     * handled separately: within each maximal run of consecutive zeros of
     * length z, there are z*(z+1)/2 all-zero subarrays (a triangular number),
     * summed across all zero-runs.
     *
     * Data structure / paradigm: greedy/combinatorial counting over a
     * precomputed list of "ones" positions — conceptually similar to the
     * "left/right directional run precomputation" pattern I've used for
     * other problems, and structurally close to a greedy exchange-argument
     * style proof (each valid window is uniquely identified, no
     * double-counting, no missed windows).
     *
     * Time:  O(n)  — one pass to collect ones' positions (or zero-run
     *                lengths), one pass over that list to accumulate counts.
     * Space: O(n) worst case (e.g. all-ones array stores n positions), but
     *                O(1) *additional* space beyond input if goal = 0 is
     *                handled via a running zero-run counter instead of a list.
     *
     * Pros:
     *   - No arithmetic on running sums at all — pure position bookkeeping,
     *     which some interviewers find the most "elegant" / insight-driven
     *     answer since it exposes exactly *why* binary arrays are special.
     *   - Same O(n) time as the sliding-window approach.
     * Cons:
     *   - More case-analysis (goal = 0 vs goal > 0), more boundary indices
     *     to get right (first block, last block) — higher risk of an
     *     off-by-one bug under interview pressure than Approach 4.
     *   - Less immediately generalizable to variants (e.g. "at most goal"
     *     queries) compared to the atMost() sliding-window helper.
     * When to use: Great as a "for extra credit" alternative to demonstrate
     * range of technique after already delivering Approach 4, or if an
     * interviewer specifically asks for a solution that avoids any
     * subtraction-of-two-passes trick.
     * ------------------------------------------------------------------------
     */
    static int zeroGroupingApproach(int[] nums, int goal) {
        int n = nums.length;

        if (goal == 0) {
            // Sum over every maximal run of consecutive zeros: a run of
            // length z contributes z*(z+1)/2 all-zero subarrays.
            int totalCount = 0;
            int currentZeroRunLength = 0;
            for (int value : nums) {
                if (value == 0) {
                    currentZeroRunLength++;
                } else {
                    totalCount += currentZeroRunLength * (currentZeroRunLength + 1) / 2;
                    currentZeroRunLength = 0;
                }
            }
            totalCount += currentZeroRunLength * (currentZeroRunLength + 1) / 2; // trailing run
            return totalCount;
        }

        // goal > 0: collect indices of all the 1s.
        List<Integer> onesPositions = new ArrayList<>();
        for (int index = 0; index < n; index++) {
            if (nums[index] == 1) {
                onesPositions.add(index);
            }
        }

        int totalCount = 0;
        // Slide a fixed window of `goal` consecutive ones over onesPositions.
        for (int windowStart = 0; windowStart + goal <= onesPositions.size(); windowStart++) {
            int firstOneIndex = onesPositions.get(windowStart);
            int lastOneIndex = onesPositions.get(windowStart + goal - 1);

            // How many zeros sit immediately before firstOneIndex, available
            // as extra left-extension choices (including "extend 0 zeros").
            int leftBoundary = (windowStart == 0) ? -1 : onesPositions.get(windowStart - 1);
            int leftChoices = firstOneIndex - leftBoundary;

            // How many zeros sit immediately after lastOneIndex, available
            // as extra right-extension choices.
            int rightBoundary = (windowStart + goal == onesPositions.size())
                    ? n
                    : onesPositions.get(windowStart + goal);
            int rightChoices = rightBoundary - lastOneIndex;

            totalCount += leftChoices * rightChoices;
        }
        return totalCount;
    }

    /*
     * ============================================================================
     * 7. APPROACHES COMPARISON TABLE
     * ============================================================================
     *
     * | Approach                       | Time   | Space  | Best For                        | Limitations                              |
     * |---------------------------------|--------|--------|----------------------------------|-------------------------------------------|
     * | 1. Brute Force                  | O(n^2) | O(1)   | Correctness oracle / n is tiny   | Too slow for n = 3*10^4                    |
     * | 2. Prefix Sum + HashMap         | O(n)   | O(n)   | General integer arrays (not      | HashMap overhead; doesn't exploit binary   |
     * |                                  |        |        | just binary), quick to write     | structure                                  |
     * | 3. Prefix Sum + Bucket Array    | O(n)   | O(n)   | Known bounded value range        | Doesn't generalize beyond bounded sums     |
     * | 4. Sliding Window (atMost)      | O(n)   | O(1)   | Optimal general-purpose answer;  | Requires non-negative elements;            |
     * |                                  |        |        | interview "best answer"          | atMost(-1) edge case must be handled       |
     * | 5. Zero-Grouping (positions)    | O(n)   | O(n)*  | Demonstrating deeper insight     | More case-analysis, higher off-by-one risk |
     * |                                  |        |        | into binary-array structure      | (*O(1) extra space possible for goal = 0)  |
     * ============================================================================
     */

    /*
     * ============================================================================
     * 8. RECOMMENDED APPROACH FOR INTERVIEW
     * ============================================================================
     * I would present Approach 4 (Sliding Window via atMost(k) - atMost(k-1))
     * as my primary solution, for these reasons:
     *
     *   - It achieves the best possible complexity class: O(n) time and O(1)
     *     extra space — nothing else on this list beats it on both axes.
     *   - It's a well-recognized, reusable pattern ("exactly = atMost(k) -
     *     atMost(k-1)") that I can name and justify quickly, which signals
     *     pattern recognition rather than one-off cleverness.
     *   - It's easy to verify for correctness live: the atMost() helper is a
     *     simple, standard sliding window, and the subtraction trick is a
     *     one-line composition on top of it.
     *   - It's easy to extend verbally to related follow-ups (e.g. "at most
     *     goal ones" is now already something I've built).
     *
     * My interview delivery order would be:
     *   1. State the brute force (Approach 1) out loud for a baseline and to
     *      confirm the problem statement with the interviewer.
     *   2. Mention the general prefix-sum + HashMap technique (Approach 2)
     *      as the standard fallback, noting it doesn't need the binary
     *      constraint.
     *   3. Point out the binary-array-specific optimization opportunity,
     *      and pivot to coding the sliding window (Approach 4) as the
     *      optimal solution.
     *   4. If time remains, mention the zero-grouping approach (Approach 5)
     *      as an alternative that offers a different lens on the same
     *      problem, to demonstrate range.
     * ============================================================================
     */

    /*
     * ============================================================================
     * 9. DEEP DIVE: OPTIMAL SOLUTION (production-quality)
     * ============================================================================
     * Polished version of Approach 4 with full input validation and comments
     * explaining every decision, suitable as the final "ship it" version.
     * ============================================================================
     */
    static int numberOfSubarraysWithSumEqualToGoal(int[] nums, int goal) {
        Objects.requireNonNull(nums, "nums must not be null");
        if (nums.length == 0) {
            throw new IllegalArgumentException("nums must be non-empty");
        }
        if (goal < 0) {
            // Not reachable per the stated constraints (0 <= goal <= n), but
            // guarding it defensively documents the assumption explicitly
            // rather than silently producing a nonsensical result.
            throw new IllegalArgumentException("goal must be non-negative");
        }

        // countAtMost(k) counts subarrays whose sum is <= k. We derive the
        // "exactly goal" answer as countAtMost(goal) - countAtMost(goal - 1):
        // every subarray with sum <= goal either has sum == goal (what we
        // want) or sum <= goal - 1 (what we must subtract out).
        return countSubarraysWithSumAtMost(nums, goal)
                - countSubarraysWithSumAtMost(nums, goal - 1);
    }

    private static int countSubarraysWithSumAtMost(int[] nums, int maxSum) {
        if (maxSum < 0) {
            // Guards the goal = 0 case, where the second call is
            // countAtMost(-1). Since every element is non-negative, no
            // subarray can have a negative sum, so the count is 0.
            return 0;
        }

        int windowStart = 0;   // left edge of the current sliding window
        int windowSum = 0;     // sum of nums[windowStart..windowEnd]
        int totalSubarrays = 0;

        for (int windowEnd = 0; windowEnd < nums.length; windowEnd++) {
            windowSum += nums[windowEnd];

            // Shrink from the left while the window sum exceeds the cap.
            // Because all elements are 0 or 1 (non-negative), removing the
            // leftmost element strictly decreases (or keeps equal) the sum,
            // so this while-loop is guaranteed to terminate, and windowStart
            // only ever moves forward — this is what gives us the amortized
            // O(n) total bound across the whole outer loop.
            while (windowSum > maxSum) {
                windowSum -= nums[windowStart];
                windowStart++;
            }

            // Every subarray ending at windowEnd with a start position in
            // [windowStart, windowEnd] has sum <= maxSum. There are exactly
            // (windowEnd - windowStart + 1) such start positions.
            totalSubarrays += windowEnd - windowStart + 1;
        }

        return totalSubarrays;
    }

    /*
     * ============================================================================
     * 10. DRY RUN / TRACE
     * ============================================================================
     * Tracing numberOfSubarraysWithSumEqualToGoal([1,0,1,0,1], goal = 2):
     *
     * We need countAtMost(2) - countAtMost(1).
     *
     * --- countAtMost(2) trace ---
     * windowStart=0, windowSum=0, totalSubarrays=0
     * windowEnd=0: windowSum += nums[0]=1 -> windowSum=1 (<=2, no shrink)
     *              totalSubarrays += (0-0+1)=1 -> totalSubarrays=1
     * windowEnd=1: windowSum += nums[1]=0 -> windowSum=1
     *              totalSubarrays += (1-0+1)=2 -> totalSubarrays=3
     * windowEnd=2: windowSum += nums[2]=1 -> windowSum=2 (<=2, no shrink)
     *              totalSubarrays += (2-0+1)=3 -> totalSubarrays=6
     * windowEnd=3: windowSum += nums[3]=0 -> windowSum=2
     *              totalSubarrays += (3-0+1)=4 -> totalSubarrays=10
     * windowEnd=4: windowSum += nums[4]=1 -> windowSum=3 (>2, shrink!)
     *              shrink: windowSum -= nums[0]=1 -> windowSum=2, windowStart=1
     *              totalSubarrays += (4-1+1)=4 -> totalSubarrays=14
     * countAtMost(2) = 14
     *
     * --- countAtMost(1) trace ---
     * windowStart=0, windowSum=0, totalSubarrays=0
     * windowEnd=0: windowSum=1 (<=1) -> totalSubarrays += 1 -> 1
     * windowEnd=1: windowSum=1 (<=1) -> totalSubarrays += 2 -> 3
     * windowEnd=2: windowSum=2 (>1, shrink)
     *              windowSum -= nums[0]=1 -> windowSum=1, windowStart=1
     *              totalSubarrays += (2-1+1)=2 -> totalSubarrays=5
     * windowEnd=3: windowSum += nums[3]=0 -> windowSum=1 (<=1)
     *              totalSubarrays += (3-1+1)=3 -> totalSubarrays=8
     * windowEnd=4: windowSum += nums[4]=1 -> windowSum=2 (>1, shrink)
     *              windowSum -= nums[1]=0 -> windowSum=2, windowStart=2 (still >1, shrink again)
     *              windowSum -= nums[2]=1 -> windowSum=1, windowStart=3
     *              totalSubarrays += (4-3+1)=2 -> totalSubarrays=10
     * countAtMost(1) = 10
     *
     * Final answer: countAtMost(2) - countAtMost(1) = 14 - 10 = 4.
     * This matches the brute-force oracle result used in the stress test
     * below, confirming Example 1's expected answer of 4.
     * ============================================================================
     */

    /*
     * ============================================================================
     * 11. CLOSING SUMMARY
     * ============================================================================
     * - Brute force (O(n^2)) is only for validation; too slow at n = 3*10^4.
     * - Prefix Sum + HashMap (O(n)/O(n)) is the general-purpose technique
     *   that works regardless of the binary constraint, at the cost of
     *   hashing overhead and O(n) space.
     * - Prefix Sum + Bucket Array (O(n)/O(n)) is a direct-addressing
     *   refinement that's faster in practice but relies on knowing the
     *   value range is bounded by n.
     * - Sliding Window / atMost trick (O(n)/O(1)) is the recommended final
     *   answer: optimal on both time and space, built from a reusable
     *   "exactly = atMost(k) - atMost(k-1)" pattern.
     * - Zero-Grouping via ones' positions (O(n)/O(n) worst case) offers an
     *   alternative, more combinatorial lens with no subtraction trick, at
     *   the cost of more delicate case analysis.
     *
     * Assumptions baked into the final solution:
     *   - nums contains only 0s and 1s (required for the atMost() shrink
     *     loop's correctness and termination guarantee).
     *   - goal is within [0, nums.length] per the stated constraints; a
     *     defensive check rejects negative goal values explicitly.
     *
     * Known limitation: if the binary-array guarantee were relaxed to allow
     * negative numbers, Approach 4 would silently break (the monotonicity
     * argument fails), and we'd need to fall back to Approach 2/3's
     * prefix-sum + hashmap technique instead.
     * ============================================================================
     */

    /*
     * ============================================================================
     * 12. FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ============================================================================
     * 1. "What if `nums` could contain arbitrary integers, including
     *     negatives?" -> Sliding window breaks (sum isn't monotonic); fall
     *     back to prefix-sum + HashMap (Approach 2), still O(n)/O(n).
     * 2. "What if we need to answer this query for many different `goal`
     *     values against the same fixed `nums`?" -> Precompute prefix sums
     *     once, O(n), then answer each goal query in O(n) via the same
     *     atMost trick (or O(n) via hashmap frequency lookups), rather than
     *     re-scanning from scratch each time if we can share the prefix
     *     frequency table across queries.
     * 3. "What if `nums` is a live/streaming array (elements arrive one at a
     *     time) and we need the running count after each new element?" ->
     *     Maintain the prefix-sum frequency map (Approach 2/3) incrementally;
     *     the two-pointer version is harder to make "online" cleanly because
     *     it doesn't naturally decompose per new element without recomputing
     *     the second window on every append.
     * 4. "Can you do this with O(1) space AND without the atMost() double
     *     subtraction, i.e. directly in one pass?" -> Yes, a single-pass
     *     variant maintains two independent left pointers (one at the sum-
     *     equals-goal-or-less boundary, one at sum-strictly-less-than-goal
     *     boundary) simultaneously, updating a running difference — this
     *     the constant-factor-optimized version of Approach 4.
     * 5. "How would you extend this to a 2D binary matrix, counting
     *     submatrices summing to goal?" -> Fix a pair of row boundaries,
     *     collapse rows between them into a 1D column-sum array, and reuse
     *     this exact 1D routine — a classic "reduce 2D to repeated 1D" trick,
     *     giving O(rows^2 * cols) overall.
     * 6. "What's the largest `n` this needs to scale to, and does the
     *     O(n) solution still fit within memory/time constraints at that
     *     scale?" -> At n = 3*10^4 all approaches here are essentially
     *     instantaneous; even at n = 10^7-10^8 the O(n)/O(1) solution stays
     *     comfortably fast while the O(n)/O(n) variants would start to
     *     pressure memory.
     * ============================================================================
     */

    /*
     * ============================================================================
     * 13. WHAT CANDIDATES TYPICALLY MISS
     * ============================================================================
     * 1. Forgetting to guard atMost(goal - 1) when goal = 0: calling the
     *    shrink-window helper with maxSum = -1 without an explicit early
     *    return causes either an infinite loop or an incorrect count,
     *    since "sum > -1" is always true for non-negative sums and the
     *    while-loop would try to shrink past an empty window.
     * 2. Assuming the two-pointer `left` boundary can move backward: it
     *    must only ever advance forward across the entire outer loop for
     *    the amortized O(n) bound to hold; resetting `left` to 0 per
     *    outer iteration silently degrades this to O(n^2).
     * 3. Miscounting subarrays ending at the current right boundary: the
     *    contribution is (right - left + 1), not (right - left) — an
     *    off-by-one that undercounts every window by exactly one subarray
     *    (the single-element subarray [right..right] itself).
     * 4. In the zero-grouping approach, mishandling the very first and
     *    very last block of ones (no "previous one" before the first
     *    block, no "next one" after the last block) — candidates often
     *    forget to treat the array boundaries (-1 and n) as valid
     *    "virtual" neighbor positions, leading to missed or double-counted
     *    boundary subarrays.
     * ============================================================================
     */

    /*
     * ============================================================================
     * MAIN: Assertions + Randomized Stress Test (brute force as oracle)
     * ============================================================================
     */
    public static void main(String[] args) {
        // --- Named worked-example assertions ---
        assert bruteForce(new int[]{1, 0, 1, 0, 1}, 2) == 4
                : "Example 1 failed for bruteForce";
        assert prefixSumHashMap(new int[]{1, 0, 1, 0, 1}, 2) == 4
                : "Example 1 failed for prefixSumHashMap";
        assert prefixSumBucketArray(new int[]{1, 0, 1, 0, 1}, 2) == 4
                : "Example 1 failed for prefixSumBucketArray";
        assert slidingWindowAtMost(new int[]{1, 0, 1, 0, 1}, 2) == 4
                : "Example 1 failed for slidingWindowAtMost";
        assert zeroGroupingApproach(new int[]{1, 0, 1, 0, 1}, 2) == 4
                : "Example 1 failed for zeroGroupingApproach";
        assert numberOfSubarraysWithSumEqualToGoal(new int[]{1, 0, 1, 0, 1}, 2) == 4
                : "Example 1 failed for the production solution";

        // --- Example 2: all zeros, goal = 0 -> triangular number 15 ---
        int[] allZeros = {0, 0, 0, 0, 0};
        assert bruteForce(allZeros, 0) == 15 : "Example 2 failed for bruteForce";
        assert numberOfSubarraysWithSumEqualToGoal(allZeros, 0) == 15
                : "Example 2 failed for the production solution";

        // --- Example 3: boundary single-element cases ---
        assert numberOfSubarraysWithSumEqualToGoal(new int[]{0}, 0) == 1
                : "Example 3a failed";
        assert numberOfSubarraysWithSumEqualToGoal(new int[]{1}, 0) == 0
                : "Example 3b failed";

        System.out.println("All named assertions passed.");

        // --- Randomized stress test: all approaches vs brute-force oracle ---
        Random random = new Random(42); // fixed seed for reproducibility
        int trialCount = 2000;
        for (int trial = 0; trial < trialCount; trial++) {
            int length = 1 + random.nextInt(30); // keep arrays small so O(n^2) oracle is fast
            int[] randomNums = new int[length];
            for (int index = 0; index < length; index++) {
                randomNums[index] = random.nextInt(2); // 0 or 1
            }
            int goal = random.nextInt(length + 1); // 0..length inclusive

            int expected = bruteForce(randomNums, goal);
            int actualHashMap = prefixSumHashMap(randomNums, goal);
            int actualBucketArray = prefixSumBucketArray(randomNums, goal);
            int actualSlidingWindow = slidingWindowAtMost(randomNums, goal);
            int actualZeroGrouping = zeroGroupingApproach(randomNums, goal);
            int actualProduction = numberOfSubarraysWithSumEqualToGoal(randomNums, goal);

            if (expected != actualHashMap
                    || expected != actualBucketArray
                    || expected != actualSlidingWindow
                    || expected != actualZeroGrouping
                    || expected != actualProduction) {
                throw new AssertionError(String.format(
                        "Mismatch on trial %d: nums=%s goal=%d expected=%d "
                                + "hashMap=%d bucketArray=%d slidingWindow=%d "
                                + "zeroGrouping=%d production=%d",
                        trial, Arrays.toString(randomNums), goal, expected,
                        actualHashMap, actualBucketArray, actualSlidingWindow,
                        actualZeroGrouping, actualProduction));
            }
        }

        System.out.println(trialCount + " randomized stress test trials passed "
                + "across all five approaches.");
    }
}
