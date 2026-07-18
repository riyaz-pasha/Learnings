import java.util.Arrays;

/*
================================================================================
 SECTION 1: RESTATE THE PROBLEM
================================================================================
 We are given:
   - An integer array `nums`.
   - An integer `k`, the maximum total number of "increment by 1" operations
     we're allowed to spend.

 In one operation we pick ANY single index and add 1 to nums[that index].
 We can spend anywhere from 0 up to k operations total, distributed however
 we like across indices (all on one index, spread across many, etc.).

 GOAL: After spending at most k operations, maximize the frequency (count of
 occurrences) of whichever single value ends up most common in the array.

 Key observations that shape the solution:
   - We only ever INCREASE values, never decrease them. So to make several
     elements equal to some target value T, every element we choose to
     "convert" into T must already be <= T, and the cost to convert element
     x into T is exactly (T - x).
   - Because we only add, the cheapest way to create a large group of equal
     elements is to pick a target value and greedily raise the SMALLEST
     nearby elements up to it -- raising a large element up to a still-larger
     target wastes operations relative to raising several small ones close
     together.
   - This screams "sort first," because after sorting, the cheapest group of
     elements to unify into a single value is always a contiguous window in
     sorted order.

 Inputs:  int[] nums, int k
 Output:  int  -- the maximum achievable frequency of any single value.

 Constraints (typical LeetCode 1838 bounds, confirmed with interviewer below):
   1 <= nums.length <= 1e5
   1 <= nums[i] <= 1e5
   0 <= k <= 1e9 (sum of increments can be large, so intermediate sums need
                   a 64-bit type)
================================================================================
 SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
================================================================================
 1. Q: Can nums contain negative numbers or zero?
    A: Assume all values are positive (1 <= nums[i] <= 1e5), consistent with
       LeetCode's constraints. (If negatives were allowed, the algorithm is
       unaffected -- only relative differences matter.)

 2. Q: Can k be zero?
    A: Yes. If k = 0, the answer is simply the frequency of the most common
       existing value (no operations available).

 3. Q: Is nums guaranteed non-empty?
    A: Yes, length >= 1. A single-element array always has answer 1.

 4. Q: Are duplicate values in nums already common, and should they be
       handled specially?
    A: No special-casing needed -- the algorithm naturally accounts for
       existing duplicates (cost to "raise" an element already equal to the
       target is zero).

 5. Q: Do we need to return which element achieves the max frequency, or
       just the frequency value itself?
    A: Just the integer frequency value, per the problem statement.

 6. Q: Can operations only increment (never decrement)?
    A: Confirmed yes -- operations strictly add 1. This is critical: it
       means only sorted-ascending contiguous windows matter.

 7. Q: What's the expected time complexity given n up to 1e5?
    A: O(n log n) (dominated by sorting) is expected and sufficient;
       an accepted solution should avoid O(n^2).

 8. Q: Is the array mutable / can we sort it in place, or must the original
       order be preserved?
    A: We may sort a copy (or sort in place if the caller doesn't need the
       original order preserved) since we only need the frequency count,
       not index positions.

 9. Q: Do we need to worry about concurrent modification / thread safety?
    A: No, this is a single-threaded, single-pass computational problem.

10. Q: What data type should the running "cost" sum use, to avoid overflow?
    A: Use `long`. With n = 1e5 elements each needing up to ~1e5 increments,
       worst-case cumulative cost can reach ~1e10, which overflows a 32-bit
       int.
================================================================================
 SECTION 3: EXAMPLES & EDGE CASES
================================================================================
 Example A (Normal case):
   nums = [1, 2, 4], k = 5
   Sorted: [1, 2, 4]
   Try raising everything to 4: cost = (4-1)+(4-2)+(4-4) = 3+2+0 = 5 <= 5.
   => Frequency 3 is achievable. Answer = 3.

 Example B (Edge case -- k = 0, already-duplicated array):
   nums = [1, 1, 1, 1], k = 0
   No operations available, but the array already has 4 equal elements.
   => Answer = 4 (the algorithm must correctly return the "free" answer
      when no budget is spent).

 Example C (Boundary / tie-breaking case -- window must shrink):
   nums = [1, 4, 8, 13], k = 5
   Sorted: [1, 4, 8, 13]
   Try target = 13 starting from index 0: cost = 12+9+5+0 = 26 > 5 (too big).
   Shrink from the left (drop the 1): target = 13, elements {4,8,13}:
      cost = 9+5+0 = 14 > 5. Still too big.
   Shrink again: {8,13}: cost = 5+0 = 5 <= 5. Frequency 2 achievable.
   Could we do better with target = 8 instead? {4,8}: cost = 4 <= 5,
      frequency 2. {1,4,8}: cost = 7+4+0 = 11 > 5. No improvement.
   => Answer = 2. This demonstrates the classic "expand right, shrink left
      when cost exceeds k" sliding-window boundary behavior, and shows that
      the optimal window is not always the rightmost/largest values.
================================================================================
*/

class MaxFrequencyAfterKIncrements {

    /*
    ============================================================================
     SECTION 4 & 5: ALL POSSIBLE APPROACHES
     (Paradigm coverage note -- SECTION 6 requirement:
       - Brute force            -> Approach 1
       - Sorting-based          -> foundation of every approach below
       - Hashing-based          -> NOT applicable. Hashing helps count exact
                                    duplicates in O(1), but here we need to
                                    reason about ORDERED numeric differences
                                    (cost to raise x up to T), which hashing
                                    cannot capture.
       - Two pointer / sliding window -> Approach 3 (optimal)
       - Divide and conquer     -> NOT naturally applicable; there's no clean
                                    way to combine solutions from two
                                    independent halves of a sorted array,
                                    since the optimal window can straddle the
                                    midpoint.
       - Greedy                 -> embedded in Approaches 2-4: always raise
                                    the closest/smallest elements to the
                                    target first.
       - Dynamic Programming     -> NOT needed. There's no overlapping
                                    subproblem structure with branching
                                    choices -- the greedy "closest elements
                                    first" argument is provably optimal, so
                                    DP would only add overhead.
       - Tree / graph traversal  -> NOT applicable, no graph/tree structure
                                    in the input.
       - Heap / priority queue   -> NOT needed; sorting once up front already
                                    gives us global ordering, a heap would
                                    add log-factor overhead for no benefit.
       - Binary search           -> Approach 2 (binary search per target) and
                                    Approach 4 (binary search on the answer).
       - Monotonic stack/deque   -> NOT applicable; there's no "next greater/
                                    smaller element" relationship being
                                    queried here.
       - Trie / segment tree     -> NOT applicable; values aren't strings/
                                    prefixes, and we don't need range updates
                                    over a value-indexed structure.
    ============================================================================
    */

    /*
    ----------------------------------------------------------------------------
     Approach 1: Brute Force (Sort + Re-scan cost for every candidate target)
    ----------------------------------------------------------------------------
     Core idea:
       Sort the array. For every index i (candidate target value = nums[i]),
       walk backward from i, summing the cost to raise each earlier element
       up to nums[i], stopping as soon as the running cost exceeds k. Track
       the best (largest) window length seen. This recomputes the cost sum
       from scratch for every candidate target instead of reusing previous
       work.

     Data structure / paradigm: sorting + linear rescan (no reuse of state).

     Time Complexity:  O(n^2) worst case -- for each of n targets, we may
                        scan up to n elements backward.
     Space Complexity: O(n) for the sorted copy (O(log n) extra for the sort
                        itself, ignoring output).

     Pros:
       - Very easy to reason about and verify correctness against.
       - Good as a "correctness oracle" to test optimized approaches.
     Cons:
       - Too slow for n = 1e5 (10^10 operations, would TLE).
     When to use:
       - Only for small inputs, unit testing, or as a baseline for
         cross-validation against a faster approach.
    ----------------------------------------------------------------------------
    */
    static int maxFrequencyBruteForce(int[] nums, int k) {
        int[] sortedNums = nums.clone();
        Arrays.sort(sortedNums);
        int arrayLength = sortedNums.length;
        int bestFrequency = 1;

        for (int targetIndex = 0; targetIndex < arrayLength; targetIndex++) {
            int targetValue = sortedNums[targetIndex];
            long cumulativeCost = 0L;
            int windowSize = 0;

            // Walk backward from targetIndex, adding cost to raise each
            // earlier element up to targetValue.
            for (int scanIndex = targetIndex; scanIndex >= 0; scanIndex--) {
                cumulativeCost += (long) (targetValue - sortedNums[scanIndex]);
                if (cumulativeCost > k) {
                    break; // Budget exceeded; this window is too large.
                }
                windowSize++;
            }
            bestFrequency = Math.max(bestFrequency, windowSize);
        }
        return bestFrequency;
    }

    /*
    ----------------------------------------------------------------------------
     Approach 2: Sorting + Prefix Sums + Binary Search (per target)
    ----------------------------------------------------------------------------
     Core idea:
       Sort the array and build a prefix-sum array so that the sum of any
       contiguous range can be computed in O(1). For every index i (target
       value = sortedNums[i]), binary search for the smallest starting index
       `left` such that the cost to raise all elements in [left, i] up to
       sortedNums[i] is <= k. The cost of raising window [left, i] up to
       sortedNums[i] is:
           cost = sortedNums[i] * (i - left + 1) - (prefixSum[i] - prefixSum[left-1])
       Because cost is monotonically non-decreasing as `left` decreases
       (window grows), binary search over `left` is valid.

     Data structure / paradigm: prefix sums + binary search + greedy target
     selection.

     Time Complexity:  O(n log n) -- sorting is O(n log n); for each of the
                        n targets we binary search in O(log n).
     Space Complexity: O(n) for the sorted array and prefix sum array.

     Pros:
       - Demonstrates strong command of binary search + prefix sums.
       - Clear, provable correctness (monotonic cost function).
     Cons:
       - Slightly more code than the sliding window approach for the same
         asymptotic complexity -- the two-pointer approach achieves the same
         bound with less bookkeeping.
     When to use:
       - Good alternative to mention as a follow-up if the interviewer wants
         to see binary search explicitly, or if the window boundary weren't
         monotonic (it is here, but the technique generalizes to cases where
         two-pointer invariants are harder to argue).
    ----------------------------------------------------------------------------
    */
    static int maxFrequencyPrefixSumBinarySearch(int[] nums, int k) {
        int[] sortedNums = nums.clone();
        Arrays.sort(sortedNums);
        int arrayLength = sortedNums.length;

        // prefixSum[i] = sum of sortedNums[0..i-1]; prefixSum[0] = 0.
        long[] prefixSum = new long[arrayLength + 1];
        for (int index = 0; index < arrayLength; index++) {
            prefixSum[index + 1] = prefixSum[index] + sortedNums[index];
        }

        int bestFrequency = 1;

        for (int targetIndex = 0; targetIndex < arrayLength; targetIndex++) {
            long targetValue = sortedNums[targetIndex];

            // Binary search the smallest `left` (0 <= left <= targetIndex)
            // such that cost(left, targetIndex) <= k.
            int low = 0;
            int high = targetIndex;
            while (low < high) {
                int mid = low + (high - low) / 2;
                long windowLength = targetIndex - mid + 1;
                long rangeSum = prefixSum[targetIndex + 1] - prefixSum[mid];
                long cost = targetValue * windowLength - rangeSum;

                if (cost <= k) {
                    high = mid; // Feasible; try to extend further left.
                } else {
                    low = mid + 1; // Too expensive; shrink window.
                }
            }
            int windowSize = targetIndex - low + 1;
            bestFrequency = Math.max(bestFrequency, windowSize);
        }
        return bestFrequency;
    }

    /*
    ----------------------------------------------------------------------------
     Approach 3 (OPTIMAL / RECOMMENDED): Sorting + Two-Pointer Sliding Window
    ----------------------------------------------------------------------------
     Core idea:
       Sort the array. Maintain a window [left, right] and a running sum of
       the elements currently inside it. As `right` expands one step at a
       time, check whether the cost to raise every element in the window up
       to nums[right] exceeds k:
           cost = nums[right] * windowLength - windowSum
       If it does, shrink from the left (removing sortedNums[left] from the
       running sum and advancing left) until the window is affordable again.
       Because both pointers only ever move forward, total pointer movement
       is O(n).

     Data structure / paradigm: two-pointer / sliding window + greedy.

     Time Complexity:  O(n log n) -- dominated by the initial sort; the
                        sliding window itself is O(n) since left and right
                        each advance at most n times total.
     Space Complexity: O(n) for the sorted copy (O(1) extra beyond that --
                        no auxiliary arrays needed).

     Pros:
       - Same asymptotic complexity as Approach 2, but simpler to code and
         reason about live in an interview -- fewer moving parts, no binary
         search boilerplate.
       - Easy to explain the invariant: "the window is always the cheapest
         way to unify its rightmost element."
     Cons:
       - Slightly less obvious why shrinking-not-restarting is safe without
         first proving the monotonic cost argument (worth stating out loud
         in an interview).
     When to use:
       - This is the approach to present as your primary solution in a real
         interview: optimal complexity, minimal code, easy to trace.
    ----------------------------------------------------------------------------
    */
    static int maxFrequencySlidingWindow(int[] nums, int k) {
        int[] sortedNums = nums.clone();
        Arrays.sort(sortedNums);
        int arrayLength = sortedNums.length;

        int leftPointer = 0;
        long windowSum = 0L; // Sum of elements currently in [leftPointer, rightPointer]
        int bestFrequency = 1;

        for (int rightPointer = 0; rightPointer < arrayLength; rightPointer++) {
            windowSum += sortedNums[rightPointer];

            // Cost to raise every element in the window up to sortedNums[rightPointer].
            long windowLength = rightPointer - leftPointer + 1;
            long costToUnifyWindow = (long) sortedNums[rightPointer] * windowLength - windowSum;

            // Shrink from the left while the window is unaffordable.
            while (costToUnifyWindow > k) {
                windowSum -= sortedNums[leftPointer];
                leftPointer++;
                windowLength = rightPointer - leftPointer + 1;
                costToUnifyWindow = (long) sortedNums[rightPointer] * windowLength - windowSum;
            }

            bestFrequency = Math.max(bestFrequency, (int) windowLength);
        }
        return bestFrequency;
    }

    /*
    ----------------------------------------------------------------------------
     Approach 4: Binary Search on the ANSWER (frequency) + feasibility check
    ----------------------------------------------------------------------------
     Core idea:
       Instead of iterating targets, binary search directly on the candidate
       answer `frequency` (1..n). For a given candidate frequency F, check
       feasibility: does there exist some contiguous sorted window of length
       F whose unify-cost is <= k? Because feasibility is monotonic (if
       frequency F is achievable, so is any F' < F), binary search on F is
       valid. Feasibility for a fixed F can be checked in O(n) by sliding a
       fixed-size window across the sorted array using prefix sums.

     Data structure / paradigm: binary search on answer + prefix sums.

     Time Complexity:  O(n log n) -- O(n) feasibility check inside an
                        O(log n) binary search over frequency, plus the
                        initial O(n log n) sort.
     Space Complexity: O(n) for the sorted array and prefix sums.

     Pros:
       - Useful general pattern ("binary search on the answer") that
         transfers to many other problems -- good to mention for breadth.
     Cons:
       - Strictly more code and a slightly larger constant factor than
         Approach 3 for no asymptotic benefit here.
     When to use:
       - Mention as an alternative if asked "how else could you solve this?"
         but don't lead with it -- it's not simpler than the sliding window.
    ----------------------------------------------------------------------------
    */
    static int maxFrequencyBinarySearchOnAnswer(int[] nums, int k) {
        int[] sortedNums = nums.clone();
        Arrays.sort(sortedNums);
        int arrayLength = sortedNums.length;

        long[] prefixSum = new long[arrayLength + 1];
        for (int index = 0; index < arrayLength; index++) {
            prefixSum[index + 1] = prefixSum[index] + sortedNums[index];
        }

        int low = 1;
        int high = arrayLength;
        int bestFrequency = 1;

        while (low <= high) {
            int candidateFrequency = low + (high - low) / 2;
            if (isFrequencyFeasible(sortedNums, prefixSum, k, candidateFrequency)) {
                bestFrequency = candidateFrequency;
                low = candidateFrequency + 1; // Try for a larger frequency.
            } else {
                high = candidateFrequency - 1; // Too expensive; try smaller.
            }
        }
        return bestFrequency;
    }

    // Feasibility check: does any fixed-size window of length `frequency`
    // have unify-cost <= k? Slide the fixed-size window across the array.
    private static boolean isFrequencyFeasible(int[] sortedNums, long[] prefixSum, int k, int frequency) {
        int arrayLength = sortedNums.length;
        for (int windowEnd = frequency - 1; windowEnd < arrayLength; windowEnd++) {
            int windowStart = windowEnd - frequency + 1;
            long targetValue = sortedNums[windowEnd];
            long rangeSum = prefixSum[windowEnd + 1] - prefixSum[windowStart];
            long cost = targetValue * frequency - rangeSum;
            if (cost <= k) {
                return true;
            }
        }
        return false;
    }

    /*
    ============================================================================
     SECTION 7: APPROACHES COMPARISON TABLE

     Approach                              | Time         | Space | Best For                          | Limitations
     ---------------------------------------------------------------------------------------------------------------------------
     1. Brute Force (rescan per target)    | O(n^2)       | O(n)  | Baseline / correctness oracle      | TLE for n > ~10^4
     2. Prefix Sum + Binary Search/target  | O(n log n)   | O(n)  | Showing binary search fluency      | More code than needed
     3. Two-Pointer Sliding Window (BEST)  | O(n log n)   | O(n)  | Interview default; fastest to code | Requires the monotonic-
                                            |              |       |                                    | cost argument to justify
                                            |              |       |                                    | "shrink don't restart"
     4. Binary Search on the Answer        | O(n log n)   | O(n)  | Demonstrating a general technique  | No benefit over Approach 3
                                            |              |       |                                    | here; extra complexity
    ============================================================================
*/

    /*
    ============================================================================
     SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW

     Present Approach 3 (Sorting + Two-Pointer Sliding Window) as the primary
     solution. Reasoning:
       - It achieves the optimal O(n log n) time complexity (sorting is the
         unavoidable bottleneck; the scan itself is linear).
       - It requires the least code and the fewest opportunities for
         off-by-one bugs compared to the binary-search variants (Approaches
         2 and 4), which matters under interview time pressure.
       - The core invariant -- "the current window is always the minimal-
         cost way to unify its rightmost element" -- is easy to state aloud,
         which signals clear communication to the interviewer.
       - It's simple to extend: if the interviewer asks a follow-up (e.g.,
         "what if you could also decrement?"), the window logic is easy to
         adapt or discuss.

     Strategy for the live interview: start by explicitly stating the
     brute-force idea (Approach 1) to show you understand correctness first,
     then pivot to explain WHY sorting + a monotonic cost function unlocks
     the two-pointer optimization -- then code Approach 3.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 9: DEEP DIVE -- PRODUCTION-QUALITY OPTIMAL SOLUTION
    ============================================================================
    */

    /**
     * Computes the maximum achievable frequency of any single value in
     * {@code nums} after performing at most {@code k} increment operations,
     * where each operation adds 1 to exactly one element.
     *
     * <p>Algorithm: sort the array, then slide a two-pointer window over it.
     * For each right boundary, the cheapest way to make every element in the
     * window equal to {@code sortedNums[right]} is to raise every smaller
     * element in the window up to it (raising is the only allowed
     * operation, so nothing cheaper exists). If that cost exceeds the
     * budget {@code k}, the window shrinks from the left -- it never needs
     * to be rebuilt from scratch, because cost is monotonically
     * non-decreasing in window size for a fixed right boundary.</p>
     *
     * @param nums input array of positive integers; must be non-null and
     *             non-empty (per problem constraints, length >= 1)
     * @param k    maximum number of increment operations available; must be
     *             non-negative
     * @return the maximum frequency of any single value achievable within
     *         the operation budget
     * @throws IllegalArgumentException if nums is null/empty or k is negative
     */
    static int maxFrequency(int[] nums, int k) {
        // Defensive checks -- a real interview should mention these even if
        // the interviewer says "assume valid input," to show rigor.
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("nums must be non-null and non-empty");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k must be non-negative");
        }

        // Sort a copy so we don't mutate the caller's array unexpectedly.
        final int[] sortedNums = nums.clone();
        Arrays.sort(sortedNums);
        final int arrayLength = sortedNums.length;

        int leftPointer = 0;          // Inclusive left boundary of the window.
        long windowSum = 0L;          // Running sum of elements in the window; long to avoid overflow.
        int bestFrequencySoFar = 1;   // Any single element trivially has frequency >= 1.

        for (int rightPointer = 0; rightPointer < arrayLength; rightPointer++) {
            windowSum += sortedNums[rightPointer];

            // Cost to raise every element currently in the window up to the
            // new rightmost value: (targetValue * count) - (sum of elements).
            long currentWindowLength = rightPointer - leftPointer + 1L;
            long costToUnifyWindow = (long) sortedNums[rightPointer] * currentWindowLength - windowSum;

            // If unifying the window costs more than our budget, shrink from
            // the left. Each element removed can only ever be removed once,
            // so this inner loop runs O(n) times total across the whole
            // outer loop, not O(n) per iteration -- keeping the algorithm
            // O(n) overall (after the O(n log n) sort).
            while (costToUnifyWindow > k) {
                windowSum -= sortedNums[leftPointer];
                leftPointer++;
                currentWindowLength = rightPointer - leftPointer + 1L;
                costToUnifyWindow = (long) sortedNums[rightPointer] * currentWindowLength - windowSum;
            }

            bestFrequencySoFar = Math.max(bestFrequencySoFar, (int) currentWindowLength);
        }

        return bestFrequencySoFar;
    }

    /*
    ============================================================================
     SECTION 10: DRY RUN / TRACE

     Using Example C: nums = [1, 4, 8, 13], k = 5
     After sorting: sortedNums = [1, 4, 8, 13]  (already sorted)

     Initial state: leftPointer = 0, windowSum = 0, bestFrequencySoFar = 1

     rightPointer = 0 (value = 1):
       windowSum = 0 + 1 = 1
       currentWindowLength = 0 - 0 + 1 = 1
       costToUnifyWindow = 1 * 1 - 1 = 0        (0 <= k=5, no shrink needed)
       bestFrequencySoFar = max(1, 1) = 1

     rightPointer = 1 (value = 4):
       windowSum = 1 + 4 = 5
       currentWindowLength = 1 - 0 + 1 = 2
       costToUnifyWindow = 4 * 2 - 5 = 3         (3 <= k=5, no shrink needed)
       bestFrequencySoFar = max(1, 2) = 2

     rightPointer = 2 (value = 8):
       windowSum = 5 + 8 = 13
       currentWindowLength = 2 - 0 + 1 = 3
       costToUnifyWindow = 8 * 3 - 13 = 11       (11 > k=5 -> SHRINK)
         Shrink step: remove sortedNums[0]=1 -> windowSum = 13 - 1 = 12,
                      leftPointer becomes 1
         currentWindowLength = 2 - 1 + 1 = 2
         costToUnifyWindow = 8 * 2 - 12 = 4       (4 <= k=5, stop shrinking)
       bestFrequencySoFar = max(2, 2) = 2

     rightPointer = 3 (value = 13):
       windowSum = 12 + 13 = 25
       currentWindowLength = 3 - 1 + 1 = 3
       costToUnifyWindow = 13 * 3 - 25 = 14      (14 > k=5 -> SHRINK)
         Shrink step: remove sortedNums[1]=4 -> windowSum = 25 - 4 = 21,
                      leftPointer becomes 2
         currentWindowLength = 3 - 2 + 1 = 2
         costToUnifyWindow = 13 * 2 - 21 = 5      (5 <= k=5, stop shrinking)
       bestFrequencySoFar = max(2, 2) = 2

     Final answer: bestFrequencySoFar = 2   (matches Example C's expected result)
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 11: CLOSING SUMMARY

     - All approaches rely on the same foundational insight: sorting turns
       "which elements should we group together" into "which contiguous
       window should we pick," because we can only increase values.
     - Approach 1 (brute force) is O(n^2) and useful only as a correctness
       baseline.
     - Approaches 2 and 4 both use binary search (on the target index, and
       on the answer frequency, respectively) and achieve optimal O(n log n)
       time, but carry more implementation complexity than necessary.
     - Approach 3 (two-pointer sliding window) is the recommended solution:
       same optimal time complexity, least code, clearest invariant.
     - Known assumptions/limitations of the final solution:
         * Assumes all values fit comfortably in `int` and that cumulative
           sums fit in `long` (safe given typical constraints of nums[i],
           k <= ~1e9 and n <= 1e5).
         * Assumes operations can only increment (if decrements were also
           allowed, the problem changes substantially -- see follow-ups).
         * Sorts a cloned array to avoid mutating caller state; this costs
           O(n) extra space, which is an acceptable and standard trade-off.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK

     1. "What if operations could also decrement a value?" -- This changes
        the problem significantly: now any target value (not just an
        existing array value) could be optimal, and cost involves absolute
        differences in both directions -- worth discussing median-based
        greedy strategies.
     2. "What if each operation had a different cost depending on the
        index?" -- Would likely need weighted prefix sums or a different
        greedy ordering (cheapest cost-per-unit first).
     3. "Can you solve this in O(n) instead of O(n log n)?" -- Only if the
        input is guaranteed to arrive pre-sorted, or if we can use a
        counting-sort-style bucket approach when the value range is small
        and bounded.
     4. "How would this change if nums were streaming and we needed the
        answer after each new element arrives?" -- Would require an
        incremental/online data structure (e.g., a balanced BST or Fenwick
        tree keyed by value) to maintain prefix sums under insertion.
     5. "What if k could be different for different elements (a per-element
        budget instead of one global budget)?" -- The greedy monotonic
        window argument breaks down; likely needs a different DP or
        flow-based formulation.
     6. "How would you parallelize this for very large n?" -- The sort can
        be parallelized (e.g., parallel merge sort); the sliding window
        itself is inherently sequential due to the shared running sum, so
        a parallel prefix-sum + independent binary search per shard
        (Approach 2's technique) might parallelize more naturally than the
        two-pointer version.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 13: WHAT CANDIDATES TYPICALLY MISS

     1. Forgetting to sort first -- without sorting, there is no way to
        argue that the optimal group of elements to unify is contiguous,
        and candidates often try to greedily pick "closest values" without
        realizing sorting makes this trivial.
     2. Integer overflow -- with n and value ranges up to 1e5, the running
        cost/sum can approach ~1e10, which silently overflows a 32-bit int
        and produces wrong answers on large test cases without any crash.
     3. Restarting the window from scratch on every shrink instead of
        maintaining a running sum -- this silently degrades the algorithm
        back to O(n^2) even though the code "looks like" a sliding window.
     4. Off-by-one errors in window length (using `right - left` instead of
        `right - left + 1`), which causes systematically undercounting the
        frequency by one, especially easy to miss since single-element
        windows (length 1) can mask the bug in small test cases.
    ============================================================================
    */

    /*
    ============================================================================
     TEST HARNESS -- cross-validates all four approaches against expected
     results for every example discussed above, plus a few extra edge cases.
    ============================================================================
    */
    public static void main(String[] args) {
        runTestCase("Example A: normal case", new int[]{1, 2, 4}, 5, 3);
        runTestCase("Example B: k = 0, already duplicated", new int[]{1, 1, 1, 1}, 0, 4);
        runTestCase("Example C: boundary / window shrink", new int[]{1, 4, 8, 13}, 5, 2);
        runTestCase("Problem statement example", new int[]{2, 2, 3}, 4, 3);
        runTestCase("Single element array", new int[]{7}, 0, 1);
        runTestCase("Large k, unify everything", new int[]{1, 2, 3, 4, 5}, 100, 5);

        System.out.println("\nAll approaches agree on all test cases. \u2705");
    }

    private static void runTestCase(String description, int[] nums, int k, int expectedFrequency) {
        int bruteForceResult = maxFrequencyBruteForce(nums, k);
        int prefixBinarySearchResult = maxFrequencyPrefixSumBinarySearch(nums, k);
        int slidingWindowResult = maxFrequencySlidingWindow(nums, k);
        int binarySearchOnAnswerResult = maxFrequencyBinarySearchOnAnswer(nums, k);
        int productionResult = maxFrequency(nums, k);

        boolean allMatchExpected =
                bruteForceResult == expectedFrequency
                        && prefixBinarySearchResult == expectedFrequency
                        && slidingWindowResult == expectedFrequency
                        && binarySearchOnAnswerResult == expectedFrequency
                        && productionResult == expectedFrequency;

        System.out.printf(
                "%-42s nums=%-20s k=%-4d expected=%d | bruteForce=%d prefixBinSearch=%d slidingWindow=%d binSearchOnAnswer=%d production=%d %s%n",
                description,
                Arrays.toString(nums),
                k,
                expectedFrequency,
                bruteForceResult,
                prefixBinarySearchResult,
                slidingWindowResult,
                binarySearchOnAnswerResult,
                productionResult,
                allMatchExpected ? "[PASS]" : "[FAIL]"
        );

        if (!allMatchExpected) {
            throw new AssertionError("Mismatch detected for test case: " + description);
        }
    }


    class MaximumFrequency {

      /**
       * Returns the maximum possible frequency after at most k increments.
       *
       * Time Complexity:
       * Sorting : O(n log n)
       * Sliding Window : O(n)
       *
       * Overall : O(n log n)
       *
       * Space Complexity:
       * O(1) (Ignoring sorting implementation)
       */
      public int maxFrequency(int[] nums, int k) {

        // Step 1: Sort so that every window's rightmost element
        // becomes the target value.
        Arrays.sort(nums);

        int left = 0;
        long windowSum = 0; // long prevents overflow
        int maxFrequency = 1;

        for (int right = 0; right < nums.length; right++) {

          // Include current number in the window.
          windowSum += nums[right];

          /*
           * Cost to make every element inside the current window
           * equal to nums[right].
           *
           * Required Total = nums[right] * windowSize
           * Current Total = windowSum
           * Operations Needed = difference
           */
          while ((long) nums[right] * (right - left + 1) - windowSum > k) {

            // Remove leftmost element until the window becomes valid.
            windowSum -= nums[left];
            left++;
          }

          // Current window is valid.
          maxFrequency = Math.max(maxFrequency, right - left + 1);
        }

        return maxFrequency;
      }
    }

}
